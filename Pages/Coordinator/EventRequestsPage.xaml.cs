using System.Windows;
using System.Windows.Controls;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class EventRequestsPage : Page
    {
        private int _eventId;
        public EventRequestsPage(int eventId)
        {
            InitializeComponent();
            _eventId = eventId;
        }

        private void Page_Loaded(object sender, RoutedEventArgs e)
        {
        }

        private void ApproveRequest_Click(object sender, RoutedEventArgs e) { }
        private void RejectRequest_Click(object sender, RoutedEventArgs e) { }
    }
}