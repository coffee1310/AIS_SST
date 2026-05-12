using System.Windows;
using System.Windows.Media;

namespace Diplom_Stud.Components
{
    public partial class CustomMessageBox : Window
    {
        public bool Result { get; private set; } = false;

        public CustomMessageBox(string message, string title, MessageType type, bool isConfirm)
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
                case MessageType.Question:
                    IconPath.Fill = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#A084FB"));
                    IconPath.Data = Geometry.Parse("M11,18h2v-2h-2V18z M12,2C6.48,2,2,6.48,2,12s4.48,10,10,10s10-4.48,10-10S17.52,2,12,2z M12,20c-4.41,0-8-3.59-8-8s3.59-8,8-8s8,3.59,8,8S16.41,20,12,20z M12,6c-2.21,0-4,1.79-4,4h2c0-1.1,0.9-2,2-2s2,0.9,2,2c0,2-3,1.75-3,5h2c0-2.25,3-2.5,3-5C16,7.79,14.21,6,12,6z");
                    break;
            }

            if (isConfirm)
            {
                btnCancel.Visibility = Visibility.Visible;
                btnOk.Content = "ДА";
            }
        }

        public static bool Show(string message, string title = "Уведомление", MessageType type = MessageType.Info, bool isConfirm = false)
        {
            var msg = new CustomMessageBox(message, title, type, isConfirm);
            msg.ShowDialog();
            return msg.Result;
        }

        private void Ok_Click(object sender, RoutedEventArgs e)
        {
            this.Result = true;
            this.DialogResult = true;
            this.Close();
        }

        private void Cancel_Click(object sender, RoutedEventArgs e)
        {
            this.Result = false;
            this.DialogResult = false;
            this.Close();
        }

        public enum MessageType
        {
            Info,
            Error,
            Success,
            Question 
        }
    }
}