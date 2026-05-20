using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class CoordinatorEventDetails : Page
    {
        private int _eventId;

        public CoordinatorEventDetails(int eventId)
        {
            InitializeComponent();
            _eventId = eventId;
        }

        private void Page_Loaded(object sender, RoutedEventArgs e)
        {
            EventMainFrame.Navigate(new EventEditPage(_eventId));
        }

        private void Tab_Checked(object sender, RoutedEventArgs e)
        {
            if (!IsLoaded || EventMainFrame == null) return;

            if (TabEdit.IsChecked == true)
                EventMainFrame.Navigate(new EventEditPage(_eventId));
            else if (TabRequests.IsChecked == true)
                EventMainFrame.Navigate(new EventRequestsPage(_eventId));
            else if (TabParticipants.IsChecked == true)
                EventMainFrame.Navigate(new EventParticipantsPage(_eventId));
            else if (TabFinalize.IsChecked == true)
                EventMainFrame.Navigate(new EventFinalizePage(_eventId));
        }

        private void EventMainFrame_PreviewMouseWheel(object sender, MouseWheelEventArgs e)
        {
            if (!e.Handled)
            {
                e.Handled = true;
                var eventArg = new MouseWheelEventArgs(e.MouseDevice, e.Timestamp, e.Delta)
                {
                    RoutedEvent = UIElement.MouseWheelEvent,
                    Source = sender
                };
                var parent = ((Control)sender).Parent as UIElement;
                parent?.RaiseEvent(eventArg);
            }
        }
    }
}