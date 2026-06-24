package com.example.ais_sst_mobile.presentation.home.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.EventRoleSelectionComponent
import com.example.ais_sst_mobile.presentation.components.CustomButton
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

// Вспомогательная функция для правильного склонения
fun getAppliedRolesPlural(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..19 -> "$count ролей"
        mod10 == 1 -> "$count роль"
        mod10 in 2..4 -> "$count роли"
        else -> "$count ролей"
    }
}

@Composable
fun EventRoleSelectionScreen(component: EventRoleSelectionComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<EventRoleSelectionScreenModel>() }
    val state by screenModel.state.collectAsState()
    val selectedRoleIds by screenModel.selectedRoleIds.collectAsState()
    val isSubmitting by screenModel.isSubmitting.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(component.eventId) {
        screenModel.loadData(component.eventId)
    }

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            when (effect) {
                is RoleSelectionEffect.ShowSuccessAndNavigateBack -> {
                    coroutineScope.launch {
                        // Показываем сообщение и ждем 1.5 секунды, чтобы пользователь успел прочитать
                        snackbarHostState.showSnackbar(
                            message = "Заявка успешно подана на ${getAppliedRolesPlural(effect.rolesCount)}!",
                            duration = SnackbarDuration.Short
                        )
                        delay(300)
                        component.onGoBack()
                    }
                }
                is RoleSelectionEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when (val currentState = state) {
                is RoleSelectionState.Loading -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
                is RoleSelectionState.Error -> {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = currentState.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                }
                is RoleSelectionState.Success -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(18.dp))

                        // Заголовок (Digital Pixel)
                        Text(
                            text = currentState.eventTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Подзаголовок (Montserrat)
                        Text(
                            text = "Выберите, как Вы хотите помочь сделать\nэто событие незабываемым!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(currentState.items) { item ->
                                val isSelected = selectedRoleIds.contains(item.id)

                                // Стилизация: Прозрачная или Фиолетовая карточка
                                val cardBgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                                val cardBorderColor = if (isSelected) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { screenModel.toggleRole(item.id) },
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                    border = BorderStroke(1.dp, cardBorderColor)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        // Заголовок и Дедлайн
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            if (item.deadlineText != null) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Outlined.Schedule,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = item.deadlineText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Сплошное описание роли (Montserrat)
                                        Text(
                                            text = item.description,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                            lineHeight = 20.sp
                                        )

                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                            contentAlignment = Alignment.BottomEnd
                                        ) {
                                            // Квадратный чекбокс из дизайна
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(
                                                        color = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color.Transparent,
                                                        shape = MaterialTheme.shapes.small
                                                    )
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                        shape = MaterialTheme.shapes.small
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Кнопка внизу экрана (прижата к низу)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Легкий градиент/фон, чтобы кнопка не сливалась со списком
                .background(Color.Transparent)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            CustomButton(
                text = "Отправить заявку",
                isLoading = isSubmitting,
                enabled = selectedRoleIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                onClick = { screenModel.submitApplications(component.eventId) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp) // Выше кнопки
        ) { data -> CustomSnackbar(data) }
    }
}