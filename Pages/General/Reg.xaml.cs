using Diplom_Stud.Components;
using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;

namespace Diplom_Stud.Pages.General
{
    public partial class Reg : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();

        private List<int> _selectedStatuses = new List<int>();
        private bool _isUpdatingPassword = false;
        private bool _isUpdatingConfirmPassword = false;
        private byte[] _selectedImageBytes = null;

        public Reg()
        {
            InitializeComponent();

            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(
                    new MediaTypeWithQualityHeaderValue("application/json"));
            }

            this.Loaded += Page_Loaded;

            cbGender.SelectedIndex = 0;
            cbCourse.SelectedIndex = 0;
            cbSpeciality.SelectedIndex = 0;

            cbGroup.AddHandler(TextBoxBase.TextChangedEvent, new TextChangedEventHandler(ComboBox_TextChanged));
        }

        private async void Page_Loaded(object sender, RoutedEventArgs e)
        {
            DoubleAnimation fadeInAnimation = new DoubleAnimation
            {
                From = 0.0,
                To = 1.0,
                Duration = TimeSpan.FromSeconds(1.0),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };
            this.BeginAnimation(Page.OpacityProperty, fadeInAnimation);

            await LoadDictionariesAsync();
        }

        private void ComboBox_TextChanged(object sender, TextChangedEventArgs e)
        {
            var cb = (ComboBox)sender;
            var tb = e.OriginalSource as TextBox;

            if (tb != null && cb.IsEditable && tb.IsFocused)
            {
                string text = tb.Text;
                cb.Items.Filter = item =>
                {
                    if (string.IsNullOrEmpty(text)) return true;
                    var cbi = item as ComboBoxItem;
                    if (cbi == null) return true;
                    return cbi.Content.ToString().IndexOf(text, StringComparison.OrdinalIgnoreCase) >= 0;
                };
                cb.IsDropDownOpen = true;
            }
        }

        private async Task LoadDictionariesAsync()
        {
            try
            {
                var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                var statResp = await _httpClient.GetAsync("/api/social_status");
                if (statResp.IsSuccessStatusCode)
                {
                    var statJson = await statResp.Content.ReadAsStringAsync();
                    var statuses = JsonSerializer.Deserialize<List<SocialStatusDto>>(statJson, options);
                    icStatusList.ItemsSource = statuses;
                }

                var specResp = await _httpClient.GetAsync("/api/specialities");
                if (specResp.IsSuccessStatusCode)
                {
                    var specJson = await specResp.Content.ReadAsStringAsync();
                    var specialities = JsonSerializer.Deserialize<List<SpecialityDto>>(specJson, options);
                    if (specialities != null)
                    {
                        foreach (var spec in specialities)
                        {
                            cbSpeciality.Items.Add(new ComboBoxItem { Content = spec.title, Tag = spec.id, Foreground = Brushes.White });
                        }
                    }
                }

                var groupResp = await _httpClient.GetAsync("/api/group");
                if (groupResp.IsSuccessStatusCode)
                {
                    var groupJson = await groupResp.Content.ReadAsStringAsync();
                    var groups = JsonSerializer.Deserialize<List<GroupDto>>(groupJson, options);
                    if (groups != null)
                    {
                        foreach (var group in groups)
                        {
                            cbGroup.Items.Add(new ComboBoxItem { Content = group.title, Tag = group.id, Foreground = Brushes.White });
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Не удалось загрузить списки.\n{ex.Message}", "Ошибка загрузки", CustomMessageBox.MessageType.Error);
            }
        }

        private void Capitalize_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (sender is TextBox tb && !string.IsNullOrWhiteSpace(tb.Text))
            {
                if (char.IsLower(tb.Text[0]))
                {
                    int caretIndex = tb.CaretIndex;
                    tb.Text = char.ToUpper(tb.Text[0]) + tb.Text.Substring(1);
                    tb.CaretIndex = caretIndex;
                }
            }
        }

        private void NumericOnly_PreviewTextInput(object sender, TextCompositionEventArgs e)
        {
            e.Handled = !e.Text.All(char.IsDigit);
        }

        private bool _isFormattingPhone = false;
        private void Phone_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (_isFormattingPhone) return;
            _isFormattingPhone = true;

            TextBox tb = sender as TextBox;
            string digits = new string(tb.Text.Where(char.IsDigit).ToArray());

            if (digits.StartsWith("7") || digits.StartsWith("8"))
                digits = digits.Substring(1);

            if (digits.Length > 10) digits = digits.Substring(0, 10);

            string formatted = "+7";
            if (digits.Length > 0)
            {
                formatted += $" ({digits.Substring(0, Math.Min(3, digits.Length))}";
                if (digits.Length >= 3)
                {
                    formatted += $") {digits.Substring(3, Math.Min(3, digits.Length - 3))}";
                    if (digits.Length >= 6)
                    {
                        formatted += $"-{digits.Substring(6, Math.Min(2, digits.Length - 6))}";
                        if (digits.Length >= 8)
                        {
                            formatted += $"-{digits.Substring(8)}";
                        }
                    }
                }
            }
            else if (tb.Text.Contains("+") || tb.Text.Contains("7") || tb.Text.Contains("8"))
            {
                formatted = "+7 ";
            }
            else
            {
                formatted = "";
            }

            tb.Text = formatted;
            tb.CaretIndex = tb.Text.Length;

            _isFormattingPhone = false;
        }

        private void StatusCheck_Click(object sender, RoutedEventArgs e)
        {
            var cb = sender as CheckBox;
            int id = (int)cb.Tag;

            if (cb.IsChecked == true) _selectedStatuses.Add(id);
            else _selectedStatuses.Remove(id);

            var toggleTemplate = btnStatusToggle.Template;
            var txtDisplay = (TextBlock)toggleTemplate.FindName("txtStatusDisplay", btnStatusToggle);

            if (txtDisplay != null)
            {
                if (_selectedStatuses.Count > 0)
                {
                    txtDisplay.Text = $"Выбрано: {_selectedStatuses.Count}";
                    txtDisplay.Foreground = Brushes.White;
                }
                else
                {
                    txtDisplay.Text = "Социальный статус";
                    txtDisplay.Foreground = (Brush)new BrushConverter().ConvertFrom("#7A7886");
                }
            }
        }

        private void SelectPhoto_Click(object sender, RoutedEventArgs e)
        {
            string msg = "Внимание: пожалуйста, отнеситесь к выбору снимка ответственно! Если фотография не будет соответствовать правилам ниже, ваша заявка на вступление может быть отклонена. Загрузите подходящее фото сразу, чтобы процесс регистрации прошел быстро и без лишних возвратов.\n\n"
                       + "Требования к снимку:\n"
                       + "• Формат: цветная фотография 3х4 (без белого уголка).\n"
                       + "• Фон: строго белый и однотонный. В кадре не должно быть теней, полос, узоров или посторонних предметов.\n"
                       + "• Поза: строго анфас. Лицо открыто.\n"
                       + "• Пропорции: лицо 70-80% площади всей фотографии.\n"
                       + "• Одежда: однотонная, чтобы не сливаться с фоном.";

            CustomMessageBox.Show(msg, "Требования к фотографии", CustomMessageBox.MessageType.Info);

            OpenFileDialog openFileDialog = new OpenFileDialog();
            openFileDialog.Filter = "Image files (*.png;*.jpeg;*.jpg)|*.png;*.jpeg;*.jpg|All files (*.*)|*.*";
            openFileDialog.Title = "Выберите фотографию";

            if (openFileDialog.ShowDialog() == true)
            {
                try
                {
                    byte[] compressedBytes = CompressAndResizeImage(openFileDialog.FileName);
                    long maxImageSizeBytes = 2 * 1024 * 1024;

                    if (compressedBytes.Length > maxImageSizeBytes)
                    {
                        CustomMessageBox.Show("Файл слишком большой даже после сжатия. Выберите фото размером до 2 МБ.", "Ошибка", CustomMessageBox.MessageType.Error);
                        tbPhoto.Text = "";
                        _selectedImageBytes = null;
                    }
                    else
                    {
                        tbPhoto.Text = openFileDialog.FileName;
                        _selectedImageBytes = compressedBytes;
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

        private void pbPassword_PasswordChanged(object sender, RoutedEventArgs e)
        {
            if (!_isUpdatingPassword)
            {
                _isUpdatingPassword = true;
                tbVisiblePassword.Text = pbPassword.Password;
                _isUpdatingPassword = false;
            }
            UpdatePasswordPlaceholder();
        }

        private void tbVisiblePassword_TextChanged(object sender, TextChangedEventArgs e)
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

        private void Password_GotFocus(object sender, RoutedEventArgs e) => UpdatePasswordPlaceholder();
        private void Password_LostFocus(object sender, RoutedEventArgs e) => UpdatePasswordPlaceholder();

        private void UpdatePasswordPlaceholder()
        {
            bool hasText = !string.IsNullOrEmpty(pbPassword.Password);
            bool isFocused = pbPassword.IsFocused || tbVisiblePassword.IsFocused || btnTogglePassword.IsFocused;
            WatermarkPassword.Visibility = (hasText || isFocused) ? Visibility.Collapsed : Visibility.Visible;
        }

        private void pbConfirmPassword_PasswordChanged(object sender, RoutedEventArgs e)
        {
            if (!_isUpdatingConfirmPassword)
            {
                _isUpdatingConfirmPassword = true;
                tbVisibleConfirmPassword.Text = pbConfirmPassword.Password;
                _isUpdatingConfirmPassword = false;
            }
            UpdateConfirmPasswordPlaceholder();
        }

        private void tbVisibleConfirmPassword_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (!_isUpdatingConfirmPassword)
            {
                _isUpdatingConfirmPassword = true;
                pbConfirmPassword.Password = tbVisibleConfirmPassword.Text;
                _isUpdatingConfirmPassword = false;
            }
            UpdateConfirmPasswordPlaceholder();
        }

        private void btnToggleConfirmPassword_Checked(object sender, RoutedEventArgs e)
        {
            pbConfirmPassword.Visibility = Visibility.Collapsed;
            tbVisibleConfirmPassword.Visibility = Visibility.Visible;
            tbVisibleConfirmPassword.Focus();
        }

        private void btnToggleConfirmPassword_Unchecked(object sender, RoutedEventArgs e)
        {
            tbVisibleConfirmPassword.Visibility = Visibility.Collapsed;
            pbConfirmPassword.Visibility = Visibility.Visible;
            pbConfirmPassword.Focus();
        }

        private void ConfirmPassword_GotFocus(object sender, RoutedEventArgs e) => UpdateConfirmPasswordPlaceholder();
        private void ConfirmPassword_LostFocus(object sender, RoutedEventArgs e) => UpdateConfirmPasswordPlaceholder();

        private void UpdateConfirmPasswordPlaceholder()
        {
            bool hasText = !string.IsNullOrEmpty(pbConfirmPassword.Password);
            bool isFocused = pbConfirmPassword.IsFocused || tbVisibleConfirmPassword.IsFocused || btnToggleConfirmPassword.IsFocused;
            WatermarkConfirmPassword.Visibility = (hasText || isFocused) ? Visibility.Collapsed : Visibility.Visible;
        }

        private async void Button_Register_Click(object sender, RoutedEventArgs e)
        {
            errLastName.Visibility = Visibility.Collapsed;
            errFirstName.Visibility = Visibility.Collapsed;
            errBirthDate.Visibility = Visibility.Collapsed;
            errStatus.Visibility = Visibility.Collapsed;
            errGender.Visibility = Visibility.Collapsed;
            errCourse.Visibility = Visibility.Collapsed;
            errGroup.Visibility = Visibility.Collapsed;
            errSpeciality.Visibility = Visibility.Collapsed;
            errStudentId.Visibility = Visibility.Collapsed;
            errAdditionalEmail.Visibility = Visibility.Collapsed;
            errPhone.Visibility = Visibility.Collapsed;
            errVkLink.Visibility = Visibility.Collapsed;
            errPassword.Visibility = Visibility.Collapsed;
            errConfirmPassword.Visibility = Visibility.Collapsed;
            errPhoto.Visibility = Visibility.Collapsed;
            errAgreement.Visibility = Visibility.Collapsed;

            bool hasError = false;

            if (string.IsNullOrWhiteSpace(tbLastName.Text)) { errLastName.Visibility = Visibility.Visible; hasError = true; }
            if (string.IsNullOrWhiteSpace(tbFirstName.Text)) { errFirstName.Visibility = Visibility.Visible; hasError = true; }
            if (string.IsNullOrWhiteSpace(tbPhone.Text) || tbPhone.Text.Length < 18) { errPhone.Visibility = Visibility.Visible; hasError = true; }
            if (string.IsNullOrWhiteSpace(tbVkLink.Text)) { errVkLink.Visibility = Visibility.Visible; hasError = true; }
            if (string.IsNullOrWhiteSpace(tbPhoto.Text)) { errPhoto.Visibility = Visibility.Visible; hasError = true; }

            string pwd = pbPassword.Password;
            if (string.IsNullOrWhiteSpace(pwd))
            {
                errPassword.Text = "Заполните пароль";
                errPassword.Visibility = Visibility.Visible;
                hasError = true;
            }
            else if (pwd.Length < 8 || !pwd.Any(char.IsUpper) || !pwd.Any(char.IsDigit) || !pwd.Any(ch => !char.IsLetterOrDigit(ch)))
            {
                errPassword.Text = "Мин. 8 символов, 1 заглавная, 1 цифра и спецсимвол";
                errPassword.Visibility = Visibility.Visible;
                hasError = true;
            }

            if (string.IsNullOrWhiteSpace(pbConfirmPassword.Password))
            {
                errConfirmPassword.Text = "Повторите пароль";
                errConfirmPassword.Visibility = Visibility.Visible;
                hasError = true;
            }
            else if (pbPassword.Password != pbConfirmPassword.Password)
            {
                errConfirmPassword.Text = "Пароли не совпадают";
                errConfirmPassword.Visibility = Visibility.Visible;
                hasError = true;
            }

            if (dpBirthDate.SelectedDate == null) { errBirthDate.Visibility = Visibility.Visible; hasError = true; }
            if (_selectedStatuses.Count == 0) { errStatus.Visibility = Visibility.Visible; hasError = true; }

            if (cbGender.SelectedIndex <= 0) { errGender.Visibility = Visibility.Visible; hasError = true; }
            if (cbCourse.SelectedIndex <= 0) { errCourse.Visibility = Visibility.Visible; hasError = true; }

            if (cbGroup.SelectedItem == null) { errGroup.Visibility = Visibility.Visible; hasError = true; }
            if (cbSpeciality.SelectedIndex <= 0) { errSpeciality.Visibility = Visibility.Visible; hasError = true; }

            if (tbStudentId.Text.Length != 6) { errStudentId.Visibility = Visibility.Visible; hasError = true; }
            if (cbAgreement.IsChecked != true) { errAgreement.Visibility = Visibility.Visible; hasError = true; }

            string additionalEmail = tbAdditionalEmail.Text.Trim();
            if (!string.IsNullOrWhiteSpace(additionalEmail))
            {
                if (!Regex.IsMatch(additionalEmail, @"^[^@\s]+@[^@\s]+\.[^@\s]+$"))
                {
                    errAdditionalEmail.Visibility = Visibility.Visible;
                    hasError = true;
                }
            }

            if (hasError) return;

            var btn = sender as Button;
            btn.IsEnabled = false;
            btn.Content = "Отправка...";

            try
            {
                string photoBase64 = "";
                if (_selectedImageBytes != null)
                {
                    photoBase64 = $"data:image/jpeg;base64,{Convert.ToBase64String(_selectedImageBytes)}";
                }

                string corporateEmail = tbStudentId.Text.Trim() + ((ComboBoxItem)cbDomain.SelectedItem).Content.ToString();

                var requestObj = new
                {
                    name = tbFirstName.Text.Trim(),
                    surname = tbLastName.Text.Trim(),
                    patronymic = tbPatronymic.Text.Trim(),
                    gender = ((ComboBoxItem)cbGender.SelectedItem).Content.ToString(),
                    dateOfBirth = dpBirthDate.SelectedDate.Value.ToString("yyyy-MM-dd"),
                    courseNumber = int.Parse(((ComboBoxItem)cbCourse.SelectedItem).Content.ToString()),
                    speciality_id = (int)((ComboBoxItem)cbSpeciality.SelectedItem).Tag,
                    group_id = (int)((ComboBoxItem)cbGroup.SelectedItem).Tag,
                    studentIdNumber = int.Parse(tbStudentId.Text.Trim()),
                    studentEmail = corporateEmail,
                    phoneNumber = tbPhone.Text.Trim().Replace(" ", "").Replace("-", "").Replace("(", "").Replace(")", ""),
                    additionalEmail = additionalEmail,
                    password = pbPassword.Password,
                    vk_link = tbVkLink.Text.Trim(),
                    social_statuses_id = _selectedStatuses.ToArray(),
                    photo = photoBase64
                };

                string jsonData = JsonSerializer.Serialize(requestObj);
                var content = new StringContent(jsonData, Encoding.UTF8, "application/json");

                HttpResponseMessage response = await _httpClient.PostAsync("/api/account_requests", content);

                if (response.IsSuccessStatusCode)
                {
                    CustomMessageBox.Show("Ваша заявка на регистрацию успешно отправлена и ожидает подтверждения администратором!", "Успех", CustomMessageBox.MessageType.Success);
                    NavigationService?.Navigate(new Auth());
                }
                else
                {
                    string err = await response.Content.ReadAsStringAsync();
                    CustomMessageBox.Show($"Ошибка сервера при регистрации.\nКод: {response.StatusCode}\n{err}", "Ошибка сервера", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Произошла ошибка при отправке запроса: {ex.Message}", "Системная ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                btn.IsEnabled = true;
                btn.Content = "Зарегистрироваться";
            }
        }

        private void LoginText_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            NavigationService?.Navigate(new Auth());
        }
    }

    public class SpecialityDto
    {
        public int id { get; set; }
        public string title { get; set; }
    }

    public class GroupDto
    {
        public int id { get; set; }
        public string title { get; set; }
        public int course { get; set; }
    }

    public class SocialStatusDto
    {
        public int id { get; set; }
        public string title { get; set; }
    }
}