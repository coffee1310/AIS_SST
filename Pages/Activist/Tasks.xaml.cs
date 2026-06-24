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
using System.Windows.Media;

namespace Diplom_Stud.Pages.Activist
{
    public partial class Tasks : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private List<TaskViewModel> _allTasksCache = new List<TaskViewModel>();
        private bool _isMineTab = false;

        public Tasks()
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
            await LoadTasksAsync();
        }

        private async void Tab_Checked(object sender, RoutedEventArgs e)
        {
            if (TabMine == null || TabAvailable == null || LoadingOverlay == null) return;

            _isMineTab = TabMine.IsChecked == true;
            await LoadTasksAsync();
        }

        private async Task LoadTasksAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyText.Visibility = Visibility.Collapsed;
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                string url = _isMineTab
                    ? "/api/tasks?isCompleted=false&isDeleted=false&assignedToMe=true&page=0&size=100&sortBy=deadline&sortDirection=ASC"
                    : "/api/tasks?isCompleted=false&isDeleted=false&isPreassigned=false&assignedToMe=false&page=0&size=100&sortBy=deadline&sortDirection=ASC";

                var response = await _httpClient.GetAsync(url);

                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var pageData = JsonSerializer.Deserialize<TaskPageResponse>(json, options);

                    _allTasksCache = pageData?.content?.Select(t => new TaskViewModel
                    {
                        Task = t,
                        IsMine = _isMineTab
                    }).ToList() ?? new List<TaskViewModel>();

                    FilterTasks(tbSearch.Text);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка загрузки задач: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private void TbSearch_TextChanged(object sender, TextChangedEventArgs e) => FilterTasks(tbSearch.Text);

        private void FilterTasks(string query)
        {
            if (string.IsNullOrWhiteSpace(query))
            {
                TasksList.ItemsSource = _allTasksCache;
            }
            else
            {
                TasksList.ItemsSource = _allTasksCache.Where(tvm =>
                    tvm.Task.title.IndexOf(query, StringComparison.OrdinalIgnoreCase) >= 0 ||
                    tvm.Task.description.IndexOf(query, StringComparison.OrdinalIgnoreCase) >= 0).ToList();
            }

            EmptyText.Visibility = TasksList.Items.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
        }

        private async void TaskAction_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is TaskViewModel tvm)
            {
                LoadingOverlay.Visibility = Visibility.Visible;
                try
                {
                    if (tvm.IsMine)
                    {
                        var payload = new { isCompleted = true };
                        var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
                        var response = await _httpClient.PutAsync($"/api/tasks/{tvm.Task.id}/completion/executor", content);

                        if (response.IsSuccessStatusCode)
                        {
                            CustomMessageBox.Show("Задача отмечена как выполненная!", "Успех", CustomMessageBox.MessageType.Success);
                            await LoadTasksAsync();
                        }
                        else
                        {
                            CustomMessageBox.Show("Ошибка завершения задачи.", "Ошибка", CustomMessageBox.MessageType.Error);
                        }
                    }
                    else
                    {
                        var payload = new { taskId = tvm.Task.id };
                        var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
                        var response = await _httpClient.PostAsync("/api/task-requests", content);

                        if (response.IsSuccessStatusCode)
                        {
                            CustomMessageBox.Show("Заявка успешно подана!", "Успех", CustomMessageBox.MessageType.Success);
                            await LoadTasksAsync();
                        }
                        else
                        {
                            CustomMessageBox.Show("Ошибка подачи заявки (возможно, вы уже подали её).", "Ошибка", CustomMessageBox.MessageType.Error);
                        }
                    }
                }
                catch (Exception ex) { CustomMessageBox.Show(ex.Message, "Ошибка", CustomMessageBox.MessageType.Error); }
                finally { LoadingOverlay.Visibility = Visibility.Collapsed; }
            }
        }
    }

    public class TaskDto
    {
        public int id { get; set; }
        public string title { get; set; }
        public string description { get; set; }
        public DateTime deadline { get; set; }
        public int maxPeopleCount { get; set; }
        public int countOfPoints { get; set; }
        public bool isCompleted { get; set; }
        public bool isDeleted { get; set; }
        public bool isPreassigned { get; set; }
        public string DeadlineDisplay => deadline.ToString("d MMMM HH:mm");
        public string PointsDisplay => $"+{countOfPoints} баллов";
    }

    public class TaskPageResponse { public List<TaskDto> content { get; set; } }

    public class TaskViewModel
    {
        public TaskDto Task { get; set; }
        public bool IsMine { get; set; }

        public string ActionText => IsMine ? "Выполнено" : "Откликнуться";
        public Brush ActionBackground => IsMine ? new SolidColorBrush(Color.FromArgb(38, 0, 200, 83)) : new SolidColorBrush(Color.FromArgb(255, 2, 179, 186));
        public Brush ActionForeground => IsMine ? new SolidColorBrush(Color.FromArgb(255, 0, 200, 83)) : Brushes.White;
    }
}