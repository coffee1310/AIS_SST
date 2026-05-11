package com.example.ais_sst_mobile.presentation.sectors

import androidx.compose.runtime.*
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.navigation.SectorListComponent
import org.koin.compose.getKoin

@Composable
fun SectorsScreen(component: SectorListComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<SectorsScreenModel>() }
    val activeRole by screenModel.activeRole.collectAsState()

    when (activeRole) {
        AppRole.SECTOR_COORDINATOR -> {
            CoordinatorSectorDashboard(screenModel, component)
        }
        else -> {
            SectorsListContent(screenModel, component, activeRole)
        }
    }
}