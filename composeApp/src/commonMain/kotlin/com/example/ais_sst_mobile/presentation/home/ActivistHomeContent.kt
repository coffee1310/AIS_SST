package com.example.ais_sst_mobile.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.presentation.components.clearFocusOnScroll
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap
import com.example.ais_sst_mobile.presentation.home.tasks.QuickTasksContent

@Composable
fun ActivistHomeContent(
    screenModel: HomeScreenModel,
    onNavigateToUpcoming: () -> Unit,
    onNavigateToUpcomingEventDetails: (Int) -> Unit,
    onNavigateToAvailableEventDetails: (Int) -> Unit
) {
    val selectedTab by screenModel.selectedTab.collectAsState()

    val state by screenModel.state.collectAsState()

    val focusManager = LocalFocusManager.current
    val activistTabs = listOf(
        Pair("Мероприятия", Icons.Outlined.Event),
        Pair("Быстрые задачи", Icons.Outlined.FlashOn),
        Pair("Внутренние проекты", Icons.Outlined.Folder)
    )
    var upcomingTextSize by remember { mutableStateOf(15.sp) }
    var availableTextSize by remember { mutableStateOf(15.sp) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTap(focusManager)
            .clearFocusOnScroll(focusManager)
    ) {
        Spacer(modifier = Modifier.height(5.dp))

        CustomTabRow(
            tabs = activistTabs,
            selectedTab = selectedTab,
            onTabSelected = { screenModel.selectTab(it) }
        )
        Spacer(modifier = Modifier.height(5.dp))

        if (selectedTab == 0) {
            when (val currentState = state) {
                is ActivistHomeState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
                is ActivistHomeState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                is ActivistHomeState.Success -> {
                    val upcomingEvents = currentState.upcoming
                    val availableEvents = currentState.available

                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .clickable { onNavigateToUpcoming() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ближайшие мероприятия",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = upcomingTextSize),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    onTextLayout = { textLayoutResult ->
                                        if (textLayoutResult.hasVisualOverflow) {
                                            upcomingTextSize *= 0.95f
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Все", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }

                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(upcomingEvents) { event ->
                                    val cardWidth = if (upcomingEvents.size == 1) Modifier.fillParentMaxWidth() else Modifier.fillParentMaxWidth(0.9f)

                                    EventCard(
                                        event = event,
                                        isHorizontal = true,
                                        modifier = cardWidth,
                                        onClick = { onNavigateToUpcomingEventDetails(event.id) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Мероприятия, доступные для регистрации",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = availableTextSize),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    onTextLayout = { textLayoutResult ->
                                        if (textLayoutResult.hasVisualOverflow) {
                                            availableTextSize *= 0.95f
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { /* TODO: Поиск */ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Search, "Поиск", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (availableEvents.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxWidth()
                                        .fillParentMaxHeight(0.3f)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Пока нет доступных мероприятий :(\nВступайте в сектора, чтобы видеть больше!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(availableEvents) { event ->
                                EventCard(
                                    event = event,
                                    isHorizontal = false,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    onClick = { onNavigateToAvailableEventDetails(event.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 1) { // <-- Вкладка "Быстрые задачи"
            QuickTasksContent() // <-- ПРОСТО ВСТАВЬ ЭТОТ ВЫЗОВ СЮДА
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Контент вкладки $selectedTab", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}