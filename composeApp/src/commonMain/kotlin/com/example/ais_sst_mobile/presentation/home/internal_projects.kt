package com.example.ais_sst_mobile.presentation.home.internal_projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Модель проекта (пока заглушка)
data class InternalProjectMock(
    val id: Int,
    val title: String,
    val description: String,
    val participantsCount: Int = 0
)

// Пока выводим только один проект (как ты просил)
val mockInternalProjects = listOf(
    InternalProjectMock(
        id = 1,
        title = "Школа Актива",
        description = "Курс для начинающих активистов. Научим основам проектной деятельности и работы в команде.",
        participantsCount = 24
    )
)

@Composable
fun InternalProjectCard(
    project: InternalProjectMock,
    onDetailsClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Описание
            Text(
                text = project.description,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Нижняя часть
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Количество участников (можно убрать, если не нужно)
                if (project.participantsCount > 0) {
                    Text(
                        text = "${project.participantsCount} участников",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Кнопка "Подробнее"
                OutlinedButton(
                    onClick = onDetailsClick,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Подробнее",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun InternalProjectsContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(mockInternalProjects) { project ->
            InternalProjectCard(
                project = project,
                onDetailsClick = { /* TODO: Переход на детали проекта */ }
            )
        }

        // Отступ снизу
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}