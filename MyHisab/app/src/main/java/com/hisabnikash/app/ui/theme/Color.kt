package com.hisabnikash.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ---------------------------------------------------------
// Hisab Nikash - Glass Design System
// ---------------------------------------------------------

// Primary Brand
val PrimaryBlue = Color(0xFF1976F3)
val PrimaryBlueDark = Color(0xFF1264D8)
val PrimaryBlueLight = Color(0xFFE8F1FF)

// Income
val IncomeGreen = Color(0xFF42B95A)
val IncomeGreenDark = Color(0xFF299A40)
val IncomeGreenLight = Color(0xFFEAF8EE)

// Expense
val ExpenseRed = Color(0xFFEF4444)
val ExpenseRedDark = Color(0xFFD93636)
val ExpenseRedLight = Color(0xFFFFEEEE)

// Balance
val BalanceBlue = Color(0xFF2196F3)
val BalanceBlueLight = Color(0xFFEAF5FF)

// ---------------------------------------------------------
// Light Theme
// ---------------------------------------------------------

val LightBackground = Color(0xFFF5F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceSecondary = Color(0xFFF9FAFC)

val LightTextPrimary = Color(0xFF111827)
val LightTextSecondary = Color(0xFF6B7280)
val LightTextTertiary = Color(0xFF9CA3AF)

val LightBorder = Color(0xFFE5E7EB)

// ---------------------------------------------------------
// Dark Theme
// ---------------------------------------------------------

val DarkBackground = Color(0xFF0D1117)
val DarkSurface = Color(0xFF171C24)
val DarkSurfaceSecondary = Color(0xFF1D2430)

val DarkTextPrimary = Color(0xFFF5F7FA)
val DarkTextSecondary = Color(0xFFB5BDC9)
val DarkTextTertiary = Color(0xFF7D8795)

val DarkBorder = Color(0xFF2B3441)

// Dynamic Theme-Aware Text Getters
val TextWhitePrimary: Color
    @Composable
    get() {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isDark) DarkTextPrimary else LightTextPrimary
    }

val TextWhiteSecondary: Color
    @Composable
    get() {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return if (isDark) DarkTextSecondary else LightTextSecondary
    }

// ---------------------------------------------------------
// Glass
// ---------------------------------------------------------

val GlassWhite = Color.White.copy(alpha = 0.72f)
val GlassWhiteStrong = Color.White.copy(alpha = 0.86f)

val GlassDark = Color(0xFF1B2430).copy(alpha = 0.78f)
val GlassDarkStrong = Color(0xFF1B2430).copy(alpha = 0.90f)

val GlassBorderLight = Color.White.copy(alpha = 0.70f)
val GlassBorderDark = Color.White.copy(alpha = 0.12f)

// ---------------------------------------------------------
// Utility
// ---------------------------------------------------------

val DividerLight = Color(0xFFE9EDF3)
val DividerDark = Color(0xFF29313D)

val OverlayBlack = Color.Black.copy(alpha = 0.45f)
