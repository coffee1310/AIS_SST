using Diplom_Stud.Components;
using Diplom_Stud.Pages.General;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;

namespace Diplom_Stud.Pages.Activist
{
    public partial class SectorDetails : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private int _sectorId;
        private SectorDto _currentSector; 

        public SectorDetails(int sectorId)
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
            DoubleAnimation fadeInAnimation = new DoubleAnimation
            {
                From = 0.0,
                To = 1.0,
                Duration = TimeSpan.FromSeconds(0.8),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };
            this.BeginAnimation(Page.OpacityProperty, fadeInAnimation);

            await LoadSectorDataAsync();
        }

        private async Task LoadSectorDataAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;

            try
            {
                if (string.IsNullOrEmpty(App.AuthToken))
                {
                    CustomMessageBox.Show("Ошибка авторизации.", "Ошибка", CustomMessageBox.MessageType.Error);
                    NavigationService?.Navigate(new Auth());
                    return;
                }

                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync("/api/sector");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var sectors = JsonSerializer.Deserialize<List<SectorDto>>(responseBody, options);

                    _currentSector = sectors?.FirstOrDefault(s => s.id == _sectorId);

                    if (_currentSector != null)
                    {
                        UpdateUI();
                    }
                    else
                    {
                        CustomMessageBox.Show("Сектор не найден.", "Ошибка", CustomMessageBox.MessageType.Error);
                    }
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка загрузки: {response.StatusCode}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private void UpdateUI()
        {
            SectorTitle.Text = _currentSector.title;
            SectorDescription.Text = _currentSector.description;

            if (!string.IsNullOrEmpty(_currentSector.photo))
            {
                BitmapImage bmp = GetImageFromBase64(_currentSector.photo);
                if (bmp != null) SectorImage.ImageSource = bmp;
            }

            if (_currentSector.coordinators != null && _currentSector.coordinators.Count > 0)
            {
                NoCoordinatorsText.Visibility = Visibility.Collapsed;
                CoordinatorsListControl.Visibility = Visibility.Visible;

                var coordList = new List<CoordinatorViewModel>();

                foreach (var c in _currentSector.coordinators)
                {
                    string fullName = $"{c.studentSurname} {c.studentName} {c.studentPatronymic}".Trim();

                    string course = c.studentCourseNumber?.ToString() ?? "";
                    string specAcronym = GetSpecialityAcronym(c.studentSpecialityTitle);
                    string group = c.studentGroupTitle ?? "";
                    string groupDisplay = !string.IsNullOrEmpty(group) ? $"Группа: {course}{specAcronym}-{group}" : "Группа не указана";

                    ImageSource avatar = new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"));
                    if (!string.IsNullOrEmpty(c.studentPhoto))
                    {
                        var bmp = GetImageFromBase64(c.studentPhoto);
                        if (bmp != null) avatar = bmp;
                    }

                    coordList.Add(new CoordinatorViewModel
                    {
                        FullName = fullName,
                        GroupInfo = groupDisplay,
                        Avatar = avatar
                    });
                }

                CoordinatorsListControl.ItemsSource = coordList;
            }
            else
            {
                NoCoordinatorsText.Visibility = Visibility.Visible;
                CoordinatorsListControl.Visibility = Visibility.Collapsed;
            }

            if (_currentSector.isCoordinator)
            {
                ActionBtn.Content = "Вы - координатор";
                ActionBtn.IsEnabled = false;
                ActionBtn.Opacity = 0.5;
            }
            else if (_currentSector.isParticipant)
            {
                ActionBtn.Content = "Выйти из сектора";
                ActionBtn.Background = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#E81123"));
                ActionBtn.IsEnabled = true;
                ActionBtn.Opacity = 1.0;
            }
            else if (_currentSector.hasActiveRequest)
            {
                ActionBtn.Content = "Заявка на рассмотрении";
                ActionBtn.IsEnabled = false;
                ActionBtn.Opacity = 0.5;
            }
            else
            {
                ActionBtn.Content = "Вступить в сектор";
                ActionBtn.Background = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#615292"));
                ActionBtn.IsEnabled = true;
                ActionBtn.Opacity = 1.0;
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
                    else
                    {
                        imageBytes = decodedFirstLevel;
                    }
                }
                catch
                {
                    int commaIndex = cleanStr.IndexOf(',');
                    if (commaIndex >= 0)
                    {
                        cleanStr = cleanStr.Substring(commaIndex + 1);
                    }
                    imageBytes = Convert.FromBase64String(cleanStr);
                }

                if (imageBytes != null)
                {
                    using (var ms = new System.IO.MemoryStream(imageBytes))
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
            catch (Exception ex)
            {
                Debug.WriteLine($"Ошибка обработки фото: {ex.Message}");
            }
            return null;
        }

        private async void ActionBtn_Click(object sender, RoutedEventArgs e)
        {
            if (_currentSector == null) return;

            ActionBtn.IsEnabled = false;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                if (_currentSector.isParticipant)
                {
                    HttpResponseMessage response = await _httpClient.DeleteAsync($"/api/sector/{_sectorId}/leave");

                    if (response.IsSuccessStatusCode)
                    {
                        CustomMessageBox.Show("Вы успешно вышли из сектора.", "Успех", CustomMessageBox.MessageType.Success);
                        await LoadSectorDataAsync();
                    }
                    else
                    {
                        string err = await response.Content.ReadAsStringAsync();
                        CustomMessageBox.Show($"Ошибка при выходе из сектора: {response.StatusCode}\n{err}", "Ошибка", CustomMessageBox.MessageType.Error);
                    }
                }
                else if (!_currentSector.isParticipant && !_currentSector.hasActiveRequest)
                {
                    int idToPass = _sectorId;

                    var content = new StringContent("", Encoding.UTF8, "application/json");
                    HttpResponseMessage response = await _httpClient.PostAsync($"/api/sector/{idToPass}", content);

                    if (response.IsSuccessStatusCode)
                    {
                        CustomMessageBox.Show("Заявка на вступление успешно отправлена!", "Успех", CustomMessageBox.MessageType.Success);
                        await LoadSectorDataAsync();
                    }
                    else
                    {
                        string err = await response.Content.ReadAsStringAsync();
                        CustomMessageBox.Show($"Ошибка при отправке заявки: {response.StatusCode}\n{err}", "Ошибка", CustomMessageBox.MessageType.Error);
                    }
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сетевая ошибка: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                ActionBtn.IsEnabled = true;
            }
        }
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
        public List<CoordinatorDto> coordinators { get; set; }
    }

    public class CoordinatorDto
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
        public string entryDate { get; set; }
        public string status { get; set; }
        public bool isCoordinator { get; set; }
    }

    public class CoordinatorViewModel
    {
        public string FullName { get; set; }
        public string GroupInfo { get; set; }
        public ImageSource Avatar { get; set; }
    }
}