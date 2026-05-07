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

        private async void Page_Loaded(object sender, RoutedEventArgs e)
        {
            await LoadRequestsAsync();
        }

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

                    if (pageData != null && pageData.content != null && pageData.content.Count > 0)
                    {
                        _totalPages = pageData.totalPages;
                        TxtPageInfo.Text = $"Страница {_currentPage + 1} из {_totalPages}";

                        BtnPrevPage.IsEnabled = _currentPage > 0;
                        BtnNextPage.IsEnabled = _currentPage < _totalPages - 1;
                        PaginationPanel.Visibility = Visibility.Visible;

                        var viewModels = new List<RequestViewModel>();

                        foreach (var dto in pageData.content)
                        {
                            // УНИВЕРСАЛЬНАЯ ПРОВЕРКА (Игнорирует капс и окончания)
                            string rawStatus = dto.status?.ToUpper() ?? "";
                            bool isPending = rawStatus.Contains("РАССМОТРЕНИ");
                            bool isApproved = rawStatus.Contains("ОДОБР") || rawStatus.Contains("ПРИНЯТ");
                            bool isRejected = rawStatus.Contains("ОТКЛОН");

                            string ageDisplay = "";
                            if (DateTime.TryParse(dto.dateOfBirth, out DateTime dob))
                            {
                                int age = DateTime.Today.Year - dob.Year;
                                if (dob.Date > DateTime.Today.AddYears(-age)) age--;
                                ageDisplay = $"Возраст: {age}";
                            }

                            string vkDisplay = string.IsNullOrEmpty(dto.vkLink) ? "" : $"ВК: {dto.vkLink}";

                            viewModels.Add(new RequestViewModel
                            {
                                Id = dto.id,
                                FullName = $"{dto.surname} {dto.name} {dto.patronymic}".Trim(),
                                CourseGroupSpec = $"{dto.courseNumber} курс, {dto.groupName} ({dto.specialityName})",
                                AgeInfo = ageDisplay,
                                StudentEmail = dto.studentEmail,
                                PhoneNumber = dto.phoneNumber,
                                VkLinkDisplay = vkDisplay,
                                HasVkLink = !string.IsNullOrEmpty(dto.vkLink),
                                Avatar = GetImageFromBase64(dto.photo) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png")),

                                ShowActions = isPending,
                                ShowStatusBadge = !isPending,
                                // Зеленый для принятых
                                StatusText = isApproved ? "ПРИНЯТО" : (isRejected ? "ОТКЛОНЕНО" : dto.status),
                                StatusColor = isApproved ? new SolidColorBrush((Color)ColorConverter.ConvertFromString("#00C853")) : new SolidColorBrush((Color)ColorConverter.ConvertFromString("#E81123")),

                                // Показ причины отказа
                                ShowRejectionReason = isRejected && !string.IsNullOrEmpty(dto.reasonForRefusal),
                                RejectionReasonDisplay = $"Причина отклонения: {dto.reasonForRefusal}"
                            });
                        }

                        RequestsListControl.ItemsSource = viewModels;
                    }
                    else
                    {
                        EmptyRequestsText.Visibility = Visibility.Visible;
                    }

                    await CheckPendingRequestsBadgeAsync();
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка получения данных: {response.StatusCode}", "Ошибка API", CustomMessageBox.MessageType.Error);
                    EmptyRequestsText.Visibility = Visibility.Visible;
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка загрузки заявок: {ex.Message}", "Ошибка сети", CustomMessageBox.MessageType.Error);
                EmptyRequestsText.Visibility = Visibility.Visible;
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
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
            catch { }
        }

        private async void PrevPage_Click(object sender, RoutedEventArgs e)
        {
            if (_currentPage > 0)
            {
                _currentPage--;
                await LoadRequestsAsync();
            }
        }

        private async void NextPage_Click(object sender, RoutedEventArgs e)
        {
            if (_currentPage < _totalPages - 1)
            {
                _currentPage++;
                await LoadRequestsAsync();
            }
        }

        private async void ApproveRequest_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int reqId)
            {
                try
                {
                    LoadingOverlay.Visibility = Visibility.Visible;
                    _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                    var content = new StringContent("", Encoding.UTF8, "application/json");
                    HttpResponseMessage response = await _httpClient.PutAsync($"/api/account_requests/accept/{reqId}", content);

                    if (response.IsSuccessStatusCode)
                    {
                        CustomMessageBox.Show("Заявка пользователя успешно одобрена.", "Успех", CustomMessageBox.MessageType.Success);

                        if (RequestsListControl.Items.Count == 1 && _currentPage > 0) _currentPage--;

                        await LoadRequestsAsync();
                    }
                    else
                    {
                        CustomMessageBox.Show($"Ошибка сервера при одобрении: {response.StatusCode}", "Ошибка API", CustomMessageBox.MessageType.Error);
                    }
                }
                catch (Exception ex)
                {
                    CustomMessageBox.Show($"Сетевая ошибка: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
                finally
                {
                    LoadingOverlay.Visibility = Visibility.Collapsed;
                }
            }
        }

        private void RejectRequest_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int reqId)
            {
                _rejectingRequestId = reqId;
                TbRejectionReason.Text = "";
                ErrRejectionReason.Visibility = Visibility.Collapsed;
                RejectionOverlay.Visibility = Visibility.Visible;
            }
        }

        private void CancelRejection_Click(object sender, RoutedEventArgs e)
        {
            RejectionOverlay.Visibility = Visibility.Collapsed;
            _rejectingRequestId = 0;
        }

        private async void ConfirmRejection_Click(object sender, RoutedEventArgs e)
        {
            string reason = TbRejectionReason.Text.Trim();

            if (string.IsNullOrEmpty(reason))
            {
                ErrRejectionReason.Visibility = Visibility.Visible;
                return;
            }

            RejectionOverlay.Visibility = Visibility.Collapsed;
            LoadingOverlay.Visibility = Visibility.Visible;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                var reqObj = new { rejectionReason = reason };
                string json = JsonSerializer.Serialize(reqObj);
                var content = new StringContent(json, Encoding.UTF8, "application/json");

                HttpResponseMessage response = await _httpClient.PutAsync($"/api/account_requests/reject/{_rejectingRequestId}", content);

                if (response.IsSuccessStatusCode)
                {
                    CustomMessageBox.Show("Заявка успешно отклонена.", "Информация", CustomMessageBox.MessageType.Success);

                    if (RequestsListControl.Items.Count == 1 && _currentPage > 0) _currentPage--;

                    await LoadRequestsAsync();
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка сервера при отклонении: {response.StatusCode}", "Ошибка API", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сетевая ошибка: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
                _rejectingRequestId = 0;
            }
        }

        private BitmapImage GetImageFromBase64(string base64String)
        {
            if (string.IsNullOrEmpty(base64String)) return null;
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

    public class PageResponse<T>
    {
        public List<T> content { get; set; }
        public int totalPages { get; set; }
        public int totalElements { get; set; }
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
        public string phoneNumber { get; set; }
        public int studentIdNumber { get; set; }
        public int courseNumber { get; set; }
        public string status { get; set; }
        public string reasonForRefusal { get; set; }
        public int groupId { get; set; }
        public string groupName { get; set; }
        public int specialityId { get; set; }
        public string specialityName { get; set; }
        public string photo { get; set; }
        public string vkLink { get; set; }
        public string createdAt { get; set; }
        public string updatedAt { get; set; }
    }

    public class RequestViewModel
    {
        public int Id { get; set; }
        public string FullName { get; set; }
        public string CourseGroupSpec { get; set; }
        public string AgeInfo { get; set; }
        public string StudentEmail { get; set; }
        public string PhoneNumber { get; set; }
        public string VkLinkDisplay { get; set; }
        public bool HasVkLink { get; set; }
        public ImageSource Avatar { get; set; }

        public bool ShowActions { get; set; }
        public bool ShowStatusBadge { get; set; }
        public string StatusText { get; set; }
        public Brush StatusColor { get; set; }

        public bool ShowRejectionReason { get; set; }
        public string RejectionReasonDisplay { get; set; }
    }
}