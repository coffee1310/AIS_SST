using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media.Animation;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class CoordinatorHome : Page
    {
        public CoordinatorHome()
        {
            InitializeComponent();
        }

        private void Page_Loaded(object sender, RoutedEventArgs e)
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
        }

        private void LoadUserData()
        {
            var user = App.CurrentUserProfile;

            if (user != null)
            {
                tbUserName.Text = $"{user.surname} {user.name} {user.patronymic}".Trim();
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
    }
}