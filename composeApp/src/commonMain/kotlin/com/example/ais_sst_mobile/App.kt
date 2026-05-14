package com.example.ais_sst_mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.example.ais_sst_mobile.navigation.RootComponent
import com.example.ais_sst_mobile.presentation.auth.LoginScreen
import com.example.ais_sst_mobile.presentation.auth.RegisterScreen
import com.example.ais_sst_mobile.presentation.main.MainScreen
import com.example.ais_sst_mobile.presentation.profile.activist.ActivistProfileScreen
import com.example.ais_sst_mobile.presentation.profile.board.BoardScreen
import com.example.ais_sst_mobile.presentation.profile.my_data.MyDataScreen
import com.example.ais_sst_mobile.presentation.profile.requests.AccountRequestsScreen
import com.example.ais_sst_mobile.presentation.profile.requests.RequestDetailsScreen
import com.example.ais_sst_mobile.presentation.sectors.create.CreateSectorScreen
import com.example.ais_sst_mobile.theme.AppTheme

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun App(root: RootComponent) {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val childStack by root.stack.subscribeAsState()
            val backHandler = (root as ComponentContext).backHandler

            Children(
                stack = childStack,
                modifier = Modifier.fillMaxSize(),
                animation = platformBackAnimation(
                    backHandler = backHandler,
                    onBack = { root.onBackClicked(0) }
                )
            ) { child ->
                when (val instance = child.instance) {
                    is RootComponent.Child.Login -> LoginScreen(instance.component)
                    is RootComponent.Child.Register -> RegisterScreen(instance.component)
                    is RootComponent.Child.Main -> MainScreen(instance.component)
                    is RootComponent.Child.AccountRequests -> AccountRequestsScreen(
                        onBackClick = { instance.component.onGoBack() },
                        component = instance.component
                    )
                    is RootComponent.Child.MyData -> MyDataScreen(
                        onBackClick = { instance.component.onGoBack() }
                    )
                    is RootComponent.Child.RequestDetails -> RequestDetailsScreen(
                        requestId = instance.component.requestId,
                        onBackClick = { instance.component.onGoBack() }
                    )
                    is RootComponent.Child.ActivistProfile -> ActivistProfileScreen(
                        userId = instance.component.userId,
                        onBackClick = instance.component.onGoBack
                    )
                    is RootComponent.Child.CreateSector -> CreateSectorScreen(
                        component = instance.component
                    )
                    is RootComponent.Child.Board -> BoardScreen(
                        component = instance.component
                    )
                }
            }
        }
    }
}