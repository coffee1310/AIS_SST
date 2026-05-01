using System.Windows;
using System.Windows.Media;

namespace Diplom_Stud.Components
{
    public partial class CustomMessageBox : Window
    {
        public CustomMessageBox(string message, string title, MessageType type)
        {
            InitializeComponent();
            txtMessage.Text = message;
            txtTitle.Text = title.ToUpper();

            switch (type)
            {
                case MessageType.Error:
                    IconPath.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#E81123"));
                    IconPath.Data = Geometry.Parse("M12,2C6.47,2,2,6.47,2,12s4.47,10,10,10s10-4.47,10-10S17.53,2,12,2z M17,15.59L15.59,17L12,13.41L8.41,17L7,15.59L10.59,12L7,8.41L8.41,7L12,10.59L15.59,7L17,8.41L13.41,12L17,15.59z");
                    break;
                case MessageType.Success:
                    IconPath.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#02B3BA"));
                    IconPath.Data = Geometry.Parse("M12,2C6.48,2,2,6.48,2,12s4.48,10,10,10s10-4.48,10-10S17.52,2,12,2z M10,17l-5-5l1.41-1.41L10,14.17l7.59-7.59L19,8L10,17z");
                    break;
            }
        }

        public static void Show(string message, string title = "Уведомление", MessageType type = MessageType.Info)
        {
            var msg = new CustomMessageBox(message, title, type);
            msg.ShowDialog();
        }

        private void Ok_Click(object sender, RoutedEventArgs e)
        {
            this.DialogResult = true;
            this.Close();
        }

        public enum MessageType
        {
            Info,
            Error,
            Success
        }
    }
}