using ClosedXML.Excel;
using Diplom_Stud.Components;
using Microsoft.Win32;
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
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using Xceed.Document.NET;
using Xceed.Words.NET;

namespace Diplom_Stud.Pages.Coordinator
{
    public partial class EventFinalizePage : Page
    {
        private int _eventId;
        private static readonly HttpClient _httpClient = new HttpClient();
        private List<Fin_RoleGroupViewModel> _blocks = new List<Fin_RoleGroupViewModel>();
        private Dictionary<string, int> _globalRolePoints = new Dictionary<string, int>();

        public EventFinalizePage(int eventId)
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
            if (_eventId <= 0) return;
            await LoadParticipantsForFinalizeAsync();
        }

        private void BackButton_Click(object sender, RoutedEventArgs e)
        {
            this.NavigationService.GoBack();
        }

        private List<T> ParsePageOrList<T>(string json, JsonSerializerOptions options)
        {
            if (string.IsNullOrWhiteSpace(json)) return new List<T>();
            string trimmed = json.TrimStart();

            if (trimmed.StartsWith("["))
                return JsonSerializer.Deserialize<List<T>>(json, options) ?? new List<T>();
            else if (trimmed.StartsWith("{"))
            {
                try
                {
                    using (var doc = JsonDocument.Parse(json))
                    {
                        if (doc.RootElement.TryGetProperty("content", out var contentElem))
                            return JsonSerializer.Deserialize<List<T>>(contentElem.GetRawText(), options) ?? new List<T>();
                    }
                }
                catch { }
            }
            return new List<T>();
        }

        private async Task<List<Fin_GlobalUserDto>> FetchAllUsersAsync(JsonSerializerOptions options)
        {
            try
            {
                var res = await _httpClient.GetAsync("/api/users/all?page=0&size=1000&sortBy=id&sortDirection=ASC");
                if (res.IsSuccessStatusCode)
                {
                    var page = JsonSerializer.Deserialize<Fin_GlobalUserPage>(await res.Content.ReadAsStringAsync(), options);
                    return page?.content ?? new List<Fin_GlobalUserDto>();
                }
            }
            catch { }
            return new List<Fin_GlobalUserDto>();
        }

        private async Task LoadParticipantsForFinalizeAsync()
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            EmptyParticipantsText.Visibility = Visibility.Collapsed;
            _blocks.Clear();
            RolesWithParticipantsList.ItemsSource = null;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

                var allUsers = await FetchAllUsersAsync(options);

                HttpResponseMessage rolesResp = await _httpClient.GetAsync("/api/roles");
                if (rolesResp.IsSuccessStatusCode)
                {
                    string rolesJson = await rolesResp.Content.ReadAsStringAsync();
                    var allRoles = JsonSerializer.Deserialize<List<Fin_GlobalRoleDto>>(rolesJson, options) ?? new List<Fin_GlobalRoleDto>();
                    foreach (var r in allRoles)
                    {
                        _globalRolePoints[r.title] = r.defaultPoints;
                    }
                }

                HttpResponseMessage resPart = await _httpClient.GetAsync($"/api/events/participants/{_eventId}");
                if (resPart.IsSuccessStatusCode)
                {
                    var list = ParsePageOrList<Fin_ParticipantUserDto>(await resPart.Content.ReadAsStringAsync(), options);
                    if (list.Count > 0)
                    {
                        var block = new Fin_RoleGroupViewModel { RoleTitle = "Обычные участники", Participants = new List<Fin_ItemViewModel>() };
                        foreach (var u in list)
                        {
                            block.Participants.Add(new Fin_ItemViewModel
                            {
                                ParticipantId = u.id,
                                StudentId = u.id,
                                FullName = $"{u.surname} {u.name} {u.patronymic}".Trim(),
                                Type = "Participant",
                                TypeDisplay = "Слушатель / Зритель",
                                DefaultPoints = 2,
                                Avatar = GetImageFromBase64(u.photo) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                            });
                        }
                        _blocks.Add(block);
                    }
                }

                HttpResponseMessage resOrg = await _httpClient.GetAsync($"/api/events/participation/filter?eventId={_eventId}&entityType=ORGANIZER&page=0&size=100");
                if (resOrg.IsSuccessStatusCode)
                {
                    var list = ParsePageOrList<Fin_ParticipationRecordDto>(await resOrg.Content.ReadAsStringAsync(), options);
                    if (list.Count > 0)
                    {
                        var block = new Fin_RoleGroupViewModel { RoleTitle = "Организаторы", Participants = new List<Fin_ItemViewModel>() };
                        foreach (var u in list)
                        {
                            var matchedUser = allUsers.FirstOrDefault(x => x.DisplayName.Equals(u.fullName, StringComparison.OrdinalIgnoreCase));

                            block.Participants.Add(new Fin_ItemViewModel
                            {
                                ParticipantId = u.id,
                                StudentId = matchedUser?.id ?? 0,
                                FullName = u.fullName ?? "Неизвестный организатор",
                                Type = "Organizer",
                                TypeDisplay = "Координация",
                                DefaultPoints = 10,
                                Avatar = GetImageFromBase64(matchedUser?.photo) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                            });
                        }
                        _blocks.Add(block);
                    }
                }

                HttpResponseMessage resRoles = await _httpClient.GetAsync($"/api/role-applications?status=ОДОБРЕНА&eventId={_eventId}");
                if (resRoles.IsSuccessStatusCode)
                {
                    var list = ParsePageOrList<Fin_RoleAppDto>(await resRoles.Content.ReadAsStringAsync(), options);
                    if (list.Count > 0)
                    {
                        var grouped = list.GroupBy(c => string.IsNullOrEmpty(c.eventRoleName) ? "Без роли" : c.eventRoleName);
                        foreach (var g in grouped)
                        {
                            var block = new Fin_RoleGroupViewModel { RoleTitle = g.Key, Participants = new List<Fin_ItemViewModel>() };

                            int pts = 10;
                            if (_globalRolePoints.ContainsKey(g.Key)) pts = _globalRolePoints[g.Key];

                            foreach (var app in g)
                            {
                                var matchedUser = allUsers.FirstOrDefault(x => x.studentEmail == app.studentEmail);

                                block.Participants.Add(new Fin_ItemViewModel
                                {
                                    ParticipantId = app.id,
                                    StudentId = matchedUser?.id ?? app.studentId ?? 0,
                                    FullName = $"{app.studentSurname} {app.studentName} {app.studentPatronymic}".Trim(),
                                    Type = "Role",
                                    TypeDisplay = "Исполнитель",
                                    DefaultPoints = pts,
                                    Avatar = GetImageFromBase64(matchedUser?.photo ?? app.studentPhoto) ?? new BitmapImage(new Uri("pack://application:,,,/Resources/prof.png"))
                                });
                            }
                            _blocks.Add(block);
                        }
                    }
                }

                RolesWithParticipantsList.ItemsSource = _blocks;
                if (_blocks.Count == 0) EmptyParticipantsText.Visibility = Visibility.Visible;
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка загрузки: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
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

        private async void FinalizeEvent_Click(object sender, RoutedEventArgs e)
        {
            if (_blocks.Count == 0 || _blocks.All(b => b.Participants.Count == 0))
            {
                CustomMessageBox.Show("Нет участников для завершения.", "Ошибка", CustomMessageBox.MessageType.Warning);
                return;
            }

            btnFinalize.IsEnabled = false;
            LoadingOverlay.Visibility = Visibility.Visible;

            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);

                var presentParts = new List<int>();
                var presentOrgs = new List<int>();
                var presentRoles = new List<int>();

                var absentParts = new List<int>();
                var absentOrgs = new List<int>();
                var absentRoles = new List<int>();

                var pointTasks = new List<Task<HttpResponseMessage>>();

                foreach (var block in _blocks)
                {
                    foreach (var p in block.Participants)
                    {
                        if (p.WasPresent)
                        {
                            if (p.Type == "Participant") presentParts.Add(p.ParticipantId);
                            else if (p.Type == "Organizer") presentOrgs.Add(p.ParticipantId);
                            else if (p.Type == "Role") presentRoles.Add(p.ParticipantId);
                        }
                        else
                        {
                            if (p.Type == "Participant") absentParts.Add(p.ParticipantId);
                            else if (p.Type == "Organizer") absentOrgs.Add(p.ParticipantId);
                            else if (p.Type == "Role") absentRoles.Add(p.ParticipantId);
                        }

                        string ptsUrl = "";
                        if (p.Type == "Participant") ptsUrl = $"/api/events/points/participant/{p.ParticipantId}?points={p.Points}";
                        else if (p.Type == "Organizer") ptsUrl = $"/api/events/points/organizer/{p.ParticipantId}?points={p.Points}";
                        else if (p.Type == "Role") ptsUrl = $"/api/events/points/participation_record/{p.ParticipantId}?points={p.Points}";

                        var emptyContent = new StringContent("", Encoding.UTF8, "application/json");
                        pointTasks.Add(_httpClient.PutAsync(ptsUrl, emptyContent));
                    }
                }

                if (presentParts.Any() || presentOrgs.Any() || presentRoles.Any())
                {
                    await SendMarkAttendance(true, presentParts, presentOrgs, presentRoles);
                }

                if (absentParts.Any() || absentOrgs.Any() || absentRoles.Any())
                {
                    await SendMarkAttendance(false, absentParts, absentOrgs, absentRoles);
                }

                await Task.WhenAll(pointTasks);

                var completeRes = await _httpClient.PutAsync($"/api/events/{_eventId}/complete", new StringContent(""));
                if (!completeRes.IsSuccessStatusCode)
                {
                    string err = await completeRes.Content.ReadAsStringAsync();
                    CustomMessageBox.Show($"Ошибка завершения мероприятия: {err}", "Внимание", CustomMessageBox.MessageType.Warning);
                    return;
                }

                CustomMessageBox.Show("Мероприятие успешно завершено!", "Успех", CustomMessageBox.MessageType.Success);
                this.NavigationService.GoBack();
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Произошла ошибка при сохранении: {ex.Message}", "Сбой", CustomMessageBox.MessageType.Error);
            }
            finally
            {
                btnFinalize.IsEnabled = true;
                LoadingOverlay.Visibility = Visibility.Collapsed;
            }
        }

        private async Task SendMarkAttendance(bool isPresent, List<int> parts, List<int> orgs, List<int> roles)
        {
            var payload = new
            {
                eventId = _eventId,
                participantIds = parts,
                organizerIds = orgs,
                participationRecordIds = roles,
                present = isPresent
            };

            var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
            await _httpClient.PostAsync("/api/events/participation/mark", content);
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

        #region Экспорт отчетов (Word / Excel)

        private async Task<EventReportDto> FetchReportAsync()
        {
            try
            {
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", App.AuthToken);
                var response = await _httpClient.GetAsync($"/api/reports/events/{_eventId}");
                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    return JsonSerializer.Deserialize<EventReportDto>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                }
            }
            catch (Exception ex)
            {
                CustomMessageBox.Show($"Ошибка загрузки отчета: {ex.Message}", "Ошибка", CustomMessageBox.MessageType.Error);
            }
            return null;
        }

        private async void ExportWord_Click(object sender, RoutedEventArgs e)
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            var report = await FetchReportAsync();
            LoadingOverlay.Visibility = Visibility.Collapsed;

            if (report == null) return;

            SaveFileDialog sfd = new SaveFileDialog
            {
                Filter = "Документ Word (*.docx)|*.docx",
                FileName = $"Отчет_{report.title}.docx"
            };

            if (sfd.ShowDialog() == true)
            {
                LoadingOverlay.Visibility = Visibility.Visible;
                await Task.Run(() => GenerateWordReport(report, sfd.FileName));
                LoadingOverlay.Visibility = Visibility.Collapsed;
                CustomMessageBox.Show("Отчет Word успешно сохранен!", "Успех", CustomMessageBox.MessageType.Success);
            }
        }

        private void GenerateWordReport(EventReportDto report, string filePath)
        {
            using (var doc = DocX.Create(filePath))
            {
                // Заголовок
                var title = doc.InsertParagraph($"Отчет по мероприятию: {report.title ?? "Без названия"}");
                title.Alignment = Alignment.center;
                title.Bold();
                title.FontSize(16);
                doc.InsertParagraph();

                // Информация о мероприятии
                var info = doc.InsertParagraph();
                info.AppendLine($"Дата проведения: {report.dateOfEvent ?? "-"}");
                info.AppendLine($"Статус: {(report.isCompleted ? "Завершено" : "В процессе")}");
                info.AppendLine($"Всего человек: {report.totalPeopleCount}");
                info.AppendLine($"- Организаторов: {report.totalOrganizersCount}");
                info.AppendLine($"- Исполнителей (Роли): {report.totalPerformersCount}");
                info.AppendLine($"- Участников: {report.totalParticipantsCount}");
                doc.InsertParagraph();

                void AddTableToWord(string tableTitle, List<ReportUserDto> users)
                {
                    if (users == null || users.Count == 0) return;

                    var blockTitle = doc.InsertParagraph(tableTitle);
                    blockTitle.Bold();
                    blockTitle.FontSize(14);
                    doc.InsertParagraph();

                    // 6 колонок: ФИО, Группа, Курс, Возраст, Присутствие, Баллы
                    var table = doc.AddTable(users.Count + 1, 6);
                    table.Design = TableDesign.TableGrid;

                    // Заголовки таблицы
                    string[] headers = { "ФИО", "Группа", "Курс", "Возраст", "Присутствие", "Баллы" };
                    for (int col = 0; col < headers.Length; col++)
                    {
                        table.Rows[0].Cells[col].Paragraphs[0].Append(headers[col]).Bold();
                    }

                    for (int i = 0; i < users.Count; i++)
                    {
                        var u = users[i];
                        if (u == null) continue;

                        table.Rows[i + 1].Cells[0].Paragraphs[0].Append(u.fio ?? "Не указано");
                        table.Rows[i + 1].Cells[1].Paragraphs[0].Append(u.groupName ?? "-");
                        table.Rows[i + 1].Cells[2].Paragraphs[0].Append(u.courseNumber?.ToString() ?? "-");
                        table.Rows[i + 1].Cells[3].Paragraphs[0].Append(u.age?.ToString() ?? "-");
                        table.Rows[i + 1].Cells[4].Paragraphs[0].Append(u.wasPresent ? "Да" : "Нет");
                        table.Rows[i + 1].Cells[5].Paragraphs[0].Append(u.pointsReceived.ToString());
                    }

                    doc.InsertTable(table);
                    doc.InsertParagraph();
                }

                AddTableToWord("Организаторы:", report.organizers);
                AddTableToWord("Исполнители (Роли):", report.performers);
                AddTableToWord("Участники:", report.participants);

                doc.Save();
            }
        }

        private async void ExportExcel_Click(object sender, RoutedEventArgs e)
        {
            LoadingOverlay.Visibility = Visibility.Visible;
            var report = await FetchReportAsync();
            LoadingOverlay.Visibility = Visibility.Collapsed;

            if (report == null) return;

            SaveFileDialog sfd = new SaveFileDialog
            {
                Filter = "Таблица Excel (*.xlsx)|*.xlsx",
                FileName = $"Отчет_{report.title}.xlsx"
            };

            if (sfd.ShowDialog() == true)
            {
                LoadingOverlay.Visibility = Visibility.Visible;
                await Task.Run(() => GenerateExcelReport(report, sfd.FileName));
                LoadingOverlay.Visibility = Visibility.Collapsed;
                CustomMessageBox.Show("Отчет Excel успешно сохранен!", "Успех", CustomMessageBox.MessageType.Success);
            }
        }

        private void GenerateExcelReport(EventReportDto report, string filePath)
        {
            using (var workbook = new XLWorkbook())
            {
                var ws = workbook.Worksheets.Add("Отчет");

                ws.Cell(1, 1).Value = "Отчет по мероприятию:";
                ws.Cell(1, 2).Value = report.title ?? "Без названия";
                ws.Cell(2, 1).Value = "Дата проведения:";
                ws.Cell(2, 2).Value = report.dateOfEvent ?? "-";
                ws.Cell(3, 1).Value = "Всего человек:";
                ws.Cell(3, 2).Value = report.totalPeopleCount;

                ws.Range(1, 1, 3, 1).Style.Font.Bold = true;

                int row = 5;

                void WriteTableToExcel(string title, List<ReportUserDto> users)
                {
                    if (users == null || users.Count == 0) return;

                    ws.Cell(row, 1).Value = title;
                    ws.Cell(row, 1).Style.Font.Bold = true;
                    ws.Cell(row, 1).Style.Font.FontSize = 14;
                    row++;

                    // Заголовки
                    string[] headers = { "ФИО", "Группа", "Курс", "Возраст", "Присутствие", "Баллы" };
                    for (int col = 0; col < headers.Length; col++)
                    {
                        ws.Cell(row, col + 1).Value = headers[col];
                        ws.Cell(row, col + 1).Style.Font.Bold = true;
                    }
                    row++;

                    foreach (var u in users)
                    {
                        if (u == null) continue;

                        ws.Cell(row, 1).Value = u.fio ?? "Не указано";
                        ws.Cell(row, 2).Value = u.groupName ?? "-";
                        ws.Cell(row, 3).Value = u.courseNumber?.ToString() ?? "-";
                        ws.Cell(row, 4).Value = u.age?.ToString() ?? "-";
                        ws.Cell(row, 5).Value = u.wasPresent ? "Да" : "Нет";
                        ws.Cell(row, 6).Value = u.pointsReceived;
                        row++;
                    }
                    row++;
                }

                WriteTableToExcel("Организаторы", report.organizers);
                WriteTableToExcel("Исполнители (Роли)", report.performers);
                WriteTableToExcel("Участники", report.participants);

                ws.Columns().AdjustToContents();
                workbook.SaveAs(filePath);
            }
        }
        #endregion
    }

    public class Fin_GlobalUserDto
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public string studentEmail { get; set; }
        public string photo { get; set; }
        public string DisplayName => $"{surname} {name} {patronymic}".Trim();
    }

    public class Fin_GlobalUserPage
    {
        public List<Fin_GlobalUserDto> content { get; set; }
    }

    public class Fin_GlobalRoleDto
    {
        public string title { get; set; }
        public int defaultPoints { get; set; }
    }

    public class Fin_ParticipantUserDto
    {
        public int id { get; set; }
        public string name { get; set; }
        public string surname { get; set; }
        public string patronymic { get; set; }
        public string studentEmail { get; set; }
        public string photo { get; set; }
    }

    public class Fin_ParticipationRecordDto
    {
        public int id { get; set; }
        public string fullName { get; set; }
        public int? totalPoints { get; set; }
        public bool? wasPresent { get; set; }
        public string entityType { get; set; }
    }

    public class Fin_RoleAppDto
    {
        public int id { get; set; }
        public int? studentId { get; set; }
        public string studentName { get; set; }
        public string studentSurname { get; set; }
        public string studentPatronymic { get; set; }
        public string studentPhoto { get; set; }
        public string studentEmail { get; set; }
        public string eventRoleName { get; set; }
        public bool? isReserve { get; set; }
    }

    public class Fin_RoleGroupViewModel : INotifyPropertyChanged
    {
        public string RoleTitle { get; set; }
        private bool _isExpanded = true;
        public bool IsExpanded
        {
            get => _isExpanded;
            set { _isExpanded = value; OnPropertyChanged(nameof(IsExpanded)); }
        }

        public List<Fin_ItemViewModel> Participants { get; set; }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string name) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class Fin_ItemViewModel : INotifyPropertyChanged
    {
        public int ParticipantId { get; set; }
        public int StudentId { get; set; }
        public string FullName { get; set; }
        public string Type { get; set; }
        public string TypeDisplay { get; set; }
        public ImageSource Avatar { get; set; }

        private int _defaultPoints;
        public int DefaultPoints
        {
            get => _defaultPoints;
            set
            {
                _defaultPoints = value;
                Points = _defaultPoints.ToString();
            }
        }

        private bool _wasPresent = true;
        public bool WasPresent
        {
            get => _wasPresent;
            set
            {
                _wasPresent = value;
                if (!_wasPresent) Points = "0";
                else Points = _defaultPoints.ToString();

                OnPropertyChanged(nameof(WasPresent));
            }
        }

        private string _points = "0";
        public string Points
        {
            get => _points;
            set { _points = value; OnPropertyChanged(nameof(Points)); }
        }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string name) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }

    public class ReportUserDto
    {
        public int userId { get; set; }
        public string fio { get; set; }
        public string groupName { get; set; }
        public int? courseNumber { get; set; }
        public int? age { get; set; }
        public bool wasPresent { get; set; }
        public int pointsReceived { get; set; }
        public bool? isReserve { get; set; }
    }

    public class ReportRoleDto
    {
        public int roleId { get; set; }
        public string roleName { get; set; }
        public string responsibleSectorName { get; set; }
        public int mainCount { get; set; }
        public int reserveCount { get; set; }
    }

    public class EventReportDto
    {
        public int eventId { get; set; }
        public string title { get; set; }
        public string dateOfEvent { get; set; }
        public bool isCompleted { get; set; }
        public bool isPublic { get; set; }
        public bool isFreeEvent { get; set; }
        public int totalPeopleCount { get; set; }
        public int totalOrganizersCount { get; set; }
        public int totalParticipantsCount { get; set; }
        public int totalPerformersCount { get; set; }

        public List<ReportUserDto> participants { get; set; }
        public List<ReportUserDto> organizers { get; set; }
        public List<ReportUserDto> performers { get; set; }
        public List<ReportRoleDto> roles { get; set; }
    }
}