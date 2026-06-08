package com.example.ais_sst_mobile.presentation.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.CalendarComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.jetbrains.compose.resources.painterResource
import ais_sst_mobile.composeapp.generated.resources.Res
import ais_sst_mobile.composeapp.generated.resources.img_calendar
import androidx.compose.foundation.BorderStroke

// --- МОДЕЛИ ---
data class CalendarDayModel(
    val date: LocalDate,
    val isSelected: Boolean,
    val isToday: Boolean,
    val isCurrentMonth: Boolean,
    val dots: List<Color> = emptyList()
)

data class CalendarEventMock(
    val id: Int,
    val category: String,
    val title: String,
    val role: String?,
    val time: String?,
    val location: String?,
    val categoryColor: Color,
    val hasImage: Boolean = false
)

// Тестовые данные для мероприятий
val mockEvents = listOf(
    CalendarEventMock(1, "Культмассовый сектор", "Концерт ко дню рождения колледжа", "Ведущий", "10:00 - 11:30", "Актовый зал", Color(0xFF604D9E), true),
    CalendarEventMock(2, "Быстрая задача", "Сделать презентацию", null, null, null, Color(0xFFE57373))
)

val weekDays = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
val monthNames = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
val monthNamesGenitive = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")

// Функция для подсчета дней. Теперь она подгоняет размер строго под количество нужных недель (без пустых нижних строк)
fun getMonthDates(baseDate: LocalDate, offsetMonths: Int, selectedDate: LocalDate, today: LocalDate): List<CalendarDayModel> {
    val totalMonths = baseDate.year * 12 + baseDate.monthNumber - 1 + offsetMonths
    val targetYear = totalMonths / 12
    val targetMonth = (totalMonths % 12) + 1
    val targetMonthFirstDate = LocalDate(targetYear, targetMonth, 1)

    val firstDayOfWeek = targetMonthFirstDate.dayOfWeek.isoDayNumber

    val nextMonthDate = if (targetMonth == 12) LocalDate(targetYear + 1, 1, 1) else LocalDate(targetYear, targetMonth + 1, 1)
    val daysInMonth = (nextMonthDate.toEpochDays() - targetMonthFirstDate.toEpochDays()).toInt()

    val prevM = if (targetMonth == 1) LocalDate(targetYear - 1, 12, 1) else LocalDate(targetYear, targetMonth - 1, 1)
    val daysInPrevMonth = (targetMonthFirstDate.toEpochDays() - prevM.toEpochDays()).toInt()

    val list = mutableListOf<CalendarDayModel>()

    // Предыдущий месяц (полупрозрачные)
    val prevMonthDaysCount = firstDayOfWeek - 1
    for (i in prevMonthDaysCount downTo 1) {
        val date = LocalDate(prevM.year, prevM.monthNumber, daysInPrevMonth - i + 1)
        list.add(CalendarDayModel(date, isSelected = false, isToday = date == today, isCurrentMonth = false))
    }

    // Текущий месяц
    for (i in 1..daysInMonth) {
        val date = LocalDate(targetYear, targetMonth, i)
        val dots = if (i % 5 == 0) listOf(Color(0xFF008A8F)) else emptyList() // Заглушка для точек
        list.add(CalendarDayModel(date, isSelected = date == selectedDate, isToday = date == today, isCurrentMonth = true, dots = dots))
    }

    // Следующий месяц (полупрозрачные), добиваем сетку только до конца последней недели
    val remaining = if (list.size % 7 == 0) 0 else 7 - (list.size % 7)
    for (i in 1..remaining) {
        val date = LocalDate(nextMonthDate.year, nextMonthDate.monthNumber, i)
        list.add(CalendarDayModel(date, isSelected = false, isToday = date == today, isCurrentMonth = false))
    }

    return list
}

@Composable
fun CalendarScreen(component: CalendarComponent) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var selectedDate by remember { mutableStateOf(today) }

    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1000 })
    val coroutineScope = rememberCoroutineScope()

    val currentOffset = pagerState.currentPage - initialPage
    val baseMonth = LocalDate(today.year, today.monthNumber, 1)

    val totalMonths = baseMonth.year * 12 + baseMonth.monthNumber - 1 + currentOffset
    val displayedYear = totalMonths / 12
    val displayedMonthIndex = (totalMonths % 12) + 1

    val monthTitle = "${monthNames[displayedMonthIndex - 1]} $displayedYear"
    val selectedDateTitle = if (selectedDate == today) {
        "Сегодня, ${selectedDate.dayOfMonth} ${monthNamesGenitive[selectedDate.monthNumber - 1]}"
    } else {
        "${selectedDate.dayOfMonth} ${monthNamesGenitive[selectedDate.monthNumber - 1]}"
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp) // Добавили отступ сверху, т.к. бара больше нет
            ) {
                item {
                    // Календарь
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
                        // Ярко выраженная обводка
                        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp)) {
                            // Шапка календаря
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Сегодня",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch { pagerState.animateScrollToPage(initialPage) }
                                        selectedDate = today
                                    }
                                )

                                Text(
                                    text = monthTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                            .clickable { },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    Icon(Icons.Outlined.SwapVert, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp).clickable { })
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Дни недели
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                weekDays.forEach { day ->
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Пейджер (Свайпы) со столбцами и строками, который подстраивает свою высоту
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                val pageOffset = page - initialPage
                                val calendarDays = remember(pageOffset, selectedDate) {
                                    getMonthDates(baseMonth, pageOffset, selectedDate, today)
                                }

                                // Используем Column и Row вместо LazyVerticalGrid, чтобы высота подстраивалась автоматически!
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val weeks = calendarDays.chunked(7)
                                    weeks.forEach { week ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                            week.forEach { dayData ->
                                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                                    DayCell(dayData, onDayClick = { selectedDate = it })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = selectedDateTitle,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }

                items(mockEvents) { event ->
                    CalendarEventCard(event)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

}

@Composable
fun DayCell(dayData: CalendarDayModel, onDayClick: (LocalDate) -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val isWeekend = dayData.date.dayOfWeek == DayOfWeek.SATURDAY || dayData.date.dayOfWeek == DayOfWeek.SUNDAY

            val alpha = when {
                !dayData.isCurrentMonth -> 0.2f // Соседние месяцы почти прозрачные
                isWeekend -> 0.5f               // Выходные серые/полупрозрачные
                else -> 1f
            }

            val backgroundColor = when {
                dayData.isSelected -> MaterialTheme.colorScheme.secondary
                dayData.isToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                else -> Color.Transparent
            }

            val textColor = if (dayData.isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable { onDayClick(dayData.date) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayData.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                dayData.dots.forEach { dotColor ->
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(dotColor.copy(alpha = alpha)))
                }
            }
        }
    }
}

@Composable
fun CalendarEventCard(event: CalendarEventMock) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(event.categoryColor))

            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(event.category, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = event.categoryColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(event.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)

                        if (event.role != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(event.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                    }

                    if (event.hasImage) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // ИСПОЛЬЗУЕМ ВАШУ КАРТИНКУ-ЗАГЛУШКУ
                            Image(
                                painter = painterResource(Res.drawable.img_calendar),
                                contentDescription = "Превью",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (event.time != null || event.location != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (event.time != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(event.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            if (event.location != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(event.location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { },
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp).width(120.dp)
                        ) {
                            Text("Подробнее", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}