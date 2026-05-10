package com.example.ais_sst_mobile.presentation.sectors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.navigation.SectorListComponent

@Composable
fun SectorsListContent(
    screenModel: SectorsScreenModel,
    component: SectorListComponent,
    activeRole: AppRole
) {
    val state by screenModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state is SectorsState.Loading || state is SectorsState.Error) {
            screenModel.loadSectors()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is SectorsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            }
            is SectorsState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            is SectorsState.Success -> {
                val sortedSectors = currentState.sectors.sortedWith(
                    compareBy<SectorDto> { if (it.isCoordinator) 0 else 1 }
                        .thenBy { if (!it.isParticipant || it.requestStatus == "Вышедший") 1 else 0 }
                        .thenBy {
                            val isPending = it.requestStatus != "Вышедший" && it.hasActiveRequest && (it.requestStatus == "На рассмотрении" || it.requestStatus == null)
                            if (isPending) 0 else 1
                        }
                        .thenBy { it.title }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sortedSectors) { sector ->
                        SectorCard(
                            sector = sector,
                            activeRole = activeRole,
                            onClick = { component.onNavigateToDetails(sector.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}