package com.example.ais_sst_mobile.presentation.home.upcoming

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ais_sst_mobile.navigation.UpcomingEventsComponent
import com.example.ais_sst_mobile.presentation.home.ActivistHomeState
import com.example.ais_sst_mobile.presentation.home.EventCard
import com.example.ais_sst_mobile.presentation.home.HomeScreenModel
import org.koin.compose.getKoin

@Composable
fun UpcomingEventsScreen(component: UpcomingEventsComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<HomeScreenModel>() }

    val state by screenModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is ActivistHomeState.Loading -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ActivistHomeState.Error -> {
                Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                )
            }
            is ActivistHomeState.Success -> {
                val events = currentState.upcoming

                if (events.isEmpty()) {
                    Text(
                        text = "Ближайших мероприятий пока нет.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(events) { event ->
                            EventCard(
                                event = event,
                                isHorizontal = false,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { component.onNavigateToUpcomingEventDetails(event.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}