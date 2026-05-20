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
    public partial class EventRequestsPage : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private int _eventId;

        private int _currentPage = 0;
        private int _pageSize = 10;
        private int _totalPages = 1;

        public EventRequestsPage(int eventId)
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
            await LoadRequestsAsync();
        }

        private async Task LoadRequestsAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyAllText.Visibility = Visibility.Collapsed;
            PaginationPanel.Visibility = Visibility.Collapsed;
            RolesWithRequestsList.ItemsSource = null;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync($"/api/role-applications?eventId={_eventId}&status=НА_РАССМОТРЕНИИ&page={_currentPage}&size={_pageSize}");

                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    var pageData = JsonSerializer.Deserialize<ReqPageResponseLocal>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    if (pageData?.content != null && pageData.content.Count > 0)
                    {
                        _totalPages = pageData.totalPages > 0 ? pageData.totalPages : 1;
                        TxtPageInfo.Text = $"Страница {_currentPage + 1} из {_totalPages}";
                        BtnPrevPage.IsEnabled = _currentPage > 0;
                        BtnNextPage.IsEnabled = _currentPage < _totalPages - 1;
                        PaginationPanel.Visibility = Visibility.Visible;

                        var groupedApps = pageData.content.GroupBy(a => string.IsNullOrEmpty(a.eventRoleName) ? "Роль не указана" : a.eventRoleName);
                        var roleBlocks = new List<RequestRoleBlockViewModel>();

                        foreach (var group in groupedApps)
                        {
                            var apps = group.Select(a =>
                            {
                                BitmapImage avatar = new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"));
                                if (!string.IsNullOrEmpty(a.studentPhoto))
                                {
                                    var bmp = GetImageFromBase64(a.studentPhoto);
                                    if (bmp != null) avatar = bmp;
                                }

                                return new RequestAppViewModel
                                {
                                    ApplicationId = a.id,
                                    StudentId = a.studentId ?? 0,
                                    FullName = $"{a.studentSurname} {a.studentName} {a.studentPatronymic}".Trim(),
                                    StudentEmail = string.IsNullOrWhiteSpace(a.studentEmail) ? "Почта не указана" : a.studentEmail,
                                    Comment = string.IsNullOrWhiteSpace(a.description) ? "Комментарий не оставлен" : a.description,
                                    Avatar = avatar
                                };
                            }).ToList();

                            roleBlocks.Add(new RequestRoleBlockViewModel
                            {
                                RoleTitle = group.Key,
                                Applications = apps
                            });
                        }

                        RolesWithRequestsList.ItemsSource = roleBlocks;
                    }
                    else
                    {
                        EmptyAllText.Visibility = Visibility.Visible;
                    }
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка загрузки заявок: {response.StatusCode}", "Ошибка", CustomMessageBox.MessageType.Error);
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

        private async void PrevPage_Click(object sender, RoutedEventArgs e)
        {
            if (_currentPage > 0) { _currentPage--; await LoadRequestsAsync(); }
        }

        private async void NextPage_Click(object sender, RoutedEventArgs e)
        {
            if (_currentPage < _totalPages - 1) { _currentPage++; await LoadRequestsAsync(); }
        }

        private async void ApproveRequest_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int appId)
            {
                await HandleApplicationActionAsync(appId, "approve", "Заявка успешно принята!");
            }
        }

        private async void RejectRequest_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int appId)
            {
                await HandleApplicationActionAsync(appId, "reject", "Заявка отклонена.");
            }
        }

        private void ReserveRequest_Click(object sender, RoutedEventArgs e)
        {
            CustomMessageBox.Show("Функция отправки в резерв пока находится в разработке.", "В разработке", CustomMessageBox.MessageType.Warning);
        }

        private void RequestCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is FrameworkElement element && element.Tag is int studentId && studentId != 0)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Activist.Profile(studentId));
            }
        }

        private async Task HandleApplicationActionAsync(int applicationId, string action, string successMessage)
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                var content = new StringContent("", Encoding.UTF8, "application/json");

                HttpResponseMessage response = await _httpClient.PutAsync($"/api/role-applications/{applicationId}/{action}", content);

                if (response.IsSuccessStatusCode)
                {
                    CustomMessageBox.Show(successMessage, "Успех", CustomMessageBox.MessageType.Success);

                    if (_currentPage > 0 && RolesWithRequestsList.Items.Count <= 1)
                        _currentPage--;

                    await LoadRequestsAsync();
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

    public class ReqPageResponseLocal
    {
        public List<ReqAppDtoLocal> content { get; set; }
        public int totalPages { get; set; }
    }

    public class ReqAppDtoLocal
    {
        public int id { get; set; }
        public int? studentId { get; set; }
        public string studentName { get; set; }
        public string studentSurname { get; set; }
        public string studentPatronymic { get; set; }
        public string studentEmail { get; set; }
        public string studentPhoto { get; set; }
        public int eventRoleId { get; set; }
        public string eventRoleName { get; set; }
        public int eventId { get; set; }
        public string eventTitle { get; set; }
        public string description { get; set; }
        public string status { get; set; }
        public bool isReserve { get; set; }
    }

    public class RequestRoleBlockViewModel : INotifyPropertyChanged
    {
        public string RoleTitle { get; set; }
        private bool _isExpanded = true;
        public bool IsExpanded
        {
            get => _isExpanded;
            set { _isExpanded = value; OnPropertyChanged(nameof(IsExpanded)); }
        }
        public List<RequestAppViewModel> Applications { get; set; }
        public Visibility EmptyTextVisibility => (Applications == null || Applications.Count == 0) ? Visibility.Visible : Visibility.Collapsed;

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string name) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class RequestAppViewModel
    {
        public int ApplicationId { get; set; }
        public int StudentId { get; set; }
        public string FullName { get; set; }
        public string StudentEmail { get; set; }
        public string Comment { get; set; }
        public Visibility CommentVisibility => string.IsNullOrWhiteSpace(Comment) ? Visibility.Collapsed : Visibility.Visible;
        public ImageSource Avatar { get; set; }
    }
}