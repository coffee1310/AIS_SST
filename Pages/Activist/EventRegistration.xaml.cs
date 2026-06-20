using Diplom_Stud.Components;
using Diplom_Stud.Pages.General;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media.Animation;

namespace Diplom_Stud.Pages.Activist
{
    public partial class EventRegistration : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private int _eventId;

        private bool _isPublicEvent = true;
        private bool _isFreeEvent = false;
        private bool _isMySector = false;
        private int _maxOrganizersCount = 0;

        private List<RegistrationRoleViewModel> _availableRoles = new List<RegistrationRoleViewModel>();
        private List<int> _selectedRoleIds = new List<int>();

        public EventRegistration(int eventId = 0)
        {
            InitializeComponent();
            _eventId = eventId;

            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            }
        }

        private async void Page_Loaded(object sender, RoutedEventArgs e)
        {
            DoubleAnimation fadeInAnimation = new DoubleAnimation
            {
                From = 0.0,
                To = 1.0,
                Duration = TimeSpan.FromSeconds(0.6),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };
            this.BeginAnimation(Page.OpacityProperty, fadeInAnimation);

            if (_eventId <= 0)
            {
                CustomMessageBox.Show("ID мероприятия не передан.", "Ошибка", CustomMessageBox.MessageType.Error);
                LoadingOverlay.Visibility = Visibility.Collapsed;
                return;
            }

            await LoadRolesAndCheckRulesAsync();
        }

        private async Task LoadRolesAndCheckRulesAsync()
        {
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage eventRes = await _httpClient.GetAsync($"/api/events/{_eventId}");
                if (eventRes.IsSuccessStatusCode)
                {
                    string responseBody = await eventRes.Content.ReadAsStringAsync();
                    var ev = JsonSerializer.Deserialize<EventDetailDtoLocal>(responseBody, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    if (ev != null)
                    {
                        EventTitleText.Text = ev.title;
                        _isPublicEvent = ev.isPublic;
                        _isFreeEvent = ev.isFreeEvent;
                        _isMySector = ev.isMySector;
                        _maxOrganizersCount = ev.maxOrganizersCount;
                    }
                }

                var roleViewModels = new List<RegistrationRoleViewModel>();
                var userApplications = await GetUserApplicationsAsync();

                var userSectors = new List<string>(App.CurrentUserProfile?.userSectors ?? new List<string>());
                if (!string.IsNullOrEmpty(App.CurrentUserProfile?.coordinatorSector))
                {
                    if (!userSectors.Contains(App.CurrentUserProfile.coordinatorSector))
                        userSectors.Add(App.CurrentUserProfile.coordinatorSector);
                }

                if (_isFreeEvent)
                {
                    HttpResponseMessage slotsRes = await _httpClient.GetAsync($"/api/events/participants/{_eventId}/slots");
                    bool participantActive = true;
                    string participantStatus = "";
                    string pSlotsInfo = "Свободно мест: ?";

                    if (slotsRes.IsSuccessStatusCode)
                    {
                        var slots = JsonSerializer.Deserialize<ParticipantSlotsDto>(await slotsRes.Content.ReadAsStringAsync(), new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                        if (slots != null)
                        {
                            pSlotsInfo = slots.availableSlots >= 2000000000 ? "Места не ограничены" : $"Свободно: {slots.availableSlots}";
                            if (slots.availableSlots <= 0)
                            {
                                participantActive = false;
                                participantStatus = "Свободных мест больше нет";
                            }
                        }
                    }

                    HttpResponseMessage myPartsRes = await _httpClient.GetAsync("/api/events/participants/my-events");
                    if (myPartsRes.IsSuccessStatusCode)
                    {
                        var myParts = JsonSerializer.Deserialize<List<MyParticipantRecordDto>>(await myPartsRes.Content.ReadAsStringAsync(), new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                        if (myParts != null && myParts.Any(p => p.eventId == _eventId))
                        {
                            participantActive = false;
                            participantStatus = "Вы уже участник";
                        }
                    }

                    if (!_isPublicEvent && !_isMySector)
                    {
                        participantActive = false;
                        participantStatus = "Недоступно для вашего сектора";
                    }

                    roleViewModels.Add(new RegistrationRoleViewModel
                    {
                        Id = -1,
                        Title = "Участник",
                        Description = "Обычное участие в мероприятии.",
                        DeadlineText = "Без дедлайна",
                        SlotsInfo = pSlotsInfo,
                        IsActive = participantActive,
                        CardOpacity = participantActive ? 1.0 : 0.4,
                        ApplicationStatus = participantStatus
                    });
                }

                if (_maxOrganizersCount > 0)
                {
                    bool orgActive = true;
                    string orgStatus = "";
                    string orgSlotsInfo = "Свободно мест: ?";

                    HttpResponseMessage orgSlotsRes = await _httpClient.GetAsync($"/api/events/{_eventId}/organizers-slots");
                    if (orgSlotsRes.IsSuccessStatusCode)
                    {
                        var slots = JsonSerializer.Deserialize<ParticipantSlotsDto>(await orgSlotsRes.Content.ReadAsStringAsync(), new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                        if (slots != null)
                        {
                            orgSlotsInfo = slots.availableSlots >= 2000000000 ? "Места не ограничены" : $"Свободно: {slots.availableSlots}";
                            if (slots.availableSlots <= 0)
                            {
                                orgActive = false;
                                orgStatus = "Свободных мест больше нет";
                            }
                        }
                    }

                    HttpResponseMessage myOrgAppsRes = await _httpClient.GetAsync("/api/role-applications/organizer/my");
                    if (myOrgAppsRes.IsSuccessStatusCode)
                    {
                        string json = await myOrgAppsRes.Content.ReadAsStringAsync();
                        var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                        List<RoleApplicationDto> myOrgAppsList = new List<RoleApplicationDto>();

                        using (var doc = JsonDocument.Parse(json))
                        {
                            if (doc.RootElement.ValueKind == JsonValueKind.Array)
                                myOrgAppsList = JsonSerializer.Deserialize<List<RoleApplicationDto>>(json, options);
                            else if (doc.RootElement.ValueKind == JsonValueKind.Object && doc.RootElement.TryGetProperty("content", out var contentEl))
                                myOrgAppsList = JsonSerializer.Deserialize<List<RoleApplicationDto>>(contentEl.GetRawText(), options);
                        }

                        var myApp = myOrgAppsList?.FirstOrDefault(a => a.eventId == _eventId);
                        if (myApp != null)
                        {
                            orgActive = false;
                            if (myApp.status == "ОДОБРЕНА" || myApp.status == "ОДОБРЕНО") orgStatus = "Вы уже организатор";
                            else if (myApp.status == "На рассмотрении" || myApp.status == "НА_РАССМОТРЕНИИ") orgStatus = "Заявка на рассмотрении";
                        }
                    }

                    if (!_isPublicEvent && !_isMySector)
                    {
                        orgActive = false;
                        orgStatus = "Недоступно для вашего сектора";
                    }

                    roleViewModels.Add(new RegistrationRoleViewModel
                    {
                        Id = -2,
                        Title = "Организатор",
                        Description = "Помощь в организации и проведении мероприятия.",
                        DeadlineText = "Без дедлайна",
                        SlotsInfo = orgSlotsInfo,
                        IsActive = orgActive,
                        CardOpacity = orgActive ? 1.0 : 0.4,
                        ApplicationStatus = orgStatus
                    });
                }

                List<int> allowedRoleIds = new List<int>();
                if (!_isPublicEvent)
                {
                    HttpResponseMessage allowedRolesRes = await _httpClient.GetAsync($"/api/event-roles?eventId={_eventId}&isDeleted=false&isMySector=true&page=0&size=100");
                    if (allowedRolesRes.IsSuccessStatusCode)
                    {
                        string allowedJson = await allowedRolesRes.Content.ReadAsStringAsync();
                        using (JsonDocument doc = JsonDocument.Parse(allowedJson))
                        {
                            if (doc.RootElement.TryGetProperty("content", out var contentProp))
                            {
                                var allowedRoles = JsonSerializer.Deserialize<List<EventRoleDtoLocal>>(contentProp.GetRawText(), new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                                if (allowedRoles != null) allowedRoleIds = allowedRoles.Select(r => r.id).ToList();
                            }
                        }
                    }
                }

                HttpResponseMessage rolesRes = await _httpClient.GetAsync($"/api/event-roles?eventId={_eventId}&isDeleted=false&page=0&size=100");
                if (rolesRes.IsSuccessStatusCode)
                {
                    string json = await rolesRes.Content.ReadAsStringAsync();
                    using (JsonDocument doc = JsonDocument.Parse(json))
                    {
                        if (doc.RootElement.TryGetProperty("content", out var contentProp))
                        {
                            var eventRoles = JsonSerializer.Deserialize<List<EventRoleDtoLocal>>(contentProp.GetRawText(), new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                            foreach (var role in eventRoles)
                            {
                                if (role.deleted) continue;

                                bool isActive = true;
                                string statusText = "";
                                var app = userApplications.FirstOrDefault(a => a.eventRoleId == role.id);

                                if (app != null)
                                {
                                    isActive = false;
                                    if (app.status == "ОДОБРЕНА" || app.status == "ОДОБРЕНО") statusText = "Вы уже участник";
                                    else if (app.status == "На рассмотрении" || app.status == "НА_РАССМОТРЕНИИ") statusText = "Заявка на рассмотрении";
                                }
                                else if (IsDeadlinePassed(role.deadline))
                                {
                                    isActive = false; statusText = "Дедлайн прошёл";
                                }
                                else if (role.isFullyFull || role.totalAvailableSlots <= 0)
                                {
                                    isActive = false; statusText = "Свободных мест больше нет";
                                }
                                else if (!_isPublicEvent && !allowedRoleIds.Contains(role.id))
                                {
                                    isActive = false; statusText = "Недоступно для вашего сектора";
                                }

                                roleViewModels.Add(new RegistrationRoleViewModel
                                {
                                    Id = role.id,
                                    Title = role.globalEventRoleTitle ?? "Роль",
                                    Description = string.IsNullOrEmpty(role.description) ? "Описание не указано" : role.description,
                                    DeadlineText = $"Дедлайн: {FormatDeadline(role.deadline)}",
                                    SlotsInfo = $"Свободно мест: {role.totalAvailableSlots}",
                                    IsActive = isActive,
                                    CardOpacity = isActive ? 1.0 : 0.4,
                                    ApplicationStatus = statusText
                                });
                            }
                        }
                    }
                }

                _availableRoles = roleViewModels;
                RolesItemsControl.ItemsSource = _availableRoles;
                EmptyRolesText.Visibility = _availableRoles.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сбой сети при загрузке ролей: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private async Task<List<RoleApplicationDto>> GetUserApplicationsAsync()
        {
            try
            {
                var response = await _httpClient.GetAsync($"/api/role-applications?eventId={_eventId}");
                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                    using (var doc = JsonDocument.Parse(json))
                    {
                        if (doc.RootElement.ValueKind == JsonValueKind.Array)
                        {
                            return JsonSerializer.Deserialize<List<RoleApplicationDto>>(json, options);
                        }
                        else if (doc.RootElement.ValueKind == JsonValueKind.Object && doc.RootElement.TryGetProperty("content", out var contentEl))
                        {
                            return JsonSerializer.Deserialize<List<RoleApplicationDto>>(contentEl.GetRawText(), options);
                        }
                    }
                }
            }
            catch (Exception ex) { Debug.WriteLine(ex.Message); }
            return new List<RoleApplicationDto>();
        }

        private bool IsDeadlinePassed(string deadlineStr)
        {
            if (string.IsNullOrEmpty(deadlineStr)) return false;
            return DateTime.TryParse(deadlineStr, out DateTime dl) && dl < DateTime.Now;
        }

        private void Role_Checked(object sender, RoutedEventArgs e)
        {
            if (sender is CheckBox cb && cb.Tag is int roleId && cb.DataContext is RegistrationRoleViewModel vm && vm.IsActive)
            {
                if (!_selectedRoleIds.Contains(roleId)) _selectedRoleIds.Add(roleId);
            }
        }

        private void Role_Unchecked(object sender, RoutedEventArgs e)
        {
            if (sender is CheckBox cb && cb.Tag is int roleId)
            {
                _selectedRoleIds.Remove(roleId);
            }
        }

        private async void SubmitBtn_Click(object sender, RoutedEventArgs e)
        {
            if (_selectedRoleIds.Count == 0)
            {
                CustomMessageBox.Show("Пожалуйста, выберите хотя бы одну роль для регистрации.", "Внимание", CustomMessageBox.MessageType.Info);
                return;
            }

            SubmitBtn.IsEnabled = false;
            LoadingOverlay.Visibility = Visibility.Visible;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                int successCount = 0;
                string commentText = tbComment.Text.Trim();

                foreach (int roleId in _selectedRoleIds)
                {
                    if (roleId == -1) 
                    {
                        var emptyJson = new StringContent("{}", Encoding.UTF8, "application/json");
                        HttpResponseMessage joinRes = await _httpClient.PostAsync($"/api/events/participants/{_eventId}/join", emptyJson);
                        if (joinRes.IsSuccessStatusCode) successCount++;
                        else Debug.WriteLine($"Ошибка регистрации участником: {await joinRes.Content.ReadAsStringAsync()}");
                    }
                    else if (roleId == -2) 
                    {
                        var payload = new { description = commentText };
                        var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
                        HttpResponseMessage orgRes = await _httpClient.PostAsync($"/api/role-applications/{_eventId}/orgainizer", content);
                        if (orgRes.IsSuccessStatusCode) successCount++;
                        else Debug.WriteLine($"Ошибка регистрации организатором: {await orgRes.Content.ReadAsStringAsync()}");
                    }
                    else 
                    {
                        var payload = new { description = commentText };
                        var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
                        HttpResponseMessage stdRes = await _httpClient.PostAsync($"/api/role-applications/{roleId}", content);
                        if (stdRes.IsSuccessStatusCode) successCount++;
                        else Debug.WriteLine($"Ошибка заявки на кастомную роль {roleId}: {await stdRes.Content.ReadAsStringAsync()}");
                    }
                }

                if (successCount > 0)
                {
                    CustomMessageBox.Show($"Успешно отправлено заявок: {successCount}.", "Успех", CustomMessageBox.MessageType.Success);
                    this.NavigationService.GoBack();
                }
                else
                {
                    CustomMessageBox.Show("Не удалось отправить ни одной заявки. Возможно, вы уже подали заявку.", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Сетевая ошибка: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                SubmitBtn.IsEnabled = true;
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private void BackButton_Click(object sender, RoutedEventArgs e)
        {
            if (this.NavigationService.CanGoBack) this.NavigationService.GoBack();
        }

        private string FormatDeadline(string deadlineStr)
        {
            if (!string.IsNullOrEmpty(deadlineStr) && DateTime.TryParse(deadlineStr, out DateTime date))
                return date.ToString("d MMMM yyyy, HH:mm", new CultureInfo("ru-RU"));
            return "Не указан";
        }
    }

    public class EventDetailDtoLocal
    {
        public int id { get; set; }
        public string title { get; set; }
        public bool isPublic { get; set; }
        public bool isFreeEvent { get; set; }
        public bool isMySector { get; set; }
        public int maxOrganizersCount { get; set; }
    }
    public class RegistrationRoleViewModel { public int Id { get; set; } public string Title { get; set; } public string Description { get; set; } public string DeadlineText { get; set; } public string SlotsInfo { get; set; } public bool IsActive { get; set; } = true; public double CardOpacity { get; set; } = 1.0; public string ApplicationStatus { get; set; } = ""; public bool IsSelected { get; set; } }
    public class EventRoleDtoLocal { public int id { get; set; } public int eventId { get; set; } public string globalEventRoleTitle { get; set; } public string description { get; set; } public string deadline { get; set; } public int totalAvailableSlots { get; set; } public bool isFullyFull { get; set; } public string sectorTitle { get; set; } public bool deleted { get; set; } }
    public class RoleApplicationDto { public int eventRoleId { get; set; } public int eventId { get; set; } public string status { get; set; } = ""; }
    public class ParticipantSlotsDto { public int currentParticipants { get; set; } public long availableSlots { get; set; } public string info { get; set; } }
    public class MyParticipantRecordDto { public int eventId { get; set; } }
}