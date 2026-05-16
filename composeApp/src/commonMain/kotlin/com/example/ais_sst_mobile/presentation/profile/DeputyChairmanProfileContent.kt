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
import com.example.ais_sst_mobile.navigation.FullScreenRoute
import com.example.ais_sst_mobile.navigation.ProfileComponent

@Composable
fun DeputyChairmanProfileContent(component: ProfileComponent, screenModel: ProfileScreenModel) {
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
            is ProfileState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            is ProfileState.Success -> {
                val user = currentState.profile

                ProfileHeader(user, activeRole, realRole) { screenModel.setRole(it) }
                Spacer(modifier = Modifier.height(22.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMenuRow(icon = Icons.Outlined.PersonOutline, title = "Мои данные", onClick = {
                        component.onNavigateToFullScreen(FullScreenRoute.MyData) })
                    ProfileMenuRow(icon = Icons.Outlined.Folder, title = "Архив мероприятий", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.Leaderboard, title = "Рейтинг", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.DocumentScanner, title = "Документы", onClick = { })
                    ProfileMenuRow(icon = Icons.Outlined.Group, title = "Правление студсовета", onClick = {
                        component.onNavigateToFullScreen(FullScreenRoute.Board)
                    })
                    ProfileMenuRow(icon = Icons.Outlined.Cases, title = "Роли на мероприятия", onClick = {
                        component.onNavigateToFullScreen(FullScreenRoute.EventGlobalRoles)
                    })
                    ProfileMenuRow(icon = Icons.Outlined.NotificationAdd, title = "Создание уведомлений", onClick = { })

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