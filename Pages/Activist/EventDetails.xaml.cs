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
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;

namespace Diplom_Stud.Pages.Activist
{
    public partial class EventDetails : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private int _eventId;

        private bool _isFreeEvent = false;
        private int _maxOrganizersCount = 0;

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
                Duration = TimeSpan.FromSeconds(0.6),
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

                        _isFreeEvent = ev.isFreeEvent;
                        _maxOrganizersCount = ev.maxOrganizersCount;

                        if (!string.IsNullOrEmpty(ev.photo))
                        {
                            var bmp = GetImageFromBase64(ev.photo);
                            if (bmp != null) EventImageBrush.ImageSource = bmp;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine(ex.Message);
            }
        }

        private async Task LoadEventRolesAsync()
        {
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                var rolesList = new List<EventRoleViewModel>();

                if (_isFreeEvent)
                {
                    rolesList.Add(new EventRoleViewModel
                    {
                        Title = "Участник",
                        Description = "Обычное участие в мероприятии",
                        DeadlineText = "Без дедлайна"
                    });
                }

                if (_maxOrganizersCount > 0)
                {
                    rolesList.Add(new EventRoleViewModel
                    {
                        Title = "Организатор",
                        Description = "Помощь в организации и проведении мероприятия",
                        DeadlineText = "По результатам отбора"
                    });
                }

                HttpResponseMessage response = await _httpClient.GetAsync($"/api/event-roles?eventId={_eventId}&page=0&size=50");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var rolesPage = JsonSerializer.Deserialize<EventRolePageResponse>(responseBody, options);

                    if (rolesPage?.content != null)
                    {
                        foreach (var role in rolesPage.content)
                        {
                            rolesList.Add(new EventRoleViewModel
                            {
                                Id = role.id,
                                Title = role.globalEventRoleTitle ?? "Роль",
                                Description = string.IsNullOrEmpty(role.description) ? "Описание не указано" : role.description,
                                DeadlineText = $"Дедлайн: {FormatDeadline(role.deadline)}"
                            });
                        }
                    }
                }

                RolesItemsControl.ItemsSource = rolesList;
                EmptyRolesText.Visibility = rolesList.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
            }
            catch (Exception ex)
            {
                Debug.WriteLine(ex.Message);
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
                if (!string.IsNullOrEmpty(startStr) && startStr.Length >= 5) timePart += startStr.Substring(0, 5);
                if (!string.IsNullOrEmpty(endStr) && endStr.Length >= 5) timePart += " - " + endStr.Substring(0, 5);
                if (!string.IsNullOrEmpty(timePart)) result += $", {timePart}";
            }
            return result;
        }

        private string FormatDeadline(string deadlineStr)
        {
            if (!string.IsNullOrEmpty(deadlineStr) && DateTime.TryParse(deadlineStr, out DateTime date))
            {
                return date.ToString("d MMMM, HH:mm", new CultureInfo("ru-RU"));
            }
            return "Не указан";
        }

        private BitmapImage GetImageFromBase64(string base64String)
        {
            if (string.IsNullOrEmpty(base64String)) return null;
            try
            {
                string cleanBase64 = base64String.Contains(",") ? base64String.Split(',')[1] : base64String;
                byte[] binaryData = Convert.FromBase64String(cleanBase64);
                using (var ms = new MemoryStream(binaryData))
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
            catch { return null; }
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

        public bool isFreeEvent { get; set; }
        public int maxOrganizersCount { get; set; }
    }
    public class EventRolePageResponse { public List<EventRoleDto> content { get; set; } }
    public class EventRoleDto { public int id { get; set; } public string globalEventRoleTitle { get; set; } public string description { get; set; } public string deadline { get; set; } }
    public class EventRoleViewModel { public int Id { get; set; } public string Title { get; set; } public string Description { get; set; } public string DeadlineText { get; set; } }
}