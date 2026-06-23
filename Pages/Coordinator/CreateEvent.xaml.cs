using Diplom_Stud.Components;
using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.ComponentModel;
using System.IO;
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
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class CreateEvent : Page, INotifyPropertyChanged
    {
        private static readonly HttpClient _httpClient = new HttpClient();

        private List<Create_RoleDto> _apiRoles = new List<Create_RoleDto>();
        private List<Create_SectorDto> _apiSectors = new List<Create_SectorDto>();

        public ObservableCollection<RoleItem> Roles { get; set; } = new ObservableCollection<RoleItem>();
        public ObservableCollection<Create_UserDto> SelectedOrganizers { get; set; } = new ObservableCollection<Create_UserDto>();
        public ObservableCollection<SectorSelectionItem> TargetSectors { get; set; } = new ObservableCollection<SectorSelectionItem>();

        private string _newBase64Image = null;
        private bool _isLoading = false;

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
            set { _rolesCountText = value; OnPropertyChanged(nameof(RolesCountText)); }
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
            _isLoading = true;
            LoadingOverlay.Visibility = Visibility.Visible;
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                var rolesResp = await _httpClient.GetAsync("/api/roles");
                if (rolesResp.IsSuccessStatusCode)
                {
                    string json = await rolesResp.Content.ReadAsStringAsync();
                    _apiRoles = JsonSerializer.Deserialize<List<Create_RoleDto>>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<Create_RoleDto>();
                }

                var sectorsResp = await _httpClient.GetAsync("/api/sector");
                if (sectorsResp.IsSuccessStatusCode)
                {
                    string json = await sectorsResp.Content.ReadAsStringAsync();
                    _apiSectors = JsonSerializer.Deserialize<List<Create_SectorDto>>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<Create_SectorDto>();

                    Application.Current.Dispatcher.Invoke(() =>
                    {
                        TargetSectors.Clear();
                        foreach (var s in _apiSectors)
                        {
                            TargetSectors.Add(new SectorSelectionItem { Sector = s, IsSelected = false });
                        }
                    });
                }

                var usersResp = await _httpClient.GetAsync("/api/users/all?page=0&size=1000&sortBy=id&sortDirection=ASC");
                if (usersResp.IsSuccessStatusCode)
                {
                    string json = await usersResp.Content.ReadAsStringAsync();
                    var pageData = JsonSerializer.Deserialize<PageResponse_Temp>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    if (pageData?.content != null)
                    {
                        var validUsers = pageData.content
                            .Where(u => string.IsNullOrEmpty(u.role) || (!u.role.ToLower().Contains("curator") && !u.role.ToLower().Contains("admin")))
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
                                    Foreground = Brushes.White
                                });
                            }
                        });
                    }
                }

                UpdateSectorsVisibility();
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка загрузки данных: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                _isLoading = false;
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private void UpdateSectorsVisibility()
        {
            if (TargetSectorsPanel != null)
            {
                TargetSectorsPanel.Visibility = (!IsPublic && IsFree) ? Visibility.Visible : Visibility.Collapsed;
            }
        }

        private void cbIsPublic_Checked(object sender, RoutedEventArgs e)
        {
            if (_isLoading) return;
            UpdateSectorsVisibility();
        }

        private void cbIsPublic_Unchecked(object sender, RoutedEventArgs e)
        {
            if (_isLoading) return;
            UpdateSectorsVisibility();
        }

        private void cbIsFree_Checked(object sender, RoutedEventArgs e)
        {
            if (_isLoading) return;
            UpdateSectorsVisibility();

            if (!Roles.Any(r => r.RoleType == RoleItem.RoleTypeEnum.Participant))
            {
                Roles.Add(new RoleItem
                {
                    RoleType = RoleItem.RoleTypeEnum.Participant,
                    RoleTitleDisplay = "Участник (Свободное)",
                    Tasks = "Участие в мероприятии",
                    Points = "10",
                    PeopleCount = "0",
                    IsExpanded = true,
                    AvailableSectors = _apiSectors,
                    AvailableRoles = _apiRoles
                });
            }
        }

        private void cbIsFree_Unchecked(object sender, RoutedEventArgs e)
        {
            if (_isLoading) return;
            UpdateSectorsVisibility();

            var participantRole = Roles.FirstOrDefault(r => r.RoleType == RoleItem.RoleTypeEnum.Participant);
            if (participantRole != null) Roles.Remove(participantRole);
        }

        private void AddOrganizerRole_Click(object sender, MouseButtonEventArgs e)
        {
            if (Roles.Any(r => r.RoleType == RoleItem.RoleTypeEnum.Organizer))
            {
                CustomMessageBox.Show("Набор организаторов уже добавлен в список ролей.", "Информация", CustomMessageBox.MessageType.Info);
                return;
            }

            Roles.Add(new RoleItem
            {
                RoleType = RoleItem.RoleTypeEnum.Organizer,
                RoleTitleDisplay = "Организатор (Набор)",
                Tasks = "Организация и помощь в проведении",
                Points = "15",
                PeopleCount = "0",
                IsExpanded = true,
                AvailableSectors = _apiSectors,
                AvailableRoles = _apiRoles
            });
        }

        private void UploadImage_Click(object sender, MouseButtonEventArgs e)
        {
            OpenFileDialog openFileDialog = new OpenFileDialog();
            openFileDialog.Filter = "Image files (*.png;*.jpeg;*.jpg)|*.png;*.jpeg;*.jpg";
            if (openFileDialog.ShowDialog() == true)
            {
                try
                {
                    byte[] imageBytes = File.ReadAllBytes(openFileDialog.FileName);
                    var bitmap = new BitmapImage();
                    using (var ms = new MemoryStream(imageBytes))
                    {
                        bitmap.BeginInit();
                        bitmap.CacheOption = BitmapCacheOption.OnLoad;
                        bitmap.StreamSource = ms;
                        bitmap.EndInit();
                        bitmap.Freeze();
                    }
                    CreateEventImage.ImageSource = bitmap;
                    UploadPlaceholder.Visibility = Visibility.Collapsed;

                    _newBase64Image = $"data:image/jpeg;base64,{Convert.ToBase64String(imageBytes)}";
                }
                catch (Exception ex)
                {
                    CustomMessageBox.Show($"Ошибка загрузки фото: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
        }

        private async void CreateFinalEvent_Click(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrWhiteSpace(EventTitle)) { CustomMessageBox.Show("Введите название.", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
            if (EventDate == null) { CustomMessageBox.Show("Выберите дату.", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
            if (string.IsNullOrWhiteSpace(StartTime) || StartTime.Length < 5) { CustomMessageBox.Show("Введите время начала.", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
            if (string.IsNullOrWhiteSpace(EndTime) || EndTime.Length < 5) { CustomMessageBox.Show("Введите время конца.", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }
            if (string.IsNullOrWhiteSpace(Venue)) { CustomMessageBox.Show("Укажите место.", "Ошибка валидации", CustomMessageBox.MessageType.Error); return; }

            foreach (var role in Roles)
            {
                if (role.RoleType == RoleItem.RoleTypeEnum.Custom)
                {
                    if (role.SelectedRole == null) { CustomMessageBox.Show("В карточке не выбрана роль.", "Ошибка", CustomMessageBox.MessageType.Error); return; }
                    if (role.DeadlineDate == null) { CustomMessageBox.Show($"Для '{role.SelectedRole.title}' не выбран дедлайн.", "Ошибка", CustomMessageBox.MessageType.Error); return; }
                    if (string.IsNullOrWhiteSpace(role.DeadlineTime) || role.DeadlineTime.Length < 5) { CustomMessageBox.Show($"Укажите время дедлайна для '{role.SelectedRole.title}'.", "Ошибка", CustomMessageBox.MessageType.Error); return; }
                }
            }

            btnCreate.IsEnabled = false;
            btnCreate.Content = "Создание...";
            LoadingOverlay.Visibility = Visibility.Visible;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                string eventDateStr = EventDate.Value.ToString("yyyy-MM-dd");
                string startDateTime = $"{eventDateStr}T{StartTime}:00";
                string endDateTime = $"{eventDateStr}T{EndTime}:00";

                // ИСПРАВЛЕНИЕ: Переменная org вместо o
                var organizerIds = SelectedOrganizers.Select(org => org.id).ToList();

                var partRole = Roles.FirstOrDefault(r => r.RoleType == RoleItem.RoleTypeEnum.Participant);
                int pCount = partRole != null && int.TryParse(partRole.PeopleCount, out int parsedP) ? parsedP : 0;

                var orgRole = Roles.FirstOrDefault(r => r.RoleType == RoleItem.RoleTypeEnum.Organizer);
                // ИСПРАВЛЕНИЕ: Переменная parsedO вместо o
                int oCount = orgRole != null && int.TryParse(orgRole.PeopleCount, out int parsedO) ? parsedO : 0;

                var eventPayload = new Dictionary<string, object>
                {
                    { "title", EventTitle },
                    { "description", EventDescription ?? "" },
                    { "photo", _newBase64Image ?? "" },
                    { "dateOfEvent", eventDateStr },
                    { "startTime", startDateTime },
                    { "endTime", endDateTime },
                    { "venue", Venue },
                    { "organizerIds", organizerIds },
                    { "referenceToPosition", string.IsNullOrWhiteSpace(EventDescription) ? "Описание отсутствует" : EventDescription },
                    { "isPublic", IsPublic },
                    { "isDraft", IsDraft },
                    { "isFreeEvent", IsFree },
                    { "maxParticipantsCount", pCount },
                    { "maxOrganizersCount", oCount }
                };

                if (!IsPublic && IsFree)
                {
                    var selectedSectors = TargetSectors.Where(s => s.IsSelected).Select(s => s.Sector.id).ToList();
                    eventPayload.Add("sectorIds", selectedSectors);
                }
                else
                {
                    eventPayload.Add("sectorIds", new List<int>());
                }

                string eventJson = JsonSerializer.Serialize(eventPayload);
                var eventContent = new StringContent(eventJson, Encoding.UTF8, "application/json");

                HttpResponseMessage eventResponse = await _httpClient.PostAsync("/api/events", eventContent);

                if (eventResponse.IsSuccessStatusCode)
                {
                    string createdEventJson = await eventResponse.Content.ReadAsStringAsync();
                    using (JsonDocument doc = JsonDocument.Parse(createdEventJson))
                    {
                        int newEventId = doc.RootElement.GetProperty("id").GetInt32();

                        foreach (var role in Roles.Where(r => r.RoleType == RoleItem.RoleTypeEnum.Custom))
                        {
                            string deadlineFormatted = $"{role.DeadlineDate.Value.ToString("yyyy-MM-dd")}T{role.DeadlineTime}:00";

                            var rolePayload = new
                            {
                                eventId = newEventId,
                                globalEventRoleId = role.SelectedRole.id,
                                capacity = int.TryParse(role.PeopleCount, out int cap) ? cap : 1,
                                reserveCapacity = 0,
                                deadline = deadlineFormatted,
                                description = role.Tasks ?? ""
                            };

                            string roleJson = JsonSerializer.Serialize(rolePayload);
                            var roleContent = new StringContent(roleJson, Encoding.UTF8, "application/json");

                            await _httpClient.PostAsync("/api/event-roles", roleContent);
                        }
                    }

                    CustomMessageBox.Show("Мероприятие успешно создано!", "Успех", CustomMessageBox.MessageType.Success);
                    this.NavigationService.GoBack();
                }
                else
                {
                    string err = await eventResponse.Content.ReadAsStringAsync();
                    CustomMessageBox.Show($"Ошибка создания мероприятия: {err}", "Ошибка сервера", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка сохранения: {ex.Message}", "Сбой", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                btnCreate.IsEnabled = true;
                btnCreate.Content = "Создать мероприятие";
                LoadingOverlay.Visibility = Visibility.Collapsed;
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
                    if (!(item is ComboBoxItem cbi)) return true;
                    return cbi.Content.ToString().IndexOf(text, StringComparison.OrdinalIgnoreCase) >= 0;
                };
                cb.IsDropDownOpen = true;
            }
        }

        private void AddOrganizer_Click(object sender, RoutedEventArgs e)
        {
            if (CbUsers.SelectedItem is ComboBoxItem selectedItem && selectedItem.Tag is Create_UserDto selectedUser)
            {
                if (!SelectedOrganizers.Any(u => u.id == selectedUser.id)) SelectedOrganizers.Add(selectedUser);
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
                        if (item.Tag is Create_UserDto matchedUser && !SelectedOrganizers.Any(u => u.id == matchedUser.id))
                            SelectedOrganizers.Add(matchedUser);
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
            if (sender is Button btn && btn.DataContext is Create_UserDto user)
                SelectedOrganizers.Remove(user);
        }

        private void Roles_CollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            int count = Roles.Count;
            if (count % 10 == 1 && count % 100 != 11) RolesCountText = $"{count} роль";
            else if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) RolesCountText = $"{count} роли";
            else RolesCountText = $"{count} ролей";
        }

        private void AddRole_Click(object sender, MouseButtonEventArgs e)
        {
            Roles.Add(new RoleItem { IsExpanded = true, AvailableRoles = _apiRoles, AvailableSectors = _apiSectors });
        }

        private void RemoveRole_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is RoleItem role)
            {
                if (role.RoleType == RoleItem.RoleTypeEnum.Participant) IsFree = false;
                Roles.Remove(role);
            }
        }

        private void NumericOnly_PreviewTextInput(object sender, TextCompositionEventArgs e) => e.Handled = !e.Text.All(char.IsDigit);
        private void Time_PreviewTextInput(object sender, TextCompositionEventArgs e) => e.Handled = !e.Text.All(char.IsDigit);

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
        protected void OnPropertyChanged(string propertyName) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }

    // ИСПРАВЛЕНИЕ: Добавлен префикс Create_ ко всем DTO
    public class Create_RoleDto
    {
        public int id { get; set; }
        public string title { get; set; }
        public int sectorId { get; set; }
        public int defaultPoints { get; set; }
    }
    public class Create_SectorDto { public int id { get; set; } public string title { get; set; } }
    public class Create_UserDto { public int id { get; set; } public string name { get; set; } public string surname { get; set; } public string patronymic { get; set; } public string role { get; set; } public string DisplayName => $"{surname} {name} {patronymic}".Trim(); }

    public class SectorSelectionItem : INotifyPropertyChanged
    {
        public Create_SectorDto Sector { get; set; }
        private bool _isSelected;
        public bool IsSelected
        {
            get => _isSelected;
            set { _isSelected = value; OnPropertyChanged(nameof(IsSelected)); }
        }
        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string propertyName) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }

    public class RoleItem : INotifyPropertyChanged
    {
        public enum RoleTypeEnum { Custom, Participant, Organizer }

        private RoleTypeEnum _roleType = RoleTypeEnum.Custom;
        public RoleTypeEnum RoleType
        {
            get => _roleType;
            set
            {
                _roleType = value;
                OnPropertyChanged(nameof(RoleType));
                OnPropertyChanged(nameof(CustomRoleVisibility));
                OnPropertyChanged(nameof(SpecialRoleVisibility));
            }
        }

        public Visibility CustomRoleVisibility => RoleType == RoleTypeEnum.Custom ? Visibility.Visible : Visibility.Collapsed;
        public Visibility SpecialRoleVisibility => RoleType != RoleTypeEnum.Custom ? Visibility.Visible : Visibility.Collapsed;

        private string _roleTitleDisplay;
        public string RoleTitleDisplay
        {
            get => _roleTitleDisplay;
            set { _roleTitleDisplay = value; OnPropertyChanged(nameof(RoleTitleDisplay)); }
        }

        public int RoleId { get; set; }
        private bool _isExpanded = false;
        public bool IsExpanded
        {
            get => _isExpanded;
            set { _isExpanded = value; OnPropertyChanged(nameof(IsExpanded)); }
        }

        private List<Create_SectorDto> _availableSectors;
        public List<Create_SectorDto> AvailableSectors
        {
            get => _availableSectors;
            set { _availableSectors = value; OnPropertyChanged(nameof(AvailableSectors)); }
        }

        private Create_SectorDto _selectedSector;
        public Create_SectorDto SelectedSector
        {
            get => _selectedSector;
            set { _selectedSector = value; OnPropertyChanged(nameof(SelectedSector)); }
        }

        private Create_RoleDto _selectedRole;
        public Create_RoleDto SelectedRole
        {
            get => _selectedRole;
            set
            {
                _selectedRole = value;
                OnPropertyChanged(nameof(SelectedRole));

                if (_selectedRole != null)
                {
                    if (AvailableSectors != null)
                    {
                        var sector = AvailableSectors.FirstOrDefault(s => s.id == _selectedRole.sectorId);
                        if (sector != null) SelectedSector = sector;
                    }
                    Points = _selectedRole.defaultPoints.ToString();
                }
            }
        }

        private List<Create_RoleDto> _availableRoles;
        public List<Create_RoleDto> AvailableRoles
        {
            get => _availableRoles;
            set { _availableRoles = value; OnPropertyChanged(nameof(AvailableRoles)); }
        }

        public string Tasks { get; set; }
        public string PeopleCount { get; set; } = "1";

        private string _points = "10";
        public string Points
        {
            get => _points;
            set { _points = value; OnPropertyChanged(nameof(Points)); }
        }

        private DateTime? _deadlineDate;
        public DateTime? DeadlineDate
        {
            get => _deadlineDate;
            set { _deadlineDate = value; OnPropertyChanged(nameof(DeadlineDate)); }
        }
        public string DeadlineTime { get; set; }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string propertyName) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }

    public class PageResponse_Temp
    {
        public List<Create_UserDto> content { get; set; }
    }
}