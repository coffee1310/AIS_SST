using Diplom_Stud.Components;
using Diplom_Stud.Pages.General;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
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
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;

namespace Diplom_Stud.Pages.Activist
{
    public partial class EventDetails : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private int _eventId;

        public EventDetails(int eventId = 0)
        {
            InitializeComponent();
            _eventId = eventId;

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

            if (_eventId <= 0)
            {
                CustomMessageBox.Show("ID мероприятия не передан.", "Ошибка", CustomMessageBox.MessageType.Error);
                LoadingOverlay.Visibility = Visibility.Collapsed;
                return;
            }

            await LoadEventDataAsync();
            await LoadEventRolesAsync();
        }

        private async Task LoadEventDataAsync()
        {
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync($"/api/events/{_eventId}");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var ev = JsonSerializer.Deserialize<EventDetailDto>(responseBody, options);

                    if (ev != null)
                    {
                        EventTitleText.Text = ev.title;
                        EventDescriptionText.Text = string.IsNullOrEmpty(ev.description) ? "Описание отсутствует." : ev.description;
                        EventVenueText.Text = string.IsNullOrEmpty(ev.venue) ? "Место не указано" : ev.venue;
                        EventTimeText.Text = FormatEventDateTime(ev.dateOfEvent, ev.startTime, ev.endTime);

                        if (!string.IsNullOrEmpty(ev.photo))
                        {
                            var bmp = GetImageFromBase64(ev.photo);
                            if (bmp != null) EventImageBrush.ImageSource = bmp;
                        }
                    }
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка загрузки мероприятия: {response.StatusCode}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сбой сети при загрузке: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
        }

        private async Task LoadEventRolesAsync()
        {
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync($"/api/event-roles?eventId={_eventId}&page=0&size=50");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var rolesPage = JsonSerializer.Deserialize<EventRolePageResponse>(responseBody, options);

                    if (rolesPage?.content != null && rolesPage.content.Count > 0)
                    {
                        var rolesList = new List<EventRoleViewModel>();

                        foreach (var role in rolesPage.content)
                        {
                            rolesList.Add(new EventRoleViewModel
                            {
                                Id = role.id,
                                Title = role.globalEventRoleTitle ?? "Роль",
                                Description = string.IsNullOrEmpty(role.description) ? "Описание не указано" : role.description,
                                DeadlineText = FormatDeadline(role.deadline)
                            });
                        }

                        RolesItemsControl.ItemsSource = rolesList;
                        EmptyRolesText.Visibility = Visibility.Collapsed;
                    }
                    else
                    {
                        RolesItemsControl.ItemsSource = null;
                        EmptyRolesText.Visibility = Visibility.Visible;
                    }
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка загрузки ролей: {response.StatusCode}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сбой сети при загрузке ролей: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private string FormatEventDateTime(string dateStr, string startStr, string endStr)
        {
            string result = "Время не указано";

            if (!string.IsNullOrEmpty(dateStr) && DateTime.TryParse(dateStr, out DateTime date))
            {
                result = date.ToString("d MMMM", new CultureInfo("ru-RU"));

                string timePart = "";
                if (!string.IsNullOrEmpty(startStr) && startStr.Length >= 5)
                {
                    timePart += startStr.Substring(0, 5);
                }

                if (!string.IsNullOrEmpty(endStr) && endStr.Length >= 5)
                {
                    timePart += " - " + endStr.Substring(0, 5);
                }

                if (!string.IsNullOrEmpty(timePart))
                {
                    result += $", {timePart}";
                }
            }

            return result;
        }

        private string FormatDeadline(string deadlineStr)
        {
            if (!string.IsNullOrEmpty(deadlineStr) && DateTime.TryParse(deadlineStr, out DateTime date))
            {
                return $"Дедлайн: {date.ToString("d MMMM, HH:mm", new CultureInfo("ru-RU"))}";
            }
            return "Дедлайн не указан";
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

        private void RegisterButton_Click(object sender, RoutedEventArgs e)
        {
            this.NavigationService.Navigate(new EventRegistration(_eventId));
        }
    }

    public class EventDetailDto
    {
        public int id { get; set; }
        public string title { get; set; }
        public string description { get; set; }
        public string photo { get; set; }
        public string dateOfEvent { get; set; }
        public string startTime { get; set; }
        public string endTime { get; set; }
        public string venue { get; set; }
        public bool isDraft { get; set; }
    }

    public class EventRolePageResponse
    {
        public List<EventRoleDto> content { get; set; }
    }

    public class EventRoleDto
    {
        public int id { get; set; }
        public string globalEventRoleTitle { get; set; }
        public string description { get; set; }
        public string deadline { get; set; }
    }

    public class EventRoleViewModel
    {
        public int Id { get; set; }
        public string Title { get; set; }
        public string Description { get; set; }
        public string DeadlineText { get; set; }
    }
}