using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media.Animation;

namespace Diplom_Stud.Pages.General
{
    public partial class Reg : Page
    {
        public Reg()
        {
            InitializeComponent();
            this.Loaded += Page_Loaded;

            // Чтобы комбобоксы не светились пустыми, выделяем нулевой (неактивный) индекс при старте
            cbStatus.SelectedIndex = 0;
            cbGender.SelectedIndex = 0;
        }

        private void Page_Loaded(object sender, RoutedEventArgs e)
        {
            // Плавное появление страницы
            DoubleAnimation fadeInAnimation = new DoubleAnimation
            {
                From = 0.0,
                To = 1.0,
                Duration = TimeSpan.FromSeconds(1.0),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };
            this.BeginAnimation(Page.OpacityProperty, fadeInAnimation);
        }

        // ==============================================
        // ВАЛИДАЦИЯ ПРИ РЕГИСТРАЦИИ
        // ==============================================

        private void Button_Register_Click(object sender, RoutedEventArgs e)
        {
            // 1. Сбрасываем старые ошибки
            errLastName.Visibility = Visibility.Collapsed;
            errFirstName.Visibility = Visibility.Collapsed;
            errPatronymic.Visibility = Visibility.Collapsed;
            errBirthDate.Visibility = Visibility.Collapsed;
            errStatus.Visibility = Visibility.Collapsed;
            errGender.Visibility = Visibility.Collapsed;
            errGroup.Visibility = Visibility.Collapsed;
            errStudentId.Visibility = Visibility.Collapsed;
            errEmail.Visibility = Visibility.Collapsed;
            errPhone.Visibility = Visibility.Collapsed;
            errPhoto.Visibility = Visibility.Collapsed;
            errAgreement.Visibility = Visibility.Collapsed;

            bool hasError = false;

            // 2. Проверяем каждое обязательное поле (со звездочкой)
            if (string.IsNullOrWhiteSpace(tbLastName.Text)) { errLastName.Visibility = Visibility.Visible; hasError = true; }
            if (string.IsNullOrWhiteSpace(tbFirstName.Text)) { errFirstName.Visibility = Visibility.Visible; hasError = true; }
            if (string.IsNullOrWhiteSpace(tbPatronymic.Text)) { errPatronymic.Visibility = Visibility.Visible; hasError = true; }
            if (string.IsNullOrWhiteSpace(tbBirthDate.Text)) { errBirthDate.Visibility = Visibility.Visible; hasError = true; }

            // Проверка комбобоксов (0 - это "Выберите статус")
            if (cbStatus.SelectedIndex <= 0) { errStatus.Visibility = Visibility.Visible; hasError = true; }
            if (cbGender.SelectedIndex <= 0) { errGender.Visibility = Visibility.Visible; hasError = true; }

            if (string.IsNullOrWhiteSpace(tbGroup.Text)) { errGroup.Visibility = Visibility.Visible; hasError = true; }
            if (string.IsNullOrWhiteSpace(tbStudentId.Text)) { errStudentId.Visibility = Visibility.Visible; hasError = true; }
            if (string.IsNullOrWhiteSpace(tbEmail.Text)) { errEmail.Visibility = Visibility.Visible; hasError = true; }

            // Необязательные поля (tbAdditionalEmail и tbVkLink) - не проверяем

            if (string.IsNullOrWhiteSpace(tbPhone.Text)) { errPhone.Visibility = Visibility.Visible; hasError = true; }
            if (string.IsNullOrWhiteSpace(tbPhoto.Text)) { errPhoto.Visibility = Visibility.Visible; hasError = true; }

            // Проверка галочки согласия
            if (cbAgreement.IsChecked != true) { errAgreement.Visibility = Visibility.Visible; hasError = true; }

            // Если есть хоть одна ошибка - останавливаем регистрацию
            if (hasError)
            {
                return;
            }

            // ==========================================
            // ЗДЕСЬ БУДЕТ ВАША ЛОГИКА ОТПРАВКИ НА СЕРВЕР
            // ==========================================

            MessageBox.Show("Все поля успешно заполнены!", "Успех", MessageBoxButton.OK, MessageBoxImage.Information);

            // После успешной регистрации перекидываем обратно на окно авторизации
            NavigationService?.Navigate(new Auth());
        }

        private void LoginText_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            NavigationService?.Navigate(new Auth());
        }
    }
}