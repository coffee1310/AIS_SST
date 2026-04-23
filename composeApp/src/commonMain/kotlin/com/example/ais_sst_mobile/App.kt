package com.example.ais_sst_mobile

import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.ais_sst_mobile.theme.AppTheme
import io.github.robinpcrd.cupertino.decompose.cupertinoPredictiveBackAnimation

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun App(root: RootComponent) {
    AppTheme {
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
            }
        }
    }
}