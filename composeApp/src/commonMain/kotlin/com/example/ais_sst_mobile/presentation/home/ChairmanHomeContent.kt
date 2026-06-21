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
import androidx.compose.material.icons.outlined.Folder
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
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.presentation.components.CustomSnackbar
import com.example.ais_sst_mobile.presentation.components.CustomTextField
import com.example.ais_sst_mobile.presentation.components.clearFocusOnScroll
import com.example.ais_sst_mobile.presentation.components.clearFocusOnTap
import org.koin.compose.getKoin

@Composable
fun ChairmanHomeContent(
    activeRole: AppRole?, // <-- Принимаем роль
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToEventDetails: (Int) -> Unit
) {
    val koin = getKoin()
    val screenModel = remember { koin.get<ChairmanHomeScreenModel>() }

    val selectedTab by screenModel.selectedTab.collectAsState()
    val searchQuery by screenModel.searchQuery.collectAsState()
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            when (effect) {
                is ChairmanHomeEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val chairmanTabs = listOf(
        Pair("Мероприятия", Icons.Outlined.Event),
        Pair("Быстрые задачи", Icons.Outlined.FlashOn),
        Pair("Внутренние проекты", Icons.Outlined.Folder)
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
                tabs = chairmanTabs,
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
                    is ChairmanEventsState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    is ChairmanEventsState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = currentState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    is ChairmanEventsState.Success -> {
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
                                            text = "В системе пока нет мероприятий.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(filteredEvents) { event ->
                                    EventCard(event = event, isHorizontal = false, modifier = Modifier.fillMaxWidth()) {
                                        onNavigateToEventDetails(event.id)
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(100.dp)) }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Контент вкладки 'Быстрые задачи'", color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Контент вкладки 'Внутренние проекты'", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp),
            snackbar = { snackbarData ->
                CustomSnackbar(snackbarData = snackbarData)
            }
        )

        // --- ЛОГИКА ОТОБРАЖЕНИЯ ФАБА ---
        // Секретарь не может создавать мероприятия (вкладка 0), но может задачи и проекты
        val showFab = when (selectedTab) {
            0 -> activeRole != AppRole.SECRETARY
            else -> true
        }

        if (showFab) {
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
                            2 -> { /* TODO: Переход на Создание проекта */ }
                        }
                    }
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = when (selectedTab) {
                        0 -> "Создать мероприятие"
                        1 -> "Создать задачу"
                        else -> "Создать проект"
                    },
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}