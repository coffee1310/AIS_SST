using Diplom_Stud.Components;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace Diplom_Stud.Pages.Curator
{
    public partial class RegistrationRequests : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();

        private string _currentStatus = "НА_РАССМОТРЕНИИ";
        private int _currentPage = 0;
        private int _pageSize = 10;
        private int _totalPages = 1;
        private int _rejectingRequestId = 0;

        public RegistrationRequests()
        {
            InitializeComponent();
            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            }
        }

        private async void Page_Loaded(object sender, RoutedEventArgs e) => await LoadRequestsAsync();

        private async void Tab_Checked(object sender, RoutedEventArgs e)
        {
            if (!IsLoaded) return;
            if (sender is RadioButton rb && rb.Tag != null)
            {
                _currentStatus = rb.Tag.ToString();
                _currentPage = 0;
                await LoadRequestsAsync();
            }
        }

        private async Task LoadRequestsAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyRequestsText.Visibility = Visibility.Collapsed;
            PaginationPanel.Visibility = Visibility.Collapsed;
            RequestsListControl.ItemsSource = null;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                string url = $"/api/account_requests/filter?status={_currentStatus}&page={_currentPage}&size={_pageSize}&sortBy=createdAt&sortDirection=DESC";
                HttpResponseMessage response = await _httpClient.GetAsync(url);

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var pageData = JsonSerializer.Deserialize<PageResponse<AccountRequestDto>>(responseBody, options);

                    if (pageData?.content != null && pageData.content.Count > 0)
                    {
                        _totalPages = pageData.totalPages;
                        TxtPageInfo.Text = $"Страница {_currentPage + 1} из {_totalPages}";
                        BtnPrevPage.IsEnabled = _currentPage > 0;
                        BtnNextPage.IsEnabled = _currentPage < _totalPages - 1;
                        PaginationPanel.Visibility = Visibility.Visible;

                        var viewModels = pageData.content.Select(dto => {
                            string rawStatus = dto.status?.ToUpper() ?? "";
                            bool isPending = rawStatus.Contains("РАССМОТРЕНИ");
                            bool isApproved = rawStatus.Contains("ОДОБР") || rawStatus.Contains("ПРИНЯТ");
                            bool isRejected = rawStatus.Contains("ОТКЛОН");

                            int age = 0;
                            if (DateTime.TryParse(dto.dateOfBirth, out DateTime dob))
                            {
                                age = DateTime.Today.Year - dob.Year;
                                if (dob.Date > DateTime.Today.AddYears(-age)) age--;
                            }

                            return new RequestViewModel
                            {
                                Id = dto.id,
                                FullName = $"{dto.surname} {dto.name} {dto.patronymic}".Trim(),
                                CourseGroupSpec = $"{dto.courseNumber} курс, {dto.groupName} ({dto.specialityName})",
                                AgeInfo = age > 0 ? $"Возраст: {age}" : "",
                                StudentEmail = dto.studentEmail,
                                PhoneNumber = dto.phoneNumber,
                                Avatar = GetImageFromBase64(dto.photo) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png")),
                                ShowActions = isPending,
                                ShowStatusBadge = !isPending,
                                StatusText = isApproved ? "ПРИНЯТО" : (isRejected ? "ОТКЛОНЕНО" : dto.status),
                                StatusColor = isApproved ? new SolidColorBrush((Color)ColorConverter.ConvertFromString("#00C853")) : new SolidColorBrush((Color)ColorConverter.ConvertFromString("#E81123")),
                                ShowRejectionReason = isRejected && !string.IsNullOrEmpty(dto.reasonForRefusal),
                                RejectionReasonDisplay = $"Причина: {dto.reasonForRefusal}"
                            };
                        }).ToList();

                        RequestsListControl.ItemsSource = viewModels;
                    }
                    else EmptyRequestsText.Visibility = Visibility.Visible;
                    await CheckPendingRequestsBadgeAsync();
                }
            }
            catch (Exception ex) { CustomMessageBox.Show(ex.Message, "Ошибка", CustomMessageBox.MessageType.Error); }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
        }

        private async void OpenDetails_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int reqId)
            {
                LoadingOverlay.Visibility = Visibility.Visible;
                try
                {
                    _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                    var response = await _httpClient.GetAsync($"/api/account_requests/filter?id={reqId}");

                    if (response.IsSuccessStatusCode)
                    {
                        var json = await response.Content.ReadAsStringAsync();
                        var pageData = JsonSerializer.Deserialize<PageResponse<AccountRequestDto>>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                        var dto = pageData?.content?.FirstOrDefault();

                        if (dto != null)
                        {
                            string rawStatus = dto.status?.ToUpper() ?? "";
                            bool isApproved = rawStatus.Contains("ОДОБР") || rawStatus.Contains("ПРИНЯТ");
                            bool isRejected = rawStatus.Contains("ОТКЛОН");

                            int age = 0;
                            if (DateTime.TryParse(dto.dateOfBirth, out DateTime dob))
                            {
                                age = DateTime.Today.Year - dob.Year;
                                if (dob.Date > DateTime.Today.AddYears(-age)) age--;
                            }

                            string socialDisplay = (dto.socialStatuses != null && dto.socialStatuses.Any())
                                ? string.Join(", ", dto.socialStatuses)
                                : "Не указаны";

                            var detailsVm = new RequestDetailsViewModel
                            {
                                Avatar = GetImageFromBase64(dto.photo) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png")),
                                FullName = $"{dto.surname} {dto.name} {dto.patronymic}".Trim(),
                                Gender = dto.gender ?? "Не указан",
                                DateOfBirth = dto.dateOfBirth ?? "Не указана",
                                Age = age > 0 ? age.ToString() : "—",
                                StudentId = dto.studentIdNumber.ToString(),
                                GroupInfo = $"{dto.courseNumber} курс, группа {dto.groupName}",
                                SpecialityName = dto.specialityName,
                                Phone = dto.phoneNumber ?? "Не указан",
                                Email = dto.studentEmail,
                                AdditionalEmailDisplay = string.IsNullOrEmpty(dto.additionalEmail) ? "Не указана" : dto.additionalEmail,
                                VkLink = dto.vkLink ?? "Не указана",
                                SocialStatusesDisplay = socialDisplay,
                                CreatedAtDisplay = DateTime.TryParse(dto.createdAt, out DateTime cd) ? $"Создана: {cd:dd.MM.yyyy HH:mm}" : "",
                                Status = isApproved ? "ПРИНЯТО" : (isRejected ? "ОТКЛОНЕНО" : "НА РАССМОТРЕНИИ"),
                                StatusColor = isApproved ? Brushes.LimeGreen : (isRejected ? Brushes.Tomato : Brushes.DeepSkyBlue),
                                RejectionReason = dto.reasonForRefusal,
                                RejectionVisibility = isRejected ? Visibility.Visible : Visibility.Collapsed
                            };

                            var detailsWindow = new RequestDetailsWindow(detailsVm);
                            detailsWindow.Owner = Window.GetWindow(this);
                            detailsWindow.ShowDialog();
                        }
                    }
                }
                catch (Exception ex) { CustomMessageBox.Show(ex.Message, "Ошибка", CustomMessageBox.MessageType.Error); }
                finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
            }
        }

        private void CancelRejection_Click(object sender, RoutedEventArgs e) => RejectionOverlay.Visibility = Visibility.Collapsed;
        private void RejectRequest_Click(object sender, RoutedEventArgs e) { _rejectingRequestId = (int)((Button)sender).Tag; TbRejectionReason.Text = ""; RejectionOverlay.Visibility = Visibility.Visible; }

        private async void ConfirmRejection_Click(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrWhiteSpace(TbRejectionReason.Text)) { ErrRejectionReason.Visibility = Visibility.Visible; return; }
            RejectionOverlay.Visibility = Visibility.Collapsed;
            LoadingOverlay.Visibility = Visibility.Visible;
            try
            {
                var content = new StringContent(JsonSerializer.Serialize(new { rejectionReason = TbRejectionReason.Text }), Encoding.UTF8, "application/json");
                var res = await _httpClient.PutAsync($"/api/account_requests/reject/{_rejectingRequestId}", content);
                if (res.IsSuccessStatusCode) { CustomMessageBox.Show("Заявка отклонена", "Успех", CustomMessageBox.MessageType.Success); await LoadRequestsAsync(); }
            }
            catch { }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
        }

        private async void ApproveRequest_Click(object sender, RoutedEventArgs e)
        {
            var id = (int)((Button)sender).Tag;
            LoadingOverlay.Visibility = Visibility.Visible;
            try
            {
                var res = await _httpClient.PutAsync($"/api/account_requests/accept/{id}", new StringContent("", Encoding.UTF8, "application/json"));
                if (res.IsSuccessStatusCode) { CustomMessageBox.Show("Заявка одобрена", "Успех", CustomMessageBox.MessageType.Success); await LoadRequestsAsync(); }
            }
            catch { }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
        }

        private async Task CheckPendingRequestsBadgeAsync()
        {
            try
            {
                HttpResponseMessage response = await _httpClient.GetAsync("/api/account_requests/filter?status=НА_РАССМОТРЕНИИ&page=0&size=1&sortBy=createdAt&sortDirection=DESC");

                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    using (JsonDocument doc = JsonDocument.Parse(json))
                    {
                        if (doc.RootElement.TryGetProperty("content", out JsonElement contentElement))
                        {
                            bool hasPending = contentElement.GetArrayLength() > 0;

                            BadgePendingRequests.Visibility = hasPending ? Visibility.Visible : Visibility.Collapsed;

                            if (Window.GetWindow(this) is MainWindow mainWindow)
                            {
                                mainWindow.SetRegistrationRequestNotification(hasPending);
                            }
                        }
                    }
                }
            }
            catch
            {
            }
        }
        private async void PrevPage_Click(object sender, RoutedEventArgs e) { if (_currentPage > 0) { _currentPage--; await LoadRequestsAsync(); } }
        private async void NextPage_Click(object sender, RoutedEventArgs e) { if (_currentPage < _totalPages - 1) { _currentPage++; await LoadRequestsAsync(); } }

        private BitmapImage GetImageFromBase64(string b64)
        {
            if (string.IsNullOrEmpty(b64)) return null;
            try
            {
                var s = b64.Contains(",") ? b64.Split(',')[1] : b64;
                var bin = Convert.FromBase64String(s);
                var img = new BitmapImage();
                using (var ms = new MemoryStream(bin))
                {
                    img.BeginInit(); img.CacheOption = BitmapCacheOption.OnLoad; img.StreamSource = ms; img.EndInit();
                }
                img.Freeze(); return img;
            }
            catch { return null; }
        }
    }

    public class AccountRequestDto
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public string gender { get; set; }
        public string dateOfBirth { get; set; }
        public string studentEmail { get; set; }
        public string additionalEmail { get; set; }
        public string phoneNumber { get; set; }
        public int studentIdNumber { get; set; }
        public int courseNumber { get; set; }
        public string status { get; set; }
        public string reasonForRefusal { get; set; }
        public string groupName { get; set; }
        public string specialityName { get; set; }
        public List<string> socialStatuses { get; set; }
        public string photo { get; set; }
        public string vkLink { get; set; }
        public string createdAt { get; set; }
    }

    public class RequestViewModel
    {
        public int Id { get; set; }
        public string FullName { get; set; }
        public string CourseGroupSpec { get; set; }
        public string AgeInfo { get; set; }
        public string StudentEmail { get; set; }
        public string PhoneNumber { get; set; }
        public ImageSource Avatar { get; set; }
        public bool ShowActions { get; set; }
        public bool ShowStatusBadge { get; set; }
        public string StatusText { get; set; }
        public Brush StatusColor { get; set; }
        public bool ShowRejectionReason { get; set; }
        public string RejectionReasonDisplay { get; set; }
    }

    public class RequestDetailsViewModel
    {
        public ImageSource Avatar { get; set; }
        public string FullName { get; set; }
        public string Gender { get; set; }
        public string DateOfBirth { get; set; }
        public string Age { get; set; }
        public string StudentId { get; set; }
        public string GroupInfo { get; set; }
        public string SpecialityName { get; set; }
        public string Phone { get; set; }
        public string Email { get; set; }
        public string AdditionalEmailDisplay { get; set; }
        public string VkLink { get; set; }
        public string SocialStatusesDisplay { get; set; }
        public string CreatedAtDisplay { get; set; }
        public string Status { get; set; }
        public Brush StatusColor { get; set; }
        public string RejectionReason { get; set; }
        public Visibility RejectionVisibility { get; set; }
    }

    public class PageResponse<T> { public List<T> content { get; set; } public int totalPages { get; set; } }
}