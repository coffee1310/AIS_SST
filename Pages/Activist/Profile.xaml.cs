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
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace Diplom_Stud.Pages.Activist
{
    /// <summary>
    /// Логика взаимодействия для Profile.xaml
    /// </summary>
    public partial class Profile : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private UserProfileData _userProfileData;

        public Profile()
        {
            InitializeComponent();

            _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
            _httpClient.DefaultRequestHeaders.Accept.Clear();
            _httpClient.DefaultRequestHeaders.Accept.Add(
                new MediaTypeWithQualityHeaderValue("application/json"));
        }

        private async void Page_Loaded(object sender, RoutedEventArgs e)
        {
            await LoadUserProfile();
        }

        private async Task LoadUserProfile()
        {
            try
            {
                // Check if we have a token
                if (string.IsNullOrEmpty(App.AuthToken))
                {
                    MessageBox.Show("Ошибка авторизации. Пожалуйста, войдите снова.",
                        "Ошибка", MessageBoxButton.OK, MessageBoxImage.Error);
                    NavigationService?.Navigate(new Auth());
                    return;
                }

                // Set the authorization header
                _httpClient.DefaultRequestHeaders.Authorization =
                    new AuthenticationHeaderValue("Bearer", App.AuthToken);

                // Make the GET request
                HttpResponseMessage response = await _httpClient.GetAsync("/api/users");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    _userProfileData = JsonSerializer.Deserialize<UserProfileData>(responseBody);

                    if (_userProfileData != null)
                    {
                        // Update UI with received data
                        UpdateUIWithUserData();
                    }
                }
                else
                {
                    string errorBody = await response.Content.ReadAsStringAsync();
                    Debug.WriteLine($"Ошибка загрузки профиля: {response.StatusCode} - {errorBody}");

                    if (response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
                    {
                        MessageBox.Show("Сессия истекла. Пожалуйста, войдите снова.",
                            "Ошибка авторизации", MessageBoxButton.OK, MessageBoxImage.Warning);
                        NavigationService?.Navigate(new Auth());
                    }
                    else
                    {
                        MessageBox.Show($"Ошибка загрузки данных профиля: {response.StatusCode}",
                            "Ошибка", MessageBoxButton.OK, MessageBoxImage.Error);
                    }
                }
            }
            catch (HttpRequestException ex)
            {
                Debug.WriteLine($"Ошибка HTTP запроса: {ex.Message}");
                MessageBox.Show($"Не удалось подключиться к серверу {App.ApiBaseUrl}. Проверьте подключение.",
                    "Ошибка подключения", MessageBoxButton.OK, MessageBoxImage.Error);
            }
            catch (JsonException ex)
            {
                Debug.WriteLine($"Ошибка обработки JSON: {ex.Message}");
                MessageBox.Show("Ошибка обработки данных от сервера.",
                    "Ошибка", MessageBoxButton.OK, MessageBoxImage.Error);
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Неожиданная ошибка: {ex.Message}");
                MessageBox.Show($"Произошла ошибка при загрузке профиля: {ex.Message}",
                    "Ошибка", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private void UpdateUIWithUserData()
        {
            try
            {
                // Find the TextBlock elements in the visual tree
                var nameTextBlock = FindNameInVisualTree<TextBlock>(this, "NameTextBlock");
                var groupTextBlock = FindNameInVisualTree<TextBlock>(this, "GroupTextBlock");
                var eventsCountTextBlock = FindNameInVisualTree<TextBlock>(this, "EventsCountTextBlock");
                var pointsCountTextBlock = FindNameInVisualTree<TextBlock>(this, "PointsCountTextBlock");
                var rankTextBlock = FindNameInVisualTree<TextBlock>(this, "RankTextBlock");
                var birthDateTextBlock = FindNameInVisualTree<TextBlock>(this, "BirthDateTextBlock");
                var studentIdTextBlock = FindNameInVisualTree<TextBlock>(this, "StudentIdTextBlock");
                var socialStatusTextBlock = FindNameInVisualTree<TextBlock>(this, "SocialStatusTextBlock");
                var genderTextBlock = FindNameInVisualTree<TextBlock>(this, "GenderTextBlock");
                var studentEmailTextBlock = FindNameInVisualTree<TextBlock>(this, "StudentEmailTextBlock");
                var phoneTextBlock = FindNameInVisualTree<TextBlock>(this, "PhoneTextBlock");
                var additionalEmailTextBlock = FindNameInVisualTree<TextBlock>(this, "AdditionalEmailTextBlock");
                var vkLinkTextBlock = FindNameInVisualTree<TextBlock>(this, "VkLinkTextBlock");

                // Update name (Full name: surname + name + patronymic)
                string fullName = $"{_userProfileData.surname} {_userProfileData.name} {_userProfileData.patronymic}";
                if (nameTextBlock != null) nameTextBlock.Text = fullName;

                // Update group info
                string groupInfo = _userProfileData.group != null ? $"Группа: {_userProfileData.group}" : "Группа: не указана";
                if (groupTextBlock != null) groupTextBlock.Text = groupInfo;

                // Update statistics cards
                if (eventsCountTextBlock != null)
                    eventsCountTextBlock.Text = _userProfileData.events_count?.ToString() ?? "0";

                if (pointsCountTextBlock != null)
                    pointsCountTextBlock.Text = _userProfileData.points_count?.ToString() ?? "0";

                if (rankTextBlock != null)
                    rankTextBlock.Text = _userProfileData.rank?.ToString() ?? "0";

                // Update personal information
                if (birthDateTextBlock != null && _userProfileData.dateOfBirth != null)
                {
                    // Format date to Russian format
                    if (DateTime.TryParse(_userProfileData.dateOfBirth, out DateTime birthDate))
                    {
                        birthDateTextBlock.Text = birthDate.ToString("dd MMMM yyyy");
                    }
                    else
                    {
                        birthDateTextBlock.Text = _userProfileData.dateOfBirth;
                    }
                }

                if (studentIdTextBlock != null)
                    studentIdTextBlock.Text = _userProfileData.id.ToString();

                if (socialStatusTextBlock != null)
                    socialStatusTextBlock.Text = _userProfileData.socialStatus ?? "Не указан";

                if (genderTextBlock != null)
                {
                    // You might want to add gender field to your API response
                    genderTextBlock.Text = "Не указан";
                }

                if (studentEmailTextBlock != null)
                    studentEmailTextBlock.Text = _userProfileData.studentEmail ?? "Не указана";

                if (phoneTextBlock != null)
                    phoneTextBlock.Text = _userProfileData.phoneNumber ?? "Не указан";

                if (additionalEmailTextBlock != null)
                    additionalEmailTextBlock.Text = _userProfileData.additionalEmail ?? "Не указана";

                if (vkLinkTextBlock != null)
                    vkLinkTextBlock.Text = _userProfileData.vkLink ?? "Не указана";

                // Update photo if available
                if (_userProfileData.photo != null && !string.IsNullOrEmpty(_userProfileData.photo))
                {
                    try
                    {
                        var photoEllipse = FindNameInVisualTree<System.Windows.Shapes.Ellipse>(this, "ProfilePhoto");
                        if (photoEllipse != null)
                        {
                            var imageBrush = new ImageBrush();
                            // Assuming photo is base64 string or URL
                            // You may need to convert base64 to BitmapImage if the API returns base64
                            // imageBrush.ImageSource = ...;
                            photoEllipse.Fill = imageBrush;
                        }
                    }
                    catch (Exception ex)
                    {
                        Debug.WriteLine($"Error loading photo: {ex.Message}");
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Error updating UI: {ex.Message}");
            }
        }

        // Helper method to find elements in the visual tree
        private T FindNameInVisualTree<T>(DependencyObject parent, string name) where T : FrameworkElement
        {
            if (parent == null) return null;

            // Check if the current element has the name we're looking for
            if (parent is FrameworkElement element && element.Name == name)
            {
                return element as T;
            }

            // Recursively search children
            for (int i = 0; i < VisualTreeHelper.GetChildrenCount(parent); i++)
            {
                var child = VisualTreeHelper.GetChild(parent, i);
                var result = FindNameInVisualTree<T>(child, name);
                if (result != null)
                {
                    return result;
                }
            }

            return null;
        }

        private async void Button_Click(object sender, RoutedEventArgs e)
        {
            // Navigate to history page
            // NavigationService?.Navigate(new HistoryPage());
            MessageBox.Show("Функция в разработке", "Информация",
                MessageBoxButton.OK, MessageBoxImage.Information);
        }
    }

    // Data model for user profile
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
        public string speciality { get; set; }
        public string group { get; set; }
        public string socialStatus { get; set; }
        public string studentEmail { get; set; }
        public string additionalEmail { get; set; }
        public string phoneNumber { get; set; }
        public string vkLink { get; set; }
        public string photo { get; set; }
        public string role { get; set; }
    }
}