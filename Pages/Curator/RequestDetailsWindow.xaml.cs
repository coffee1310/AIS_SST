using System.Windows;
using System.Windows.Input;

namespace Diplom_Stud.Pages.Curator
{
    public partial class RequestDetailsWindow : Window
    {
        public RequestDetailsWindow(RequestDetailsViewModel viewModel)
        {
            InitializeComponent();
            DataContext = viewModel; 
        }

        private void CloseDetails_Click(object sender, RoutedEventArgs e)
        {
            this.Close();
        }

        private void Window_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.LeftButton == MouseButtonState.Pressed)
            {
                DragMove();
            }
        }
    }
}