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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>
    fun onBackClicked(toIndex: Int)

    sealed class Child {
        class Login(val component: LoginComponent) : Child()
        class Register(val component: RegisterComponent) : Child()
        class ForgotPassword(val component: ForgotPasswordComponent) : Child()
        class Main(val component: MainComponent) : Child()
        class AccountRequests(val component: AccountRequestsComponent) : Child()
        class MyData(val component: MyDataComponent) : Child()
        class RequestDetails(val component: RequestDetailsComponent) : Child()
        class ActivistProfile(val component: ActivistProfileComponent) : Child()
        class CreateSector(val component: CreateSectorComponent) : Child()
        class Board(val component: BoardComponent) : Child()
        class EditSector(val component: EditSectorComponent) : Child()
        class EventRoles(val component: EventRolesComponent) : Child()
        class CreateRole(val component: CreateRoleComponent) : Child()
        class EditRole(val component: EditRoleComponent) : Child()
        class CreateEvent(val component: CreateEventComponent) : Child()
        class EditEvent(val component: EditEventComponent) : Child()
        class Rating(val component: RatingComponent) : Child()
        class Portfolio(val component: PortfolioComponent) : Child()   // ← ДОБАВЛЕНО
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    private val sessionManager: SessionManager by inject()
    private val navigation = StackNavigation<Config>()

    init {
        MainScope().launch {
            sessionManager.isAuthorizedFlow.collect { isAuthorized ->
                if (!isAuthorized) {
                    navigation.replaceAll(Config.Login)
                }
            }
        }
    }

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
                    onLoginSuccess = { navigation.replaceAll(Config.Main) },
                    onNavigateToForgotPassword = { navigation.pushNew(Config.ForgotPassword) }
                )
            )
            is Config.Register -> RootComponent.Child.Register(
                RegisterComponent(componentContext = context, onGoBack = { navigation.pop() })
            )
            is Config.ForgotPassword -> RootComponent.Child.ForgotPassword(
                ForgotPasswordComponent(
                    componentContext = context,
                    onGoBack = { navigation.pop() },
                    onPasswordResetSuccess = { navigation.pop() }
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
                            is FullScreenRoute.CreateSector -> navigation.pushNew(Config.CreateSector)
                            is FullScreenRoute.Board -> navigation.pushNew(Config.Board)
                            is FullScreenRoute.EditSector -> navigation.pushNew(Config.EditSector(route.sectorId))
                            is FullScreenRoute.EventGlobalRoles -> navigation.pushNew(Config.EventRoles)
                            is FullScreenRoute.CreateRole -> navigation.pushNew(Config.CreateRole)
                            is FullScreenRoute.EditRole -> navigation.pushNew(Config.EditRole(route.roleId))
                            is FullScreenRoute.CreateEvent -> navigation.pushNew(Config.CreateEvent)
                            is FullScreenRoute.EditEvent -> navigation.pushNew(Config.EditEvent(route.eventId))
                            is FullScreenRoute.Rating -> navigation.pushNew(Config.Rating)
                            is FullScreenRoute.Portfolio -> navigation.pushNew(Config.Portfolio)   // ← ДОБАВЛЕНО
                        }
                    }
                )
            )
            is Config.Portfolio -> RootComponent.Child.Portfolio(
                PortfolioComponent(
                    componentContext = context,
                    onGoBack = { navigation.pop() }
                )
            )
            is Config.AccountRequests -> RootComponent.Child.AccountRequests(
                AccountRequestsComponent(
                    componentContext = context,
                    onGoBack = { navigation.pop() },
                    onNavigateToRequestDetails = { id -> navigation.pushNew(Config.RequestDetails(id)) }
                )
            )
            is Config.MyData -> RootComponent.Child.MyData(
                MyDataComponent(componentContext = context, onGoBack = { navigation.pop() })
            )
            is Config.RequestDetails -> RootComponent.Child.RequestDetails(
                RequestDetailsComponent(componentContext = context, requestId = config.id, onGoBack = { navigation.pop() })
            )
            is Config.ActivistProfile -> RootComponent.Child.ActivistProfile(
                ActivistProfileComponent(componentContext = context, userId = config.userId, onGoBack = { navigation.pop() })
            )
            is Config.CreateSector -> RootComponent.Child.CreateSector(
                CreateSectorComponent(componentContext = context, onGoBack = { navigation.pop() })
            )
            is Config.Board -> RootComponent.Child.Board(
                BoardComponent(
                    componentContext = context,
                    onGoBack = { navigation.pop() },
                    onNavigateToActivistProfile = { userId -> navigation.pushNew(Config.ActivistProfile(userId)) }
                )
            )
            is Config.EditSector -> RootComponent.Child.EditSector(
                EditSectorComponent(componentContext = context, sectorId = config.sectorId, onGoBack = { navigation.pop() })
            )
            is Config.EventRoles -> RootComponent.Child.EventRoles(
                EventRolesComponent(
                    componentContext = context,
                    onGoBack = { navigation.pop() },
                    onNavigateToFullScreen = { route ->
                        when (route) {
                            is FullScreenRoute.CreateRole -> navigation.pushNew(Config.CreateRole)
                            is FullScreenRoute.EditRole -> navigation.pushNew(Config.EditRole(route.roleId))
                            else -> {}
                        }
                    }
                )
            )
            is Config.CreateRole -> RootComponent.Child.CreateRole(
                CreateRoleComponent(componentContext = context, onGoBack = { navigation.pop() })
            )
            is Config.EditRole -> RootComponent.Child.EditRole(
                EditRoleComponent(componentContext = context, roleId = config.roleId, onGoBack = { navigation.pop() })
            )
            is Config.CreateEvent -> RootComponent.Child.CreateEvent(
                CreateEventComponent(componentContext = context, onGoBack = { navigation.pop() })
            )
            is Config.EditEvent -> RootComponent.Child.EditEvent(
                EditEventComponent(componentContext = context, eventId = config.eventId, onGoBack = { navigation.pop() })
            )
            is Config.Rating -> RootComponent.Child.Rating(
                RatingComponent(componentContext = context, onGoBack = { navigation.pop() })
            )
        }

    @Serializable
    private sealed interface Config {
        @Serializable data object Login : Config
        @Serializable data object Register : Config
        @Serializable data object ForgotPassword : Config
        @Serializable data object Main : Config
        @Serializable data object AccountRequests : Config
        @Serializable data object MyData : Config
        @Serializable data class RequestDetails(val id: Int) : Config
        @Serializable data class ActivistProfile(val userId: Int) : Config
        @Serializable data object CreateSector : Config
        @Serializable data object Board : Config
        @Serializable data class EditSector(val sectorId: Int) : Config
        @Serializable data object EventRoles : Config
        @Serializable data object CreateRole : Config
        @Serializable data class EditRole(val roleId: Int) : Config
        @Serializable data object CreateEvent : Config
        @Serializable data class EditEvent(val eventId: Int) : Config
        @Serializable data object Rating : Config
        @Serializable data object Portfolio : Config          // ← ДОБАВЛЕНО
    }
}