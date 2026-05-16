using System.Windows;
using System.Windows.Controls;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class EventParticipantsPage : Page
    {
        private int _eventId;
        public EventParticipantsPage(int eventId)
        {
            InitializeComponent();
            _eventId = eventId;
        }

        private void Page_Loaded(object sender, RoutedEventArgs e)
        {
            // Подвязывай список утвержденных участников
        }

        private void KickParticipant_Click(object sender, RoutedEventArgs e) { }
    }
}