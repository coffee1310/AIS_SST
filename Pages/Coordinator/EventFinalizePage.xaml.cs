using System.Windows;
using System.Windows.Controls;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class EventFinalizePage : Page
    {
        private int _eventId;
        public EventFinalizePage(int eventId)
        {
            InitializeComponent();
            _eventId = eventId;
        }

        private void Page_Loaded(object sender, RoutedEventArgs e)
        {
            // Подвязывай список тех же участников, но с полем для баллов
        }

        private void FinalizeEvent_Click(object sender, RoutedEventArgs e) { }
    }
}