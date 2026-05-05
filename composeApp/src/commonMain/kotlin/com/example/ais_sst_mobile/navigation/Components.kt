package com.example.ais_sst_mobile.navigation

import com.arkivanov.decompose.ComponentContext

sealed interface FullScreenRoute {
    data object AccountRequests : FullScreenRoute
    data object MyData : FullScreenRoute

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
class SectorsComponent(componentContext: ComponentContext) : ComponentContext by componentContext
class ProfileComponent(
    componentContext: ComponentContext,
    val onLogout: () -> Unit,
    val onNavigateToFullScreen: (FullScreenRoute) -> Unit
) : ComponentContext by componentContext

class AccountRequestsComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext
class MyDataComponent(
    componentContext: ComponentContext,
    val onGoBack: () -> Unit
) : ComponentContext by componentContext