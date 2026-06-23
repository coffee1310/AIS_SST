using Diplom_Stud.Components;
using System;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace Diplom_Stud.Pages.General
{
    public partial class ForgotPassword : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private string _userEmail = string.Empty;
        private string _resetCode = string.Empty;

        public ForgotPassword()
        {
            InitializeComponent();

            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            }
        }

        private async void BtnSendCode_Click(object sender, RoutedEventArgs e)
        {
            HideError();
            string email = tbEmail.Text.Trim();

            if (string.IsNullOrEmpty(email) || !IsValidEmail(email))
            {
                ShowError("Введите корректный адрес электронной почты.");
                return;
            }

            _userEmail = email;
            btnSendCode.IsEnabled = false;
            btnSendCode.Content = "Отправка...";

            try
            {
                var payload = new { email = _userEmail };
                var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

                HttpResponseMessage res = await _httpClient.PostAsync("/api/auth/password-reset/request", content);

                if (res.StatusCode == System.Net.HttpStatusCode.MethodNotAllowed || res.StatusCode == System.Net.HttpStatusCode.UnsupportedMediaType)
                {
                    res = await _httpClient.PostAsync($"/api/auth/password-reset/request?email={Uri.EscapeDataString(_userEmail)}", null);
                }

                if (res.IsSuccessStatusCode)
                {
                    step1Panel.Visibility = Visibility.Collapsed;
                    step2Panel.Visibility = Visibility.Visible;
                    tbSubtitle.Text = "Код отправлен на вашу почту. Введите его ниже.";
                }
                else
                {
                    string err = await res.Content.ReadAsStringAsync();
                    ShowError($"Не удалось отправить код:\n{err}");
                }
            }
            catch (Exception ex)
            {
                ShowError($"Сбой сети: {ex.Message}");
            }
            finally
            {
                btnSendCode.IsEnabled = true;
                btnSendCode.Content = "Отправить код";
            }
        }

        private void BtnVerifyCode_Click(object sender, RoutedEventArgs e)
        {
            HideError();
            string code = tbCode.Text.Trim();

            if (string.IsNullOrWhiteSpace(code))
            {
                ShowError("Пожалуйста, введите код из письма.");
                return;
            }

            _resetCode = code;
            step2Panel.Visibility = Visibility.Collapsed;
            step3Panel.Visibility = Visibility.Visible;
            tbSubtitle.Text = "Придумайте новый надежный пароль.";
        }

        private async void BtnResetPassword_Click(object sender, RoutedEventArgs e)
        {
            HideError();
            string newPassword = pbNewPassword.Password;
            string confirmPassword = pbConfirmPassword.Password;

            if (!IsPasswordValid(newPassword))
            {
                ShowError("Пароль должен содержать минимум 8 символов, заглавные и строчные буквы, а также цифры.");
                return;
            }

            if (newPassword != confirmPassword)
            {
                ShowError("Пароли не совпадают.");
                return;
            }

            btnResetPassword.IsEnabled = false;
            btnResetPassword.Content = "Сохранение...";

            try
            {
                var payload = new
                {
                    email = _userEmail,
                    code = _resetCode,
                    newPassword = newPassword
                };
                var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

                HttpResponseMessage response = await _httpClient.PostAsync("/api/auth/password-reset/verify", content);

                if (response.IsSuccessStatusCode)
                {
                    CustomMessageBox.Show("Пароль успешно изменен! Теперь вы можете войти.", "Успех", CustomMessageBox.MessageType.Success);
                    NavigationService?.Navigate(new Auth());
                }
                else
                {
                    string err = await response.Content.ReadAsStringAsync();
                    ShowError($"Ошибка сброса пароля:\n{err}");
                }
            }
            catch (Exception ex)
            {
                ShowError($"Ошибка сети: {ex.Message}");
            }
            finally
            {
                btnResetPassword.IsEnabled = true;
                btnResetPassword.Content = "Сменить пароль";
            }
        }

        private void LoginText_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            NavigationService?.Navigate(new Auth());
        }

        private void ShowError(string message)
        {
            txtError.Text = message;
            txtError.Visibility = Visibility.Visible;
        }

        private void HideError()
        {
            txtError.Visibility = Visibility.Collapsed;
        }

        private bool IsValidEmail(string email)
        {
            return Regex.IsMatch(email, @"^[^@\s]+@[^@\s]+\.[^@\s]+$");
        }

        private bool IsPasswordValid(string password)
        {
            return Regex.IsMatch(password, @"^(?=.*[a-zа-я])(?=.*[A-ZА-Я])(?=.*\d).{8,}$");
        }

        private void Code_PreviewTextInput(object sender, TextCompositionEventArgs e)
        {
            e.Handled = !e.Text.All(char.IsDigit);
        }

        private void NewPassword_PasswordChanged(object sender, RoutedEventArgs e) => UpdateNewPasswordPlaceholder();
        private void NewPassword_GotFocus(object sender, RoutedEventArgs e) => UpdateNewPasswordPlaceholder();
        private void NewPassword_LostFocus(object sender, RoutedEventArgs e) => UpdateNewPasswordPlaceholder();

        private void ConfirmPassword_PasswordChanged(object sender, RoutedEventArgs e) => UpdateConfirmPasswordPlaceholder();
        private void ConfirmPassword_GotFocus(object sender, RoutedEventArgs e) => UpdateConfirmPasswordPlaceholder();
        private void ConfirmPassword_LostFocus(object sender, RoutedEventArgs e) => UpdateConfirmPasswordPlaceholder();

        private void UpdateNewPasswordPlaceholder()
        {
            bool hasText = !string.IsNullOrEmpty(pbNewPassword.Password);
            bool isFocused = pbNewPassword.IsFocused;
            tbNewPassPlaceholder.Visibility = (hasText || isFocused) ? Visibility.Collapsed : Visibility.Visible;
        }

        private void UpdateConfirmPasswordPlaceholder()
        {
            bool hasText = !string.IsNullOrEmpty(pbConfirmPassword.Password);
            bool isFocused = pbConfirmPassword.IsFocused;
            tbConfirmPassPlaceholder.Visibility = (hasText || isFocused) ? Visibility.Collapsed : Visibility.Visible;
        }
    }
}