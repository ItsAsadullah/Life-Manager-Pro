package com.hisabnikash.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,

    secondary = BalanceBlue,
    onSecondary = androidx.compose.ui.graphics.Color.White,

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,

    surfaceVariant = LightSurfaceSecondary,
    onSurfaceVariant = LightTextSecondary,

    outline = LightBorder,

    error = ExpenseRed,
    onError = androidx.compose.ui.graphics.Color.White
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,

    secondary = BalanceBlue,
    onSecondary = androidx.compose.ui.graphics.Color.White,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkSurfaceSecondary,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkBorder,

    error = ExpenseRed,
    onError = androidx.compose.ui.graphics.Color.White
)

val isAppInDarkTheme: Boolean
    @Composable
    get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HisabNikashTypography,
        shapes = HisabNikashShapes,
        content = content
    )
}
