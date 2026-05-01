using Diplom_Stud.Pages.Activist;
using Diplom_Stud.Pages.General;
using Diplom_Stud.Components;
using System;
using System.IO;
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

        // Этот метод вызывается из Profile.xaml.cs после успешного получения данных
        public void UpdateUserMenu()
        {
            var data = App.CurrentUserProfile;
            if (data == null) return;

            // Находим элементы внутри RadioButton NavProfile
            if (NavProfile.Content is StackPanel panel)
            {
                // Ищем аватарку, Имя и Email по порядку
                if (panel.Children.Count >= 3)
                {
                    var ellipse = panel.Children[0] as System.Windows.Shapes.Ellipse;
                    var nameText = panel.Children[1] as TextBlock;
                    var emailText = panel.Children[2] as TextBlock;

                    if (nameText != null)
                        nameText.Text = $"{data.surname} {data.name}"; // Обычно в меню пишут Фамилию и Имя

                    if (emailText != null)
                        emailText.Text = data.studentEmail ?? "";

                    // Загружаем фото в боковое меню
                    if (ellipse != null && !string.IsNullOrEmpty(data.photo))
                    {
                        try
                        {
                            string base64Data = data.photo;
                            int commaIndex = base64Data.IndexOf(',');
                            if (commaIndex >= 0)
                            {
                                base64Data = base64Data.Substring(commaIndex + 1);
                            }

                            byte[] imageBytes = Convert.FromBase64String(base64Data);
                            using (var ms = new MemoryStream(imageBytes))
                            {
                                var bitmap = new BitmapImage();
                                bitmap.BeginInit();
                                bitmap.CacheOption = BitmapCacheOption.OnLoad;
                                bitmap.StreamSource = ms;
                                bitmap.EndInit();

                                ellipse.Fill = new ImageBrush(bitmap) { Stretch = Stretch.UniformToFill };
                            }
                        }
                        catch (Exception ex)
                        {
                            System.Diagnostics.Debug.WriteLine($"Ошибка загрузки фото в меню: {ex.Message}");
                        }
                    }
                }
            }
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