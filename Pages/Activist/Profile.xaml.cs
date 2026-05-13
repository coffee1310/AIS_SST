using Diplom_Stud.Components;
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
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;

namespace Diplom_Stud.Pages.Activist
{
    public partial class Profile : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();

        public Profile()
        {
            InitializeComponent();

            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(
                    new MediaTypeWithQualityHeaderValue("application/json"));
            }
        }

        private async void Page_Loaded(object sender, RoutedEventArgs e)
        {
            await LoadUserProfile();
        }

        private async Task LoadUserProfile()
        {
            try
            {
                if (string.IsNullOrEmpty(App.AuthToken))
                {
                    CustomMessageBox.Show("Ошибка авторизации. Пожалуйста, войдите снова.", "Ошибка", CustomMessageBox.MessageType.Error);
                    NavigationService?.Navigate(new Auth());
                    return;
                }

                _httpClient.DefaultRequestHeaders.Authorization =
                    new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync("/api/users/me");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();

                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var userData = JsonSerializer.Deserialize<UserProfileData>(responseBody, options);

                    if (userData != null)
                    {
                        App.CurrentUserProfile = userData;
                        UpdateUIWithUserData();

                        if (Window.GetWindow(this) is MainWindow mainWindow)
                        {
                            mainWindow.UpdateUserMenu();
                        }
                    }
                }
                else
                {
                    if (response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
                    {
                        CustomMessageBox.Show("Сессия истекла. Пожалуйста, войдите снова.", "Ошибка авторизации", CustomMessageBox.MessageType.Error);
                        NavigationService?.Navigate(new Auth());
                    }
                    else
                    {
                        CustomMessageBox.Show($"Ошибка загрузки данных профиля: {response.StatusCode}", "Ошибка", CustomMessageBox.MessageType.Error);
                    }
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Произошла ошибка при загрузке профиля: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
        }

        private void UpdateUIWithUserData()
        {
            var data = App.CurrentUserProfile;
            if (data == null) return;

            try
            {
                NameTextBlock.Text = $"{data.surname} {data.name} {data.patronymic}".Trim();

                bool isCoordinator = data.roleTitle == "Coordinator" || data.roleTitle == "Sector_coordinator" || data.roleTitle == "Admin";

                if (isCoordinator)
                {
                    RoleArrow.Visibility = Visibility.Visible;
                    btnRoleSwitch.IsHitTestVisible = true;
                    RoleTextBlock.Text = App.IsActivistMode ? "Активист студсовета" : GetRoleDisplayName(data.roleTitle, data.coordinatorSector);
                }
                else
                {
                    RoleArrow.Visibility = Visibility.Collapsed;
                    btnRoleSwitch.IsHitTestVisible = false;
                    RoleTextBlock.Text = GetRoleDisplayName(data.roleTitle, data.coordinatorSector);
                }

                string course = data.courseNumber?.ToString() ?? "";
                string specAcronym = data.shortSpecialityTitle ?? "";
                string group = data.groupTitle ?? "";

                if (!string.IsNullOrEmpty(group))
                {
                    GroupTextBlock.Text = $"Группа: {course}{specAcronym}-{group}";
                }
                else
                {
                    GroupTextBlock.Text = "Группа: не указана";
                }

                EventsCountTextBlock.Text = data.events_count?.ToString() ?? "0";
                PointsCountTextBlock.Text = data.points_count?.ToString() ?? "0";
                RankTextBlock.Text = data.rank?.ToString() ?? "0";

                if (!string.IsNullOrEmpty(data.dateOfBirth))
                {
                    if (DateTime.TryParse(data.dateOfBirth, out DateTime birthDate))
                        BirthDateTextBlock.Text = birthDate.ToString("dd MMMM yyyy");
                    else
                        BirthDateTextBlock.Text = data.dateOfBirth;
                }
                else
                {
                    BirthDateTextBlock.Text = "Не указана";
                }

                GenderTextBlock.Text = !string.IsNullOrEmpty(data.gender) ? data.gender : "Не указан";
                StudentEmailTextBlock.Text = data.studentEmail ?? "Не указана";

                PhoneTextBlock.Text = FormatPhoneNumber(data.phoneNumber);

                AdditionalEmailTextBlock.Text = data.additionalEmail ?? "Не указана";
                VkLinkTextBlock.Text = data.vkLink ?? "Не указана";

                if (data.socialStatuses != null && data.socialStatuses.Count > 0)
                {
                    SocialStatusesTextBlock.Text = string.Join("\n", data.socialStatuses);
                }
                else
                {
                    SocialStatusesTextBlock.Text = "Отсутствуют";
                }

                if (!string.IsNullOrEmpty(data.photo))
                {
                    BitmapImage bmp = GetImageFromBase64(data.photo);
                    if (bmp != null)
                    {
                        ProfilePhoto.Fill = new ImageBrush(bmp) { Stretch = Stretch.UniformToFill };
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Error UI: {ex.Message}");
            }
        }

        private string FormatPhoneNumber(string phone)
        {
            if (string.IsNullOrWhiteSpace(phone)) return "Не указан";

            var digits = new string(phone.Where(char.IsDigit).ToArray());

            if (digits.Length == 11 && (digits.StartsWith("7") || digits.StartsWith("8")))
            {
                return $"+7 ({digits.Substring(1, 3)}) {digits.Substring(4, 3)}-{digits.Substring(7, 2)}-{digits.Substring(9, 2)}";
            }
            else if (digits.Length == 10)
            {
                return $"+7 ({digits.Substring(0, 3)}) {digits.Substring(3, 3)}-{digits.Substring(6, 2)}-{digits.Substring(8, 2)}";
            }

            return phone;
        }

        private string GetRoleDisplayName(string role, string sector)
        {
            if (role == "Sector_coordinator" || role == "Coordinator")
            {
                string display = "Координатор";
                if (!string.IsNullOrEmpty(sector))
                {
                    display += $" ({sector})";
                }
                return display;
            }
            else if (role == "Activist")
            {
                return "Активист студсовета";
            }
            else if (role == "Admin")
            {
                return "Администратор";
            }

            return !string.IsNullOrEmpty(role) ? role : "Студент";
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
            catch (Exception ex)
            {
                Debug.WriteLine($"Ошибка обработки фото: {ex.Message}");
            }
            return null;
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            NavigationService?.Navigate(new PointsHistory());
        }

        private void RoleSwitch_Click(object sender, RoutedEventArgs e)
        {
            RolePopup.IsOpen = true;
        }

        private void SetCoordinatorRole_Click(object sender, RoutedEventArgs e)
        {
            RolePopup.IsOpen = false;
            App.IsActivistMode = false; 
            UpdateUIWithUserData();

            if (Window.GetWindow(this) is MainWindow mainWindow)
            {
                mainWindow.UpdateUserMenu();
                NavigationService?.Navigate(new Diplom_Stud.Pages.Coordinator.CoordinatorHome());
            }
        }

        private void SetActivistRole_Click(object sender, RoutedEventArgs e)
        {
            RolePopup.IsOpen = false;
            App.IsActivistMode = true;
            UpdateUIWithUserData();

            if (Window.GetWindow(this) is MainWindow mainWindow)
            {
                mainWindow.UpdateUserMenu();
                NavigationService?.Navigate(new Diplom_Stud.Pages.Activist.Home());
            }
        }
    }

    public class UserProfileData
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public int? events_count { get; set; }
        public int? points_count { get; set; }
        public int? rank { get; set; }
        public string dateOfBirth { get; set; }
        public int? courseNumber { get; set; }
        public string specialityTitle { get; set; }
        public string shortSpecialityTitle { get; set; } 
        public string groupTitle { get; set; }
        public string studentEmail { get; set; }
        public string additionalEmail { get; set; }
        public string phoneNumber { get; set; }
        public string vkLink { get; set; }
        public string photo { get; set; }
        public string roleTitle { get; set; }
        public string gender { get; set; }
        public string coordinatorSector { get; set; }
        public List<string> socialStatuses { get; set; }
    }
}