using Diplom_Stud.Components;
using System;
using System.Collections.Generic;
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
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class Rating : System.Windows.Controls.Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();
        private List<RatingItemViewModel> _allRatingItems = new List<RatingItemViewModel>();
        private int _currentPage = 0;
        private int _pageSize = 8;
        private int _totalPages = 1;

        public Rating()
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
            DoubleAnimation fadeInAnimation = new DoubleAnimation
            {
                From = 0.0,
                To = 1.0,
                Duration = TimeSpan.FromSeconds(0.8),
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };
            this.BeginAnimation(Page.OpacityProperty, fadeInAnimation);

            await LoadRatingDataAsync();
        }

        private async Task LoadRatingDataAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyText.Visibility = Visibility.Collapsed;
            PaginationPanel.Visibility = Visibility.Collapsed;
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                HttpResponseMessage reportRes = await _httpClient.GetAsync("/api/reports/users");
                HttpResponseMessage usersRes = await _httpClient.GetAsync("/api/users/all?page=0&size=10000");

                if (reportRes.IsSuccessStatusCode && usersRes.IsSuccessStatusCode)
                {
                    string reportJson = await reportRes.Content.ReadAsStringAsync();
                    string usersJson = await usersRes.Content.ReadAsStringAsync();

                    var ratingData = JsonSerializer.Deserialize<List<RatingReportDto>>(reportJson, options) ?? new List<RatingReportDto>();
                    var usersPage = JsonSerializer.Deserialize<UserPageResponse>(usersJson, options);
                    var usersList = usersPage?.content ?? new List<UserAllItemDto>();

                    var filteredRatingData = ratingData
                        .Where(r => !IsCuratorRole(r.role))
                        .ToList();

                    var viewModels = new List<RatingItemViewModel>();

                    foreach (var r in filteredRatingData.OrderBy(x => x.position))
                    {
                        viewModels.Add(new RatingItemViewModel
                        {
                            UserId = r.userId,
                            Place = r.position,
                            FullName = r.fio ?? $"{r.userSurname} {r.userName} {r.patronymic}".Trim(),
                            Role = GetRussianRole(r.role),
                            EventsCount = r.eventsCount ?? 0,
                            Points = r.totalPoints ?? 0,
                            RowBackground = GetRowBackground(r.position)
                        });
                    }

                    _allRatingItems = viewModels;
                    ApplyFilterAndPaginate();
                }
                else
                {
                    EmptyText.Visibility = Visibility.Visible;
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка загрузки рейтинга: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private void ApplyFilterAndPaginate()
        {
            if (_allRatingItems == null || _allRatingItems.Count == 0)
            {
                RatingItemsControl.ItemsSource = null;
                TotalActivistsText.Text = "Всего активистов: 0";
                PaginationPanel.Visibility = Visibility.Collapsed;
                EmptyText.Visibility = Visibility.Visible;
                return;
            }

            var filtered = _allRatingItems.AsEnumerable();

            string searchTerm = SearchBox?.Text?.Trim().ToLowerInvariant() ?? "";
            if (!string.IsNullOrEmpty(searchTerm))
            {
                filtered = filtered.Where(x =>
                    (x.FullName?.ToLowerInvariant().Contains(searchTerm) ?? false) ||
                    (x.Role?.ToLowerInvariant().Contains(searchTerm) ?? false)
                );
            }

            var filteredList = filtered.ToList();

            _totalPages = filteredList.Count == 0 ? 1 : (int)Math.Ceiling(filteredList.Count / (double)_pageSize);
            if (_currentPage >= _totalPages) _currentPage = _totalPages - 1;
            if (_currentPage < 0) _currentPage = 0;

            var pageItems = filteredList.Skip(_currentPage * _pageSize).Take(_pageSize).ToList();

            RatingItemsControl.ItemsSource = pageItems;

            TotalActivistsText.Text = $"Всего активистов: {_allRatingItems.Count}";
            TxtPageInfo.Text = $"Страница {_currentPage + 1} из {_totalPages}";
            BtnPrevPage.IsEnabled = _currentPage > 0;
            BtnNextPage.IsEnabled = _currentPage < _totalPages - 1;

            PaginationPanel.Visibility = (_totalPages > 1 || !string.IsNullOrEmpty(searchTerm)) ? Visibility.Visible : Visibility.Collapsed;
            EmptyText.Visibility = pageItems.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
        }

        private void SearchBox_TextChanged(object sender, System.Windows.Controls.TextChangedEventArgs e)
        {
            if (SearchPlaceholder != null)
                SearchPlaceholder.Visibility = string.IsNullOrWhiteSpace(SearchBox.Text) ? Visibility.Visible : Visibility.Collapsed;

            _currentPage = 0;
            ApplyFilterAndPaginate();
        }

        private string GetRussianRole(string originalRole)
        {
            if (string.IsNullOrWhiteSpace(originalRole))
                return "Активист";

            string lower = originalRole.ToLowerInvariant().Trim();

            if (lower.Contains("coordinator") || lower.Contains("координатор") || lower.Contains("sector_coordinator"))
                return "Координатор сектора";

            if (lower.Contains("activist") || lower == "активист" || lower == "participant")
                return "Активист";

            if (lower.Contains("chairman") || lower == "председатель")
                return "Председатель";

            if (lower.Contains("deputy_chairman") || lower == "заместитель председателя")
                return "Заместитель председателя";

            if (lower.Contains("secretary") || lower == "секретарь")
                return "Секретарь";

            return originalRole;
        }

        private bool IsCuratorRole(string role)
        {
            if (string.IsNullOrWhiteSpace(role)) return false;
            string lower = role.ToLowerInvariant();
            return lower.Contains("curator") || lower.Contains("куратор");
        }

        private void UserCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is FrameworkElement element && element.Tag is int userId)
            {
                this.NavigationService.Navigate(new Diplom_Stud.Pages.Activist.Profile(userId));
            }
        }

        private void PrevPage_Click(object sender, RoutedEventArgs e)
        {
            if (_currentPage > 0)
            {
                _currentPage--;
                ApplyFilterAndPaginate();
            }
        }

        private void NextPage_Click(object sender, RoutedEventArgs e)
        {
            if (_currentPage < _totalPages - 1)
            {
                _currentPage++;
                ApplyFilterAndPaginate();
            }
        }

        private Brush GetRowBackground(int place)
        {
            if (place == 1)
            {
                var brush = new LinearGradientBrush
                {
                    StartPoint = new Point(0, 0),
                    EndPoint = new Point(1, 0)
                };
                brush.GradientStops.Add(new GradientStop(Color.FromRgb(0x2D, 0x3A, 0x8C), 0));
                brush.GradientStops.Add(new GradientStop(Color.FromRgb(0x00, 0xD4, 0xC8), 1));
                return brush;
            }
            return new SolidColorBrush(Color.FromRgb(0x1C, 0x1B, 0x29));
        }

    }

    public class RatingReportDto
    {
        public int userId { get; set; }
        public string userName { get; set; }
        public string userSurname { get; set; }
        public string patronymic { get; set; }
        public string role { get; set; }
        public string fio { get; set; }
        public int? totalPoints { get; set; }
        public int? eventsCount { get; set; }
        public int position { get; set; }
    }

    public class UserPageResponse { public List<UserAllItemDto> content { get; set; } }

    public class UserAllItemDto
    {
        public int id { get; set; }
        public string photo { get; set; }
    }

    public class RatingItemViewModel
    {
        public int UserId { get; set; }
        public int Place { get; set; }
        public string FullName { get; set; }
        public string Role { get; set; }
        public int EventsCount { get; set; }
        public int Points { get; set; }
        public ImageSource Avatar { get; set; }
        public Brush RowBackground { get; set; }

        public Brush PlaceColor
        {
            get
            {
                if (Place == 1) return new SolidColorBrush((Color)ColorConverter.ConvertFromString("#FFD700"));
                if (Place == 2) return new SolidColorBrush((Color)ColorConverter.ConvertFromString("#C0C0C0"));
                if (Place == 3) return new SolidColorBrush((Color)ColorConverter.ConvertFromString("#CD7F32"));
                return Brushes.White;
            }
        }
    }
}