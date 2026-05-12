using Diplom_Stud.Components;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Input;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class CreateEvent : Page, INotifyPropertyChanged
    {
        private static readonly HttpClient _httpClient = new HttpClient();

        private List<RoleDto> _apiRoles = new List<RoleDto>();
        private List<SectorDto> _apiSectors = new List<SectorDto>();

        public ObservableCollection<RoleItem> Roles { get; set; } = new ObservableCollection<RoleItem>();

        public ObservableCollection<UserDto> SelectedOrganizers { get; set; } = new ObservableCollection<UserDto>();

        private string _eventTitle;
        public string EventTitle { get => _eventTitle; set { _eventTitle = value; OnPropertyChanged(nameof(EventTitle)); } }

        private string _eventDescription;
        public string EventDescription { get => _eventDescription; set { _eventDescription = value; OnPropertyChanged(nameof(EventDescription)); } }

        private DateTime? _eventDate;
        public DateTime? EventDate { get => _eventDate; set { _eventDate = value; OnPropertyChanged(nameof(EventDate)); } }

        private string _startTime;
        public string StartTime { get => _startTime; set { _startTime = value; OnPropertyChanged(nameof(StartTime)); } }

        private string _endTime;
        public string EndTime { get => _endTime; set { _endTime = value; OnPropertyChanged(nameof(EndTime)); } }

        private string _venue;
        public string Venue { get => _venue; set { _venue = value; OnPropertyChanged(nameof(Venue)); } }

        private bool _isPublic = true;
        public bool IsPublic { get => _isPublic; set { _isPublic = value; OnPropertyChanged(nameof(IsPublic)); } }

        private bool _isFree = false;
        public bool IsFree { get => _isFree; set { _isFree = value; OnPropertyChanged(nameof(IsFree)); } }

        private bool _isDraft = false;
        public bool IsDraft { get => _isDraft; set { _isDraft = value; OnPropertyChanged(nameof(IsDraft)); } }

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

            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            }

            Roles.CollectionChanged += Roles_CollectionChanged;

            CbUsers.AddHandler(TextBoxBase.TextChangedEvent, new TextChangedEventHandler(CbUsers_TextChanged));

            MainRoot.DataContext = this;
        }

        private async void Page_Loaded(object sender, RoutedEventArgs e)
        {
            await LoadDataFromApiAsync();
        }

        private async Task LoadDataFromApiAsync()
        {
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                var rolesResp = await _httpClient.GetAsync("/api/roles");
                if (rolesResp.IsSuccessStatusCode)
                {
                    string json = await rolesResp.Content.ReadAsStringAsync();
                    _apiRoles = JsonSerializer.Deserialize<List<RoleDto>>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<RoleDto>();
                }

                var sectorsResp = await _httpClient.GetAsync("/api/sector");
                if (sectorsResp.IsSuccessStatusCode)
                {
                    string json = await sectorsResp.Content.ReadAsStringAsync();
                    _apiSectors = JsonSerializer.Deserialize<List<SectorDto>>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<SectorDto>();
                }

                var usersResp = await _httpClient.GetAsync("/api/users/all?page=0&size=1000&sortBy=id&sortDirection=ASC");
                if (usersResp.IsSuccessStatusCode)
                {
                    string json = await usersResp.Content.ReadAsStringAsync();
                    var pageData = JsonSerializer.Deserialize<PageResponse_Temp>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    if (pageData?.content != null)
                    {
                        var validUsers = pageData.content
                            .Where(u => string.IsNullOrEmpty(u.role) ||
                                       (!u.role.ToLower().Contains("curator") && !u.role.ToLower().Contains("admin")))
                            .ToList();

                        Application.Current.Dispatcher.Invoke(() =>
                        {
                            CbUsers.Items.Clear();
                            foreach (var user in validUsers)
                            {
                                CbUsers.Items.Add(new ComboBoxItem
                                {
                                    Content = user.DisplayName,
                                    Tag = user, 
                                    Foreground = System.Windows.Media.Brushes.White
                                });
                            }
                        });
                    }
                }

                if (Roles.Count == 0)
                {
                    Roles.Add(new RoleItem { IsExpanded = true, AvailableRoles = _apiRoles, AvailableSectors = _apiSectors });
                }
                else
                {
                    foreach (var role in Roles)
                    {
                        role.AvailableRoles = _apiRoles;
                        role.AvailableSectors = _apiSectors;
                    }
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка сети при загрузке справочников: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
        }
        private void CbUsers_TextChanged(object sender, TextChangedEventArgs e)
        {
            var cb = (ComboBox)sender;
            var tb = e.OriginalSource as TextBox;

            if (tb != null && cb.IsEditable && tb.IsFocused)
            {
                string text = tb.Text;
                cb.Items.Filter = item =>
                {
                    if (string.IsNullOrEmpty(text)) return true;
                    var cbi = item as ComboBoxItem;
                    if (cbi == null) return true;

                    return cbi.Content.ToString().IndexOf(text, StringComparison.OrdinalIgnoreCase) >= 0;
                };
                cb.IsDropDownOpen = true;
            }
        }

        private void AddOrganizer_Click(object sender, RoutedEventArgs e)
        {
            if (CbUsers.SelectedItem is ComboBoxItem selectedItem && selectedItem.Tag is UserDto selectedUser)
            {
                if (!SelectedOrganizers.Any(u => u.id == selectedUser.id))
                {
                    SelectedOrganizers.Add(selectedUser);
                }
                CbUsers.SelectedItem = null;
                CbUsers.Text = "";
                CbUsers.Items.Filter = null;
            }
            else if (!string.IsNullOrWhiteSpace(CbUsers.Text))
            {
                string typedText = CbUsers.Text.Trim();
                foreach (ComboBoxItem item in CbUsers.Items)
                {
                    if (item.Content.ToString().Equals(typedText, StringComparison.OrdinalIgnoreCase))
                    {
                        if (item.Tag is UserDto matchedUser && !SelectedOrganizers.Any(u => u.id == matchedUser.id))
                        {
                            SelectedOrganizers.Add(matchedUser);
                        }
                        break;
                    }
                }
                CbUsers.SelectedItem = null;
                CbUsers.Text = "";
                CbUsers.Items.Filter = null;
            }
        }

        private void RemoveOrganizer_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is UserDto user)
            {
                SelectedOrganizers.Remove(user);
            }
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
            Roles.Add(new RoleItem { IsExpanded = true, AvailableRoles = _apiRoles, AvailableSectors = _apiSectors });
        }

        private void RemoveRole_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is RoleItem role)
            {
                Roles.Remove(role);
            }
        }

        private async void CreateFinalEvent_Click(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrWhiteSpace(EventTitle)) { CustomMessageBox.Show("Введите название мероприятия.", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
            if (EventDate == null) { CustomMessageBox.Show("Выберите дату проведения мероприятия.", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
            if (string.IsNullOrWhiteSpace(StartTime) || StartTime.Length < 5) { CustomMessageBox.Show("Введите корректное время начала (ЧЧ:ММ).", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
            if (string.IsNullOrWhiteSpace(EndTime) || EndTime.Length < 5) { CustomMessageBox.Show("Введите корректное время конца (ЧЧ:ММ).", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
            if (string.IsNullOrWhiteSpace(Venue)) { CustomMessageBox.Show("Укажите место проведения.", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }

            foreach (var role in Roles)
            {
                if (role.SelectedRole == null) { CustomMessageBox.Show("В одной из добавленных карточек не выбрана роль.", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
                if (role.DeadlineDate == null) { CustomMessageBox.Show($"Для роли '{role.SelectedRole.title}' не выбран дедлайн (дата).", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
                if (string.IsNullOrWhiteSpace(role.DeadlineTime) || role.DeadlineTime.Length < 5) { CustomMessageBox.Show($"Для роли '{role.SelectedRole.title}' укажите время дедлайна (ЧЧ:ММ).", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
            }

            btnCreate.IsEnabled = false;
            btnCreate.Content = "Отправка...";

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                var eventPayload = new
                {
                    title = EventTitle,
                    description = EventDescription ?? "",
                    dateOfEvent = EventDate.Value.ToString("yyyy-MM-dd"),
                    startTime = StartTime + ":00",
                    endTime = EndTime + ":00",
                    venue = Venue,
                    referenceToPosition = string.IsNullOrWhiteSpace(EventDescription) ? "Описание отсутствует" : EventDescription,
                    isPublic = IsPublic,
                    @public = IsPublic,
                    isDraft = IsDraft,
                    draft = IsDraft,
                    organizer_id = App.CurrentUser?.Id ?? 0,
                    organizerId = App.CurrentUser?.Id ?? 0
                };

                string eventJson = JsonSerializer.Serialize(eventPayload);
                var eventContent = new StringContent(eventJson, Encoding.UTF8, "application/json");

                HttpResponseMessage eventResponse = await _httpClient.PostAsync("/api/events", eventContent);

                if (!eventResponse.IsSuccessStatusCode)
                {
                    string err = await eventResponse.Content.ReadAsStringAsync();
                    CustomMessageBox.Show($"Не удалось создать мероприятие: {eventResponse.StatusCode}\n{err}", "Ошибка сервера", CustomMessageBox.MessageType.Error);
                    return;
                }

                string eventResultJson = await eventResponse.Content.ReadAsStringAsync();
                var createdEvent = JsonSerializer.Deserialize<EventResponseDto>(eventResultJson, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                if (createdEvent == null || createdEvent.id == 0)
                {
                    CustomMessageBox.Show("Мероприятие создано, но сервер не вернул его ID. Роли не добавлены.", "Ошибка", CustomMessageBox.MessageType.Error);
                    return;
                }

                int newEventId = createdEvent.id;

                int rolesAdded = 0;
                foreach (var role in Roles)
                {
                    string deadlineFormatted = $"{role.DeadlineDate.Value.ToString("yyyy-MM-dd")}T{role.DeadlineTime}:00";

                    var rolePayload = new
                    {
                        eventId = newEventId,
                        globalEventRoleId = role.SelectedRole.id,
                        capacity = int.TryParse(role.PeopleCount, out int cap) ? cap : 1,
                        reserveCapacity = int.TryParse(role.ReserveCount, out int res) ? res : 0,
                        deadline = deadlineFormatted,
                        description = role.Tasks ?? ""
                    };

                    string roleJson = JsonSerializer.Serialize(rolePayload);
                    var roleContent = new StringContent(roleJson, Encoding.UTF8, "application/json");

                    HttpResponseMessage roleResponse = await _httpClient.PostAsync("/api/event-roles", roleContent);
                    if (roleResponse.IsSuccessStatusCode) rolesAdded++;
                }

                int organizersAdded = 0;
                foreach (var org in SelectedOrganizers)
                {
                    var orgContent = new StringContent("", Encoding.UTF8, "application/json");
                    HttpResponseMessage orgResponse = await _httpClient.PostAsync($"/api/events/{newEventId}/organizers/{org.id}", orgContent);
                    if (orgResponse.IsSuccessStatusCode) organizersAdded++;
                }

                CustomMessageBox.Show($"Мероприятие успешно создано!\nДобавлено ролей: {rolesAdded}/{Roles.Count}\nДобавлено доп. организаторов: {organizersAdded}/{SelectedOrganizers.Count}", "Успех", CustomMessageBox.MessageType.Success);
                this.NavigationService.GoBack();
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Произошла ошибка при отправке запроса: {ex.Message}", "Сбой", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                btnCreate.IsEnabled = true;
                btnCreate.Content = "Создать мероприятие";
            }
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

    public class UserDto
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public string groupName { get; set; }
        public string specialityShortTitle { get; set; }

        public string role { get; set; }

        public string FullName => $"{surname} {name} {patronymic}".Trim();
        public UserDto() { }

        public string DisplayName => string.IsNullOrEmpty(groupName)
            ? FullName
            : $"{FullName} ({specialityShortTitle}-{groupName})";

        public override string ToString() => DisplayName;
    }

    public class EventResponseDto
    {
        public int id { get; set; }
    }

    public class RoleDto
    {
        public int id { get; set; }
        public string title { get; set; }
        public string description { get; set; }
        public int sectorId { get; set; }
        public string sectorTitle { get; set; }
        public string createdAt { get; set; }
        public string updatedAt { get; set; }

        public override string ToString()
        {
            return title;
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

        private List<SectorDto> _availableSectors;
        public List<SectorDto> AvailableSectors
        {
            get => _availableSectors;
            set { _availableSectors = value; OnPropertyChanged(nameof(AvailableSectors)); }
        }

        private SectorDto _selectedSector;
        public SectorDto SelectedSector
        {
            get => _selectedSector;
            set { _selectedSector = value; OnPropertyChanged(nameof(SelectedSector)); }
        }

        private RoleDto _selectedRole;
        public RoleDto SelectedRole
        {
            get => _selectedRole;
            set
            {
                _selectedRole = value;
                OnPropertyChanged(nameof(SelectedRole));

                if (_selectedRole != null && AvailableSectors != null)
                {
                    var sector = AvailableSectors.FirstOrDefault(s => s.id == _selectedRole.sectorId);
                    if (sector != null)
                    {
                        SelectedSector = sector;
                    }
                }
            }
        }

        private List<RoleDto> _availableRoles;
        public List<RoleDto> AvailableRoles
        {
            get => _availableRoles;
            set { _availableRoles = value; OnPropertyChanged(nameof(AvailableRoles)); }
        }

        public string Tasks { get; set; }
        public string PeopleCount { get; set; } = "1";
        public string Points { get; set; } = "10";
        public string ReserveCount { get; set; } = "0";

        private DateTime? _deadlineDate;
        public DateTime? DeadlineDate
        {
            get => _deadlineDate;
            set { _deadlineDate = value; OnPropertyChanged(nameof(DeadlineDate)); }
        }

        public string DeadlineTime { get; set; }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string propertyName)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }
    }

    public class PageResponse_Temp
    {
        public List<UserDto> content { get; set; }
        public int totalPages { get; set; }
        public int totalElements { get; set; }
    }
}