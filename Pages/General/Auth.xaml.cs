using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Windows.Threading;

namespace Diplom_Stud.Pages.General
{
    /// <summary>
    /// Логика взаимодействия для Auth.xaml
    /// </summary>
    public partial class Auth : Page
    {
        private DispatcherTimer _slideTimer;
        private int _currentSlideIndex = 0;
        private readonly int _totalSlides = 3;

        public Auth()
        {
            InitializeComponent();
        }
        

        private void Page_Loaded(object sender, RoutedEventArgs e)
        {
            // Инициализация таймера для перелистывания каждые 5 секунд
            _slideTimer = new DispatcherTimer();
            _slideTimer.Interval = TimeSpan.FromSeconds(5);
            _slideTimer.Tick += SlideTimer_Tick;
            _slideTimer.Start();
        }

        private void Page_Unloaded(object sender, RoutedEventArgs e)
        {
            // Остановка таймера при закрытии страницы
            _slideTimer?.Stop();
        }

        private void SlideTimer_Tick(object sender, EventArgs e)
        {
            // Переключение на следующий слайд
            int nextSlideIndex = (_currentSlideIndex + 1) % _totalSlides;
            AnimateSlideTransition(_currentSlideIndex, nextSlideIndex);
            _currentSlideIndex = nextSlideIndex;
        }

        private void AnimateSlideTransition(int fromIndex, int toIndex)
        {
            // Получаем изображения по индексам
            Image fromImage = GetImageByIndex(fromIndex);
            Image toImage = GetImageByIndex(toIndex);

            if (fromImage == null || toImage == null) return;

            // Создаем анимацию для текущего изображения (плавно исчезает)
            DoubleAnimation fromAnimation = new DoubleAnimation
            {
                From = 1,
                To = 0,
                Duration = TimeSpan.FromSeconds(1),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseInOut }
            };

            // Создаем анимацию для нового изображения (плавно появляется)
            DoubleAnimation toAnimation = new DoubleAnimation
            {
                From = 0,
                To = 1,
                Duration = TimeSpan.FromSeconds(1),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseInOut }
            };

            // Применяем анимации
            fromImage.BeginAnimation(Image.OpacityProperty, fromAnimation);
            toImage.BeginAnimation(Image.OpacityProperty, toAnimation);

            // Обновляем индикаторы
            UpdateIndicators(toIndex);
        }

        private Image GetImageByIndex(int index)
        {
            switch (index)
            {
                case 0: return SlideImage1;
                case 1: return SlideImage2;
                case 2: return SlideImage3;
                default: return null;
            }
        }

        private void UpdateIndicators(int activeIndex)
        {
            // Сброс всех индикаторов
            Indicator1.Fill = System.Windows.Media.Brushes.Gray;
            Indicator2.Fill = System.Windows.Media.Brushes.Gray;
            Indicator3.Fill = System.Windows.Media.Brushes.Gray;

            // Подсветка активного индикатора
            switch (activeIndex)
            {
                case 0:
                    Indicator1.Fill = System.Windows.Media.Brushes.MediumPurple;
                    break;
                case 1:
                    Indicator2.Fill = System.Windows.Media.Brushes.MediumPurple;
                    break;
                case 2:
                    Indicator3.Fill = System.Windows.Media.Brushes.MediumPurple;
                    break;
            }
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            // Обработка кнопки входа
            // Ваш существующий код
        }
    }
}
