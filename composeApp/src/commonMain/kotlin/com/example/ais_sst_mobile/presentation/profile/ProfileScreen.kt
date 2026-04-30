package com.example.ais_sst_mobile.presentation.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.navigation.ProfileComponent
import org.koin.compose.getKoin

@Composable
fun ProfileScreen(component: ProfileComponent) {
    val koin = getKoin()
    val screenModel = remember { koin.get<ProfileScreenModel>() }
    val activeRole by screenModel.activeRole.collectAsState()

    when (activeRole) {
        AppRole.ACTIVIST, AppRole.STUDENT -> {
            ActivistProfileContent(component, screenModel)
        }

        AppRole.SECTOR_COORDINATOR -> {
            CoordinatorProfileContent(component, screenModel)
        }

        AppRole.CHAIRMAN -> {
            ChairmanProfileContent(component, screenModel)
        }

        AppRole.DEPUTY_CHAIRMAN -> {
            DeputyChairmanProfileContent(component, screenModel)
        }

        AppRole.SECRETARY -> {
            SecretaryProfileContent(component, screenModel)
        }

        AppRole.CURATOR -> {
            CuratorProfileContent(component, screenModel)
        }

        AppRole.ADMINISTRATOR -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Профиль для роли: ${activeRole.uiName} в разработке", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Роль: ${activeRole.uiName} в разработке или произошла ошибка", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}