package com.example.ais_sst_mobile.presentation.home.upcoming

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ais_sst_mobile.navigation.UpcomingEventsComponent
import com.example.ais_sst_mobile.presentation.home.EventCard
import com.example.ais_sst_mobile.presentation.home.HomeScreenModel
import org.koin.compose.getKoin

@Composable
fun UpcomingEventsScreen(component: UpcomingEventsComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<HomeScreenModel>() }
    val events by screenModel.upcomingEvents.collectAsState()

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