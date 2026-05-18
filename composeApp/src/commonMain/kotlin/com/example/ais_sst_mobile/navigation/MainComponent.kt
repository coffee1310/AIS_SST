package com.example.ais_sst_mobile.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

class MainComponent(
    componentContext: ComponentContext,
    val onLogout: () -> Unit,
    val onNavigateToFullScreen: (FullScreenRoute) -> Unit
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Home,
        handleBackButton = true,
        childFactory = ::createChild
    )

    fun onTabSelected(index: Int) {
        val config = when (index) {
            0 -> Config.Home
            1 -> Config.Tasks
            2 -> Config.Calendar
            3 -> Config.Sectors
            4 -> Config.Profile
            else -> Config.Home
        }
        navigation.bringToFront(config)
    }

    private fun createChild(config: Config, context: ComponentContext): Child =
        when (config) {
            is Config.Home -> Child.Home(HomeComponent(context, onNavigateToFullScreen))
            is Config.Tasks -> Child.Tasks(TasksComponent(context))
            is Config.Calendar -> Child.Calendar(CalendarComponent(context))
            is Config.Sectors -> Child.Sectors(SectorsComponent(context, onNavigateToFullScreen))
            is Config.Profile -> Child.Profile(ProfileComponent(context, onLogout, onNavigateToFullScreen))
        }

    sealed class Child {
        class Home(val component: HomeComponent) : Child()
        class Tasks(val component: TasksComponent) : Child()
        class Calendar(val component: CalendarComponent) : Child()
        class Sectors(val component: SectorsComponent) : Child()
        class Profile(val component: ProfileComponent) : Child()
    }

    @Serializable
    private sealed interface Config {
        @Serializable data object Home : Config
        @Serializable data object Tasks : Config
        @Serializable data object Calendar : Config
        @Serializable data object Sectors : Config
        @Serializable data object Profile : Config
    }
}