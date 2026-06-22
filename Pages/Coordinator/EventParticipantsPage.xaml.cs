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

                var blocks = new List<Part_RoleGroupViewModel>();
                var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                HttpResponseMessage resPart = await _httpClient.GetAsync($"/api/events/participants/{_eventId}");
                if (resPart.IsSuccessStatusCode)
                {
                    string json = await resPart.Content.ReadAsStringAsync();
                    var list = JsonSerializer.Deserialize<List<Part_ParticipantUserDto>>(json, options);
                    if (list != null && list.Count > 0)
                    {
                        var block = new Part_RoleGroupViewModel { RoleTitle = "Участник", Participants = new List<Part_ItemViewModel>() };
                        foreach (var u in list)
                        {
                            block.Participants.Add(new Part_ItemViewModel
                            {
                                ParticipantId = u.id,
                                StudentId = u.id,
                                FullName = $"{u.surname} {u.name} {u.patronymic}".Trim(),
                                StudentEmail = string.IsNullOrWhiteSpace(u.studentEmail) ? "Почта не указана" : u.studentEmail,
                                Type = "Participant",
                                IsReserve = false,
                                Avatar = GetImageFromBase64(u.photo) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                            });
                        }
                        blocks.Add(block);
                    }
                }

                HttpResponseMessage resEv = await _httpClient.GetAsync($"/api/events/{_eventId}");
                if (resEv.IsSuccessStatusCode)
                {
                    string json = await resEv.Content.ReadAsStringAsync();
                    var ev = JsonSerializer.Deserialize<Part_EditEventDto>(json, options);
                    if (ev?.organizers != null && ev.organizers.Count > 0)
                    {
                        var block = new Part_RoleGroupViewModel { RoleTitle = "Организатор", Participants = new List<Part_ItemViewModel>() };
                        foreach (var u in ev.organizers)
                        {
                            block.Participants.Add(new Part_ItemViewModel
                            {
                                ParticipantId = u.id,
                                StudentId = u.id,
                                FullName = $"{u.surname} {u.name} {u.patronymic}".Trim(),
                                StudentEmail = string.IsNullOrWhiteSpace(u.studentEmail) ? "Почта не указана" : u.studentEmail,
                                Type = "Organizer",
                                IsReserve = false,
                                Avatar = GetImageFromBase64(u.photo) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                            });
                        }
                        blocks.Add(block);
                    }
                }

                HttpResponseMessage resRoles = await _httpClient.GetAsync($"/api/role-applications?status=ОДОБРЕНА&eventId={_eventId}");
                if (resRoles.IsSuccessStatusCode)
                {
                    string json = await resRoles.Content.ReadAsStringAsync();
                    var page = JsonSerializer.Deserialize<Part_PageResponse>(json, options);
                    if (page?.content != null && page.content.Count > 0)
                    {
                        var grouped = page.content.GroupBy(c => string.IsNullOrEmpty(c.eventRoleName) ? "Роль не указана" : c.eventRoleName);
                        foreach (var g in grouped)
                        {
                            var block = new Part_RoleGroupViewModel { RoleTitle = g.Key, Participants = new List<Part_ItemViewModel>() };
                            foreach (var app in g)
                            {
                                block.Participants.Add(new Part_ItemViewModel
                                {
                                    ParticipantId = app.id,
                                    StudentId = app.studentId ?? 0,
                                    FullName = $"{app.studentSurname} {app.studentName} {app.studentPatronymic}".Trim(),
                                    StudentEmail = string.IsNullOrWhiteSpace(app.studentEmail) ? "Почта не указана" : app.studentEmail,
                                    Type = "Role",
                                    IsReserve = app.isReserve,
                                    Avatar = GetImageFromBase64(app.studentPhoto) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                                });
                            }
                            blocks.Add(block);
                        }
                    }
                }

                RolesWithParticipantsList.ItemsSource = blocks;
                if (blocks.Count == 0) EmptyParticipantsText.Visibility = Visibility.Visible;
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка загрузки участников: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
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
            if (sender is Button btn && btn.DataContext is Part_ItemViewModel p)
            {
                MessageBoxResult result = MessageBox.Show($"Вы уверены, что хотите исключить {p.FullName}?", "Исключение", MessageBoxButton.YesNo, MessageBoxImage.Warning);

                if (result == MessageBoxResult.Yes)
                {
                    LoadingOverlay.Visibility = Visibility.Visible;
                    try
                    {
                        _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                        HttpResponseMessage response = null;

                        if (p.Type == "Participant")
                        {
                            response = await _httpClient.DeleteAsync($"/api/events/participants/{p.ParticipantId}/soft");
                        }
                        else if (p.Type == "Organizer")
                        {
                            response = await _httpClient.DeleteAsync($"/api/events/organizers/{p.ParticipantId}/soft");
                        }
                        else if (p.Type == "Role")
                        {
                            response = await _httpClient.DeleteAsync($"/api/events/participants/participation-records/{p.ParticipantId}");
                        }

                        if (response != null && response.IsSuccessStatusCode)
                        {
                            CustomMessageBox.Show("Участник успешно исключен.", "Успех", CustomMessageBox.MessageType.Success);
                            await LoadParticipantsAsync();
                        }
                        else
                        {
                            string err = response != null ? await response.Content.ReadAsStringAsync() : "Неизвестная ошибка";
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
                if (string.IsNullOrEmpty(base64String)) return null;
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

    public class Part_ParticipantUserDto
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public string studentEmail { get; set; }
        public string photo { get; set; }
    }

    public class Part_EditEventDto
    {
        public List<Part_ParticipantUserDto> organizers { get; set; }
    }

    public class Part_PageResponse
    {
        public List<Part_RoleAppDto> content { get; set; }
    }

    public class Part_RoleAppDto
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

    public class Part_RoleGroupViewModel : INotifyPropertyChanged
    {
        public string RoleTitle { get; set; }
        private bool _isExpanded = true;
        public bool IsExpanded
        {
            get => _isExpanded;
            set { _isExpanded = value; OnPropertyChanged(nameof(IsExpanded)); }
        }

        public List<Part_ItemViewModel> Participants { get; set; }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string name) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class Part_ItemViewModel
    {
        public int ParticipantId { get; set; }
        public int StudentId { get; set; }
        public string FullName { get; set; }
        public string StudentEmail { get; set; }
        public bool IsReserve { get; set; }
        public string Type { get; set; }
        public ImageSource Avatar { get; set; }

        public string StatusLabel => IsReserve ? "Резерв" : "Основной состав";
        public Brush BackgroundBrush => IsReserve ? new SolidColorBrush(Color.FromArgb(20, 255, 165, 0)) : Brushes.Transparent;
    }
}