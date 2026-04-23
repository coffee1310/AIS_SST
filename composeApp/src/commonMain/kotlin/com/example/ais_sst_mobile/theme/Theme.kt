package com.example.ais_sst_mobile.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.robinpcrd.cupertino.theme.CupertinoTheme
import io.github.robinpcrd.cupertino.adaptive.AdaptiveTheme
import io.github.robinpcrd.cupertino.adaptive.ExperimentalAdaptiveApi
import io.github.robinpcrd.cupertino.theme.darkColorScheme
import io.github.robinpcrd.cupertino.theme.lightColorScheme
import org.jetbrains.compose.resources.Font
import ais_sst_mobile.composeapp.generated.resources.Res
import ais_sst_mobile.composeapp.generated.resources.digital_pixel_regular
import ais_sst_mobile.composeapp.generated.resources.montserrat_light

@OptIn(ExperimentalAdaptiveApi::class)
@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val materialColors = if (useDarkTheme) DarkColors else LightColors
    val cupertinoColors = if (useDarkTheme) darkColorScheme() else lightColorScheme()

    val digitalFont = FontFamily(Font(Res.font.digital_pixel_regular))
    val montserratFont = FontFamily(Font(Res.font.montserrat_light))

    val typography = Typography(
        titleLarge = TextStyle(
            fontFamily = digitalFont,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            letterSpacing = 1.sp
        ),
        titleMedium = TextStyle(
            fontFamily = digitalFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = montserratFont,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp
        ),
        labelMedium = TextStyle(
            fontFamily = montserratFont,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp
        )
    )

    AdaptiveTheme(
        material = {
            MaterialTheme(
                colorScheme = materialColors,
                typography = typography,
                shapes = AppShapes,
                content = it
            )
        },
        cupertino = {
            CupertinoTheme(
                colorScheme = cupertinoColors,
                content = it
            )
        },
        content = content
    )
}