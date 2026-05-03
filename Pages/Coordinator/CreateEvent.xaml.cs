using Diplom_Stud.Components;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class CreateEvent : Page, INotifyPropertyChanged
    {
        public ObservableCollection<RoleItem> Roles { get; set; } = new ObservableCollection<RoleItem>();

        private string _rolesCountText = "0 ролей";
        public string RolesCountText
        {
            get => _rolesCountText;
            set
            {
                _rolesCountText = value;
                OnPropertyChanged(nameof(RolesCountText));
            }
        }

        public CreateEvent()
        {
            InitializeComponent();

            Roles.CollectionChanged += Roles_CollectionChanged;

            Roles.Add(new RoleItem { IsExpanded = true, SelectedRole = "Фотограф" });

            this.DataContext = this;
        }

        private void Roles_CollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            UpdateRolesCount();
        }

        private void UpdateRolesCount()
        {
            int count = Roles.Count;
            if (count % 10 == 1 && count % 100 != 11)
                RolesCountText = $"{count} роль";
            else if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20))
                RolesCountText = $"{count} роли";
            else
                RolesCountText = $"{count} ролей";
        }

        private void AddRole_Click(object sender, MouseButtonEventArgs e)
        {
            Roles.Add(new RoleItem { IsExpanded = true });
        }

        private void RemoveRole_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is RoleItem role)
            {
                Roles.Remove(role);
            }
        }

        private void CreateFinalEvent_Click(object sender, RoutedEventArgs e)
        {
            CustomMessageBox.Show("Мероприятие успешно создано!", "Успех", CustomMessageBox.MessageType.Success);
            this.NavigationService.GoBack();
        }

        private void NumericOnly_PreviewTextInput(object sender, TextCompositionEventArgs e)
        {
            e.Handled = !e.Text.All(char.IsDigit);
        }

        private void Time_PreviewTextInput(object sender, TextCompositionEventArgs e)
        {
            e.Handled = !e.Text.All(char.IsDigit);
        }

        private bool _isTimeFormatting = false;

        private void Time_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (_isTimeFormatting) return;

            if (sender is TextBox tb)
            {
                _isTimeFormatting = true;
                string text = tb.Text.Replace(":", "");

                if (text.Length > 4) text = text.Substring(0, 4);

                if (text.Length >= 3)
                {
                    tb.Text = text.Insert(2, ":");
                    tb.CaretIndex = tb.Text.Length;
                }
                else
                {
                    tb.Text = text;
                    tb.CaretIndex = tb.Text.Length;
                }
                _isTimeFormatting = false;
            }
        }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string propertyName)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }
    }

    public class RoleItem : INotifyPropertyChanged
    {
        private bool _isExpanded = false;
        public bool IsExpanded
        {
            get => _isExpanded;
            set { _isExpanded = value; OnPropertyChanged(nameof(IsExpanded)); }
        }

        private string _selectedRole;
        public string SelectedRole
        {
            get => _selectedRole;
            set { _selectedRole = value; OnPropertyChanged(nameof(SelectedRole)); }
        }

        public string Tasks { get; set; }
        public string PeopleCount { get; set; } = "1";
        public string Points { get; set; } = "10";
        public string ReserveCount { get; set; } = "0";
        public string DeadlineDate { get; set; }
        public string DeadlineTime { get; set; }
        public string Sector { get; set; } = "Мультимедийный сектор";
        public List<string> AvailableRoles { get; set; } = new List<string> { "Фотограф", "Видеограф", "Волонтер регистрации", "Дизайнер", "SMM-специалист", "Ведущий" };
        public List<string> AvailableSectors { get; set; } = new List<string> { "Мультимедийный сектор", "Спортивный сектор", "Сектор контроля качества" };

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string propertyName)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }
    }
}