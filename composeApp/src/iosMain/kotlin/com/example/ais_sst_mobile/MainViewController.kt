package com.example.ais_sst_mobile

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.example.ais_sst_mobile.di.initKoin
import com.example.ais_sst_mobile.navigation.DefaultRootComponent
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val lifecycle = LifecycleRegistry()
    val root = DefaultRootComponent(DefaultComponentContext(lifecycle))

    return ComposeUIViewController(
        configure = {
            initKoin()
            onFocusBehavior = OnFocusBehavior.DoNothing
        }
    ) {
        App(root)
    }
}