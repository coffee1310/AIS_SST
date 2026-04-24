using Diplom_Stud.Pages.General;
using System.Windows;
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

            else if (e.Content is Pages.Activist.Events)
            {
                NavEvents.IsChecked = true;
            }

            else if (e.Content is Pages.Activist.Sectors) 
                NavSectors.IsChecked = true;

            else if (e.Content is Pages.Activist.Tasks) 
            {
                NavTasks.IsChecked = true;
            }

            else if (e.Content is Pages.Activist.Projects)
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