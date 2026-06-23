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

        private int _rejectingApplicationId = 0;
        private bool _isRejectingOrganizer = false;

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

        private List<T> ParsePageOrList<T>(string json, JsonSerializerOptions options)
        {
            if (string.IsNullOrWhiteSpace(json)) return new List<T>();
            string trimmed = json.TrimStart();

            if (trimmed.StartsWith("["))
            {
                return JsonSerializer.Deserialize<List<T>>(json, options) ?? new List<T>();
            }
            else if (trimmed.StartsWith("{"))
            {
                try
                {
                    using (var doc = JsonDocument.Parse(json))
                    {
                        if (doc.RootElement.TryGetProperty("content", out var contentElem))
                        {
                            return JsonSerializer.Deserialize<List<T>>(contentElem.GetRawText(), options) ?? new List<T>();
                        }
                    }
                }
                catch { }
            }
            return new List<T>();
        }

        private async Task<List<Req_GlobalUserDto>> FetchAllUsersAsync(JsonSerializerOptions options)
        {
            try
            {
                var res = await _httpClient.GetAsync("/api/users/all?page=0&size=1000&sortBy=id&sortDirection=ASC");
                if (res.IsSuccessStatusCode)
                {
                    var page = JsonSerializer.Deserialize<Req_GlobalUserPage>(await res.Content.ReadAsStringAsync(), options);
                    return page?.content ?? new List<Req_GlobalUserDto>();
                }
            }
            catch { }
            return new List<Req_GlobalUserDto>();
        }

        private async Task LoadRequestsAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyAllText.Visibility = Visibility.Collapsed;
            RolesWithRequestsList.ItemsSource = null;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                var blocks = new List<Req_RoleBlockViewModel>();
                var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                var allUsers = await FetchAllUsersAsync(options);

                HttpResponseMessage resRoles = await _httpClient.GetAsync($"/api/role-applications?status=НА_РАССМОТРЕНИИ&eventId={_eventId}");
                if (resRoles.IsSuccessStatusCode)
                {
                    string json = await resRoles.Content.ReadAsStringAsync();
                    var list = ParsePageOrList<Req_RoleAppDto>(json, options);

                    if (list.Count > 0)
                    {
                        var grouped = list.GroupBy(c => string.IsNullOrEmpty(c.eventRoleName) ? "Роль не указана" : c.eventRoleName);
                        foreach (var g in grouped)
                        {
                            var block = new Req_RoleBlockViewModel { RoleTitle = g.Key, Applications = new List<Req_AppViewModel>() };
                            foreach (var app in g)
                            {
                                var matchedUser = allUsers.FirstOrDefault(u => u.studentEmail == app.studentEmail);

                                block.Applications.Add(new Req_AppViewModel
                                {
                                    ApplicationId = app.id,
                                    StudentId = matchedUser?.id ?? app.studentId ?? 0,
                                    FullName = $"{app.studentSurname} {app.studentName} {app.studentPatronymic}".Trim(),
                                    StudentEmail = string.IsNullOrWhiteSpace(app.studentEmail) ? "Почта не указана" : app.studentEmail,
                                    Comment = string.IsNullOrWhiteSpace(app.description) ? "Комментарий не оставлен" : app.description,
                                    Avatar = GetImageFromBase64(matchedUser?.photo ?? app.studentPhoto) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png")),
                                    IsOrganizer = false
                                });
                            }
                            blocks.Add(block);
                        }
                    }
                }

                HttpResponseMessage resOrg = await _httpClient.GetAsync($"/api/role-applications/organizer/event/{_eventId}?status=НА_РАССМОТРЕНИИ");
                if (resOrg.IsSuccessStatusCode)
                {
                    string json = await resOrg.Content.ReadAsStringAsync();
                    var list = ParsePageOrList<Req_RoleAppDto>(json, options);

                    if (list.Count > 0)
                    {
                        var block = new Req_RoleBlockViewModel { RoleTitle = "Организатор", Applications = new List<Req_AppViewModel>() };
                        foreach (var app in list)
                        {
                            var matchedUser = allUsers.FirstOrDefault(u => u.studentEmail == app.studentEmail);

                            block.Applications.Add(new Req_AppViewModel
                            {
                                ApplicationId = app.id,
                                StudentId = matchedUser?.id ?? app.studentId ?? 0,
                                FullName = $"{app.studentSurname} {app.studentName} {app.studentPatronymic}".Trim(),
                                StudentEmail = string.IsNullOrWhiteSpace(app.studentEmail) ? "Почта не указана" : app.studentEmail,
                                Comment = string.IsNullOrWhiteSpace(app.description) ? "Комментарий не оставлен" : app.description,
                                Avatar = GetImageFromBase64(matchedUser?.photo ?? app.studentPhoto) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png")),
                                IsOrganizer = true
                            });
                        }
                        blocks.Add(block);
                    }
                }

                RolesWithRequestsList.ItemsSource = blocks;
                if (blocks.Count == 0) EmptyAllText.Visibility = Visibility.Visible;
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка загрузки заявок: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private async void ApproveRequest_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is Req_AppViewModel app)
            {
                LoadingOverlay.Visibility = Visibility.Visible;
                try
                {
                    HttpResponseMessage res;
                    if (app.IsOrganizer)
                    {
                        res = await _httpClient.PutAsync($"/api/role-applications/organizer/{app.ApplicationId}/approve", new StringContent(""));
                    }
                    else
                    {
                        res = await _httpClient.PutAsync($"/api/role-applications/{app.ApplicationId}/approve", new StringContent(""));
                    }

                    if (res.IsSuccessStatusCode)
                    {
                        CustomMessageBox.Show("Заявка одобрена", "Успех", CustomMessageBox.MessageType.Success);
                        await LoadRequestsAsync();
                    }
                    else
                    {
                        string err = await res.Content.ReadAsStringAsync();
                        CustomMessageBox.Show($"Ошибка: {res.StatusCode}\n{err}", "Ошибка", CustomMessageBox.MessageType.Error);
                    }
                }
                catch (Exception ex) { CustomMessageBox.Show(ex.Message, "Ошибка", CustomMessageBox.MessageType.Error); }
                finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
            }
        }

        private void RejectRequest_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is Req_AppViewModel app)
            {
                _rejectingApplicationId = app.ApplicationId;
                _isRejectingOrganizer = app.IsOrganizer;
                TbRejectionReason.Text = "";
                ErrRejectionReason.Visibility = Visibility.Collapsed;
                RejectionOverlay.Visibility = Visibility.Visible;
            }
        }

        private void CancelRejection_Click(object sender, RoutedEventArgs e)
        {
            RejectionOverlay.Visibility = Visibility.Collapsed;
        }

        private async void ConfirmRejection_Click(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrWhiteSpace(TbRejectionReason.Text))
            {
                ErrRejectionReason.Visibility = Visibility.Visible;
                return;
            }

            RejectionOverlay.Visibility = Visibility.Collapsed;
            LoadingOverlay.Visibility = Visibility.Visible;

            try
            {
                var payload = new { rejectionReason = TbRejectionReason.Text };
                var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

                HttpResponseMessage res;
                if (_isRejectingOrganizer)
                {
                    res = await _httpClient.PutAsync($"/api/role-applications/organizer/{_rejectingApplicationId}/reject", content);
                }
                else
                {
                    res = await _httpClient.PutAsync($"/api/role-applications/{_rejectingApplicationId}/reject", content);
                }

                if (res.IsSuccessStatusCode)
                {
                    CustomMessageBox.Show("Заявка отклонена", "Успех", CustomMessageBox.MessageType.Success);
                    await LoadRequestsAsync();
                }
                else
                {
                    string err = await res.Content.ReadAsStringAsync();
                    CustomMessageBox.Show($"Не удалось отклонить заявку:\n{err}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex) { CustomMessageBox.Show(ex.Message, "Ошибка", CustomMessageBox.MessageType.Error); }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
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

    public class Req_GlobalUserDto
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public string studentEmail { get; set; }
        public string photo { get; set; }
    }

    public class Req_GlobalUserPage
    {
        public List<Req_GlobalUserDto> content { get; set; }
    }

    public class Req_RoleAppDto
    {
        public int id { get; set; }
        public int? studentId { get; set; }
        public string studentName { get; set; }
        public string studentSurname { get; set; }
        public string studentPatronymic { get; set; }
        public string studentEmail { get; set; }
        public string studentPhoto { get; set; }
        public string description { get; set; }
        public string eventRoleName { get; set; }
        public bool? isReserve { get; set; }
    }

    public class Req_RoleBlockViewModel : INotifyPropertyChanged
    {
        public string RoleTitle { get; set; }

        private bool _isExpanded = true;
        public bool IsExpanded
        {
            get => _isExpanded;
            set { _isExpanded = value; OnPropertyChanged(nameof(IsExpanded)); }
        }

        public List<Req_AppViewModel> Applications { get; set; }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string name) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class Req_AppViewModel
    {
        public int ApplicationId { get; set; }
        public int StudentId { get; set; }
        public string FullName { get; set; }
        public string StudentEmail { get; set; }
        public string Comment { get; set; }
        public Visibility CommentVisibility => string.IsNullOrWhiteSpace(Comment) ? Visibility.Collapsed : Visibility.Visible;
        public ImageSource Avatar { get; set; }
        public bool IsOrganizer { get; set; }
        public Brush BackgroundBrush => Brushes.Transparent;
    }
}