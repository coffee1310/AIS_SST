package com.example.ais_sst_mobile.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

sealed interface FullScreenRoute {
    data object AccountRequests : FullScreenRoute
    data object MyData : FullScreenRoute
    data class RequestDetails(val id: Int) : FullScreenRoute
    data class ActivistProfile(val userId: Int) : FullScreenRoute
    data object CreateSector : FullScreenRoute
    data object Board : FullScreenRoute
    data class EditSector(val sectorId: Int) : FullScreenRoute
    data object EventGlobalRoles : FullScreenRoute
    data object CreateRole : FullScreenRoute
    data class EditRole(val roleId: Int) : FullScreenRoute
}

class LoginComponent(
    componentContext: ComponentContext,
    val onNavigateToRegister: () -> Unit,
    val onLoginSuccess: () -> Unit
) : ComponentContext by componentContext

class RegisterComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class TasksComponent(componentContext: ComponentContext) : ComponentContext by componentContext
class CalendarComponent(componentContext: ComponentContext) : ComponentContext by componentContext

class SectorListComponent(
    componentContext: ComponentContext,
    val onNavigateToDetails: (Int) -> Unit,
    val onNavigateToActivistProfile: (Int) -> Unit,
    val onNavigateToCreateSector: () -> Unit
) : ComponentContext by componentContext

class SectorDetailsComponent(
    componentContext: ComponentContext,
    val sectorId: Int,
    val onGoBack: () -> Unit,
    val onNavigateToParticipants: (Int, String) -> Unit,
    val onNavigateToActivistProfile: (Int) -> Unit,
    val onNavigateToFullScreen: (FullScreenRoute) -> Unit
) : ComponentContext by componentContext

class SectorParticipantsComponent(
    componentContext: ComponentContext,
    val sectorId: Int,
    val sectorTitle: String,
    val onGoBack: () -> Unit,
    val onNavigateToActivistProfile: (Int) -> Unit
) : ComponentContext by componentContext

class SectorsComponent(
    componentContext: ComponentContext,
    val onNavigateToFullScreen: (FullScreenRoute) -> Unit
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.List,
        handleBackButton = true,
        childFactory = ::createChild
    )
    fun resetToRoot() {
        navigation.replaceAll(Config.List)
    }
    private fun createChild(config: Config, context: ComponentContext): Child =
        when (config) {
            is Config.List -> Child.List(
                SectorListComponent(
                    componentContext = context,
                    onNavigateToDetails = { id -> navigation.pushNew(Config.Details(id)) },
                    onNavigateToActivistProfile = { userId ->
                        onNavigateToFullScreen(FullScreenRoute.ActivistProfile(userId))
                    },
                    onNavigateToCreateSector = {
                        onNavigateToFullScreen(FullScreenRoute.CreateSector)
                    }
                )
            )
            is Config.Details -> Child.Details(
                SectorDetailsComponent(
                    componentContext = context,
                    sectorId = config.id,
                    onGoBack = { navigation.pop() },
                    onNavigateToParticipants = { id, title ->
                        navigation.pushNew(Config.Participants(id, title))
                    },
                    onNavigateToActivistProfile = { userId ->
                        onNavigateToFullScreen(FullScreenRoute.ActivistProfile(userId))
                    },
                    onNavigateToFullScreen = onNavigateToFullScreen
                )
            )
            is Config.Participants -> Child.Participants(
                SectorParticipantsComponent(
                    componentContext = context,
                    sectorId = config.sectorId,
                    sectorTitle = config.sectorTitle,
                    onGoBack = { navigation.pop() },
                    onNavigateToActivistProfile = { userId ->
                        onNavigateToFullScreen(FullScreenRoute.ActivistProfile(userId))
                    }
                )
            )
        }

    sealed class Child {
        class List(val component: SectorListComponent) : Child()
        class Details(val component: SectorDetailsComponent) : Child()
        class Participants(val component: SectorParticipantsComponent) : Child()
    }

    @Serializable
    private sealed interface Config {
        @Serializable data object List : Config
        @Serializable data class Details(val id: Int) : Config
        @Serializable data class Participants(val sectorId: Int, val sectorTitle: String) : Config
    }
}

class ProfileComponent(
    componentContext: ComponentContext,
    val onLogout: () -> Unit,
    val onNavigateToFullScreen: (FullScreenRoute) -> Unit
) : ComponentContext by componentContext

class AccountRequestsComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit,
    val onNavigateToRequestDetails: (Int) -> Unit
) : ComponentContext by componentContext

class MyDataComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class RequestDetailsComponent(
    componentContext: ComponentContext,
    val requestId: Int,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class ActivistProfileComponent(
    componentContext: ComponentContext,
    val userId: Int,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class CreateSectorComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class BoardComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit,
    val onNavigateToActivistProfile: (Int) -> Unit
) : ComponentContext by componentContext

class EditSectorComponent(
    componentContext: ComponentContext,
    val sectorId: Int,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class EventRolesComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit,
    val onNavigateToFullScreen: (FullScreenRoute) -> Unit
) : ComponentContext by componentContext

class CreateRoleComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class EditRoleComponent(
    componentContext: ComponentContext,
    val roleId: Int,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class HomeComponent(
    componentContext: ComponentContext,
    val onNavigateToFullScreen: (FullScreenRoute) -> Unit
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Feed,
        handleBackButton = true,
        childFactory = ::createChild
    )
    fun onBackClicked() {
        navigation.pop()
    }
    private fun createChild(config: Config, context: ComponentContext): Child =
        when (config) {
            is Config.Feed -> Child.Feed(
                HomeFeedComponent(
                    componentContext = context,
                    onNavigateToUpcoming = { navigation.pushNew(Config.UpcomingEvents) },
                    onNavigateToUpcomingEventDetails = { id -> navigation.pushNew(Config.UpcomingEventDetails(id)) },
                    onNavigateToAvailableEventDetails = { id -> navigation.pushNew(Config.AvailableEventDetails(id)) },
                    onNavigateToFullScreen = onNavigateToFullScreen
                )
            )
            // ВОЗВРАЩАЕМ ВЕТКУ UpcomingEvents
            is Config.UpcomingEvents -> Child.UpcomingEvents(
                UpcomingEventsComponent(
                    componentContext = context,
                    onGoBack = { navigation.pop() },
                    onNavigateToUpcomingEventDetails = { id -> navigation.pushNew(Config.UpcomingEventDetails(id)) }
                )
            )
            is Config.UpcomingEventDetails -> Child.UpcomingEventDetails(
                UpcomingEventDetailsComponent(
                    componentContext = context,
                    eventId = config.eventId,
                    onGoBack = { navigation.pop() }
                )
            )
            is Config.AvailableEventDetails -> Child.AvailableEventDetails(
                AvailableEventDetailsComponent(
                    componentContext = context,
                    eventId = config.eventId,
                    onGoBack = { navigation.pop() }
                )
            )
        }

    sealed class Child {
        class Feed(val component: HomeFeedComponent) : Child()
        class UpcomingEvents(val component: UpcomingEventsComponent) : Child() // ВЕРНУЛИ СЮДА
        class UpcomingEventDetails(val component: UpcomingEventDetailsComponent) : Child()
        class AvailableEventDetails(val component: AvailableEventDetailsComponent) : Child()
    }

    @Serializable
    private sealed interface Config {
        @Serializable data object Feed : Config
        @Serializable data object UpcomingEvents : Config // ВЕРНУЛИ СЮДА
        @Serializable data class UpcomingEventDetails(val eventId: Int) : Config
        @Serializable data class AvailableEventDetails(val eventId: Int) : Config
    }
}

class HomeFeedComponent(
    componentContext: ComponentContext,
    val onNavigateToUpcoming: () -> Unit,
    val onNavigateToUpcomingEventDetails: (Int) -> Unit,
    val onNavigateToAvailableEventDetails: (Int) -> Unit,
    val onNavigateToFullScreen: (FullScreenRoute) -> Unit
) : ComponentContext by componentContext

class UpcomingEventsComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit,
    val onNavigateToUpcomingEventDetails: (Int) -> Unit
) : ComponentContext by componentContext

class UpcomingEventDetailsComponent(
    componentContext: ComponentContext,
    val eventId: Int,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class AvailableEventDetailsComponent(
    componentContext: ComponentContext,
    val eventId: Int,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext