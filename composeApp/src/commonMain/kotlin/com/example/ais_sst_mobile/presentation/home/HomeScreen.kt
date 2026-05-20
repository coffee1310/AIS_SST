package com.example.ais_sst_mobile.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.navigation.FullScreenRoute
import com.example.ais_sst_mobile.navigation.HomeComponent
import com.example.ais_sst_mobile.navigation.HomeFeedComponent
import com.example.ais_sst_mobile.platformTransitionAnimation
import com.example.ais_sst_mobile.presentation.home.details.EventDetailsScreen
import com.example.ais_sst_mobile.presentation.home.details.UpcomingEventDetailsScreen
import com.example.ais_sst_mobile.presentation.home.upcoming.UpcomingEventsScreen
import org.koin.compose.getKoin

@Composable
fun HomeScreen(component: HomeComponent) {
    val childStack by component.stack.subscribeAsState()

    Children(
        stack = childStack,
        animation = platformTransitionAnimation()
    ) { child ->
        when (val instance = child.instance) {
            is HomeComponent.Child.Feed -> HomeFeedScreen(instance.component)
            is HomeComponent.Child.UpcomingEvents -> UpcomingEventsScreen(instance.component)
            is HomeComponent.Child.UpcomingEventDetails -> UpcomingEventDetailsScreen(instance.component)
            is HomeComponent.Child.CoordinatorEventDetails -> EventDetailsScreen(instance.component)
            is HomeComponent.Child.AvailableEventDetails -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Детали доступного мероприятия (скоро)", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun HomeFeedScreen(component: HomeFeedComponent) {
    val koin = getKoin()
    val screenModel = koin.get<HomeScreenModel>()
    val activeRole by screenModel.activeRole.collectAsState()

    when (activeRole) {
        AppRole.ACTIVIST, AppRole.STUDENT -> ActivistHomeContent(
            screenModel = screenModel,
            onNavigateToUpcoming = component.onNavigateToUpcoming,
            onNavigateToUpcomingEventDetails = component.onNavigateToUpcomingEventDetails,
            onNavigateToAvailableEventDetails = component.onNavigateToAvailableEventDetails
        )
        AppRole.SECTOR_COORDINATOR -> CoordinatorHomeContent(
            onNavigateToCreateEvent = { component.onNavigateToFullScreen(FullScreenRoute.CreateEvent) },
            onNavigateToEventDetails = { eventId -> component.onNavigateToCoordinatorEventDetails(eventId) }
        )
        AppRole.CHAIRMAN -> ChairmanHomeContent(screenModel)
        AppRole.CURATOR -> CuratorHomeContent(screenModel)
        AppRole.DEPUTY_CHAIRMAN -> DeputyChairmanHomeContent(screenModel)
        AppRole.SECRETARY -> SecretaryHomeContent(screenModel)
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

@Composable
fun ChairmanHomeContent(screenModel: HomeScreenModel) { PlaceholderHomeContent("Председателя") }
@Composable
fun CuratorHomeContent(screenModel: HomeScreenModel) { PlaceholderHomeContent("Куратора") }
@Composable
fun DeputyChairmanHomeContent(screenModel: HomeScreenModel) { PlaceholderHomeContent("Заместителя председателя") }
@Composable
fun SecretaryHomeContent(screenModel: HomeScreenModel) { PlaceholderHomeContent("Секретаря") }
@Composable
fun PlaceholderHomeContent(roleName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Главная страница для: $roleName в разработке", color = MaterialTheme.colorScheme.onSurface)
    }
}