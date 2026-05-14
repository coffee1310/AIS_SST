using Diplom_Stud.Components;
using Microsoft.Win32;
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
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class CoordinatorPanel : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private int _sectorId;
        private SectorDto _currentLoadedSector;
        private string _newBase64Image = null;

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
                CustomMessageBox.Show("Внимание: Передан ID сектора = 0. Скорее всего, сектор не найден в профиле.", "Дебаг", CustomMessageBox.MessageType.Error);
            }

            await CheckPendingRequestsAsync();
            await LoadParticipantsAsync();
        }

        private async void Tab_Checked(object sender, RoutedEventArgs e)
        {
            if (!IsLoaded) return;

            if (TabParticipants.IsChecked == true)
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
            else if (TabEdit.IsChecked == true)
            {
                GridParticipants.Visibility = Visibility.Collapsed;
                GridRequests.Visibility = Visibility.Collapsed;
                GridEdit.Visibility = Visibility.Visible;
                await LoadEditDataAsync();
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
                    _currentLoadedSector = sectors?.FirstOrDefault(s => s.id == _sectorId);

                    if (_currentLoadedSector != null)
                    {
                        EditDescriptionBox.Text = _currentLoadedSector.description;
                        if (!string.IsNullOrEmpty(_currentLoadedSector.photo))
                        {
                            BitmapImage bmp = GetImageFromBase64(_currentLoadedSector.photo);
                            if (bmp != null) EditSectorImage.ImageSource = bmp;
                            _newBase64Image = _currentLoadedSector.photo;
                        }
                        else
                        {
                            EditSectorImage.ImageSource = new BitmapImage(new Uri("pack://application:,,,/Resources/sector.png"));
                            _newBase64Image = null;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сбой сети: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
        }

        private async Task LoadParticipantsAsync(string searchQuery = "")
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyParticipantsText.Visibility = Visibility.Collapsed;
            ParticipantsListControl.ItemsSource = null;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                string url = $"/api/sector/{_sectorId}/participants?page=0&size=1000&sortBy=entryDate&sortDirection=DESC";
                HttpResponseMessage response = await _httpClient.GetAsync(url);

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var pageData = JsonSerializer.Deserialize<PageResponse<ParticipantDto>>(responseBody, options);

                    var participants = new List<ParticipantViewModel>();

                    if (pageData?.content != null)
                    {
                        foreach (var p in pageData.content)
                        {
                            if (p.status != "Активный") continue;

                            string fullName = $"{p.studentSurname} {p.studentName} {p.studentPatronymic}".Trim();
                            if (p.isCoordinator) fullName += " (Координатор)";

                            if (!string.IsNullOrEmpty(searchQuery))
                            {
                                bool matchName = fullName.IndexOf(searchQuery, StringComparison.OrdinalIgnoreCase) >= 0;
                                bool matchGroup = p.studentGroupTitle != null && p.studentGroupTitle.IndexOf(searchQuery, StringComparison.OrdinalIgnoreCase) >= 0;

                                if (!matchName && !matchGroup) continue;
                            }

                            string course = p.studentCourseNumber?.ToString() ?? "";
                            string specAcronym = GetSpecialityAcronym(p.studentSpecialityTitle);
                            string group = p.studentGroupTitle ?? "";
                            string groupDisplay = !string.IsNullOrEmpty(group) ? $"{course}{specAcronym}-{group}" : "Нет";

                            participants.Add(new ParticipantViewModel
                            {
                                Id = p.studentId, 
                                FullName = fullName,
                                IsCoordinator = p.isCoordinator,
                                GroupAndEmail = $"Группа: {groupDisplay} | {p.studentEmail}",
                                Avatar = GetImageFromBase64(p.studentPhoto) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                            });
                        }
                    }

                    var sortedParticipants = participants
                        .OrderByDescending(p => p.IsCoordinator)
                        .ThenBy(p => p.FullName)
                        .ToList();

                    ParticipantsListControl.ItemsSource = sortedParticipants;
                    if (sortedParticipants.Count == 0) EmptyParticipantsText.Visibility = Visibility.Visible;
                }
            }
            catch (Exception ex) { CustomMessageBox.Show($"Сбой сети: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error); }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
        }

        private async Task LoadRequestsAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyRequestsText.Visibility = Visibility.Collapsed;
            RequestsListControl.ItemsSource = null;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync("/api/sector/introductions/filter?status=НА_РАССМОТРЕНИИ");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var allRequests = JsonSerializer.Deserialize<List<IntroductionDto>>(responseBody, options);

                    var activeRequests = allRequests?.Where(r => r.sector_id == _sectorId).ToList();
                    var requestsViewModels = new List<ParticipantViewModel>();

                    bool hasActive = activeRequests != null && activeRequests.Count > 0;

                    BadgeRequests.Visibility = hasActive ? Visibility.Visible : Visibility.Collapsed;
                    if (Window.GetWindow(this) is MainWindow mainWindow)
                    {
                        mainWindow.SetSectorNotification(hasActive);
                    }

                    if (hasActive)
                    {
                        HttpResponseMessage usersRes = await _httpClient.GetAsync("/api/users/all?page=0&size=1000&sortBy=id&sortDirection=ASC");
                        List<UserListDto> allUsers = new List<UserListDto>();

                        if (usersRes.IsSuccessStatusCode)
                        {
                            string usersBody = await usersRes.Content.ReadAsStringAsync();
                            var pageData = JsonSerializer.Deserialize<PageResponse<UserListDto>>(usersBody, options);
                            if (pageData?.content != null) allUsers = pageData.content;
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
            }
            catch (Exception ex) { CustomMessageBox.Show($"Ошибка загрузки заявок: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error); }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
        }

        private async void SearchParticipants_Click(object sender, RoutedEventArgs e)
        {
            await LoadParticipantsAsync(SearchBox.Text.Trim());
        }

        private async Task CheckPendingRequestsAsync()
        {
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                HttpResponseMessage response = await _httpClient.GetAsync("/api/sector/introductions/filter?status=НА_РАССМОТРЕНИИ");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var allRequests = JsonSerializer.Deserialize<List<IntroductionDto>>(responseBody, options);

                    bool hasActive = allRequests?.Any(r => r.sector_id == _sectorId) == true;

                    BadgeRequests.Visibility = hasActive ? Visibility.Visible : Visibility.Collapsed;
                    if (Window.GetWindow(this) is MainWindow mainWindow)
                    {
                        mainWindow.SetSectorNotification(hasActive);
                    }
                }
            }
            catch { }
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
                }
                catch (Exception ex) { CustomMessageBox.Show($"Ошибка: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error); }
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
                }
                catch (Exception ex) { CustomMessageBox.Show($"Ошибка: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error); }
                finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
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
                    }
                    catch (Exception ex) { CustomMessageBox.Show($"Ошибка: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error); }
                    finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
                }
            }
        }

        private void ParticipantCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is Border border && border.Tag is int userId)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Activist.Profile(userId));
            }
        }

        private void UploadImage_Click(object sender, RoutedEventArgs e)
        {
            OpenFileDialog openFileDialog = new OpenFileDialog();
            openFileDialog.Filter = "Image files (*.png;*.jpeg;*.jpg)|*.png;*.jpeg;*.jpg|All files (*.*)|*.*";
            openFileDialog.Title = "Выберите обложку сектора";

            if (openFileDialog.ShowDialog() == true)
            {
                try
                {
                    byte[] compressedBytes = CompressAndResizeImage(openFileDialog.FileName);
                    long maxImageSizeBytes = 2 * 1024 * 1024;

                    if (compressedBytes.Length > maxImageSizeBytes)
                    {
                        CustomMessageBox.Show("Файл слишком большой даже после сжатия. Выберите фото размером до 2 МБ.", "Ошибка", CustomMessageBox.MessageType.Error);
                    }
                    else
                    {
                        using (var ms = new MemoryStream(compressedBytes))
                        {
                            var bitmap = new BitmapImage();
                            bitmap.BeginInit();
                            bitmap.CacheOption = BitmapCacheOption.OnLoad;
                            bitmap.StreamSource = ms;
                            bitmap.EndInit();
                            bitmap.Freeze();
                            EditSectorImage.ImageSource = bitmap;
                        }

                        _newBase64Image = $"data:image/jpeg;base64,{Convert.ToBase64String(compressedBytes)}";
                    }
                }
                catch (Exception ex)
                {
                    CustomMessageBox.Show($"Ошибка при обработке фотографии: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
        }

        private byte[] CompressAndResizeImage(string filePath)
        {
            using (var stream = new FileStream(filePath, FileMode.Open, FileAccess.Read))
            {
                BitmapDecoder decoder = BitmapDecoder.Create(stream, BitmapCreateOptions.PreservePixelFormat, BitmapCacheOption.OnLoad);
                BitmapSource original = decoder.Frames[0];

                double maxWidth = 800;
                double maxHeight = 800;
                BitmapSource finalImage = original;

                if (original.PixelWidth > maxWidth || original.PixelHeight > maxHeight)
                {
                    double ratioX = maxWidth / original.PixelWidth;
                    double ratioY = maxHeight / original.PixelHeight;
                    double ratio = Math.Min(ratioX, ratioY);

                    TransformedBitmap resized = new TransformedBitmap(original, new ScaleTransform(ratio, ratio));
                    finalImage = resized;
                }

                JpegBitmapEncoder encoder = new JpegBitmapEncoder();
                encoder.QualityLevel = 80;
                encoder.Frames.Add(BitmapFrame.Create(finalImage));

                using (MemoryStream ms = new MemoryStream())
                {
                    encoder.Save(ms);
                    return ms.ToArray();
                }
            }
        }

        private async void SaveSector_Click(object sender, RoutedEventArgs e)
        {
            if (_currentLoadedSector == null) return;

            string newDescription = EditDescriptionBox.Text.Trim();

            if (string.IsNullOrWhiteSpace(newDescription))
            {
                CustomMessageBox.Show("Описание сектора не может быть пустым.", "Ошибка валидации", CustomMessageBox.MessageType.Error);
                return;
            }

            LoadingOverlay.Visibility = Visibility.Visible;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                var updatePayload = new
                {
                    description = newDescription,
                    photo = _newBase64Image ?? ""
                };

                string jsonPayload = JsonSerializer.Serialize(updatePayload);
                var content = new StringContent(jsonPayload, Encoding.UTF8, "application/json");

                HttpResponseMessage response = await _httpClient.PutAsync($"/api/sector/{_sectorId}", content);

                if (response.IsSuccessStatusCode)
                {
                    CustomMessageBox.Show("Изменения успешно сохранены!", "Успех", CustomMessageBox.MessageType.Success);

                    _currentLoadedSector.description = newDescription;
                    _currentLoadedSector.photo = _newBase64Image ?? "";
                }
                else
                {
                    string err = await response.Content.ReadAsStringAsync();
                    CustomMessageBox.Show($"Ошибка сохранения: {response.StatusCode}\n{err}", "Ошибка сервера", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Произошла ошибка при отправке запроса: {ex.Message}", "Сбой", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private string GetSpecialityAcronym(string title)
        {
            if (string.IsNullOrWhiteSpace(title)) return "";
            var words = title.Split(new[] { ' ', '-' }, StringSplitOptions.RemoveEmptyEntries);
            string acronym = "";
            foreach (var word in words)
            {
                if (word.Length > 0 && char.IsLetter(word[0])) acronym += char.ToUpper(word[0]);
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
        public bool IsCoordinator { get; set; }
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
        public string photo { get; set; }

        public override string ToString()
        {
            return title;
        }
    }

    public class IntroductionDto
    {
        public int id { get; set; }
        public int sector_id { get; set; }
        public int user_id { get; set; }
        public string status { get; set; }
    }

    public class ParticipantDto
    {
        public int id { get; set; }
        public int studentId { get; set; }
        public string studentName { get; set; }
        public string studentSurname { get; set; }
        public string studentPatronymic { get; set; }
        public string studentEmail { get; set; }
        public string studentPhoto { get; set; }
        public int? studentCourseNumber { get; set; }
        public string studentGroupTitle { get; set; }
        public string studentSpecialityTitle { get; set; }
        public string status { get; set; }
        public bool isCoordinator { get; set; }
    }

    public class UserListDto
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public int? courseNumber { get; set; }
        public string studentEmail { get; set; }
        public string photo { get; set; }
        public string role { get; set; }
        public string groupName { get; set; }
        public string specialityName { get; set; }
    }
}