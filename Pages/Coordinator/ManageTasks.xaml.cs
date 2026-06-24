using Diplom_Stud.Components;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using Diplom_Stud.Pages.Activist; 

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class ManageTasks : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();

        public ManageTasks()
        {
            InitializeComponent();
            if (_httpClient.BaseAddress == null)
            {
                _httpClient.BaseAddress = new Uri(App.ApiBaseUrl);
                _httpClient.DefaultRequestHeaders.Accept.Clear();
                _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            }
        }

        private async void Page_Loaded(object sender, RoutedEventArgs e)
        {
            await LoadDataAsync();
        }

        private async Task LoadDataAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                var reqRes = await _httpClient.GetAsync("/api/task-requests?myTasks=true&pendingOnly=true&page=0&size=100&sortBy=filingDate&sortDirection=DESC");
                if (reqRes.IsSuccessStatusCode)
                {
                    var reqData = JsonSerializer.Deserialize<TaskRequestPageResponse>(await reqRes.Content.ReadAsStringAsync(), options);
                    RequestsList.ItemsSource = reqData?.content;
                    EmptyReqText.Visibility = (reqData?.content == null || reqData.content.Count == 0) ? Visibility.Visible : Visibility.Collapsed;
                }

                var tasksRes = await _httpClient.GetAsync("/api/tasks?createdByMe=true&isCompleted=false&isDeleted=false&page=0&size=100&sortBy=deadline&sortDirection=ASC");
                if (tasksRes.IsSuccessStatusCode)
                {
                    var taskData = JsonSerializer.Deserialize<ExtendedTaskPageResponse>(await tasksRes.Content.ReadAsStringAsync(), options);
                    MyTasksList.ItemsSource = taskData?.content;
                    EmptyTasksText.Visibility = (taskData?.content == null || taskData.content.Count == 0) ? Visibility.Visible : Visibility.Collapsed;
                }
            }
            catch (Exception ex) { CustomMessageBox.Show(ex.Message, "Ошибка", CustomMessageBox.MessageType.Error); }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
        }

        private void Numeric_Preview(object sender, TextCompositionEventArgs e) => e.Handled = !e.Text.All(char.IsDigit);

        private async void CreateTask_Click(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrWhiteSpace(TbTitle.Text) || !DpDeadline.SelectedDate.HasValue ||
                string.IsNullOrWhiteSpace(TbMaxPeople.Text) || string.IsNullOrWhiteSpace(TbPoints.Text))
            {
                CustomMessageBox.Show("Заполните все поля задачи.", "Ошибка", CustomMessageBox.MessageType.Warning);
                return;
            }

            LoadingOverlay.Visibility = Visibility.Visible;
            try
            {
                var payload = new
                {
                    title = TbTitle.Text.Trim(),
                    description = TbDescription.Text.Trim(),
                    deadline = DpDeadline.SelectedDate.Value.ToString("yyyy-MM-ddT23:59:59Z"),
                    maxPeopleCount = int.Parse(TbMaxPeople.Text),
                    countOfPoints = int.Parse(TbPoints.Text),
                    isPreassigned = false
                };

                var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
                var response = await _httpClient.PostAsync("/api/tasks", content);

                if (response.IsSuccessStatusCode)
                {
                    CustomMessageBox.Show("Задача успешно создана!", "Успех", CustomMessageBox.MessageType.Success);
                    TbTitle.Text = ""; TbDescription.Text = ""; DpDeadline.SelectedDate = null; TbMaxPeople.Text = ""; TbPoints.Text = "";
                    await LoadDataAsync();
                }
                else CustomMessageBox.Show("Не удалось создать задачу", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            catch (Exception ex) { CustomMessageBox.Show(ex.Message, "Ошибка", CustomMessageBox.MessageType.Error); }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
        }

        private async void ApproveReq_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int reqId) await SendPutRequest($"/api/task-requests/{reqId}/approve", "Заявка одобрена!");
        }

        private async void RejectReq_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int reqId) await SendPutRequest($"/api/task-requests/{reqId}/reject", "Заявка отклонена.");
        }

        private async void CompleteTask_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int taskId) await SendPutRequest($"/api/tasks/{taskId}/completion/creator?isCompleted=true", "Задача успешно завершена!");
        }

        private async void DeleteTask_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is int taskId)
            {
                var result = MessageBox.Show("Точно удалить задачу?", "Удаление", MessageBoxButton.YesNo, MessageBoxImage.Warning);
                if (result == MessageBoxResult.Yes)
                {
                    LoadingOverlay.Visibility = Visibility.Visible;
                    try
                    {
                        var res = await _httpClient.DeleteAsync($"/api/tasks/{taskId}");
                        if (res.IsSuccessStatusCode) await LoadDataAsync();
                        else CustomMessageBox.Show("Ошибка удаления.", "Ошибка", CustomMessageBox.MessageType.Error);
                    }
                    catch { }
                    finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
                }
            }
        }

        private async Task SendPutRequest(string endpoint, string successMessage)
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            try
            {
                var res = await _httpClient.PutAsync(endpoint, new StringContent(""));
                if (res.IsSuccessStatusCode)
                {
                    CustomMessageBox.Show(successMessage, "Успех", CustomMessageBox.MessageType.Success);
                    await LoadDataAsync();
                }
                else CustomMessageBox.Show("Действие не выполнено", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            catch (Exception ex) { CustomMessageBox.Show(ex.Message, "Ошибка", CustomMessageBox.MessageType.Error); }
            finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
        }
    }

    public class TaskRequestDto
    {
        public int id { get; set; }
        public int taskId { get; set; }
        public string taskTitle { get; set; }
        public string studentName { get; set; }
        public string studentSurname { get; set; }
        public string FullName => $"{studentName} {studentSurname}";
    }

    public class TaskRequestPageResponse
    {
        public List<TaskRequestDto> content { get; set; }
    }

    public class ExtendedTaskPageResponse
    {
        public List<ExtendedTaskDto> content { get; set; }
    }
    public class ExtendedTaskDto : TaskDto
    {
        public int assignedUsersCount { get; set; }
    }
}