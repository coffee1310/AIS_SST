using Diplom_Stud.Components;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class EventParticipantsPage : Page
    {
        private int _eventId;
        private static readonly HttpClient _httpClient = new HttpClient();

        public EventParticipantsPage(int eventId)
        {
            InitializeComponent();
            _eventId = eventId;

            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            }
        }

        private async void Page_Loaded(object sender, RoutedEventArgs e)
        {
            if (_eventId <= 0) return;
            await LoadParticipantsAsync();
        }

        private async Task LoadParticipantsAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyParticipantsText.Visibility = Visibility.Collapsed;
            RolesWithParticipantsList.ItemsSource = null;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync($"/api/role-applications?eventId={_eventId}&status=ОДОБРЕНА&page=0&size=100");

                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    var pageData = JsonSerializer.Deserialize<PartPageResponseLocal>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    if (pageData?.content != null && pageData.content.Count > 0)
                    {
                        var groupedApps = pageData.content.GroupBy(a => string.IsNullOrEmpty(a.eventRoleName) ? "Роль не указана" : a.eventRoleName);
                        var roleBlocks = new List<ParticipantRoleGroupViewModel>();

                        foreach (var group in groupedApps)
                        {
                            var participants = group.Select(a =>
                            {
                                BitmapImage avatar = new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"));
                                if (!string.IsNullOrEmpty(a.studentPhoto))
                                {
                                    var bmp = GetImageFromBase64(a.studentPhoto);
                                    if (bmp != null) avatar = bmp;
                                }

                                return new EventParticipantItemViewModel
                                {
                                    ParticipantId = a.id,
                                    StudentId = a.studentId ?? 0,
                                    FullName = $"{a.studentSurname} {a.studentName} {a.studentPatronymic}".Trim(),
                                    StudentEmail = string.IsNullOrWhiteSpace(a.studentEmail) ? "Почта не указана" : a.studentEmail,
                                    IsReserve = a.isReserve,
                                    Avatar = avatar
                                };
                            })
                            .OrderBy(p => p.IsReserve)
                            .ToList();

                            roleBlocks.Add(new ParticipantRoleGroupViewModel
                            {
                                RoleTitle = group.Key,
                                Participants = participants
                            });
                        }

                        RolesWithParticipantsList.ItemsSource = roleBlocks;
                    }
                    else
                    {
                        EmptyParticipantsText.Visibility = Visibility.Visible;
                    }
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка загрузки участников: {response.StatusCode}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сбой сети: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private void ParticipantCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is FrameworkElement element && element.Tag is int studentId && studentId != 0)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Activist.Profile(studentId));
            }
        }

        private async void KickParticipant_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int applicationId)
            {
                MessageBoxResult result = MessageBox.Show("Вы уверены, что хотите исключить этого участника с мероприятия?", "Исключение", MessageBoxButton.YesNo, MessageBoxImage.Warning);

                if (result == MessageBoxResult.Yes)
                {
                    LoadingOverlay.Visibility = Visibility.Visible;
                    try
                    {
                        _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                        var content = new StringContent("", Encoding.UTF8, "application/json");
                        HttpResponseMessage response = await _httpClient.PutAsync($"/api/role-applications/{applicationId}/reject", content);

                        if (response.IsSuccessStatusCode)
                        {
                            CustomMessageBox.Show("Участник успешно исключен.", "Успех", CustomMessageBox.MessageType.Success);
                            await LoadParticipantsAsync();
                        }
                        else
                        {
                            string err = await response.Content.ReadAsStringAsync();
                            CustomMessageBox.Show($"Ошибка выполнения действия:\n{err}", "Ошибка", CustomMessageBox.MessageType.Error);
                        }
                    }
                    catch (Exception ex)
                    {
                        CustomMessageBox.Show($"Сбой сети: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
                    }
                    finally
                    {
                        LoadingOverlay.Visibility = Visibility.Collapsed;
                    }
                }
            }
        }

        private BitmapImage GetImageFromBase64(string base64String)
        {
            try
            {
                string cleanStr = base64String.Trim().Replace("\r", "").Replace("\n", "");
                byte[] imageBytes = null;
                try
                {
                    byte[] decodedFirstLevel = Convert.FromBase64String(cleanStr);
                    string textInside = Encoding.UTF8.GetString(decodedFirstLevel);
                    if (textInside.StartsWith("data:image"))
                    {
                        int commaIndex = textInside.IndexOf(',');
                        if (commaIndex >= 0) imageBytes = Convert.FromBase64String(textInside.Substring(commaIndex + 1));
                    }
                    else { imageBytes = decodedFirstLevel; }
                }
                catch
                {
                    int commaIndex = cleanStr.IndexOf(',');
                    if (commaIndex >= 0) cleanStr = cleanStr.Substring(commaIndex + 1);
                    imageBytes = Convert.FromBase64String(cleanStr);
                }

                if (imageBytes != null)
                {
                    using (var ms = new MemoryStream(imageBytes))
                    {
                        var bitmap = new BitmapImage();
                        bitmap.BeginInit();
                        bitmap.CacheOption = BitmapCacheOption.OnLoad;
                        bitmap.StreamSource = ms;
                        bitmap.EndInit();
                        bitmap.Freeze();
                        return bitmap;
                    }
                }
            }
            catch { }
            return null;
        }
    }

    public class PartPageResponseLocal
    {
        public List<PartAppDtoLocal> content { get; set; }
        public int totalPages { get; set; }
    }

    public class PartAppDtoLocal
    {
        public int id { get; set; }
        public int? studentId { get; set; }
        public string studentName { get; set; }
        public string studentSurname { get; set; }
        public string studentPatronymic { get; set; }
        public string studentEmail { get; set; }
        public string studentPhoto { get; set; }
        public string eventRoleName { get; set; }
        public bool isReserve { get; set; }
    }

    public class ParticipantRoleGroupViewModel : INotifyPropertyChanged
    {
        public string RoleTitle { get; set; }
        private bool _isExpanded = true;
        public bool IsExpanded
        {
            get => _isExpanded;
            set { _isExpanded = value; OnPropertyChanged(nameof(IsExpanded)); }
        }

        public List<EventParticipantItemViewModel> Participants { get; set; }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string name) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class EventParticipantItemViewModel
    {
        public int ParticipantId { get; set; }
        public int StudentId { get; set; }
        public string FullName { get; set; }
        public string StudentEmail { get; set; }
        public bool IsReserve { get; set; }

        public string StatusLabel => IsReserve ? "Резерв" : "Основной состав";
        public Brush BackgroundBrush => IsReserve ? new SolidColorBrush(Color.FromArgb(20, 255, 165, 0)) : Brushes.Transparent;

        public ImageSource Avatar { get; set; }
    }
}