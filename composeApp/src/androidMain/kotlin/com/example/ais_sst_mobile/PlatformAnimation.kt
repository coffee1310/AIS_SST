package com.example.ais_sst_mobile

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.essenty.backhandler.BackHandler
import com.arkivanov.decompose.extensions.compose.stack.animation.fade

@Composable
actual fun <C : Any, T : Any> platformBackAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit
): StackAnimation<C, T> = stackAnimation()

@Composable
actual fun <C : Any, T : Any> platformTransitionAnimation(): StackAnimation<C, T>? = null