package com.example.ais_sst_mobile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ais_sst_mobile.theme.GradientInnerDark
import com.example.ais_sst_mobile.theme.GradientInnerLight
import com.example.ais_sst_mobile.theme.GradientOuterDark
import com.example.ais_sst_mobile.theme.GradientOuterLight

@Composable
fun AppBackground(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val innerColor = if (useDarkTheme) GradientInnerDark else GradientInnerLight
    val middleColor = if (useDarkTheme) GradientOuterDark else GradientOuterLight
    val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0.0f to innerColor,
                        0.55f to middleColor,
                        1.0f to Color.Transparent,
                        center = Offset(0f, 0f),
                        radius = 1000f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0.0f to innerColor,
                        0.35f to middleColor,
                        1.0f to Color.Transparent,
                        center = Offset.Infinite,
                        radius = 1200f
                    )
                )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}