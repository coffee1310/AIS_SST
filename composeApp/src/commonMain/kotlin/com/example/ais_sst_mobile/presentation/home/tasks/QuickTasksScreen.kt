package com.example.ais_sst_mobile.presentation.home.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. Временная модель данных (потом заменишь на DTO с бэкенда)
data class QuickTaskMock(
    val id: Int,
    val title: String,
    val description: String,
    val deadline: String
)

// 2. Тестовые данные точь-в-точь как на макете
val mockTasks = listOf(
    QuickTaskMock(
        id = 1,
        title = "Помощь в организации мероприятия",
        description = "Требуется помощь в расстановке стульев и оборудования перед началом мероприятия в актовом зале.",
        deadline = "5 июня, 14:00"
    ),
    QuickTaskMock(
        id = 2,
        title = "Разработка афиши",
        description = "Нужен дизайнер для создания яркой и привлекательной афиши для предстоящего концерта. Примеры работ приветствуются.",
        deadline = "10 июня"
    ),
    QuickTaskMock(
        id = 3,
        title = "Фотосъемка Дня открытых дверей",
        description = "Ищем фотографа для съемки мероприятия. Важно запечатлеть ключевые моменты и атмосферу. Наличие своей техники обязательно.",
        deadline = "18 июня"
    )
)

// 3. Главный экран со списком
@Composable
fun QuickTasksContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(mockTasks) { task ->
            QuickTaskCard(
                task = task,
                onRespondClick = { /* TODO: Обработка отклика */ }
            )
        }

        // Отступ снизу, чтобы нижнее меню навигации не перекрывало последнюю карточку
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

// 4. Дизайн самой карточки задачи
@Composable
fun QuickTaskCard(
    task: QuickTaskMock,
    onRespondClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        // Используем тот же цвет подложки, что и у карточек мероприятий
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
        ),
        // Тонкая рамка для обводки карточки
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок (пиксельный шрифт из темы)
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Описание
            Text(
                text = task.description,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Нижний ряд: дедлайн и кнопка
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка часов + текст даты
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Дедлайн",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = task.deadline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Кнопка "Откликнуться"
                OutlinedButton(
                    onClick = onRespondClick,
                    shape = MaterialTheme.shapes.small, // Закругления 8.dp по вашей теме
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Откликнуться",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}