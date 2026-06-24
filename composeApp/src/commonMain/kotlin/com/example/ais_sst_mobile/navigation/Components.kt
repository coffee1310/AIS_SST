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
    data object CreateEvent : FullScreenRoute
    data class EditEvent(val eventId: Int) : FullScreenRoute
    data object Rating : FullScreenRoute
    data object Portfolio : FullScreenRoute
    data object AboutApp : FullScreenRoute          // ← ДОБАВЛЕНО
}

class LoginComponent(
    componentContext: ComponentContext,
    val onNavigateToRegister: () -> Unit,
    val onLoginSuccess: () -> Unit,
    val onNavigateToForgotPassword: () -> Unit
) : ComponentContext by componentContext

class RegisterComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class ForgotPasswordComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit,
    val onPasswordResetSuccess: () -> Unit
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
                    onNavigateToCoordinatorEventDetails = { id -> navigation.pushNew(Config.CoordinatorEventDetails(id)) },
                    onNavigateToAvailableEventDetails = { id -> navigation.pushNew(Config.AvailableEventDetails(id)) },
                    onNavigateToFullScreen = onNavigateToFullScreen
                )
            )
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
                    onGoBack = { navigation.pop() },
                    onNavigateToRoleSelection = { id -> navigation.pushNew(Config.EventRoleSelection(id)) }
                )
            )
            is Config.EventRoleSelection -> Child.EventRoleSelection(
                EventRoleSelectionComponent(
                    componentContext = context,
                    eventId = config.eventId,
                    onGoBack = { navigation.pop() }
                )
            )
            is Config.CoordinatorEventDetails -> Child.CoordinatorEventDetails(
                CoordinatorEventDetailsComponent(
                    componentContext = context,
                    eventId = config.eventId,
                    onGoBack = { navigation.pop() },
                    onNavigateToActivistProfile = { userId ->
                        onNavigateToFullScreen(FullScreenRoute.ActivistProfile(userId))
                    },
                    onNavigateToEditEvent = { eventId ->
                        onNavigateToFullScreen(FullScreenRoute.EditEvent(eventId))
                    }
                )
            )
        }

    sealed class Child {
        class Feed(val component: HomeFeedComponent) : Child()
        class UpcomingEvents(val component: UpcomingEventsComponent) : Child()
        class UpcomingEventDetails(val component: UpcomingEventDetailsComponent) : Child()
        class AvailableEventDetails(val component: AvailableEventDetailsComponent) : Child()
        class EventRoleSelection(val component: EventRoleSelectionComponent) : Child()
        class CoordinatorEventDetails(val component: CoordinatorEventDetailsComponent) : Child()
    }

    @Serializable
    private sealed interface Config {
        @Serializable data object Feed : Config
        @Serializable data object UpcomingEvents : Config
        @Serializable data class UpcomingEventDetails(val eventId: Int) : Config
        @Serializable data class AvailableEventDetails(val eventId: Int) : Config
        @Serializable data class EventRoleSelection(val eventId: Int) : Config
        @Serializable data class CoordinatorEventDetails(val eventId: Int) : Config
    }
}

class HomeFeedComponent(
    componentContext: ComponentContext,
    val onNavigateToUpcoming: () -> Unit,
    val onNavigateToUpcomingEventDetails: (Int) -> Unit,
    val onNavigateToAvailableEventDetails: (Int) -> Unit,
    val onNavigateToCoordinatorEventDetails: (Int) -> Unit,
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
    val onGoBack: () -> Unit,
    val onNavigateToRoleSelection: (Int) -> Unit
) : ComponentContext by componentContext

class EditEventComponent(
    componentContext: ComponentContext,
    val eventId: Int,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class EventRoleSelectionComponent(
    componentContext: ComponentContext,
    val eventId: Int,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class CreateEventComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

class CoordinatorEventDetailsComponent(
    componentContext: ComponentContext,
    val eventId: Int,
    val onGoBack: () -> Unit,
    val onNavigateToActivistProfile: (Int) -> Unit,
    val onNavigateToEditEvent: (Int) -> Unit
) : ComponentContext by componentContext

class RatingComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

// ==================== PORTFOLIO ====================
class PortfolioComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext

// ==================== ABOUT APP ====================
class AboutAppComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext