using Diplom_Stud.Components;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace Diplom_Stud.Pages.Activist
{
    /// <summary>
    /// Логика взаимодействия для Notifications.xaml
    /// </summary>
    public partial class Notifications : Page
    {
        private ClientWebSocket _webSocket;
        private CancellationTokenSource _cts;
        private readonly string _wsUrl = "wss://ais-sst.ru/ws-endpoint/websocket";

        public Notifications()
        {
            InitializeComponent();
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

            // Запускаем подключение к вебсокету
            await ConnectStompAsync();
        }
        private async void Page_Unloaded(object sender, RoutedEventArgs e)
        {
            // Отключаемся при уходе со страницы
            if (_webSocket != null && _webSocket.State == WebSocketState.Open)
            {
                _cts?.Cancel();
                try
                {
                    await _webSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Page closed", CancellationToken.None);
                }
                catch { }
                _webSocket.Dispose();
            }
        }

        private async Task ConnectStompAsync()
        {
            _cts = new CancellationTokenSource();
            _webSocket = new ClientWebSocket();

            try
            {
                await _webSocket.ConnectAsync(new Uri(_wsUrl), _cts.Token);
                Debug.WriteLine("✅ WebSocket Подключен");

                // 1. Отправляем STOMP CONNECT
                // Важно: в App.AuthToken не должно быть слова Bearer, добавляем его сами, если его там нет
                string token = App.AuthToken.StartsWith("Bearer") ? App.AuthToken : $"Bearer {App.AuthToken}";
                string connectFrame = $"CONNECT\nAuthorization:{token}\naccept-version:1.2,1.1,1.0\n\n\0";
                await SendFrameAsync(connectFrame);

                // 2. Запускаем фоновый поток для прослушивания входящих сообщений
                _ = ReceiveLoopAsync();

                // 3. Немного ждем ответа CONNECTED от сервера и подписываемся на каналы
                await Task.Delay(1000);
                string subPersonal = "SUBSCRIBE\nid:sub-0\ndestination:/user/queue/notifications\n\n\0";
                string subGlobal = "SUBSCRIBE\nid:sub-1\ndestination:/topic/public\n\n\0";

                await SendFrameAsync(subPersonal);
                await SendFrameAsync(subGlobal);
                Debug.WriteLine("📡 STOMP Подписки отправлены");
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"❌ Ошибка подключения WebSocket: {ex.Message}");
            }
        }

        private async Task SendFrameAsync(string frame)
        {
            if (_webSocket.State == WebSocketState.Open)
            {
                var bytes = Encoding.UTF8.GetBytes(frame);
                await _webSocket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, _cts.Token);
            }
        }

        private async Task ReceiveLoopAsync()
        {
            var buffer = new byte[8192];
            var messageBuilder = new StringBuilder();

            try
            {
                while (_webSocket.State == WebSocketState.Open && !_cts.IsCancellationRequested)
                {
                    var result = await _webSocket.ReceiveAsync(new ArraySegment<byte>(buffer), _cts.Token);
                    if (result.MessageType == WebSocketMessageType.Close)
                    {
                        await _webSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "", CancellationToken.None);
                        break;
                    }

                    string text = Encoding.UTF8.GetString(buffer, 0, result.Count);
                    messageBuilder.Append(text);

                    // Если фрейм завершен, обрабатываем
                    if (result.EndOfMessage)
                    {
                        string fullMessage = messageBuilder.ToString();
                        messageBuilder.Clear();

                        // Обработка нескольких STOMP-фреймов, если они склеились
                        var frames = fullMessage.Split(new[] { '\0' }, StringSplitOptions.RemoveEmptyEntries);
                        foreach (var frame in frames)
                        {
                            ProcessStompFrame(frame);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Отключение: {ex.Message}");
            }
        }

        private void ProcessStompFrame(string frame)
        {
            // Игнорируем Heartbeat-пинги (пустые сообщения)
            if (string.IsNullOrWhiteSpace(frame) || frame == "\n") return;

            // Если пришел фрейм MESSAGE с уведомлением
            if (frame.StartsWith("MESSAGE"))
            {
                try
                {
                    // Разделяем заголовки и тело фрейма по двум переносам строки
                    int bodyIndex = frame.IndexOf("\n\n");
                    if (bodyIndex >= 0)
                    {
                        string body = frame.Substring(bodyIndex + 2).TrimEnd('\0', '\n', '\r');

                        var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                        var notification = JsonSerializer.Deserialize<NotificationData>(body, options);

                        if (notification != null && !string.IsNullOrEmpty(notification.message))
                        {
                            // Выводим MessageBox в UI-потоке
                            Application.Current.Dispatcher.Invoke(() =>
                            {
                                string title = string.IsNullOrEmpty(notification.type) ? "Уведомление" : $"Уведомление: {notification.type}";
                                CustomMessageBox.Show(notification.message, title, CustomMessageBox.MessageType.Info);
                            });
                        }
                    }
                }
                catch (Exception ex)
                {
                    Debug.WriteLine($"Ошибка разбора STOMP MESSAGE: {ex.Message}");
                }
            }
            else
            {
                // Для дебага выводим другие STOMP ответы (например, CONNECTED)
                Debug.WriteLine($"STOMP: {frame}");
            }
        }
    }

    // DTO для парсинга тела уведомления
    public class NotificationData
    {
        public string message { get; set; }
        public string type { get; set; }
    }
}

