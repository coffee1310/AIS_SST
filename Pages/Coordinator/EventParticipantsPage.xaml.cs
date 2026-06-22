using Diplom_Stud.Components;
using System;
using System.Collections.Generic;
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
    public partial class EventParticipantsPage : Page
    {
        private int _eventId;
        private static readonly HttpClient _httpClient = new HttpClient();

        public EventParticipantsPage(int eventId)
        {
            InitializeComponent();
            _eventId = eventId;

            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            }

            CbAddUser.AddHandler(TextBoxBase.TextChangedEvent, new TextChangedEventHandler(CbAddUser_TextChanged));
        }

        private async void Page_Loaded(object sender, RoutedEventArgs e)
        {
            if (_eventId <= 0) return;
            await LoadParticipantsAsync();
        }

        private List<T> ParsePageOrList<T>(string json, JsonSerializerOptions options)
        {
            if (string.IsNullOrWhiteSpace(json)) return new List<T>();
            string trimmed = json.TrimStart();

            if (trimmed.StartsWith("["))
            {
                return JsonSerializer.Deserialize<List<T>>(json, options) ?? new List<T>();
            }
            else if (trimmed.StartsWith("{"))
            {
                try
                {
                    using (var doc = JsonDocument.Parse(json))
                    {
                        if (doc.RootElement.TryGetProperty("content", out var contentElem))
                        {
                            return JsonSerializer.Deserialize<List<T>>(contentElem.GetRawText(), options) ?? new List<T>();
                        }
                    }
                }
                catch { }
            }
            return new List<T>();
        }

        private async Task LoadParticipantsAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyParticipantsText.Visibility = Visibility.Collapsed;
            RolesWithParticipantsList.ItemsSource = null;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                await PopulateAddUserComboBox(options);
                await PopulateAddRoleComboBox(options);

                var blocks = new List<Part_RoleGroupViewModel>();

                HttpResponseMessage resPart = await _httpClient.GetAsync($"/api/events/participants/{_eventId}");
                if (resPart.IsSuccessStatusCode)
                {
                    string json = await resPart.Content.ReadAsStringAsync();
                    var list = ParsePageOrList<Part_ParticipantUserDto>(json, options);

                    if (list.Count > 0)
                    {
                        var block = new Part_RoleGroupViewModel { RoleTitle = "Участник", Participants = new List<Part_ItemViewModel>() };
                        foreach (var u in list)
                        {
                            block.Participants.Add(new Part_ItemViewModel
                            {
                                ParticipantId = u.id,
                                StudentId = u.id,
                                FullName = $"{u.surname} {u.name} {u.patronymic}".Trim(),
                                StudentEmail = string.IsNullOrWhiteSpace(u.studentEmail) ? "Почта не указана" : u.studentEmail,
                                Type = "Participant",
                                IsReserve = false,
                                Avatar = GetImageFromBase64(u.photo) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                            });
                        }
                        blocks.Add(block);
                    }
                }

                HttpResponseMessage resOrg = await _httpClient.GetAsync($"/api/events/participation/filter?eventId={_eventId}&entityType=ORGANIZER&page=0&size=100");
                if (resOrg.IsSuccessStatusCode)
                {
                    string json = await resOrg.Content.ReadAsStringAsync();
                    var list = ParsePageOrList<Part_ParticipationRecordDto>(json, options);

                    if (list.Count > 0)
                    {
                        var block = new Part_RoleGroupViewModel { RoleTitle = "Организатор", Participants = new List<Part_ItemViewModel>() };
                        foreach (var u in list)
                        {
                            block.Participants.Add(new Part_ItemViewModel
                            {
                                ParticipantId = u.id,
                                StudentId = 0,
                                FullName = u.fullName ?? "Неизвестный организатор",
                                StudentEmail = "Почта не указана",
                                Type = "Organizer",
                                IsReserve = false,
                                Avatar = new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                            });
                        }
                        blocks.Add(block);
                    }
                }

                HttpResponseMessage resRoles = await _httpClient.GetAsync($"/api/role-applications?status=ОДОБРЕНА&eventId={_eventId}");
                if (resRoles.IsSuccessStatusCode)
                {
                    string json = await resRoles.Content.ReadAsStringAsync();
                    var list = ParsePageOrList<Part_RoleAppDto>(json, options);

                    if (list.Count > 0)
                    {
                        var grouped = list.GroupBy(c => string.IsNullOrEmpty(c.eventRoleName) ? "Роль не указана" : c.eventRoleName);
                        foreach (var g in grouped)
                        {
                            var block = new Part_RoleGroupViewModel { RoleTitle = g.Key, Participants = new List<Part_ItemViewModel>() };
                            foreach (var app in g)
                            {
                                block.Participants.Add(new Part_ItemViewModel
                                {
                                    ParticipantId = app.id,
                                    StudentId = app.studentId ?? 0,
                                    FullName = $"{app.studentSurname} {app.studentName} {app.studentPatronymic}".Trim(),
                                    StudentEmail = string.IsNullOrWhiteSpace(app.studentEmail) ? "Почта не указана" : app.studentEmail,
                                    Type = "Role",
                                    IsReserve = app.isReserve,
                                    Avatar = GetImageFromBase64(app.studentPhoto) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                                });
                            }
                            blocks.Add(block);
                        }
                    }
                }

                RolesWithParticipantsList.ItemsSource = blocks;
                if (blocks.Count == 0) EmptyParticipantsText.Visibility = Visibility.Visible;
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка загрузки участников: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }


        private async Task PopulateAddUserComboBox(JsonSerializerOptions options)
        {
            HttpResponseMessage usersResp = await _httpClient.GetAsync("/api/users/all?page=0&size=1000&sortBy=id&sortDirection=ASC");
            if (usersResp.IsSuccessStatusCode)
            {
                var pageData = JsonSerializer.Deserialize<Part_UserPageResponse>(await usersResp.Content.ReadAsStringAsync(), options);
                if (pageData?.content != null)
                {
                    var validUsers = pageData.content
                        .Where(u => string.IsNullOrEmpty(u.role) || (!u.role.ToLower().Contains("curator") && !u.role.ToLower().Contains("admin")))
                        .ToList();

                    Application.Current.Dispatcher.Invoke(() =>
                    {
                        CbAddUser.Items.Clear();
                        foreach (var user in validUsers)
                        {
                            CbAddUser.Items.Add(new ComboBoxItem
                            {
                                Content = user.DisplayName,
                                Tag = user,
                                Foreground = Brushes.White
                            });
                        }
                    });
                }
            }
        }

        private async Task PopulateAddRoleComboBox(JsonSerializerOptions options)
        {
            Application.Current.Dispatcher.Invoke(() => CbAddRole.Items.Clear());

            HttpResponseMessage evRes = await _httpClient.GetAsync($"/api/events/{_eventId}");
            if (evRes.IsSuccessStatusCode)
            {
                var ev = JsonSerializer.Deserialize<Part_EditEventDto>(await evRes.Content.ReadAsStringAsync(), options);
                if (ev != null && ev.isFreeEvent)
                {
                    Application.Current.Dispatcher.Invoke(() =>
                        CbAddRole.Items.Add(new AssignRoleItem { IsRegularParticipant = true, DisplayTitle = "Обычный участник" })
                    );
                }
            }

            HttpResponseMessage erRes = await _httpClient.GetAsync($"/api/event-roles?eventId={_eventId}&isDeleted=false&page=0&size=100");
            if (erRes.IsSuccessStatusCode)
            {
                var pageData = ParsePageOrList<Part_EventRoleDto>(await erRes.Content.ReadAsStringAsync(), options);
                Application.Current.Dispatcher.Invoke(() =>
                {
                    foreach (var r in pageData)
                    {
                        CbAddRole.Items.Add(new AssignRoleItem { EventRoleId = r.id, IsRegularParticipant = false, DisplayTitle = $"{r.globalEventRoleTitle} (Роль)" });
                    }
                });
            }

            Application.Current.Dispatcher.Invoke(() => { if (CbAddRole.Items.Count > 0) CbAddRole.SelectedIndex = 0; });
        }

        private void CbAddUser_TextChanged(object sender, TextChangedEventArgs e)
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

        private async void AddParticipantManual_Click(object sender, RoutedEventArgs e)
        {
            Part_UserDto selectedUser = null;
            if (CbAddUser.SelectedItem is ComboBoxItem item && item.Tag is Part_UserDto u)
            {
                selectedUser = u;
            }
            else
            {
                string text = CbAddUser.Text.Trim();
                foreach (ComboBoxItem cbItem in CbAddUser.Items)
                {
                    if (cbItem.Content.ToString().Equals(text, StringComparison.OrdinalIgnoreCase))
                    {
                        selectedUser = cbItem.Tag as Part_UserDto;
                        break;
                    }
                }
            }

            if (selectedUser == null)
            {
                CustomMessageBox.Show("Выберите пользователя из списка.", "Внимание", CustomMessageBox.MessageType.Warning);
                return;
            }

            var selectedRole = CbAddRole.SelectedItem as AssignRoleItem;
            if (selectedRole == null)
            {
                CustomMessageBox.Show("Выберите роль для пользователя.", "Внимание", CustomMessageBox.MessageType.Warning);
                return;
            }

            LoadingOverlay.Visibility = Visibility.Visible;
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                HttpResponseMessage response = null;

                if (selectedRole.IsRegularParticipant)
                {
                    var payload = new { studentId = selectedUser.id, userId = selectedUser.id };
                    var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
                    response = await _httpClient.PostAsync($"/api/events/{_eventId}/participants", content);
                }
                else
                {
                    var payload = new { studentId = selectedUser.id, userId = selectedUser.id, eventRoleId = selectedRole.EventRoleId };
                    var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
                    response = await _httpClient.PostAsync($"/api/events/{_eventId}/participation-records", content);
                }

                if (response != null && response.IsSuccessStatusCode)
                {
                    CustomMessageBox.Show("Участник успешно добавлен!", "Успех", CustomMessageBox.MessageType.Success);
                    CbAddUser.SelectedItem = null;
                    CbAddUser.Text = "";
                    await LoadParticipantsAsync();
                }
                else
                {
                    string err = response != null ? await response.Content.ReadAsStringAsync() : "Неизвестная ошибка";
                    CustomMessageBox.Show($"Не удалось добавить участника:\n{err}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show(ex.Message, "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }



        private void ParticipantCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is FrameworkElement element && element.Tag is int studentId && studentId != 0)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Activist.Profile(studentId));
            }
        }

        private async void KickParticipant_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is Part_ItemViewModel p)
            {
                MessageBoxResult result = MessageBox.Show($"Вы уверены, что хотите исключить {p.FullName}?", "Исключение", MessageBoxButton.YesNo, MessageBoxImage.Warning);

                if (result == MessageBoxResult.Yes)
                {
                    LoadingOverlay.Visibility = Visibility.Visible;
                    try
                    {
                        _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                        HttpResponseMessage response = null;

                        if (p.Type == "Participant")
                        {
                            response = await _httpClient.DeleteAsync($"/api/events/participants/{p.ParticipantId}/soft");
                        }
                        else if (p.Type == "Organizer")
                        {
                            response = await _httpClient.DeleteAsync($"/api/events/organizers/{p.ParticipantId}/soft");
                        }
                        else if (p.Type == "Role")
                        {
                            response = await _httpClient.DeleteAsync($"/api/events/participants/participation-records/{p.ParticipantId}");
                        }

                        if (response != null && response.IsSuccessStatusCode)
                        {
                            CustomMessageBox.Show("Участник успешно исключен.", "Успех", CustomMessageBox.MessageType.Success);
                            await LoadParticipantsAsync();
                        }
                        else
                        {
                            string err = response != null ? await response.Content.ReadAsStringAsync() : "Неизвестная ошибка";
                            CustomMessageBox.Show($"Ошибка выполнения действия:\n{err}", "Ошибка", CustomMessageBox.MessageType.Error);
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
            }
        }

        private BitmapImage GetImageFromBase64(string base64String)
        {
            try
            {
                if (string.IsNullOrEmpty(base64String)) return null;
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
    }

    public class Part_UserPageResponse
    {
        public List<Part_UserDto> content { get; set; }
    }

    public class Part_UserDto
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public string role { get; set; }
        public string DisplayName => $"{surname} {name} {patronymic}".Trim();
    }

    public class AssignRoleItem
    {
        public int EventRoleId { get; set; }
        public string DisplayTitle { get; set; }
        public bool IsRegularParticipant { get; set; }

        public override string ToString()
        {
            return DisplayTitle;
        }
    }

    public class Part_EventRoleDto
    {
        public int id { get; set; }
        public string globalEventRoleTitle { get; set; }
    }

    public class Part_ParticipantUserDto
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public string studentEmail { get; set; }
        public string photo { get; set; }
    }

    public class Part_ParticipationRecordDto
    {
        public int id { get; set; }
        public string role { get; set; }
        public string fullName { get; set; }
        public int totalPoints { get; set; }
        public bool wasPresent { get; set; }
        public string entityType { get; set; }
    }

    public class Part_EditEventDto
    {
        public bool isFreeEvent { get; set; }
        public List<Part_ParticipantUserDto> organizers { get; set; }
    }

    public class Part_RoleAppDto
    {
        public int id { get; set; }
        public int? studentId { get; set; }
        public string studentName { get; set; }
        public string studentSurname { get; set; }
        public string studentPatronymic { get; set; }
        public string studentEmail { get; set; }
        public string studentPhoto { get; set; }
        public string eventRoleName { get; set; }
        public bool isReserve { get; set; }
    }

    public class Part_RoleGroupViewModel : INotifyPropertyChanged
    {
        public string RoleTitle { get; set; }
        private bool _isExpanded = true;
        public bool IsExpanded
        {
            get => _isExpanded;
            set { _isExpanded = value; OnPropertyChanged(nameof(IsExpanded)); }
        }

        public List<Part_ItemViewModel> Participants { get; set; }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string name) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class Part_ItemViewModel
    {
        public int ParticipantId { get; set; }
        public int StudentId { get; set; }
        public string FullName { get; set; }
        public string StudentEmail { get; set; }
        public bool IsReserve { get; set; }
        public string Type { get; set; }
        public ImageSource Avatar { get; set; }

        public string StatusLabel => IsReserve ? "Резерв" : "Основной состав";
        public Brush BackgroundBrush => IsReserve ? new SolidColorBrush(Color.FromArgb(20, 255, 165, 0)) : Brushes.Transparent;
    }
}