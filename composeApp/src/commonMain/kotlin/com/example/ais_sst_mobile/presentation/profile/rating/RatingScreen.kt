package com.example.ais_sst_mobile.presentation.profile.rating

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.RatingComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.clearFocusOnScroll
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap

// Тестовая модель данных
data class RatingItemMock(
    val rank: Int,
    val fullName: String,
    val role: String,
    val points: Int
)

val mockRatingData = listOf(
    RatingItemMock(1, "Иванова Марина Петровна", "Активист", 2500),
    RatingItemMock(2, "Козлов Сергей Владимирович", "Координатор сектора", 2485),
    RatingItemMock(3, "Крылова Полина Александровна", "Активист", 2370),
    RatingItemMock(4, "Смирнова Валентина Андреевна", "Активист", 2265),
    RatingItemMock(5, "Воронин Максим Олегович", "Активист", 2230),
    RatingItemMock(6, "Орлов Антон Романович", "Активист", 2125),
    RatingItemMock(7, "Казакова Олеся Алексеевна", "Активист", 2000),
    RatingItemMock(8, "Назаров Дмитрий Викторович", "Активист", 1985)
)

@Composable
fun RatingScreen(
    onBackClick: () -> Unit,
    component: RatingComponent
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Локальная фильтрация для тестов (потом это будет делать ScreenModel)
    val filteredList = remember(searchQuery) {
        if (searchQuery.isBlank()) mockRatingData
        else mockRatingData.filter { it.fullName.contains(searchQuery, ignoreCase = true) }
    }

    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap(focusManager)
                .clearFocusOnScroll(focusManager)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Шапка (TopBar) с отступами под статус-бар
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomBackButton(onClick = onBackClick)

                    Text(
                        text = "Рейтинг",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.width(40.dp)) // Баланс для иконки "Назад"
                }

                // Строка поиска и фильтр
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Поиск по ФИО",
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { /* TODO: Фильтры рейтинга */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Фильтр",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = "Список активистов (${filteredList.size})",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Чуть меньше отступ, чтобы выглядело как список
                ) {
                    items(filteredList) { item ->
                        RatingItemRow(item)
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }

            // Снэкбар для вывода ошибок или уведомлений
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                snackbar = { snackbarData ->
                    CustomSnackbar(snackbarData = snackbarData)
                }
            )
        }
    }
}

@Composable
fun RatingItemRow(item: RatingItemMock) {
    val isTop3 = item.rank <= 3
    val rankBackgroundColor = if (isTop3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
    val rankTextColor = if (isTop3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (isTop3) 0.3f else 0.1f))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кружок с местом
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(rankBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.rank.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = rankTextColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // ФИО и Роль
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.fullName,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.role,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Баллы
        Text(
            text = item.points.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}