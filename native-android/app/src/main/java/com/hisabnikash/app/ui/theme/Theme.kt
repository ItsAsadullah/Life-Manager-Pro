package com.hisabnikash.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColors = lightColorScheme(
    primary = Color(0xFF12664F),
    secondary = Color(0xFF45645B),
    tertiary = Color(0xFF405E7A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF78DAB7),
    secondary = Color(0xFFB1CCBF),
    tertiary = Color(0xFFA8CAE9),
)

@Composable
fun HisabNikashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
