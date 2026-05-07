package com.example.ais_sst_mobile.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import com.arkivanov.decompose.router.stack.replaceAll

sealed interface FullScreenRoute {
    data object AccountRequests : FullScreenRoute
    data object MyData : FullScreenRoute
    data class RequestDetails(val id: Int) : FullScreenRoute

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

class HomeComponent(componentContext: ComponentContext) : ComponentContext by componentContext
class TasksComponent(componentContext: ComponentContext) : ComponentContext by componentContext
class CalendarComponent(componentContext: ComponentContext) : ComponentContext by componentContext
class SectorListComponent(
    componentContext: ComponentContext,
    val onNavigateToDetails: (Int) -> Unit
) : ComponentContext by componentContext

class SectorDetailsComponent(
    componentContext: ComponentContext,
    val sectorId: Int,
    val onGoBack: () -> Unit
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
                    onNavigateToDetails = { id -> navigation.pushNew(Config.Details(id)) }
                )
            )
            is Config.Details -> Child.Details(
                SectorDetailsComponent(
                    componentContext = context,
                    sectorId = config.id,
                    onGoBack = { navigation.pop() }
                )
            )
        }

    sealed class Child {
        class List(val component: SectorListComponent) : Child()
        class Details(val component: SectorDetailsComponent) : Child()
    }

    @Serializable
    private sealed interface Config {
        @Serializable data object List : Config
        @Serializable data class Details(val id: Int) : Config
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