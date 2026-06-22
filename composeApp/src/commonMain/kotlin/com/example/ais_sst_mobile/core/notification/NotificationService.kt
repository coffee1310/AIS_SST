package com.example.ais_sst_mobile.core.notifications

import com.example.ais_sst_mobile.core.prefs.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

// Модель того, что приходит от сервера (из JSON)
@Serializable
data class NotificationDto(
    val message: String
)

// Модель для нашего UI
data class AppNotification(
    val title: String,
    val message: String,
    val isGlobal: Boolean
)

class NotificationService(
    private val sessionManager: SessionManager,
    sharedHttpClient: HttpClient
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _notifications = MutableSharedFlow<AppNotification>()
    val notifications = _notifications.asSharedFlow()

    private val wsHttpClient = HttpClient {
        install(WebSockets) {
            pingInterval = 10000
        }
    }

    private val stompClient = StompClient(KtorWebSocketClient(wsHttpClient))

    private val json = Json { ignoreUnknownKeys = true }

    fun connect() {
        serviceScope.launch {
            // ВРЕМЕННО ОТКЛЮЧЕНО ДО НАСТРОЙКИ NGINX
            // Чтобы не блокировать сеть iOS, мы не подключаемся к сокетам.
            // КАК ТОЛЬКО бэкендер пропишет `proxy_set_header Upgrade $http_upgrade;` в Nginx,
            // просто раскомментируй строку connectToSockets() ниже!

            // connectToSockets()
        }
    }

    private suspend fun connectToSockets() = coroutineScope {
        try {
            val token = sessionManager.fetchAuthToken()
            if (token.isNullOrBlank()) return@coroutineScope

            val wsUrl = "wss://ais-sst.ru/ws-endpoint/websocket"

            println("NOTIFICATIONS: Попытка подключения к $wsUrl")

            val session = stompClient.connect(
                url = wsUrl,
                customStompConnectHeaders = mapOf("Authorization" to "Bearer $token")
            )

            println("NOTIFICATIONS: Успешно подключено к STOMP!")

            launch {
                session.subscribe(StompSubscribeHeaders(destination = "/topic/public")).collect { msg ->
                    try {
                        val dto = json.decodeFromString<NotificationDto>(msg.bodyAsText)
                        _notifications.emit(
                            AppNotification(title = "Глобальное уведомление", message = dto.message, isGlobal = true)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            launch {
                session.subscribe(StompSubscribeHeaders(destination = "/user/queue/notifications")).collect { msg ->
                    try {
                        val dto = json.decodeFromString<NotificationDto>(msg.bodyAsText)
                        _notifications.emit(
                            AppNotification(title = "Личное сообщение", message = dto.message, isGlobal = false)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            println("NOTIFICATIONS: Ошибка подключения -> ${e.message}")
        }
    }
}