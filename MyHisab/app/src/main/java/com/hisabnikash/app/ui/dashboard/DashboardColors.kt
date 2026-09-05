package com.hisabnikash.app.ui.dashboard

import androidx.compose.ui.graphics.Color

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// iOS-style Dashboard Colors (theme Color.kt থেকে আলাদা রাখা হয়েছে)
val TextWhitePrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
val TextWhiteSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

// iOS System Colors
val IncomeGreen = Color(0xFF34C759)   // iOS Green
val ExpenseRed = Color(0xFFFF3B30)    // iOS Red
val BalanceBlue = Color(0xFF0A84FF)   // iOS Blue
