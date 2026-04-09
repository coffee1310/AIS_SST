package com.example.ais_sst_mobile.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Базовые цвета
val PrimaryPurple = Color(0xFF604D9E)
val AccentTeal = Color(0xFF008A8F)
val TextDark = Color(0xFF111111)

// Градиентные цвета (Вынесли из AppBackground)
val GradientInnerLight = Color(0xFF453281)
val GradientOuterLight = Color(0xFFD1C4E9)
val BackgroundLight = Color(0xFFFFFFFF)

val GradientInnerDark = Color(0xFF433179)
val GradientOuterDark = Color(0xFF2F2546)
val BackgroundDark = Color(0xFF0E0A15)
val WhiteColor = Color.White

val LightColors = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = WhiteColor,
    secondary = AccentTeal,
    onSecondary = WhiteColor,
    background = BackgroundLight,
    surface = Color.Transparent,
    onSurface = TextDark,
    outline = PrimaryPurple
)

val DarkColors = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = WhiteColor,
    secondary = AccentTeal,
    background = BackgroundDark,
    surface = Color.Transparent,
    onSurface = Color.White,
    outline = WhiteColor
)