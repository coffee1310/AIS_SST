package com.example.ais_sst_mobile.core.notifications

import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val message: String,
    val type: String? = "INFO"
) {
    // Вычисляемое свойство для иконки
    val isGlobal: Boolean get() = type == "PUBLIC"

    // Заголовок для UI (можно динамически менять в зависимости от типа)
    val title: String get() = if (isGlobal) "Общее уведомление" else "Уведомление"
}