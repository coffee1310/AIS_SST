package com.example.ais_sst_mobile.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ais_sst_mobile.core.notifications.AppNotification
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PushNotificationOverlay(
    notification: AppNotification?,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var activeNotification by remember { mutableStateOf<AppNotification?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Слушаем изменение входящего уведомления
    LaunchedEffect(notification) {
        if (notification != null) {
            activeNotification = notification
            isVisible = true

            // Держим уведомление на экране 6 секунд (стандартное время чтения)
            delay(6000)
            isVisible = false

            // Даем время на завершение анимации ухода вверх
            delay(400)
            onDismiss()
        } else {
            isVisible = false
        }
    }

    // Вспомогательная функция для закрытия по клику/свайпу
    val dismissManually = {
        if (isVisible) {
            isVisible = false
            coroutineScope.launch {
                delay(400)
                onDismiss()
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it - 150 }, // Выезд далеко из-за верхней границы экрана
            animationSpec = tween(400)
        ) + fadeIn(tween(400)),
        exit = slideOutVertically(
            targetOffsetY = { -it - 150 },
            animationSpec = tween(400)
        ) + fadeOut(tween(400)),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .zIndex(100f) // Гарантируем, что оверлей перекрывает всё
    ) {
        activeNotification?.let { notif ->
            // Дизайн "Системного Push-уведомления"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        // Позволяем смахнуть уведомление вверх
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -5) { dismissManually() }
                        }
                    }
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null // Убираем эффект волны (ripple), чтобы клик был как в системе
                    ) { dismissManually() },
                shape = RoundedCornerShape(24.dp), // Плавные углы как в iOS
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), // Полупрозрачный фон
                shadowElevation = 12.dp, // Глубокая тень
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp)
                ) {
                    // --- ВЕРХНЯЯ СТРОКА: Иконка, Название приложения, Время ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "АИС ССТ",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "сейчас",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- ЗАГОЛОВОК И ТЕКСТ УВЕДОМЛЕНИЯ ---
                    Text(
                        text = notif.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notif.message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}