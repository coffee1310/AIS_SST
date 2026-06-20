using Diplom_Stud.Components;
using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Diagnostics;
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
    public partial class EventEditPage : Page, INotifyPropertyChanged
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private int _eventId;
        private string _newBase64Image = null;
        private bool _isLoading = false;

        private List<RoleDto> _apiRoles = new List<RoleDto>();
        private List<SectorDto> _apiSectors = new List<SectorDto>();

        private List<int> _rolesToDelete = new List<int>();
        private List<int> _initialOrganizerIds = new List<int>();

        public ObservableCollection<RoleItem> Roles { get; set; } = new ObservableCollection<RoleItem>();
        public ObservableCollection<UserDto> SelectedOrganizers { get; set; } = new ObservableCollection<UserDto>();
        public ObservableCollection<SectorSelectionItem> TargetSectors { get; set; } = new ObservableCollection<SectorSelectionItem>();

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

        public EventEditPage(int eventId)
        {
            InitializeComponent();
            _eventId = eventId;

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
            if (_eventId <= 0) return;
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

                var evResp = await _httpClient.GetAsync($"/api/events/{_eventId}");
                if (evResp.IsSuccessStatusCode)
                {
                    string json = await evResp.Content.ReadAsStringAsync();
                    var ev = JsonSerializer.Deserialize<EditEventDtoLocal>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    if (ev != null)
                    {
                        EventTitle = ev.title;
                        EventDescription = ev.description;
                        if (DateTime.TryParse(ev.dateOfEvent, out DateTime d)) EventDate = d;

                        StartTime = ev.startTime?.Length >= 5 ? ev.startTime.Substring(0, 5) : ev.startTime;
                        EndTime = ev.endTime?.Length >= 5 ? ev.endTime.Substring(0, 5) : ev.endTime;
                        Venue = ev.venue;

                        IsPublic = ev.isPublic;
                        IsFree = ev.isFreeEvent;
                        IsDraft = ev.isDraft;

                        TargetSectors.Clear();
                        foreach (var s in _apiSectors)
                        {
                            bool isSel = ev.sectors != null && ev.sectors.Any(sec => sec.id == s.id);
                            TargetSectors.Add(new SectorSelectionItem { Sector = s, IsSelected = isSel });
                        }

                        TargetSectorsPanel.Visibility = (!IsPublic && IsFree) ? Visibility.Visible : Visibility.Collapsed;

                        if (!string.IsNullOrEmpty(ev.photo))
                        {
                            var bmp = GetImageFromBase64(ev.photo);
                            if (bmp != null)
                            {
                                EditEventImage.ImageSource = bmp;
                                UploadPlaceholder.Visibility = Visibility.Collapsed;
                                _newBase64Image = ev.photo;
                            }
                        }

                        if (ev.organizers != null)
                        {
                            foreach (var org in ev.organizers)
                            {
                                SelectedOrganizers.Add(org);
                                _initialOrganizerIds.Add(org.id);
                            }
                        }

                        Roles.Clear();
                        if (ev.isFreeEvent)
                        {
                            Roles.Add(new RoleItem
                            {
                                RoleType = RoleItem.RoleTypeEnum.Participant,
                                RoleTitleDisplay = "Участник (Свободное)",
                                Tasks = "Участие в мероприятии",
                                Points = "10",
                                PeopleCount = ev.maxParticipantsCount.ToString(),
                                IsExpanded = false,
                                AvailableSectors = _apiSectors,
                                AvailableRoles = _apiRoles
                            });
                        }
                        if (ev.maxOrganizersCount > 0)
                        {
                            Roles.Add(new RoleItem
                            {
                                RoleType = RoleItem.RoleTypeEnum.Organizer,
                                RoleTitleDisplay = "Организатор (Набор)",
                                Tasks = "Организация и помощь в проведении",
                                Points = "15",
                                PeopleCount = ev.maxOrganizersCount.ToString(),
                                IsExpanded = false,
                                AvailableSectors = _apiSectors,
                                AvailableRoles = _apiRoles
                            });
                        }
                    }
                }

                var rolesListResp = await _httpClient.GetAsync($"/api/event-roles?eventId={_eventId}&isDeleted=false&page=0&size=100");
                if (rolesListResp.IsSuccessStatusCode)
                {
                    string json = await rolesListResp.Content.ReadAsStringAsync();
                    var pageData = JsonSerializer.Deserialize<EditEventRolePageLocal>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    if (pageData?.content != null)
                    {
                        foreach (var roleDto in pageData.content.Where(r => !r.deleted))
                        {
                            var globalRole = _apiRoles.FirstOrDefault(r => r.id == roleDto.globalEventRoleId);
                            var globalSector = globalRole != null ? _apiSectors.FirstOrDefault(s => s.id == globalRole.sectorId) : null;

                            DateTime? dlDate = null;
                            string dlTime = "";
                            if (DateTime.TryParse(roleDto.deadline, out DateTime dl))
                            {
                                dlDate = dl;
                                dlTime = dl.ToString("HH:mm");
                            }

                            Roles.Add(new RoleItem
                            {
                                RoleId = roleDto.id,
                                RoleType = RoleItem.RoleTypeEnum.Custom,
                                IsExpanded = false,
                                AvailableRoles = _apiRoles,
                                AvailableSectors = _apiSectors,
                                SelectedRole = globalRole,
                                SelectedSector = globalSector,
                                Tasks = roleDto.description,
                                PeopleCount = roleDto.capacity.ToString(),
                                DeadlineDate = dlDate,
                                DeadlineTime = dlTime
                            });
                        }
                    }
                }
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

        private void UpdateTargetSectorsVisibility()
        {
            if (TargetSectorsPanel != null)
            {
                TargetSectorsPanel.Visibility = (!IsPublic && IsFree) ? Visibility.Visible : Visibility.Collapsed;
            }
        }

        private void cbIsPublic_Checked(object sender, RoutedEventArgs e)
        {
            if (_isLoading) return;
            UpdateTargetSectorsVisibility();
        }

        private void cbIsPublic_Unchecked(object sender, RoutedEventArgs e)
        {
            if (_isLoading) return;
            UpdateTargetSectorsVisibility();
        }

        private void cbIsFree_Checked(object sender, RoutedEventArgs e)
        {
            if (_isLoading) return;
            UpdateTargetSectorsVisibility();

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
            UpdateTargetSectorsVisibility();

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
                    EditEventImage.ImageSource = bitmap;
                    UploadPlaceholder.Visibility = Visibility.Collapsed;
                    _newBase64Image = $"data:image/jpeg;base64,{Convert.ToBase64String(imageBytes)}";
                }
                catch (Exception ex)
                {
                    CustomMessageBox.Show($"Ошибка загрузки фото: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
        }

        private async void DeleteEvent_Click(object sender, RoutedEventArgs e)
        {
            bool confirmed = CustomMessageBox.Show("Вы уверены, что хотите удалить это мероприятие?", "Подтверждение", CustomMessageBox.MessageType.Question, true);
            if (!confirmed) return;

            LoadingOverlay.Visibility = Visibility.Visible;
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                HttpResponseMessage res = await _httpClient.DeleteAsync($"/api/events/{_eventId}");
                if (res.IsSuccessStatusCode)
                {
                    CustomMessageBox.Show("Мероприятие успешно удалено.", "Успех", CustomMessageBox.MessageType.Success);
                    this.NavigationService.GoBack();
                }
                else
                {
                    string err = await res.Content.ReadAsStringAsync();
                    CustomMessageBox.Show($"Ошибка удаления: {err}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сбой сети: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private async void SaveFinalEvent_Click(object sender, RoutedEventArgs e)
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

            btnSave.IsEnabled = false;
            btnDeleteEvent.IsEnabled = false;
            btnSave.Content = "Сохранение...";
            LoadingOverlay.Visibility = Visibility.Visible;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                string eventDateStr = EventDate.Value.ToString("yyyy-MM-dd");
                string startDateTime = $"{eventDateStr}T{StartTime}:00";
                string endDateTime = $"{eventDateStr}T{EndTime}:00";

                var finalOrganizerIds = SelectedOrganizers.Select(u => u.id).ToList();

                var partRole = Roles.FirstOrDefault(r => r.RoleType == RoleItem.RoleTypeEnum.Participant);
                int pCount = partRole != null && int.TryParse(partRole.PeopleCount, out int parsedP) ? parsedP : 0;

                var orgRole = Roles.FirstOrDefault(r => r.RoleType == RoleItem.RoleTypeEnum.Organizer);
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
                    { "organizerIds", finalOrganizerIds },
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

                string eventJson = JsonSerializer.Serialize(eventPayload);
                var eventContent = new StringContent(eventJson, Encoding.UTF8, "application/json");

                HttpResponseMessage eventResponse = await _httpClient.PutAsync($"/api/events/{_eventId}", eventContent);

                if (!eventResponse.IsSuccessStatusCode)
                {
                    string err = await eventResponse.Content.ReadAsStringAsync();
                    CustomMessageBox.Show($"Ошибка обновления мероприятия: {err}", "Ошибка сервера", CustomMessageBox.MessageType.Error);
                    return;
                }

                var addedOrganizers = finalOrganizerIds.Except(_initialOrganizerIds).ToList();
                var removedOrganizers = _initialOrganizerIds.Except(finalOrganizerIds).ToList();

                foreach (int id in removedOrganizers)
                {
                    await _httpClient.DeleteAsync($"/api/events/{_eventId}/organizers/{id}");
                }
                foreach (int id in addedOrganizers)
                {
                    await _httpClient.PostAsync($"/api/events/{_eventId}/organizers/{id}", new StringContent(""));
                }

                foreach (int roleId in _rolesToDelete)
                {
                    await _httpClient.DeleteAsync($"/api/event-roles/{roleId}");
                }

                foreach (var role in Roles.Where(r => r.RoleType == RoleItem.RoleTypeEnum.Custom))
                {
                    string deadlineFormatted = $"{role.DeadlineDate.Value.ToString("yyyy-MM-dd")}T{role.DeadlineTime}:00";
                    var rolePayload = new
                    {
                        eventId = _eventId,
                        globalEventRoleId = role.SelectedRole.id,
                        capacity = int.TryParse(role.PeopleCount, out int cap) ? cap : 1,
                        reserveCapacity = 0,
                        deadline = deadlineFormatted,
                        description = role.Tasks ?? ""
                    };

                    string roleJson = JsonSerializer.Serialize(rolePayload);
                    var roleContent = new StringContent(roleJson, Encoding.UTF8, "application/json");

                    if (role.RoleId == 0)
                    {
                        await _httpClient.PostAsync("/api/event-roles", roleContent);
                    }
                    else
                    {
                        await _httpClient.PutAsync($"/api/event-roles/{role.RoleId}", roleContent);
                    }
                }

                CustomMessageBox.Show("Изменения успешно сохранены!", "Успех", CustomMessageBox.MessageType.Success);
                this.NavigationService.GoBack();
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка сохранения: {ex.Message}", "Сбой", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                btnSave.IsEnabled = true;
                btnDeleteEvent.IsEnabled = true;
                btnSave.Content = "Сохранить изменения";
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
            if (CbUsers.SelectedItem is ComboBoxItem selectedItem && selectedItem.Tag is UserDto selectedUser)
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
                        if (item.Tag is UserDto matchedUser && !SelectedOrganizers.Any(u => u.id == matchedUser.id))
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
            if (sender is Button btn && btn.DataContext is UserDto user)
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
                if (role.RoleType == RoleItem.RoleTypeEnum.Participant)
                {
                    IsFree = false;
                }
                else
                {
                    if (role.RoleId > 0)
                    {
                        _rolesToDelete.Add(role.RoleId);
                    }
                    Roles.Remove(role);
                }
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

        private BitmapImage GetImageFromBase64(string base64String)
        {
            try
            {
                string cleanStr = base64String.Trim().Replace("\r", "").Replace("\n", "");
                byte[] imageBytes = null;
                try
                {
                    byte[] decodedFirstLevel = Convert.FromBase64String(cleanStr);
                    string textInside = Encoding.UTF8.GetString(decodedFirstLevel);
                    if (textInside.StartsWith("data:image"))
                    {
                        int commaIndex = textInside.IndexOf(',');
                        if (commaIndex >= 0) imageBytes = Convert.FromBase64String(textInside.Substring(commaIndex + 1));
                    }
                    else { imageBytes = decodedFirstLevel; }
                }
                catch
                {
                    int commaIndex = cleanStr.IndexOf(',');
                    if (commaIndex >= 0) cleanStr = cleanStr.Substring(commaIndex + 1);
                    imageBytes = Convert.FromBase64String(cleanStr);
                }

                if (imageBytes != null)
                {
                    using (var ms = new MemoryStream(imageBytes))
                    {
                        var bitmap = new BitmapImage();
                        bitmap.BeginInit();
                        bitmap.CacheOption = BitmapCacheOption.OnLoad;
                        bitmap.StreamSource = ms;
                        bitmap.EndInit();
                        bitmap.Freeze();
                        return bitmap;
                    }
                }
            }
            catch { }
            return null;
        }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string propertyName) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }

    public class EditEventDtoLocal
    {
        public int id { get; set; }
        public string title { get; set; }
        public string description { get; set; }
        public string dateOfEvent { get; set; }
        public string startTime { get; set; }
        public string endTime { get; set; }
        public string venue { get; set; }
        public string photo { get; set; }
        public bool isPublic { get; set; }
        public bool isDraft { get; set; }
        public bool isFreeEvent { get; set; }
        public int maxParticipantsCount { get; set; }
        public int maxOrganizersCount { get; set; }
        public List<SectorDto> sectors { get; set; }
        public List<UserDto> organizers { get; set; }
    }

    public class EditEventRolePageLocal
    {
        public List<EditEventRoleDtoLocal> content { get; set; }
    }

    public class EditEventRoleDtoLocal
    {
        public int id { get; set; }
        public int globalEventRoleId { get; set; }
        public string description { get; set; }
        public string deadline { get; set; }
        public int capacity { get; set; }
        public int reserveCapacity { get; set; }
        public bool deleted { get; set; }
    }
}