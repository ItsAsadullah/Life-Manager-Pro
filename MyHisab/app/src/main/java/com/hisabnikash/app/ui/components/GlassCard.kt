package com.hisabnikash.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium iOS Liquid Glass Effect (Light & Dark Mode Compatible)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color? = null,
    glassAlpha: Float = 0.15f,
    borderAlphaHigh: Float = 0.5f,
    borderAlphaLow: Float = 0.1f,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Gradient Border with theme adaptation
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlphaHigh),
                Color.White.copy(alpha = borderAlphaLow),
                Color.White.copy(alpha = borderAlphaLow),
                Color.White.copy(alpha = borderAlphaHigh)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.09f),
                Color.Black.copy(alpha = 0.04f),
                Color.Black.copy(alpha = 0.04f),
                Color.Black.copy(alpha = 0.08f)
            )
        )
    }

    // Glass Background Fill
    val backgroundBrush = if (backgroundColor != null) {
        Brush.verticalGradient(
            colors = listOf(
                backgroundColor.copy(alpha = (backgroundColor.alpha * 1.05f).coerceIn(0f, 1f)),
                backgroundColor.copy(alpha = (backgroundColor.alpha * 0.92f).coerceIn(0f, 1f))
            )
        )
    } else {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = glassAlpha),
                    Color.White.copy(alpha = glassAlpha * 0.5f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    Color.White.copy(alpha = 0.80f)
                )
            )
        }
    }

    val shadowElevation = if (!isDark && elevation == 0.dp) 2.dp else elevation

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                clip = false,
                ambientColor = if (isDark) Color.Black else Color.Black.copy(alpha = 0.08f),
                spotColor = if (isDark) Color.Black else Color.Black.copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(brush = backgroundBrush)
            .border(
                width = if (isDark) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            ),
        content = content
    )
}
