package com.example.ais_sst_mobile.presentation.sectors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CoordinatorSectorDashboard(screenModel: SectorsScreenModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Дашборд Координатора в разработке", color = MaterialTheme.colorScheme.onSurface)
    }
}