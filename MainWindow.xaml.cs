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

        // Вызывается каждый раз, когда фрейм меняет страницу
        private void MainFrame_Navigated(object sender, NavigationEventArgs e)
        {
            // Прячем меню на страницах входа и регистрации
            if (e.Content is Pages.General.Auth || e.Content is Pages.General.Reg)
            {
                LeftMenu.Visibility = Visibility.Collapsed;
            }
            else
            {
                LeftMenu.Visibility = Visibility.Visible;
            }

            // АВТОМАТИЧЕСКАЯ ПОДСВЕТКА МЕНЮ В ЗАВИСИМОСТИ ОТ СТРАНИЦЫ
            if (e.Content is Pages.Activist.Profile)
            {
                NavProfile.IsChecked = true;
            }
            else if (e.Content is Pages.Activist.Home)
            {
                NavHome.IsChecked = true;
            }
            // По аналогии будете добавлять остальные страницы:
            // else if (e.Content is Pages.Activist.Events) { NavEvents.IsChecked = true; }
        }

        // === МЕТОДЫ ДЛЯ МЕНЮ ===

        private void MenuProfile_Click(object sender, RoutedEventArgs e)
        {
            // При клике на карточку профиля в меню переходим на саму страницу профиля
            if (!(MainFrame.Content is Pages.Activist.Profile))
            {
                MainFrame.Navigate(new Pages.Activist.Profile());
            }
        }

        private void MenuHome_Click(object sender, RoutedEventArgs e)
        {
            // Переход на страницу Главная
            if (!(MainFrame.Content is Pages.Activist.Home))
            {
                MainFrame.Navigate(new Pages.Activist.Home());
            }
        }

        // Кнопка Выход
        private void Logout_Click(object sender, RoutedEventArgs e)
        {
            // Показываем стандартное окно подтверждения
            MessageBoxResult result = MessageBox.Show(
                "Вы уверены, что хотите выйти из аккаунта?",
                "Подтверждение выхода",
                MessageBoxButton.YesNo,
                MessageBoxImage.Question);

            // Если пользователь нажал "Да"
            if (result == MessageBoxResult.Yes)
            {
                // Очищаем выделение в меню, чтобы при следующем входе оно было чистым
                NavProfile.IsChecked = false;
                NavHome.IsChecked = false;
                NavEvents.IsChecked = false;
                NavTasks.IsChecked = false;
                NavProjects.IsChecked = false;
                NavRating.IsChecked = false;
                NavNotifications.IsChecked = false;

                // Переходим на страницу входа
                MainFrame.Navigate(new Pages.General.Auth());
            }
        }

        // === МЕТОДЫ ДЛЯ КАСТОМНОЙ ШАПКИ ОКНА ===

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