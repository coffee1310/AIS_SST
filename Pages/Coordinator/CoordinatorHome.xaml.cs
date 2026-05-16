using Diplom_Stud.Components;
using Diplom_Stud.Pages.Activist;
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
            EmptyDraftsText.Visibility = Visibility.Collapsed;
            EmptyEventsText.Visibility = Visibility.Collapsed;

            try
            {
                if (string.IsNullOrEmpty(App.AuthToken))
                {
                    CustomMessageBox.Show("Ошибка авторизации.", "Ошибка", CustomMessageBox.MessageType.Error);
                    return;
                }

                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync("/api/events?page=0&size=50&sortBy=id&sortDirection=DESC");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var pageData = JsonSerializer.Deserialize<EventPageResponseLocal>(responseBody, options);

                    if (pageData?.content != null)
                    {
                        var drafts = new List<EventViewModelLocal>();
                        var activeEvents = new List<EventViewModelLocal>();

                        int currentUserId = App.CurrentUserProfile?.id ?? 0;

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

                            var vm = new EventViewModelLocal
                            {
                                Id = ev.id,
                                Title = ev.title + (ev.isDraft ? " (Черновик)" : ""),
                                DateTimeDisplay = dateDisplay,
                                Venue = ev.venue ?? "Место не указано",
                                Image = bmp,
                                PlaceholderVisibility = phVis,
                                ImageVisibility = imgVis
                            };

                            if (ev.isDraft)
                            {
                                drafts.Add(vm);
                            }
                            else
                            {
                                activeEvents.Add(vm);
                            }
                        }

                        DraftsItemsControl.ItemsSource = drafts;
                        EventsItemsControl.ItemsSource = activeEvents;

                        if (drafts.Count == 0) EmptyDraftsText.Visibility = Visibility.Visible;
                        if (activeEvents.Count == 0) EmptyEventsText.Visibility = Visibility.Visible;
                    }
                }
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
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Activist.EventDetails(eventId));
            }
        }

        private void DraftCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is Border border && border.Tag is int eventId)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Activist.EventDetails(eventId));
            }
        }

        private void EditDraft_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int eventId)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Activist.EventDetails(eventId));
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
    }
}