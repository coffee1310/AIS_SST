package com.example.ais_sst_mobile.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.example.ais_sst_mobile.core.prefs.SessionManager
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>
    fun onBackClicked(toIndex: Int)

    sealed class Child {
        class Login(val component: LoginComponent) : Child()
        class Register(val component: RegisterComponent) : Child()
        class Main(val component: MainComponent) : Child()
        class AccountRequests(val component: AccountRequestsComponent) : Child()
        class MyData(val component: MyDataComponent) : Child()
        class RequestDetails(val component: RequestDetailsComponent) : Child()
        class ActivistProfile(val component: ActivistProfileComponent) : Child()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext, KoinComponent {
    private val sessionManager: SessionManager by inject()

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = if (sessionManager.isLoggedIn()) Config.Main else Config.Login,
        handleBackButton = true,
        childFactory = ::createChild
    )

    override fun onBackClicked(toIndex: Int) {
        navigation.pop()
    }

    private fun createChild(config: Config, context: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.Login -> RootComponent.Child.Login(
                LoginComponent(
                    componentContext = context,
                    onNavigateToRegister = { navigation.pushNew(Config.Register) },
                    onLoginSuccess = { navigation.replaceAll(Config.Main) }
                )
            )
            is Config.Register -> RootComponent.Child.Register(
                RegisterComponent(
                    componentContext = context,
                    onGoBack = { navigation.pop() }
                )
            )
            is Config.Main -> RootComponent.Child.Main(
                MainComponent(
                    componentContext = context,
                    onLogout = { navigation.replaceAll(Config.Login) },
                    onNavigateToFullScreen = { route ->
                        when (route) {
                            is FullScreenRoute.AccountRequests -> navigation.pushNew(Config.AccountRequests)
                            is FullScreenRoute.MyData -> navigation.pushNew(Config.MyData)
                            is FullScreenRoute.RequestDetails -> navigation.pushNew(Config.RequestDetails(route.id))
                            is FullScreenRoute.ActivistProfile -> navigation.pushNew(Config.ActivistProfile(route.userId))
                        }
                    }
                )
            )
            is Config.AccountRequests -> RootComponent.Child.AccountRequests(
                AccountRequestsComponent(
                    componentContext = context,
                    onGoBack = { navigation.pop() },
                    onNavigateToRequestDetails = { id ->
                        navigation.pushNew(Config.RequestDetails(id))
                    }
                )
            )
            is Config.MyData -> RootComponent.Child.MyData(
                MyDataComponent(
                    componentContext = context,
                    onGoBack = { navigation.pop() }
                )
            )
            is Config.RequestDetails -> RootComponent.Child.RequestDetails(
                RequestDetailsComponent(
                    componentContext = context,
                    requestId = config.id,
                    onGoBack = { navigation.pop() }
                )
            )
            is Config.ActivistProfile -> RootComponent.Child.ActivistProfile(
                ActivistProfileComponent(
                    componentContext = context,
                    userId = config.userId,
                    onGoBack = { navigation.pop() }
                )
            )
        }

    @Serializable
    private sealed interface Config {
        @Serializable data object Login : Config
        @Serializable data object Register : Config
        @Serializable data object Main : Config
        @Serializable data object AccountRequests : Config
        @Serializable data object MyData : Config
        @Serializable data class RequestDetails(val id: Int) : Config
        @Serializable data class ActivistProfile(val userId: Int) : Config
    }
}