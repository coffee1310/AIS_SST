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
    public partial class Events : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();

        private List<AllEvents_ViewModel> _allLoadedEvents = new List<AllEvents_ViewModel>();

        private int _currentPage = 0;
        private int _pageSize = 9;
        private int _totalPages = 1;
        private string _searchQuery = "";

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
                Duration = TimeSpan.FromSeconds(0.6),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };
            this.BeginAnimation(Page.OpacityProperty, fadeInAnimation);

            await LoadAllEventsOnceAsync();
        }

        private async Task LoadAllEventsOnceAsync()
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

                var activeEventsDict = new Dictionary<int, AllEvents_ViewModel>();

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

                _allLoadedEvents = activeEventsDict.Values
                    .Where(v => v.EventDate.Date >= DateTime.Today)
                    .OrderBy(v => v.EventDate)
                    .ToList();


                UpdateView();
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

        private async Task<List<AllEvents_EventDto>> FetchEventsFromApi(string url)
        {
            try
            {
                HttpResponseMessage response = await _httpClient.GetAsync(url);
                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    var pageData = JsonSerializer.Deserialize<AllEvents_PageResponse>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    return pageData?.content ?? new List<AllEvents_EventDto>();
                }
            }
            catch { }
            return new List<AllEvents_EventDto>();
        }

        private async Task<List<AllEvents_AppDto>> GetUserApplicationsAsync()
        {
            try
            {
                int currentUserId = App.CurrentUserProfile?.id ?? 0;
                if (currentUserId == 0) return new List<AllEvents_AppDto>();

                var response = await _httpClient.GetAsync($"/api/applications/user/{currentUserId}");
                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    var page = JsonSerializer.Deserialize<AllEvents_AppPageResponse>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    return page?.content ?? new List<AllEvents_AppDto>();
                }
            }
            catch { }
            return new List<AllEvents_AppDto>();
        }

        private AllEvents_ViewModel MapToViewModel(AllEvents_EventDto ev, bool isOrganizer)
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

            return new AllEvents_ViewModel
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

        private void UpdateView()
        {
            var filtered = _allLoadedEvents.AsEnumerable();

            if (!string.IsNullOrWhiteSpace(_searchQuery))
            {
                filtered = filtered.Where(e => e.Title != null && e.Title.IndexOf(_searchQuery, StringComparison.OrdinalIgnoreCase) >= 0);
            }

            var listToDisplay = filtered.ToList();

            _totalPages = (int)Math.Ceiling(listToDisplay.Count / (double)_pageSize);
            if (_totalPages == 0) _totalPages = 1;

            if (_currentPage >= _totalPages) _currentPage = _totalPages - 1;
            if (_currentPage < 0) _currentPage = 0;

            var pageItems = listToDisplay.Skip(_currentPage * _pageSize).Take(_pageSize).ToList();

            EventsList.ItemsSource = pageItems;
            EmptyEventsText.Visibility = pageItems.Count == 0 ? Visibility.Visible : Visibility.Collapsed;

            UpdatePaginationUI();
        }

        private void UpdatePaginationUI()
        {
            PaginationPanel.Children.Clear();

            if (_totalPages <= 1) return;

            Button prevButton = new Button
            {
                Content = "<",
                Style = (Style)FindResource("PaginationButtonStyle"),
                IsEnabled = _currentPage > 0
            };
            prevButton.Click += (s, e) => { _currentPage--; UpdateView(); };
            PaginationPanel.Children.Add(prevButton);

            for (int i = 0; i < _totalPages; i++)
            {
                Button pageBtn = new Button
                {
                    Content = (i + 1).ToString(),
                    Style = i == _currentPage ? (Style)FindResource("PaginationActiveButtonStyle") : (Style)FindResource("PaginationButtonStyle"),
                    Tag = i
                };
                pageBtn.Click += PageButton_Click;
                PaginationPanel.Children.Add(pageBtn);
            }

            Button nextButton = new Button
            {
                Content = ">",
                Style = (Style)FindResource("PaginationButtonStyle"),
                IsEnabled = _currentPage < _totalPages - 1
            };
            nextButton.Click += (s, e) => { _currentPage++; UpdateView(); };
            PaginationPanel.Children.Add(nextButton);
        }

        private void PageButton_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int pageIndex)
            {
                if (_currentPage != pageIndex)
                {
                    _currentPage = pageIndex;
                    UpdateView();
                }
            }
        }

        private void tbSearch_TextChanged(object sender, TextChangedEventArgs e)
        {
            _searchQuery = tbSearch.Text.Trim();
            _currentPage = 0;
            UpdateView();
        }

        private void EventCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is Border border && border.Tag is int eventId)
            {
                this.NavigationService.Navigate(new EventDetails(eventId));
            }
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

    public class AllEvents_PageResponse { public List<AllEvents_EventDto> content { get; set; } }
    public class AllEvents_EventDto { public int id { get; set; } public string title { get; set; } public string photo { get; set; } public string dateOfEvent { get; set; } public string startTime { get; set; } public string venue { get; set; } public bool isCompleted { get; set; } public bool isDeleted { get; set; } public bool isMySector { get; set; } public bool isFreeEvent { get; set; } }
    public class AllEvents_ViewModel { public int Id { get; set; } public string Title { get; set; } public string DateTimeDisplay { get; set; } public string Venue { get; set; } public ImageSource Image { get; set; } public Visibility OrganizerBadgeVisibility { get; set; } = Visibility.Collapsed; public Visibility ParticipantBadgeVisibility { get; set; } = Visibility.Collapsed; public Visibility ImageVisibility { get; set; } public DateTime EventDate { get; set; } public Brush CardBorderBrush { get; set; } public Thickness CardBorderThickness { get; set; } }
    public class AllEvents_AppPageResponse { public List<AllEvents_AppDto> content { get; set; } }
    public class AllEvents_AppDto { public int id { get; set; } public int eventId { get; set; } public string status { get; set; } }
}