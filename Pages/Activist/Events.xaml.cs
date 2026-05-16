using Diplom_Stud.Components;
using Diplom_Stud.Pages.Coordinator;
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

namespace Diplom_Stud.Pages.Activist
{
    public partial class Events : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private List<EventViewModel> _allEvents = new List<EventViewModel>();

        public Events()
        {
            InitializeComponent();

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

            var user = App.CurrentUserProfile;
            if (user != null)
            {
                bool canCreate = user.roleTitle == "Coordinator" || user.roleTitle == "Sector_coordinator" || user.roleTitle == "Admin";
                if (!canCreate || App.IsActivistMode)
                {
                    btnCreateEvent.Visibility = Visibility.Collapsed;
                }
            }

            await LoadEventsAsync();
        }

        private async Task LoadEventsAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyEventsText.Visibility = Visibility.Collapsed;
            EventsItemsControl.ItemsSource = null;
            _allEvents.Clear();

            try
            {
                if (string.IsNullOrEmpty(App.AuthToken))
                {
                    CustomMessageBox.Show("Ошибка авторизации. Пожалуйста, войдите снова.", "Ошибка", CustomMessageBox.MessageType.Error);
                    NavigationService?.Navigate(new Auth());
                    return;
                }

                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync("/api/events?page=0&size=50&sortBy=id&sortDirection=DESC");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var pageData = JsonSerializer.Deserialize<EventPageResponse>(responseBody, options);

                    if (pageData?.content != null)
                    {
                        foreach (var ev in pageData.content)
                        {
                            string dateDisplay = "Дата не указана";
                            if (!string.IsNullOrEmpty(ev.dateOfEvent) && DateTime.TryParse(ev.dateOfEvent, out DateTime date))
                            {
                                dateDisplay = date.ToString("d MMMM", new CultureInfo("ru-RU"));
                            }

                            if (!string.IsNullOrEmpty(ev.startTime) && ev.startTime.Length >= 5)
                            {
                                dateDisplay += $", {ev.startTime.Substring(0, 5)}";
                            }

                            ImageSource imgSource = new BitmapImage(new Uri("pack://application:,,,/Resources/event1.png")); 
                            if (!string.IsNullOrEmpty(ev.photo))
                            {
                                var decodedBmp = GetImageFromBase64(ev.photo);
                                if (decodedBmp != null) imgSource = decodedBmp;
                            }

                            string displayTitle = ev.title;
                            Brush titleColor = Brushes.White;

                            if (ev.isDraft)
                            {
                                displayTitle += " (Черновик)";
                                titleColor = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#A084FB")); 
                            }

                            _allEvents.Add(new EventViewModel
                            {
                                Id = ev.id,
                                Title = displayTitle,
                                TitleColor = titleColor,
                                DateTimeDisplay = dateDisplay,
                                Venue = ev.venue ?? "Место не указано",
                                Image = imgSource
                            });
                        }
                    }

                    EventsItemsControl.ItemsSource = _allEvents;
                    if (_allEvents.Count == 0) EmptyEventsText.Visibility = Visibility.Visible;
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка загрузки мероприятий: {response.StatusCode}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сбой сети: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private void tbSearch_TextChanged(object sender, TextChangedEventArgs e)
        {
            string query = tbSearch.Text.Trim().ToLower();
            if (string.IsNullOrEmpty(query))
            {
                EventsItemsControl.ItemsSource = _allEvents;
            }
            else
            {
                var filtered = _allEvents.Where(ev => ev.Title.ToLower().Contains(query) || ev.Venue.ToLower().Contains(query)).ToList();
                EventsItemsControl.ItemsSource = filtered;
            }
        }

        private void EventCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is Border border && border.Tag is int eventId)
            {
                this.NavigationService.Navigate(new EventDetails(eventId));
            }
        }

        private void CreateEvent_Click(object sender, RoutedEventArgs e)
        {
            this.NavigationService.Navigate(new CreateEvent());
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
            catch (Exception ex)
            {
                Debug.WriteLine($"Ошибка обработки фото: {ex.Message}");
            }
            return null;
        }
    }

    public class EventPageResponse
    {
        public List<EventDto> content { get; set; }
        public int totalElements { get; set; }
    }

    public class EventDto
    {
        public int id { get; set; }
        public string title { get; set; }
        public string description { get; set; }
        public string photo { get; set; }
        public string dateOfEvent { get; set; }
        public string startTime { get; set; }
        public string endTime { get; set; }
        public string venue { get; set; }
        public int eventCreatorId { get; set; }
        public string referenceToPosition { get; set; }
        public string eventCreatorName { get; set; }
        public string eventCreatorSurname { get; set; }
        public bool isActive { get; set; }
        public bool isPublic { get; set; }
        public bool isDraft { get; set; }
        public bool isCompleted { get; set; }
        public string createdAt { get; set; }
        public string updatedAt { get; set; }
    }

    public class EventViewModel
    {
        public int Id { get; set; }
        public string Title { get; set; }
        public Brush TitleColor { get; set; }
        public string DateTimeDisplay { get; set; }
        public string Venue { get; set; }
        public ImageSource Image { get; set; }
    }
}