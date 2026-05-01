using Diplom_Stud.Components;
using Diplom_Stud.Pages.Activist;
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
using System.Windows.Input;
using System.Windows.Media.Animation;
using System.Windows.Threading;

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
                _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            }
        }

        private void NumericOnly_PreviewTextInput(object sender, TextCompositionEventArgs e)
        {
            e.Handled = !e.Text.All(char.IsDigit);
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
            if (_slideTimer != null)
            {
                _slideTimer.Stop();
            }
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

            DoubleAnimation fadeOut = new DoubleAnimation { From = 1, To = 0, Duration = TimeSpan.FromSeconds(1) };
            DoubleAnimation fadeIn = new DoubleAnimation { From = 0, To = 1, Duration = TimeSpan.FromSeconds(1) };

            fromSlide.BeginAnimation(OpacityProperty, fadeOut);
            toSlide.BeginAnimation(OpacityProperty, fadeIn);
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

            if (activeIndex == 0) Indicator1.Fill = System.Windows.Media.Brushes.MediumPurple;
            else if (activeIndex == 1) Indicator2.Fill = System.Windows.Media.Brushes.MediumPurple;
            else if (activeIndex == 2) Indicator3.Fill = System.Windows.Media.Brushes.MediumPurple;
        }

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
            tbVisiblePassword.Focus();
        }

        private void btnTogglePassword_Unchecked(object sender, RoutedEventArgs e)
        {
            tbVisiblePassword.Visibility = Visibility.Collapsed;
            pbPassword.Visibility = Visibility.Visible;
            pbPassword.Focus();
        }

        private void PasswordBox_GotFocus(object sender, RoutedEventArgs e)
        {
            UpdatePasswordPlaceholder();
        }

        private void PasswordBox_LostFocus(object sender, RoutedEventArgs e)
        {
            UpdatePasswordPlaceholder();
        }

        private void UpdatePasswordPlaceholder()
        {
            bool hasText = !string.IsNullOrEmpty(pbPassword.Password);
            bool isFocused = pbPassword.IsFocused || tbVisiblePassword.IsFocused || btnTogglePassword.IsFocused;
            tbPasswordPlaceholder.Visibility = (hasText || isFocused) ? Visibility.Collapsed : Visibility.Visible;
        }

        private async void Button_Click(object sender, RoutedEventArgs e)
        {
            tbLoginError.Visibility = Visibility.Collapsed;
            tbPasswordError.Visibility = Visibility.Collapsed;

            string studentId = tbLogin.Text.Trim();
            string password = pbPassword.Password;

            bool hasError = false;

            if (studentId.Length != 6)
            {
                tbLoginError.Visibility = Visibility.Visible;
                hasError = true;
            }

            if (string.IsNullOrEmpty(password))
            {
                tbPasswordError.Visibility = Visibility.Visible;
                hasError = true;
            }

            if (hasError) return;

            string domain = "";
            if (cbDomain.SelectedItem is ComboBoxItem selectedItem)
            {
                domain = selectedItem.Content.ToString();
            }
            string fullEmail = studentId + domain;

            var button = sender as Button;
            button.IsEnabled = false;
            button.Content = "Вход...";

            try
            {
                bool success = await AuthenticateUser(fullEmail, password);
                if (success)
                {
                    NavigateToUserProfile();
                }
                else
                {
                    CustomMessageBox.Show("Неверный номер билета или пароль!", "Ошибка входа", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show(ex.Message, "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                button.IsEnabled = true;
                button.Content = "Войти";
            }
        }

        private void NavigateToUserProfile()
        {
            if (NavigationService != null)
            {
                NavigationService.Navigate(new Profile());
            }
        }

        private async Task<bool> AuthenticateUser(string email, string password)
        {
            try
            {
                var loginData = new { email, password };
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

                        _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue(authResponse.type, authResponse.token);
                        return true;
                    }
                }
                return false;
            }
            catch
            {
                throw new Exception("Не удалось подключиться к серверу.");
            }
        }

        private void RegisterText_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (NavigationService != null)
            {
                NavigationService.Navigate(new Reg());
            }
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