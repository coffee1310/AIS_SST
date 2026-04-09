package com.example.ais_sst_mobile

import androidx.compose.ui.window.ComposeUIViewController
import com.example.ais_sst_mobile.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) { App() }