using Diplom_Stud.Components;
using Diplom_Stud.Pages.Activist;
using Diplom_Stud.Pages.General;
using System;
using System.Collections.Generic;
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
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class CoordinatorPanel : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private int _sectorId;

        public CoordinatorPanel(int sectorId)
        {
            InitializeComponent();
            _sectorId = sectorId;

            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            }
        }

        private async void Page_Loaded(object sender, RoutedEventArgs e)
        {
            if (_sectorId == 0)
            {
                CustomMessageBox.Show("Внимание: Передан ID сектора = 0. Скорее всего, сектор не найден в MainWindow.", "Дебаг", CustomMessageBox.MessageType.Error);
            }

            await LoadEditDataAsync();
        }

        private async void Tab_Checked(object sender, RoutedEventArgs e)
        {
            if (!IsLoaded) return;

            if (TabEdit.IsChecked == true)
            {
                GridEdit.Visibility = Visibility.Visible;
                GridParticipants.Visibility = Visibility.Collapsed;
                GridRequests.Visibility = Visibility.Collapsed;
                await LoadEditDataAsync();
            }
            else if (TabParticipants.IsChecked == true)
            {
                GridEdit.Visibility = Visibility.Collapsed;
                GridParticipants.Visibility = Visibility.Visible;
                GridRequests.Visibility = Visibility.Collapsed;
                await LoadParticipantsAsync();
            }
            else if (TabRequests.IsChecked == true)
            {
                GridEdit.Visibility = Visibility.Collapsed;
                GridParticipants.Visibility = Visibility.Collapsed;
                GridRequests.Visibility = Visibility.Visible;
                await LoadRequestsAsync();
            }
        }

        private async Task LoadEditDataAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync("/api/sector");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                    var sectors = JsonSerializer.Deserialize<List<SectorDto>>(responseBody, options);
                    var sector = sectors?.FirstOrDefault(s => s.id == _sectorId);

                    if (sector != null)
                    {
                        EditDescriptionBox.Text = sector.description;
                        if (!string.IsNullOrEmpty(sector.photo))
                        {
                            BitmapImage bmp = GetImageFromBase64(sector.photo);
                            if (bmp != null) EditSectorImage.ImageSource = bmp;
                        }
                    }
                    else
                    {
                        CustomMessageBox.Show("Ваш сектор не найден в базе данных.", "Ошибка", CustomMessageBox.MessageType.Error);
                    }
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка загрузки информации о секторе: {response.StatusCode}", "Ошибка API", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сбой сети или парсинга (Edit): {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
        }

        private void UploadImage_Click(object sender, RoutedEventArgs e)
        {
            CustomMessageBox.Show("Здесь будет логика загрузки картинки", "Инфо", CustomMessageBox.MessageType.Success);
        }

        private void SaveSector_Click(object sender, RoutedEventArgs e)
        {
            CustomMessageBox.Show("Изменения успешно сохранены", "Успех", CustomMessageBox.MessageType.Success);
        }

        private async void SearchParticipants_Click(object sender, RoutedEventArgs e)
        {
            await LoadParticipantsAsync(SearchBox.Text.Trim());
        }

        private async Task LoadParticipantsAsync(string searchQuery = "")
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyParticipantsText.Visibility = Visibility.Collapsed;
            ParticipantsListControl.ItemsSource = null;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                string url = $"/api/users/all?page=0&size=50&sortBy=id&sortDirection=ASC&sectorId={_sectorId}";
                HttpResponseMessage response = await _httpClient.GetAsync(url);

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                    var pageData = JsonSerializer.Deserialize<PageResponse<UserListDto>>(responseBody, options);

                    var participants = new List<ParticipantViewModel>();

                    if (pageData?.content != null)
                    {
                        foreach (var user in pageData.content)
                        {
                            string fullName = $"{user.surname} {user.name} {user.patronymic}".Trim();

                            if (!string.IsNullOrEmpty(searchQuery))
                            {
                                bool matchName = fullName.IndexOf(searchQuery, StringComparison.OrdinalIgnoreCase) >= 0;
                                bool matchGroup = user.groupName != null && user.groupName.IndexOf(searchQuery, StringComparison.OrdinalIgnoreCase) >= 0;

                                if (!matchName && !matchGroup) continue;
                            }

                            string course = user.courseNumber?.ToString() ?? "";
                            string specAcronym = GetSpecialityAcronym(user.specialityName);
                            string group = user.groupName ?? "";
                            string groupDisplay = !string.IsNullOrEmpty(group) ? $"{course}{specAcronym}-{group}" : "Нет";

                            participants.Add(new ParticipantViewModel
                            {
                                Id = user.id,
                                FullName = fullName,
                                GroupAndEmail = $"Группа: {groupDisplay} | {user.studentEmail}",
                                Avatar = GetImageFromBase64(user.photo) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                            });
                        }
                    }

                    ParticipantsListControl.ItemsSource = participants;
                    if (participants.Count == 0) EmptyParticipantsText.Visibility = Visibility.Visible;
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка загрузки участников: {response.StatusCode}", "Ошибка API", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сбой сети или парсинга (Участники): {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private async void RemoveParticipant_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int userId)
            {
                MessageBoxResult result = MessageBox.Show("Вы уверены, что хотите исключить участника?", "Подтверждение", MessageBoxButton.YesNo, MessageBoxImage.Warning);
                if (result == MessageBoxResult.Yes)
                {
                    try
                    {
                        LoadingOverlay.Visibility = Visibility.Visible;
                        _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                        HttpResponseMessage response = await _httpClient.DeleteAsync($"/api/sector/{_sectorId}/kick/{userId}");

                        if (response.IsSuccessStatusCode)
                        {
                            CustomMessageBox.Show("Участник успешно исключен.", "Успех", CustomMessageBox.MessageType.Success);
                            await LoadParticipantsAsync();
                        }
                        else
                        {
                            CustomMessageBox.Show($"Ошибка при исключении: {response.StatusCode}", "Ошибка API", CustomMessageBox.MessageType.Error);
                        }
                    }
                    catch (Exception ex)
                    {
                        CustomMessageBox.Show($"Сетевая ошибка при исключении: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
                    }
                    finally
                    {
                        LoadingOverlay.Visibility = Visibility.Collapsed;
                    }
                }
            }
        }

        private async Task LoadRequestsAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyRequestsText.Visibility = Visibility.Collapsed;
            RequestsListControl.ItemsSource = null;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync("/api/sector/introductions");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                    var allRequests = JsonSerializer.Deserialize<List<IntroductionDto>>(responseBody, options);

                    var activeRequests = allRequests?.Where(r => r.sector_id == _sectorId && r.status == "На рассмотрении").ToList();

                    var requestsViewModels = new List<ParticipantViewModel>();

                    if (activeRequests != null && activeRequests.Count > 0)
                    {
                        HttpResponseMessage usersRes = await _httpClient.GetAsync("/api/users/all?page=0&size=1000&sortBy=id&sortDirection=ASC");
                        List<UserListDto> allUsers = new List<UserListDto>();

                        if (usersRes.IsSuccessStatusCode)
                        {
                            string usersBody = await usersRes.Content.ReadAsStringAsync();
                            var pageData = JsonSerializer.Deserialize<PageResponse<UserListDto>>(usersBody, options);
                            if (pageData?.content != null)
                            {
                                allUsers = pageData.content;
                            }
                        }

                        foreach (var req in activeRequests)
                        {
                            string fullName = $"Пользователь ID: {req.user_id}";
                            string groupEmail = "Данные профиля не найдены";
                            ImageSource avatar = new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"));

                            var user = allUsers.FirstOrDefault(u => u.id == req.user_id);

                            if (user != null)
                            {
                                fullName = $"{user.surname} {user.name} {user.patronymic}".Trim();

                                string course = user.courseNumber?.ToString() ?? "";
                                string specAcronym = GetSpecialityAcronym(user.specialityName);
                                string group = user.groupName ?? "";
                                string groupDisplay = !string.IsNullOrEmpty(group) ? $"{course}{specAcronym}-{group}" : "Нет";

                                groupEmail = $"Группа: {groupDisplay} | {user.studentEmail}";

                                var bmp = GetImageFromBase64(user.photo);
                                if (bmp != null) avatar = bmp;
                            }

                            requestsViewModels.Add(new ParticipantViewModel
                            {
                                Id = req.user_id,
                                RequestId = req.id,
                                FullName = fullName,
                                GroupAndEmail = groupEmail,
                                Avatar = avatar
                            });
                        }
                    }

                    RequestsListControl.ItemsSource = requestsViewModels;
                    if (requestsViewModels.Count == 0) EmptyRequestsText.Visibility = Visibility.Visible;
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка получения заявок: {response.StatusCode}", "Ошибка API", CustomMessageBox.MessageType.Error);
                    EmptyRequestsText.Visibility = Visibility.Visible;
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка загрузки заявок: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
                EmptyRequestsText.Visibility = Visibility.Visible;
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
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
                    HttpResponseMessage response = await _httpClient.PutAsync($"/api/sector/accept/{reqId}", content);

                    if (response.IsSuccessStatusCode)
                    {
                        CustomMessageBox.Show("Заявка успешно одобрена.", "Успех", CustomMessageBox.MessageType.Success);
                        await LoadRequestsAsync();
                    }
                    else
                    {
                        CustomMessageBox.Show($"Ошибка принятия заявки: {response.StatusCode}", "Ошибка API", CustomMessageBox.MessageType.Error);
                    }
                }
                catch (Exception ex) { CustomMessageBox.Show($"Ошибка сети: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error); }
                finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
            }
        }

        private async void RejectRequest_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int reqId)
            {
                try
                {
                    LoadingOverlay.Visibility = Visibility.Visible;
                    _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                    var content = new StringContent("", Encoding.UTF8, "application/json");
                    HttpResponseMessage response = await _httpClient.PutAsync($"/api/sector/reject/{reqId}", content);

                    if (response.IsSuccessStatusCode)
                    {
                        CustomMessageBox.Show("Заявка отклонена.", "Информация", CustomMessageBox.MessageType.Success);
                        await LoadRequestsAsync();
                    }
                    else
                    {
                        CustomMessageBox.Show($"Ошибка отклонения: {response.StatusCode}", "Ошибка API", CustomMessageBox.MessageType.Error);
                    }
                }
                catch (Exception ex) { CustomMessageBox.Show($"Ошибка сети: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error); }
                finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
            }
        }

        private string GetSpecialityAcronym(string title)
        {
            if (string.IsNullOrWhiteSpace(title)) return "";

            var words = title.Split(new[] { ' ', '-' }, StringSplitOptions.RemoveEmptyEntries);
            string acronym = "";

            foreach (var word in words)
            {
                if (word.Length > 0 && char.IsLetter(word[0]))
                {
                    acronym += char.ToUpper(word[0]);
                }
            }

            return acronym;
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
                        if (commaIndex >= 0)
                        {
                            string actualBase64 = textInside.Substring(commaIndex + 1);
                            imageBytes = Convert.FromBase64String(actualBase64);
                        }
                    }
                    else { imageBytes = decodedFirstLevel; }
                }
                catch
                {
                    int commaIndex = cleanStr.IndexOf(',');
                    if (commaIndex >= 0) { cleanStr = cleanStr.Substring(commaIndex + 1); }
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
            catch (Exception ex) { Debug.WriteLine($"Ошибка обработки фото: {ex.Message}"); }

            return null;
        }
    }

    public class ParticipantViewModel
    {
        public int Id { get; set; }
        public int RequestId { get; set; }
        public string FullName { get; set; }
        public string GroupAndEmail { get; set; }
        public ImageSource Avatar { get; set; }
    }

    public class PageResponse<T>
    {
        public List<T> content { get; set; }
        public int totalPages { get; set; }
        public int totalElements { get; set; }
    }

    public class SectorDto
    {
        public int id { get; set; }
        public string title { get; set; }
        public string description { get; set; }
        public bool isParticipant { get; set; }
        public bool isCoordinator { get; set; }
        public bool hasActiveRequest { get; set; }
        public string requestStatus { get; set; }
        public int participantCount { get; set; }
        public string photo { get; set; }
        public string coordinatorName { get; set; }
        public string coordinatorSurname { get; set; }
        public string coordinatorPatronymic { get; set; }
        public string coordinatorFullName { get; set; }
        public string coordinatorPhoto { get; set; }
    }

    public class IntroductionDto
    {
        public int id { get; set; }
        public int sector_id { get; set; }
        public int user_id { get; set; }
        public string status { get; set; }
    }

    public class UserListDto
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public string gender { get; set; }
        public int? courseNumber { get; set; }
        public string studentEmail { get; set; }
        public string phoneNumber { get; set; }
        public string photo { get; set; }
        public string role { get; set; }
        public string groupName { get; set; }
        public string specialityName { get; set; }
    }
}