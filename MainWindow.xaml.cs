using Diplom_Stud.Pages.Activist;
using Diplom_Stud.Pages.Coordinator;
using Diplom_Stud.Pages.Curator;
using Diplom_Stud.Pages.General;
using Diplom_Stud.Components;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;

namespace Diplom_Stud
{
    public partial class MainWindow : Window
    {
        private static readonly HttpClient _httpClient = new HttpClient();

        public MainWindow()
        {
            InitializeComponent();
            MainFrame.Navigate(new Pages.General.Auth());

            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            }
        }

        public void UpdateUserMenu()
        {
            var data = App.CurrentUserProfile;
            if (data == null) return;

            if (NavProfile.Content is StackPanel panel)
            {
                if (panel.Children.Count >= 3)
                {
                    var ellipse = panel.Children[0] as System.Windows.Shapes.Ellipse;
                    var nameText = panel.Children[1] as TextBlock;
                    var emailText = panel.Children[2] as TextBlock;

                    bool isCuratorMenu = data.roleTitle == "Curator" || data.roleTitle == "Admin_curator";

                    if (nameText != null)
                    {
                        if (isCuratorMenu)
                            nameText.Text = $"{data.name} {data.patronymic}".Trim();
                        else
                            nameText.Text = $"{data.surname} {data.name}".Trim();
                    }

                    if (emailText != null)
                        emailText.Text = data.studentEmail ?? "";

                    if (ellipse != null)
                    {
                        if (!string.IsNullOrEmpty(data.photo))
                        {
                            BitmapImage bmp = GetImageFromBase64(data.photo);
                            ellipse.Fill = bmp != null
                                ? new ImageBrush(bmp) { Stretch = Stretch.UniformToFill }
                                : new ImageBrush(new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))) { Stretch = Stretch.UniformToFill };
                        }
                        else
                        {
                            ellipse.Fill = new ImageBrush(new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))) { Stretch = Stretch.UniformToFill };
                        }
                    }
                }
            }


            bool isActuallyCoordinator = data.roleTitle == "Sector_coordinator" || data.roleTitle == "Coordinator";
            bool showCoordinatorMenu = isActuallyCoordinator && !App.IsActivistMode;

            if (showCoordinatorMenu)
            {
                NavSectorsText.Text = "Мой сектор";
                _ = CheckSectorNotificationsAsync();
            }
            else
            {
                NavSectorsText.Text = "Сектора";
                NavSectorsBadge.Visibility = Visibility.Collapsed;
            }

            bool isGlobalAdmin = data.roleTitle == "Curator" || data.roleTitle == "Admin" || data.roleTitle == "Admin_curator";
            bool showAdminMenu = isGlobalAdmin && !App.IsActivistMode;

            if (showAdminMenu)
            {
                NavRegistrationRequests.Visibility = Visibility.Visible;
                _ = CheckRegistrationRequestsNotificationsAsync();
            }
            else
            {
                NavRegistrationRequests.Visibility = Visibility.Collapsed;
                NavRegistrationRequestsBadge.Visibility = Visibility.Collapsed;
            }
        }

        public async Task CheckSectorNotificationsAsync()
        {
            var data = App.CurrentUserProfile;
            if (data == null || App.IsActivistMode || (data.roleTitle != "Sector_coordinator" && data.roleTitle != "Coordinator"))
            {
                SetSectorNotification(false);
                return;
            }

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage reqResp = await _httpClient.GetAsync("/api/sector/introductions/filter?status=НА_РАССМОТРЕНИИ");
                if (reqResp.IsSuccessStatusCode)
                {
                    string reqBody = await reqResp.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var allRequests = JsonSerializer.Deserialize<List<Pages.Coordinator.IntroductionDto>>(reqBody, options);

                    bool hasActive = allRequests?.Any(r => r.sector_id == data.coordinatorSectorId) == true;
                    SetSectorNotification(hasActive);
                }
            }
            catch { }
        }

        public void SetSectorNotification(bool hasNotifications)
        {
            Application.Current.Dispatcher.Invoke(() =>
            {
                NavSectorsBadge.Visibility = hasNotifications ? Visibility.Visible : Visibility.Collapsed;
            });
        }

        public async Task CheckRegistrationRequestsNotificationsAsync()
        {
            var data = App.CurrentUserProfile;
            if (data == null || App.IsActivistMode || (data.roleTitle != "Curator" && data.roleTitle != "Admin" && data.roleTitle != "Admin_curator"))
            {
                SetRegistrationRequestNotification(false);
                return;
            }

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync("/api/account_requests/filter?status=НА_РАССМОТРЕНИИ&page=0&size=1&sortBy=createdAt&sortDirection=DESC");
                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    using (JsonDocument doc = JsonDocument.Parse(json))
                    {
                        JsonElement root = doc.RootElement;
                        if (root.TryGetProperty("content", out JsonElement contentElement))
                        {
                            SetRegistrationRequestNotification(contentElement.GetArrayLength() > 0);
                        }
                    }
                }
            }
            catch { }
        }

        public void SetRegistrationRequestNotification(bool hasNotifications)
        {
            Application.Current.Dispatcher.Invoke(() =>
            {
                NavRegistrationRequestsBadge.Visibility = hasNotifications ? Visibility.Visible : Visibility.Collapsed;
            });
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
                        if (commaIndex >= 0)
                        {
                            string actualBase64 = textInside.Substring(commaIndex + 1);
                            imageBytes = Convert.FromBase64String(actualBase64);
                        }
                    }
                    else
                    {
                        imageBytes = decodedFirstLevel;
                    }
                }
                catch
                {
                    int commaIndex = cleanStr.IndexOf(',');
                    if (commaIndex >= 0)
                    {
                        cleanStr = cleanStr.Substring(commaIndex + 1);
                    }
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
                System.Diagnostics.Debug.WriteLine($"Ошибка обработки фото: {ex.Message}");
            }
            return null;
        }

        private void MainFrame_Navigated(object sender, NavigationEventArgs e)
        {
            if (e.Content is Pages.General.Auth || e.Content is Pages.General.Reg)
            {
                LeftMenu.Visibility = Visibility.Collapsed;
            }
            else
            {
                LeftMenu.Visibility = Visibility.Visible;
            }

            if (e.Content is Pages.Activist.Profile)
            {
                NavProfile.IsChecked = true;
            }
            else if (e.Content is Pages.Activist.Home || e.Content is Pages.Coordinator.CoordinatorHome)
            {
                NavHome.IsChecked = true;
            }
            else if (e.Content is Pages.Activist.Events || e.Content is Pages.Coordinator.CoordinatorEvents || e.Content is Pages.Activist.EventDetails || e.Content is Pages.Activist.EventRegistration)
            {
                NavEvents.IsChecked = true;
            }
            else if (e.Content is Pages.Activist.Sectors || e.Content is Pages.Activist.SectorDetails || e.Content is Pages.Coordinator.CoordinatorPanel)
            {
                NavSectors.IsChecked = true;
            }
            else if (e.Content is Pages.Curator.RegistrationRequests)
            {
                NavRegistrationRequests.IsChecked = true;
            }
            else if (e.Content is Pages.Activist.Tasks)
            {
                NavTasks.IsChecked = true;
            }
            else if (e.Content is Pages.Activist.Projects || e.Content is Pages.Activist.ProjectDetails)
            {
                NavProjects.IsChecked = true;
            }
            else if (e.Content is Pages.Activist.Rating)
            {
                NavRating.IsChecked = true;
            }
            else if (e.Content is Pages.Activist.Notifications)
            {
                NavNotifications.IsChecked = true;
            }
        }

        public void MenuProfile_Click(object sender, RoutedEventArgs e)
        {
            if (!(MainFrame.Content is Pages.Activist.Profile))
            {
                MainFrame.Navigate(new Pages.Activist.Profile());
            }
        }

        public void MenuHome_Click(object sender, RoutedEventArgs e)
        {
            var data = App.CurrentUserProfile;
            if (data == null) return;

            bool isCoordinator = data.roleTitle == "Coordinator" || data.roleTitle == "Sector_coordinator" || data.roleTitle == "Admin";

            if (isCoordinator && !App.IsActivistMode)
            {
                if (!(MainFrame.Content is Pages.Coordinator.CoordinatorHome))
                {
                    MainFrame.Navigate(new Pages.Coordinator.CoordinatorHome());
                }
            }
            else
            {
                if (!(MainFrame.Content is Pages.Activist.Home))
                {
                    MainFrame.Navigate(new Pages.Activist.Home());
                }
            }
        }

        private void MenuEvents_Click(object sender, RoutedEventArgs e)
        {
            var data = App.CurrentUserProfile;
            if (data == null) return;

            bool isCoordinator = data.roleTitle == "Coordinator" || data.roleTitle == "Sector_coordinator" || data.roleTitle == "Admin";

            if (isCoordinator && !App.IsActivistMode)
            {
                if (!(MainFrame.Content is Pages.Coordinator.CoordinatorEvents))
                {
                    MainFrame.Navigate(new Pages.Coordinator.CoordinatorEvents());
                }
            }
            else
            {
                if (!(MainFrame.Content is Pages.Activist.Events))
                {
                    MainFrame.Navigate(new Pages.Activist.Events());
                }
            }
        }

        private void MenuSectors_Click(object sender, RoutedEventArgs e)
        {
            var data = App.CurrentUserProfile;
            if (data == null) return;

            bool isCoordinator = data.roleTitle == "Sector_coordinator" || data.roleTitle == "Coordinator";

            if (isCoordinator && !App.IsActivistMode)
            {
                int sectorId = data.coordinatorSectorId ?? 0;

                if (sectorId > 0)
                {
                    if (!(MainFrame.Content is Pages.Coordinator.CoordinatorPanel currentPanel) || currentPanel.Tag?.ToString() != sectorId.ToString())
                    {
                        var panel = new Pages.Coordinator.CoordinatorPanel(sectorId);
                        panel.Tag = sectorId;
                        MainFrame.Navigate(panel);
                    }
                }
                else
                {
                    CustomMessageBox.Show("Сектор не найден в вашем профиле.", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            else
            {
                if (!(MainFrame.Content is Pages.Activist.Sectors))
                {
                    MainFrame.Navigate(new Pages.Activist.Sectors());
                }
            }
        }

        private void MenuRegistrationRequests_Click(object sender, RoutedEventArgs e)
        {
            if (!(MainFrame.Content is Pages.Curator.RegistrationRequests))
            {
                MainFrame.Navigate(new Pages.Curator.RegistrationRequests());
            }
        }

        private void MenuTasks_Click(object sender, RoutedEventArgs e)
        {
            if (!(MainFrame.Content is Pages.Activist.Tasks))
            {
                MainFrame.Navigate(new Pages.Activist.Tasks());
            }
        }

        private void MenuProjects_Click(object sender, RoutedEventArgs e)
        {
            if (!(MainFrame.Content is Pages.Activist.Projects))
            {
                MainFrame.Navigate(new Pages.Activist.Projects());
            }
        }

        private void MenuRating_Click(object sender, RoutedEventArgs e)
        {
            if (!(MainFrame.Content is Pages.Activist.Rating))
            {
                MainFrame.Navigate(new Pages.Activist.Rating());
            }
        }

        private void MenuNotifications_Click(object sender, RoutedEventArgs e)
        {
            if (!(MainFrame.Content is Pages.Activist.Notifications))
            {
                MainFrame.Navigate(new Pages.Activist.Notifications());
            }
        }

        private void Logout_Click(object sender, RoutedEventArgs e)
        {
            bool isConfirmed = CustomMessageBox.Show("Вы действительно хотите выйти из аккаунта?", "Подтверждение", CustomMessageBox.MessageType.Question, true);

            if (!isConfirmed)
            {
                return;
            }

            NavProfile.IsChecked = false;
            NavHome.IsChecked = false;
            NavEvents.IsChecked = false;
            NavSectors.IsChecked = false;
            NavRegistrationRequests.IsChecked = false;
            NavTasks.IsChecked = false;
            NavProjects.IsChecked = false;
            NavRating.IsChecked = false;
            NavNotifications.IsChecked = false;

            NavSectorsText.Text = "Сектора";
            NavSectorsBadge.Visibility = Visibility.Collapsed;

            NavRegistrationRequestsBadge.Visibility = Visibility.Collapsed;

            App.AuthToken = null;
            App.RefreshToken = null;
            App.CurrentUserProfile = null;
            App.IsActivistMode = false;

            App.ClearSession();

            MainFrame.Navigate(new Pages.General.Auth());

            CustomMessageBox.Show("Вы успешно вышли из системы.", "УВЕДОМЛЕНИЕ", CustomMessageBox.MessageType.Success);
        }

        private void Minimize_Click(object sender, RoutedEventArgs e)
        {
            this.WindowState = WindowState.Minimized;
        }

        private void Maximize_Click(object sender, RoutedEventArgs e)
        {
            if (this.WindowState == WindowState.Maximized)
                this.WindowState = WindowState.Normal;
            else
                this.WindowState = WindowState.Maximized;
        }

        private void Close_Click(object sender, RoutedEventArgs e)
        {
            Application.Current.Shutdown();
        }
    }
}