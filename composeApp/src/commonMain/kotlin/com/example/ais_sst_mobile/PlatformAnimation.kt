package com.example.ais_sst_mobile

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.essenty.backhandler.BackHandler

@Composable
expect fun <C : Any, T : Any> platformBackAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit
): StackAnimation<C, T>