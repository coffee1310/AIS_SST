package com.example.ais_sst_mobile.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import ais_sst_mobile.composeapp.generated.resources.Res
import ais_sst_mobile.composeapp.generated.resources.digital_pixel_regular
import ais_sst_mobile.composeapp.generated.resources.montserrat_light

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    val digitalFont = FontFamily(Font(Res.font.digital_pixel_regular))
    val montserratFont = FontFamily(Font(Res.font.montserrat_light))

    val typography = Typography(
        // Для кнопок и крупных заголовков (Digital Pixel)
        titleLarge = TextStyle(
            fontFamily = digitalFont,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            letterSpacing = 1.sp
        ),
        // Для обычного текста и полей ввода (Montserrat)
        bodyLarge = TextStyle(
            fontFamily = montserratFont,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp
        ),
        // Для мелкого текста ("Нет аккаунта?") (Montserrat)
        labelMedium = TextStyle(
            fontFamily = montserratFont,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp
        )
    )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = AppShapes,
        content = content
    )
}