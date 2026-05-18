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
using System.Windows.Navigation;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class CoordinatorHome : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();

        public CoordinatorHome()
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
            await LoadAllEventsAsync();
        }

        private void LoadUserData()
        {
            var user = App.CurrentUserProfile;

            if (user != null)
            {
                tbUserName.Text = $"{user.surname} {user.name} {user.patronymic}".Trim();
            }
        }

        private async Task LoadAllEventsAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;

            DraftsHeaderGrid.Visibility = Visibility.Collapsed;
            DraftsItemsControl.Visibility = Visibility.Collapsed;
            EmptyEventsText.Visibility = Visibility.Collapsed;

            try
            {
                if (string.IsNullOrEmpty(App.AuthToken))
                {
                    CustomMessageBox.Show("Ошибка авторизации.", "Ошибка", CustomMessageBox.MessageType.Error);
                    return;
                }

                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                int userId = App.CurrentUserProfile?.id ?? 0;

                var draftsDto = await FetchEventsFromApi($"/api/events?isDraft=true&creatorId={userId}&page=0&size=50");

                var myEventsDto = await FetchEventsFromApi($"/api/events?isDraft=false&creatorId={userId}&page=0&size=50");

                var orgEventsDto = await FetchEventsFromApi($"/api/events?isDraft=false&isOrganizer=true&page=0&size=50");

                var sectorEventsDto = await FetchEventsFromApi($"/api/events?isDraft=false&isResponsibleSector=true&page=0&size=50");

                var activeEventsDict = new Dictionary<int, EventDtoLocal>();

                foreach (var ev in myEventsDto) activeEventsDict[ev.id] = ev;
                foreach (var ev in orgEventsDto) activeEventsDict[ev.id] = ev;
                foreach (var ev in sectorEventsDto) activeEventsDict[ev.id] = ev;

                var draftsVm = draftsDto.Select(MapToViewModel).ToList();

                var activeEventsVm = activeEventsDict.Values.Select(MapToViewModel).ToList();

                var sortedActiveEventsVm = activeEventsVm
                    .OrderByDescending(v => v.IsOverdue)
                    .ThenBy(v => v.EventDate)
                    .ToList();

                if (draftsVm.Count > 0)
                {
                    DraftsHeaderGrid.Visibility = Visibility.Visible;
                    DraftsItemsControl.Visibility = Visibility.Visible;
                    DraftsItemsControl.ItemsSource = draftsVm;
                }

                EventsItemsControl.ItemsSource = sortedActiveEventsVm;
                if (sortedActiveEventsVm.Count == 0) EmptyEventsText.Visibility = Visibility.Visible;

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
            catch (Exception ex)
            {
                Debug.WriteLine($"Ошибка FetchEventsFromApi ({url}): {ex.Message}");
            }
            return new List<EventDtoLocal>();
        }

        private EventViewModelLocal MapToViewModel(EventDtoLocal ev)
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

            BitmapImage bmp = null;
            Visibility phVis = Visibility.Visible;
            Visibility imgVis = Visibility.Collapsed;

            if (!string.IsNullOrEmpty(ev.photo))
            {
                bmp = GetImageFromBase64(ev.photo);
                if (bmp != null)
                {
                    phVis = Visibility.Collapsed;
                    imgVis = Visibility.Visible;
                }
            }
            else if (!ev.isDraft)
            {
                bmp = new BitmapImage(new Uri("pack://application:,,,/Resources/event1.png"));
                phVis = Visibility.Collapsed;
                imgVis = Visibility.Visible;
            }

            bool isOverdue = !ev.isDraft && !ev.isCompleted && parsedDate < DateTime.Now.Date;

            return new EventViewModelLocal
            {
                Id = ev.id,
                Title = ev.title + (ev.isDraft ? " (Черновик)" : ""),
                DateTimeDisplay = dateDisplay,
                Venue = ev.venue ?? "Место не указано",
                Image = bmp,
                PlaceholderVisibility = phVis,
                ImageVisibility = imgVis,
                EventDate = parsedDate,
                IsOverdue = isOverdue,
                CardBorderBrush = isOverdue ? new SolidColorBrush((Color)ColorConverter.ConvertFromString("#E81123")) : new SolidColorBrush((Color)ColorConverter.ConvertFromString("#2A283C")),
                CardBorderThickness = isOverdue ? new Thickness(2) : new Thickness(1)
            };
        }

        private void RoleSwitch_Click(object sender, RoutedEventArgs e)
        {
            RolePopup.IsOpen = true;
        }

        private void SetActivistRole_Click(object sender, RoutedEventArgs e)
        {
            RolePopup.IsOpen = false;
            App.IsActivistMode = true;

            if (Window.GetWindow(this) is MainWindow mainWindow)
            {
                mainWindow.UpdateUserMenu();
            }

            NavigationService?.Navigate(new Diplom_Stud.Pages.Activist.Home());
        }

        private void EventCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is Border border && border.Tag is int eventId)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Coordinator.CoordinatorEventDetails(eventId));
            }
        }

        private void DraftCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is Border border && border.Tag is int eventId)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Coordinator.CoordinatorEventDetails(eventId));
            }
        }

        private void EditDraft_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int eventId)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Coordinator.CoordinatorEventDetails(eventId));
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
            catch (Exception ex)
            {
                Debug.WriteLine($"Ошибка обработки фото: {ex.Message}");
            }
            return null;
        }
    }

    public class EventPageResponseLocal
    {
        public List<EventDtoLocal> content { get; set; }
    }

    public class EventDtoLocal
    {
        public int id { get; set; }
        public string title { get; set; }
        public string photo { get; set; }
        public string dateOfEvent { get; set; }
        public string startTime { get; set; }
        public string venue { get; set; }
        public int eventCreatorId { get; set; }
        public bool isDraft { get; set; }
        public bool isCompleted { get; set; }
    }

    public class EventViewModelLocal
    {
        public int Id { get; set; }
        public string Title { get; set; }
        public string DateTimeDisplay { get; set; }
        public string Venue { get; set; }
        public ImageSource Image { get; set; }
        public Visibility PlaceholderVisibility { get; set; }
        public Visibility ImageVisibility { get; set; }

        public DateTime EventDate { get; set; }
        public bool IsOverdue { get; set; }
        public Brush CardBorderBrush { get; set; }
        public Thickness CardBorderThickness { get; set; }
    }
}