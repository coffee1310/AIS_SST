using Diplom_Stud.Components;
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
    public partial class Home : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private List<EventViewModelLocal> _allEvents = new List<EventViewModelLocal>();
        private int _currentEventIndex = 0;

        public Home()
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

            LoadUserData();
            await LoadActivistEventsAsync();
        }

        private void LoadUserData()
        {
            var user = App.CurrentUserProfile;
            if (user != null)
            {
                tbUserName.Text = $"{user.surname} {user.name} {user.patronymic}".Trim();
                tbEventsCount.Text = user.events_count?.ToString() ?? "0";
                tbPointsCount.Text = user.points_count?.ToString() ?? "0";
                tbRank.Text = user.rank?.ToString() ?? "0";
            }
        }

        private async Task LoadActivistEventsAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyEventsText.Visibility = Visibility.Collapsed;

            try
            {
                if (string.IsNullOrEmpty(App.AuthToken))
                {
                    CustomMessageBox.Show("Ошибка авторизации.", "Ошибка", CustomMessageBox.MessageType.Error);
                    return;
                }

                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                var orgEventsDto = await FetchEventsFromApi("/api/events?isDraft=false&isOrganizer=true&isDeleted=false&page=0&size=50");

                var sectorEventsDto = await FetchEventsFromApi("/api/events?isDraft=false&isMySector=true&isDeleted=false&page=0&size=50");

                var publicEventsDto = await FetchEventsFromApi("/api/events?isDraft=false&isPublic=true&isDeleted=false&page=0&size=50");

                var userApplications = await GetUserApplicationsAsync();

                var activeEventsDict = new Dictionary<int, EventViewModelLocal>();

                foreach (var ev in orgEventsDto)
                {
                    if (!ev.isDeleted)
                    {
                        activeEventsDict[ev.id] = MapToViewModel(ev, isOrganizer: true);
                    }
                }

                foreach (var ev in sectorEventsDto.Concat(publicEventsDto))
                {
                    if (!ev.isDeleted && !activeEventsDict.ContainsKey(ev.id))
                    {
                        activeEventsDict[ev.id] = MapToViewModel(ev, isOrganizer: false);
                    }
                }

                foreach (var vm in activeEventsDict.Values)
                {
                    bool isApplied = userApplications.Any(a => a.eventId == vm.Id &&
                                    (a.status == "ОДОБРЕНА" || a.status == "НА_РАССМОТРЕНИИ" || a.status == "НА РАССМОТРЕНИИ" || a.status == "ОДОБРЕНО"));
                    if (isApplied)
                    {
                        vm.ParticipantBadgeVisibility = Visibility.Visible;
                    }
                }

                _allEvents = activeEventsDict.Values
                    .Where(v => v.EventDate.Date >= DateTime.Today)
                    .OrderBy(v => v.EventDate)
                    .ToList();

                _currentEventIndex = 0;
                UpdateEventCarousel();

                if (_allEvents.Count == 0)
                    EmptyEventsText.Visibility = Visibility.Visible;
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сбой сети при загрузке мероприятий: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private void UpdateEventCarousel()
        {
            if (_allEvents == null || _allEvents.Count == 0) return;

            var displayEvents = _allEvents.Skip(_currentEventIndex).Take(3).ToList();
            EventsItemsControl.ItemsSource = displayEvents;

            BtnPrevEvent.Visibility = _currentEventIndex > 0 ? Visibility.Visible : Visibility.Collapsed;
            BtnNextEvent.Visibility = _currentEventIndex + 3 < _allEvents.Count ? Visibility.Visible : Visibility.Collapsed;
        }

        private void BtnPrevEvent_Click(object sender, RoutedEventArgs e)
        {
            if (_currentEventIndex > 0)
            {
                _currentEventIndex--;
                UpdateEventCarousel();
            }
        }

        private void BtnNextEvent_Click(object sender, RoutedEventArgs e)
        {
            if (_currentEventIndex + 3 < _allEvents.Count)
            {
                _currentEventIndex++;
                UpdateEventCarousel();
            }
        }

        private async Task<List<ApplicationDto>> GetUserApplicationsAsync()
        {
            try
            {
                int currentUserId = App.CurrentUserProfile?.id ?? 0;
                if (currentUserId == 0) return new List<ApplicationDto>();

                var response = await _httpClient.GetAsync($"/api/applications/user/{currentUserId}");
                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    var page = JsonSerializer.Deserialize<ApplicationPageResponse>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    return page?.content ?? new List<ApplicationDto>();
                }
            }
            catch (Exception ex) { Debug.WriteLine($"Не удалось загрузить заявки: {ex.Message}"); }
            return new List<ApplicationDto>();
        }

        private async Task<List<EventDtoLocal>> FetchEventsFromApi(string url)
        {
            try
            {
                HttpResponseMessage response = await _httpClient.GetAsync(url);
                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    var pageData = JsonSerializer.Deserialize<EventPageResponseLocal>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    return pageData?.content ?? new List<EventDtoLocal>();
                }
            }
            catch { }
            return new List<EventDtoLocal>();
        }

        private EventViewModelLocal MapToViewModel(EventDtoLocal ev, bool isOrganizer)
        {
            string dateDisplay = "Дата не указана";
            DateTime parsedDate = DateTime.MaxValue;

            if (!string.IsNullOrEmpty(ev.dateOfEvent) && DateTime.TryParse(ev.dateOfEvent, out DateTime date))
            {
                parsedDate = date;
                dateDisplay = date.ToString("d MMMM", new CultureInfo("ru-RU"));
            }
            if (!string.IsNullOrEmpty(ev.startTime) && ev.startTime.Length >= 5)
            {
                dateDisplay += $", {ev.startTime.Substring(0, 5)}";
            }

            BitmapImage bmp = new BitmapImage(new Uri("pack://application:,,,/Resources/event1.png"));
            if (!string.IsNullOrEmpty(ev.photo))
            {
                var decodedBmp = GetImageFromBase64(ev.photo);
                if (decodedBmp != null) bmp = decodedBmp;
            }

            return new EventViewModelLocal
            {
                Id = ev.id,
                Title = ev.title,
                DateTimeDisplay = dateDisplay,
                Venue = ev.venue ?? "Место не указано",
                Image = bmp,
                EventDate = parsedDate,
                OrganizerBadgeVisibility = isOrganizer ? Visibility.Visible : Visibility.Collapsed,
                ParticipantBadgeVisibility = Visibility.Collapsed,
                CardBorderBrush = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#2A283C")),
                CardBorderThickness = new Thickness(1)
            };
        }

        private void EventCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is Border border && border.Tag is int eventId)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Activist.EventDetails(eventId));
            }
        }

        private void AllEvents_Click(object sender, RoutedEventArgs e)
        {
            this.NavigationService.Navigate(new Diplom_Stud.Pages.Activist.Events());
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
            catch { }
            return null;
        }
    }

    public class EventPageResponseLocal { public List<EventDtoLocal> content { get; set; } }

    public class EventDtoLocal { public int id { get; set; } public string title { get; set; } public string photo { get; set; } public string dateOfEvent { get; set; } public string startTime { get; set; } public string venue { get; set; } public bool isCompleted { get; set; } public bool isDeleted { get; set; } public bool isMySector { get; set; } public bool isFreeEvent { get; set; } }

    public class EventViewModelLocal { public int Id { get; set; } public string Title { get; set; } public string DateTimeDisplay { get; set; } public string Venue { get; set; } public ImageSource Image { get; set; } public Visibility OrganizerBadgeVisibility { get; set; } public Visibility ParticipantBadgeVisibility { get; set; } = Visibility.Collapsed; public DateTime EventDate { get; set; } public Brush CardBorderBrush { get; set; } public Thickness CardBorderThickness { get; set; } }
    public class ApplicationPageResponse { public List<ApplicationDto> content { get; set; } }
    public class ApplicationDto { public int eventId { get; set; } public string status { get; set; } = ""; }
}