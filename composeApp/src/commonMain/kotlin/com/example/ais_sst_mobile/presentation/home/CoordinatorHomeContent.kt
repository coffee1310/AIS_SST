package com.example.ais_sst_mobile.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.clearFocusOnScroll
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap
import com.example.ais_sst_mobile.presentation.home.tasks.QuickTasksContent
import org.koin.compose.getKoin

@Composable
fun CoordinatorHomeContent(onNavigateToCreateEvent: () -> Unit,
                           onNavigateToEventDetails: (Int) -> Unit) {
    val koin = getKoin()
    val screenModel = remember { koin.get<CoordinatorHomeScreenModel>() }

    val selectedTab by screenModel.selectedTab.collectAsState()
    val searchQuery by screenModel.searchQuery.collectAsState()
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            when (effect) {
                is CoordinatorHomeEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val coordinatorTabs = listOf(
        Pair("Мероприятия", Icons.Outlined.Event),
        Pair("Быстрые задачи", Icons.Outlined.FlashOn)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTap(focusManager)
            .clearFocusOnScroll(focusManager)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(5.dp))

            CustomTabRow(
                tabs = coordinatorTabs,
                selectedTab = selectedTab,
                onTabSelected = { screenModel.selectTab(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomTextField(
                        value = searchQuery,
                        onValueChange = { screenModel.updateSearchQuery(it) },
                        placeholder = "Название мероприятия",
                        modifier = Modifier.weight(1f).height(52.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), MaterialTheme.shapes.medium)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { /* TODO: Фильтры */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, "Фильтр", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val state by screenModel.state.collectAsState()

                when (val currentState = state) {
                    is CoordinatorEventsState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    is CoordinatorEventsState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = currentState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    is CoordinatorEventsState.Success -> {
                        val filteredEvents = currentState.events.filter { it.title.contains(searchQuery, ignoreCase = true) }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (filteredEvents.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxWidth().fillParentMaxHeight(0.6f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "У вас пока нет ни одного созданного мероприятия.\nНажмите '+' чтобы создать!",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(filteredEvents) { event ->
                                    EventCard(event = event, isHorizontal = false, modifier = Modifier.fillMaxWidth()) {
                                        onNavigateToEventDetails(event.id)                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(100.dp)) }
                        }
                    }
                }
            } else {
                QuickTasksContent()
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp), // Отступ, чтобы быть выше кнопки "+"
            snackbar = { snackbarData ->
                CustomSnackbar(snackbarData = snackbarData)
            }
        )

        Box(
            modifier = Modifier
                .padding(end = 16.dp, bottom = 16.dp)
                .size(62.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
                .clickable {
                    when (selectedTab) {
                        0 -> { onNavigateToCreateEvent() }
                        1 -> { /* TODO: Переход на Создание задачи */ }
                    }
                }
                .align(Alignment.BottomEnd),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (selectedTab == 0) "Создать мероприятие" else "Создать задачу",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}