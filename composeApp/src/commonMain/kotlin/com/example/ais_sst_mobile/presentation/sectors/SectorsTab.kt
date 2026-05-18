package com.example.ais_sst_mobile.presentation.sectors

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.navigation.SectorsComponent
import com.example.ais_sst_mobile.platformTransitionAnimation
import org.koin.compose.getKoin

@Composable
fun SectorsTab(component: SectorsComponent) {
    val childStack by component.stack.subscribeAsState()

    val koin = getKoin()
    val sessionManager = remember { koin.get<SessionManager>() }
    val activeRole by sessionManager.activeRoleFlow.collectAsState()

    var previousRole by rememberSaveable { mutableStateOf(activeRole) }

    LaunchedEffect(activeRole) {
        if (previousRole != activeRole) {
            component.resetToRoot()
            previousRole = activeRole
        }
    }

    Children(
        stack = childStack,
        modifier = Modifier.fillMaxSize(),
        animation = platformTransitionAnimation()
    ) { child ->
        when (val instance = child.instance) {
            is SectorsComponent.Child.List -> SectorsScreen(
                component = instance.component
            )
            is SectorsComponent.Child.Details -> SectorDetailsScreen(
                component = instance.component
            )
            is SectorsComponent.Child.Participants -> SectorParticipantsScreen(
                component = instance.component
            )
        }
    }
}