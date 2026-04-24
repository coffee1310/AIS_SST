using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Windows.Threading;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Diagnostics;
using Diplom_Stud.Pages.Activist;

namespace Diplom_Stud.Pages.General
{
    public partial class Auth : Page
    {
        private DispatcherTimer _slideTimer;
        private int _currentSlideIndex = 0;
        private readonly int _totalSlides = 3;

        private static readonly HttpClient _httpClient = new HttpClient(); 

        private bool _isUpdatingPassword = false;

        public Auth()
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

        private void Page_Loaded(object sender, RoutedEventArgs e)
        {
            DoubleAnimation fadeInAnimation = new DoubleAnimation
            {
                From = 0.0,
                To = 1.0,
                Duration = TimeSpan.FromSeconds(1.0),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };
            this.BeginAnimation(Page.OpacityProperty, fadeInAnimation);
            _slideTimer = new DispatcherTimer();
            _slideTimer.Interval = TimeSpan.FromSeconds(5);
            _slideTimer.Tick += SlideTimer_Tick;
            _slideTimer.Start();
        }

        private void Page_Unloaded(object sender, RoutedEventArgs e)
        {
            _slideTimer?.Stop();
        }

        private void SlideTimer_Tick(object sender, EventArgs e)
        {
            int nextSlideIndex = (_currentSlideIndex + 1) % _totalSlides;
            AnimateSlideTransition(_currentSlideIndex, nextSlideIndex);
            _currentSlideIndex = nextSlideIndex;
        }

        private void AnimateSlideTransition(int fromIndex, int toIndex)
        {
            Border fromSlide = GetSlideByIndex(fromIndex);
            Border toSlide = GetSlideByIndex(toIndex);

            if (fromSlide == null || toSlide == null) return;

            DoubleAnimation fromAnimation = new DoubleAnimation
            {
                From = 1,
                To = 0,
                Duration = TimeSpan.FromSeconds(1),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseInOut }
            };

            DoubleAnimation toAnimation = new DoubleAnimation
            {
                From = 0,
                To = 1,
                Duration = TimeSpan.FromSeconds(1),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseInOut }
            };

            fromSlide.BeginAnimation(UIElement.OpacityProperty, fromAnimation);
            toSlide.BeginAnimation(UIElement.OpacityProperty, toAnimation);

            UpdateIndicators(toIndex);
        }
        private Border GetSlideByIndex(int index)
        {
            switch (index)
            {
                case 0: return SlideImage1;
                case 1: return SlideImage2;
                case 2: return SlideImage3;
                default: return null;
            }
        }

        private void UpdateIndicators(int activeIndex)
        {
            Indicator1.Fill = System.Windows.Media.Brushes.Gray;
            Indicator2.Fill = System.Windows.Media.Brushes.Gray;
            Indicator3.Fill = System.Windows.Media.Brushes.Gray;

            switch (activeIndex)
            {
                case 0:
                    Indicator1.Fill = System.Windows.Media.Brushes.MediumPurple;
                    break;
                case 1:
                    Indicator2.Fill = System.Windows.Media.Brushes.MediumPurple;
                    break;
                case 2:
                    Indicator3.Fill = System.Windows.Media.Brushes.MediumPurple;
                    break;
            }
        }

        // ==============================================
        // ЛОГИКА ГЛАЗИКА И ПОЛЯ ПАРОЛЯ
        // ==============================================

        private void PasswordBox_PasswordChanged(object sender, RoutedEventArgs e)
        {
            if (!_isUpdatingPassword)
            {
                _isUpdatingPassword = true;
                tbVisiblePassword.Text = pbPassword.Password;
                _isUpdatingPassword = false;
            }
            UpdatePasswordPlaceholder();
        }

        private void VisiblePassword_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (!_isUpdatingPassword)
            {
                _isUpdatingPassword = true;
                pbPassword.Password = tbVisiblePassword.Text;
                _isUpdatingPassword = false;
            }
            UpdatePasswordPlaceholder();
        }

        private void btnTogglePassword_Checked(object sender, RoutedEventArgs e)
        {
            pbPassword.Visibility = Visibility.Collapsed;
            tbVisiblePassword.Visibility = Visibility.Visible;
            tbVisiblePassword.Focus(); // Оставляем фокус на текстовом поле
        }

        private void btnTogglePassword_Unchecked(object sender, RoutedEventArgs e)
        {
            tbVisiblePassword.Visibility = Visibility.Collapsed;
            pbPassword.Visibility = Visibility.Visible;
            pbPassword.Focus();
        }

        private void PasswordBox_GotFocus(object sender, RoutedEventArgs e) => UpdatePasswordPlaceholder();
        private void PasswordBox_LostFocus(object sender, RoutedEventArgs e) => UpdatePasswordPlaceholder();

        private void UpdatePasswordPlaceholder()
        {
            bool hasText = !string.IsNullOrEmpty(pbPassword.Password);
            // Если текст есть ИЛИ хоть один из элементов (текстовое поле, пароль или кнопка глазика) в фокусе - прячем плейсхолдер
            bool isFocused = pbPassword.IsFocused || tbVisiblePassword.IsFocused || btnTogglePassword.IsFocused;

            tbPasswordPlaceholder.Visibility = (hasText || isFocused) ? Visibility.Collapsed : Visibility.Visible;
        }

        // ==============================================
        // АВТОРИЗАЦИЯ
        // ==============================================

        private async void Button_Click(object sender, RoutedEventArgs e)
        {
            // Сбрасываем старые ошибки при новом клике
            tbLoginError.Visibility = Visibility.Collapsed;
            tbPasswordError.Visibility = Visibility.Collapsed;

            string login = tbLogin.Text.Trim();
            string password = pbPassword.Password; // Всегда читаем отсюда, т.к. поля синхронизированы

            bool hasError = false;

            // Проверка логина
            if (string.IsNullOrEmpty(login))
            {
                tbLoginError.Visibility = Visibility.Visible;
                hasError = true;
            }

            // Проверка пароля
            if (string.IsNullOrEmpty(password))
            {
                tbPasswordError.Visibility = Visibility.Visible;
                hasError = true;
            }

            // Если есть хоть одна ошибка - прерываем выполнение
            if (hasError) return;

            var button = sender as Button;
            button.IsEnabled = false;
            button.Content = "Вход...";

            try
            {
                bool success = await AuthenticateUser(login, password);

                if (success)
                {
                    NavigateToUserProfile();
                }
                else
                {
                    MessageBox.Show("Неверный email или пароль!", "Ошибка авторизации",
                        MessageBoxButton.OK, MessageBoxImage.Error);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Ошибка подключения к серверу: {ex.Message}",
                    "Ошибка", MessageBoxButton.OK, MessageBoxImage.Error);
            }
            finally
            {
                button.IsEnabled = true;
                button.Content = "Войти";
            }
        }

        private void NavigateToUserProfile()
        {
            if (App.CurrentUser?.Roles != null)
            {
                foreach (var role in App.CurrentUser.Roles)
                {
                    switch (role.ToLower())
                    {
                        case "activist":
                            NavigationService?.Navigate(new Profile());
                            return;
                        case "admin":
                            // NavigationService?.Navigate(new Admin.Profile());
                            return;
                        case "teacher":
                            // NavigationService?.Navigate(new Teacher.Profile());
                            return;
                    }
                }
            }

            NavigationService?.Navigate(new Profile());
        }

        private async Task<bool> AuthenticateUser(string email, string password)
        {
            try
            {
                var loginData = new
                {
                    email = email,
                    password = password
                };

                string jsonData = JsonSerializer.Serialize(loginData);
                var content = new StringContent(jsonData, Encoding.UTF8, "application/json");

                HttpResponseMessage response = await _httpClient.PostAsync("/api/auth/login", content);

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();

                    var authResponse = JsonSerializer.Deserialize<AuthResponse>(responseBody);

                    if (authResponse != null && !string.IsNullOrEmpty(authResponse.token))
                    {
                        App.AuthToken = authResponse.token;

                        App.CurrentUser = new UserData
                        {
                            Id = authResponse.id,
                            Email = authResponse.email,
                            Name = authResponse.name,
                            Surname = authResponse.surname,
                            Roles = authResponse.roles,
                            Token = authResponse.token,
                            TokenType = authResponse.type
                        };

                        _httpClient.DefaultRequestHeaders.Authorization =
                            new AuthenticationHeaderValue(authResponse.type, authResponse.token);

                        Debug.WriteLine($"Пользователь {authResponse.name} {authResponse.surname} успешно авторизован");
                        Debug.WriteLine($"Токен: {authResponse.token}");
                        Debug.WriteLine($"Роли: {string.Join(", ", authResponse.roles)}");

                        return true;
                    }
                }
                else
                {
                    string errorBody = await response.Content.ReadAsStringAsync();
                    Debug.WriteLine($"Ошибка авторизации: {response.StatusCode} - {errorBody}");
                }

                return false;
            }
            catch (HttpRequestException ex)
            {
                Debug.WriteLine($"Ошибка HTTP запроса: {ex.Message}");
                throw new Exception($"Не удалось подключиться к серверу {App.ApiBaseUrl}. Проверьте подключение.");
            }
            catch (JsonException ex)
            {
                Debug.WriteLine($"Ошибка обработки JSON: {ex.Message}");
                throw new Exception("Ошибка обработки данных от сервера.");
            }
        }

        private void RegisterText_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            NavigationService?.Navigate(new Reg());
        }
    }

    public class AuthResponse
    {
        public string token { get; set; }
        public string type { get; set; }
        public int id { get; set; }
        public string email { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public List<string> roles { get; set; }
    }
}