package com.example.ais_sst_mobile.presentation.home.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ais_sst_mobile.navigation.EventRoleSelectionComponent

@Composable
fun EventRoleSelectionScreen(component: EventRoleSelectionComponent) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Здесь будет выбор роли для мероприятия ${component.eventId}", color = MaterialTheme.colorScheme.onSurface)
    }
}