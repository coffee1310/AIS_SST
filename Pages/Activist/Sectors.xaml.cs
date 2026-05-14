using Diplom_Stud.Components;
using Diplom_Stud.Pages.General;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
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

namespace Diplom_Stud.Pages.Activist
{
    public partial class Sectors : Page
    {
        private static readonly HttpClient _httpClient = new HttpClient();

        public Sectors()
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

            await LoadSectorsAsync();
        }

        private async Task LoadSectorsAsync()
        {
            try
            {
                if (string.IsNullOrEmpty(App.AuthToken))
                {
                    CustomMessageBox.Show("Ошибка авторизации. Пожалуйста, войдите снова.", "Ошибка", CustomMessageBox.MessageType.Error);
                    NavigationService?.Navigate(new Auth());
                    return;
                }

                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                HttpResponseMessage response = await _httpClient.GetAsync("/api/sector");

                if (response.IsSuccessStatusCode)
                {
                    string responseBody = await response.Content.ReadAsStringAsync();
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var sectors = JsonSerializer.Deserialize<List<SectorDto>>(responseBody, options);

                    if (sectors != null)
                    {
                        var viewModels = new List<SectorViewModel>();

                        foreach (var sector in sectors)
                        {
                            var vm = new SectorViewModel
                            {
                                Id = sector.id,
                                Title = sector.title,
                                Description = sector.description
                            };

                            if (sector.isCoordinator)
                            {
                                vm.ButtonText = "Координатор";
                                vm.ButtonStyle = FindResource("SectorActiveButtonStyle") as Style;
                            }
                            else if (sector.isParticipant)
                            {
                                vm.ButtonText = "Участник";
                                vm.ButtonStyle = FindResource("SectorActiveButtonStyle") as Style;
                            }
                            else if (sector.hasActiveRequest)
                            {
                                vm.ButtonText = "Ожидание";
                                vm.ButtonStyle = FindResource("SectorInactiveButtonStyle") as Style;
                            }
                            else
                            {
                                vm.ButtonText = "Вступить";
                                vm.ButtonStyle = FindResource("SectorActiveButtonStyle") as Style;
                            }

                            vm.Image = new BitmapImage(new Uri("pack://application:,,,/Resources/sector.png"));

                            if (!string.IsNullOrEmpty(sector.photo))
                            {
                                BitmapImage bmp = GetImageFromBase64(sector.photo);
                                if (bmp != null)
                                {
                                    vm.Image = bmp;
                                }
                            }

                            viewModels.Add(vm);
                        }

                        SectorsItemsControl.ItemsSource = viewModels;
                    }
                }
                else
                {
                    CustomMessageBox.Show($"Ошибка загрузки секторов: {response.StatusCode}", "Ошибка", CustomMessageBox.MessageType.Error);
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Произошла ошибка при загрузке данных: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
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
                        if (commaIndex >= 0)
                        {
                            string actualBase64 = textInside.Substring(commaIndex + 1);
                            imageBytes = Convert.FromBase64String(actualBase64);
                        }
                    }
                    else
                    {
                        imageBytes = decodedFirstLevel;
                    }
                }
                catch
                {
                    int commaIndex = cleanStr.IndexOf(',');
                    if (commaIndex >= 0)
                    {
                        cleanStr = cleanStr.Substring(commaIndex + 1);
                    }
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
            catch (Exception ex)
            {
                Debug.WriteLine($"Ошибка обработки фото сектора: {ex.Message}");
            }
            return null;
        }

        private void SectorCard_Click(object sender, MouseButtonEventArgs e)
        {
            if (sender is Border border && border.Tag is int sectorId)
            {
                this.NavigationService.Navigate(new SectorDetails(sectorId));
            }
        }
    }

    public class SectorDto
    {
        public int id { get; set; }
        public string title { get; set; }
        public string description { get; set; }
        public bool isParticipant { get; set; }
        public bool isCoordinator { get; set; }
        public bool hasActiveRequest { get; set; }
        public string requestStatus { get; set; }
        public int participantCount { get; set; }
        public string photo { get; set; }

        public List<CoordinatorDto> coordinators { get; set; }
    }

    public class CoordinatorDto
    {
        public int id { get; set; }
        public int studentId { get; set; }
        public string studentName { get; set; }
        public string studentSurname { get; set; }
        public string studentPatronymic { get; set; }
        public string studentEmail { get; set; }
        public string studentPhoto { get; set; }
        public int? studentCourseNumber { get; set; }
        public string studentGroupTitle { get; set; }
        public string studentSpecialityTitle { get; set; }
        public string entryDate { get; set; }
        public string status { get; set; }
        public bool isCoordinator { get; set; }
    }

    public class SectorViewModel
    {
        public int Id { get; set; }
        public string Title { get; set; }
        public string Description { get; set; }
        public ImageSource Image { get; set; }
        public string ButtonText { get; set; }
        public Style ButtonStyle { get; set; }
    }
}