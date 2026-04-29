package com.example.ais_sst_mobile.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val PrimaryPurple = Color(0xFF604D9E)
val AccentTeal = Color(0xFF008A8F)
val TextDark = Color(0xFF111111)

val GradientInnerLight = Color(0xFF453281)
val GradientOuterLight = Color(0xFFD1C4E9)
val BackgroundLight = Color(0xFFFFFFFF)

val GradientInnerDark = Color(0xFF433179)
val GradientOuterDark = Color(0xFF2F2546)
val BackgroundDark = Color(0xFF0E0A15)
val WhiteColor = Color.White
val BackgroundCustomTab = Color(0xFF463874)
val BackgroundCard = Color(0xFF575757)
val MenuInactiveDark = Color(0xFFCBCBCB)
val MenuInactiveLight = Color(0xFF4E4E4E)
val IconDark = Color(0xFF02B3BA)

val LightColors = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = WhiteColor,
    secondary = AccentTeal,
    onSecondary = WhiteColor,
    background = BackgroundLight,
    surface = Color.Transparent,
    onSurface = TextDark,
    outline = PrimaryPurple,
    onBackground = BackgroundCustomTab,
    onPrimaryContainer = PrimaryPurple,
    outlineVariant = MenuInactiveLight,
    surfaceTint = AccentTeal
)

val DarkColors = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = WhiteColor,
    secondary = AccentTeal,
    background = BackgroundDark,
    surface = Color.Transparent,
    onSurface = WhiteColor,
    outline = WhiteColor,
    onBackground = BackgroundCustomTab,
    onPrimaryContainer = BackgroundCard,
    outlineVariant = MenuInactiveDark,
    surfaceTint = IconDark
)