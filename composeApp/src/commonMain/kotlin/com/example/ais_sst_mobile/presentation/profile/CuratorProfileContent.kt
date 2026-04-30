package com.example.ais_sst_mobile.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ais_sst_mobile.navigation.ProfileComponent

@Composable
fun CuratorProfileContent(component: ProfileComponent, screenModel: ProfileScreenModel) {
    val state by screenModel.state.collectAsState()
    val realRole = screenModel.realRole
    val activeRole by screenModel.activeRole.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        when (val currentState = state) {
            is ProfileState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary) }
            is ProfileState.Error -> Text(currentState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            is ProfileState.Success -> {
                val user = currentState.profile

                ProfileHeader(user, activeRole, realRole) { screenModel.setRole(it) }
                Spacer(modifier = Modifier.height(24.dp))

                ProfileStatsCard(user)
                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMenuRow(icon = Icons.Outlined.PersonOutline, title = "Мои данные", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.StarOutline, title = "История баллов", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.WorkOutline, title = "Портфолио", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.Settings, title = "Настройки", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.Info, title = "О студсовете", onClick = { })
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileMenuRow(icon = Icons.Outlined.HelpOutline, title = "Поддержка", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.Info, title = "О приложении", onClick = { })
                }

                Spacer(modifier = Modifier.height(40.dp))
                LogoutButton { screenModel.logout(); component.onLogout() }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}