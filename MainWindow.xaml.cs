using Diplom_Stud.Pages.Activist;
using Diplom_Stud.Pages.General;
using Diplom_Stud.Components;
using System;
using System.IO;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;

namespace Diplom_Stud
{
    public partial class MainWindow : Window
    {
        public MainWindow()
        {
            InitializeComponent();
            MainFrame.Navigate(new Pages.General.Auth());
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

                    if (nameText != null)
                        nameText.Text = $"{data.surname} {data.name}";

                    if (emailText != null)
                        emailText.Text = data.studentEmail ?? "";

                    if (ellipse != null && !string.IsNullOrEmpty(data.photo))
                    {
                        BitmapImage bmp = GetImageFromBase64(data.photo);
                        if (bmp != null)
                        {
                            ellipse.Fill = new ImageBrush(bmp) { Stretch = Stretch.UniformToFill };
                        }
                    }
                }
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
            else if (e.Content is Pages.Activist.Home)
            {
                NavHome.IsChecked = true;
            }
            else if (e.Content is Pages.Activist.Events || e.Content is Pages.Activist.EventDetails || e.Content is Pages.Activist.EventRegistration)
            {
                NavEvents.IsChecked = true;
            }
            else if (e.Content is Pages.Activist.Sectors || e.Content is Pages.Activist.SectorDetails)
            {
                NavSectors.IsChecked = true;
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

        private void MenuProfile_Click(object sender, RoutedEventArgs e)
        {
            if (!(MainFrame.Content is Pages.Activist.Profile))
            {
                MainFrame.Navigate(new Pages.Activist.Profile());
            }
        }

        private void MenuHome_Click(object sender, RoutedEventArgs e)
        {
            if (!(MainFrame.Content is Pages.Activist.Home))
            {
                MainFrame.Navigate(new Pages.Activist.Home());
            }
        }

        private void MenuEvents_Click(object sender, RoutedEventArgs e)
        {
            if (!(MainFrame.Content is Pages.Activist.Events))
            {
                MainFrame.Navigate(new Pages.Activist.Events());
            }
        }

        private void MenuSectors_Click(object sender, RoutedEventArgs e)
        {
            if (!(MainFrame.Content is Pages.Activist.Sectors))
            {
                MainFrame.Navigate(new Pages.Activist.Sectors());
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
            MessageBoxResult result = MessageBox.Show(
                "Вы уверены, что хотите выйти из аккаунта?",
                "Подтверждение выхода",
                MessageBoxButton.YesNo,
                MessageBoxImage.Question);

            if (result == MessageBoxResult.Yes)
            {
                NavProfile.IsChecked = false;
                NavHome.IsChecked = false;
                NavEvents.IsChecked = false;
                NavTasks.IsChecked = false;
                NavProjects.IsChecked = false;
                NavRating.IsChecked = false;
                NavNotifications.IsChecked = false;

                App.AuthToken = null;
                App.CurrentUserProfile = null;

                MainFrame.Navigate(new Pages.General.Auth());
            }
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