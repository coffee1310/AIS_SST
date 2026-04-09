package com.example.ais_sst_mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.example.ais_sst_mobile.presentation.auth.LoginScreen
import com.example.ais_sst_mobile.theme.AppTheme


@Composable
@Preview
fun App() {
    AppTheme {
        Navigator(LoginScreen())
    }
}