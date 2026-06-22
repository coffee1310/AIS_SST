package com.example.ais_sst_mobile.core.notifications

import com.example.ais_sst_mobile.core.prefs.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.coroutines.isActive

class NotificationService(
    private val sessionManager: SessionManager,
    private val client: HttpClient // Передаем общий клиент, он теперь не будет блокироваться
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    private val _notifications = MutableSharedFlow<AppNotification>()
    val notifications = _notifications.asSharedFlow()

    fun connect() {
        serviceScope.launch {
            val token = sessionManager.fetchAuthToken() ?: return@launch

            while (isActive) {
                try {
                    // ПОДКЛЮЧАЕМСЯ К ЧИСТОМУ WEBSOCKET (как в примере бэкендера)
                    client.webSocket("wss://ais-sst.ru/ws-endpoint/websocket") {
                        println("✅ Подключено к сокету")

                        // 1. STOMP CONNECT
                        send(Frame.Text("CONNECT\nAuthorization:Bearer $token\naccept-version:1.2\n\n\u0000"))

                        // 2. ПОДПИСКИ
                        launch {
                            delay(1000)
                            send(Frame.Text("SUBSCRIBE\nid:sub-0\ndestination:/user/queue/notifications\n\n\u0000"))
                            send(Frame.Text("SUBSCRIBE\nid:sub-1\ndestination:/topic/public\n\n\u0000"))
                        }

                        // 3. ЧТЕНИЕ СООБЩЕНИЙ
                        // ... внутри цикла чтения сообщений ...
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                println("📩 Получен фрейм: $text") // Визуальный контроль сырых данных

                                if (text.contains("MESSAGE")) {
                                    val body = text.substringAfter("\n\n").trimEnd('\u0000')
                                    println("📦 Тело сообщения: $body") // Визуальный контроль JSON

                                    try {
                                        val notification = json.decodeFromString<AppNotification>(body)
                                        _notifications.emit(notification)
                                        println("✅ Уведомление успешно отправлено в поток: ${notification.title}")
                                    } catch (e: Exception) {
                                        println("❌ Ошибка парсинга: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    println(" Ошибка: ${e.message}")
                }
            }
        }
    }
}