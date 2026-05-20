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
                Duration = TimeSpan.FromSeconds(0.8),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };
            this.BeginAnimation(Page.OpacityProperty, fadeInAnimation);

            if (_eventId <= 0)
            {
                CustomMessageBox.Show("ID мероприятия не передан.", "Ошибка", CustomMessageBox.MessageType.Error);
                LoadingOverlay.Visibility = Visibility.Collapsed;
                return;
            }

            await LoadEventDataAsync();
            await LoadEventRolesAsync();
        }

        private async Task LoadEventDataAsync()
        {
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                HttpResponseMessage response = await _httpClient.GetAsync($"/api/events/{_eventId}");
                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var ev = JsonSerializer.Deserialize<EventDetailDto>(responseBody, options);
                    if (ev != null)
                    {
                        EventTitleText.Text = ev.title;
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Ошибка загрузки названия: {ex.Message}");
            }
        }

        private async Task LoadEventRolesAsync()
        {
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync($"/api/event-roles?eventId={_eventId}&page=0&size=50");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var rolesPage = JsonSerializer.Deserialize<EventRolePageResponse>(responseBody, options);

                    if (rolesPage?.content != null && rolesPage.content.Count > 0)
                    {
                        var userApplications = await GetUserApplicationsAsync();

                        var rolesList = new List<RegistrationRoleViewModel>();

                        foreach (var role in rolesPage.content)
                        {
                            bool deadlinePassed = IsDeadlinePassed(role.deadline);
                            var app = userApplications.FirstOrDefault(a => a.eventRoleId == role.id);

                            string statusText = "";
                            bool canApply = !deadlinePassed && app == null;

                            if (app != null)
                            {
                                if (app.status == "ОДОБРЕНА")
                                {
                                    statusText = "Вы уже участник";
                                    canApply = false;
                                }
                                else if (app.status == "На рассмотрении")
                                {
                                    statusText = "Заявка на рассмотрении";
                                    canApply = false;
                                }
                            }
                            else if (deadlinePassed)
                            {
                                statusText = "Дедлайн прошёл";
                            }

                            rolesList.Add(new RegistrationRoleViewModel
                            {
                                Id = role.id,
                                Title = role.globalEventRoleTitle ?? "Роль",
                                Description = string.IsNullOrEmpty(role.description) ? "Описание не указано" : role.description,
                                DeadlineText = FormatDeadline(role.deadline),
                                CanApply = canApply,
                                ApplicationStatus = statusText
                            });
                        }

                        RolesItemsControl.ItemsSource = rolesList;
                        EmptyRolesText.Visibility = Visibility.Collapsed;

                        SubmitBtn.IsEnabled = true;
                        SubmitBtn.Opacity = 1.0;
                    }
                    else
                    {
                        RolesItemsControl.ItemsSource = null;
                        EmptyRolesText.Visibility = Visibility.Visible;
                        SubmitBtn.IsEnabled = false;
                    }
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка загрузки ролей: {response.StatusCode}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
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
                    var page = JsonSerializer.Deserialize<RoleApplicationPageResponse>(json, options);
                    return page?.content ?? new List<RoleApplicationDto>();
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Не удалось загрузить заявки пользователя: {ex.Message}");
            }
            return new List<RoleApplicationDto>();
        }

        private bool IsDeadlinePassed(string deadlineStr)
        {
            if (string.IsNullOrEmpty(deadlineStr)) return false;
            return DateTime.TryParse(deadlineStr, out DateTime dl) && dl < DateTime.Now;
        }

        private void Role_Checked(object sender, RoutedEventArgs e)
        {
            if (sender is CheckBox cb &&
                cb.Tag is int roleId &&
                cb.DataContext is RegistrationRoleViewModel vm &&
                vm.CanApply)
            {
                if (!_selectedRoleIds.Contains(roleId))
                    _selectedRoleIds.Add(roleId);
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
                CustomMessageBox.Show("Пожалуйста, выберите хотя бы одну роль для регистрации.", "Внимание", CustomMessageBox.MessageType.Error);
                return;
            }

            SubmitBtn.IsEnabled = false;
            LoadingOverlay.Visibility = Visibility.Visible;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                string commentText = tbComment.Text.Trim();
                int successCount = 0;
                string lastError = string.Empty;

                foreach (int roleId in _selectedRoleIds)
                {
                    var payload = new { description = commentText };
                    string jsonPayload = JsonSerializer.Serialize(payload);
                    var content = new StringContent(jsonPayload, Encoding.UTF8, "application/json");

                    HttpResponseMessage response = await _httpClient.PostAsync($"/api/role-applications/{roleId}", content);

                    if (response.IsSuccessStatusCode)
                        successCount++;
                    else
                        lastError = await response.Content.ReadAsStringAsync();
                }

                if (successCount == _selectedRoleIds.Count)
                {
                    CustomMessageBox.Show("Ваши заявки успешно отправлены!", "Успех", CustomMessageBox.MessageType.Success);
                    if (this.NavigationService.CanGoBack)
                        this.NavigationService.GoBack();
                }
                else if (successCount > 0)
                {
                    CustomMessageBox.Show($"Часть заявок отправлена ({successCount} из {_selectedRoleIds.Count}).\nОшибка остальных: {lastError}", "Внимание", CustomMessageBox.MessageType.Warning);
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка при отправке заявки:\n{lastError}", "Ошибка", CustomMessageBox.MessageType.Error);
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
            if (this.NavigationService.CanGoBack)
                this.NavigationService.GoBack();
        }

        private string FormatDeadline(string deadlineStr)
        {
            if (!string.IsNullOrEmpty(deadlineStr) && DateTime.TryParse(deadlineStr, out DateTime date))
                return date.ToString("d MMMM, HH:mm", new CultureInfo("ru-RU"));
            return "Не указан";
        }
    }

    public class RegistrationRoleViewModel
    {
        public int Id { get; set; }
        public string Title { get; set; }
        public string Description { get; set; }
        public string DeadlineText { get; set; }
        public bool CanApply { get; set; } = true;
        public string ApplicationStatus { get; set; } = "";
    }

    public class RoleApplicationPageResponse
    {
        public List<RoleApplicationDto> content { get; set; }
    }

    public class RoleApplicationDto
    {
        public int eventRoleId { get; set; }
        public string status { get; set; } = "";
    }
}