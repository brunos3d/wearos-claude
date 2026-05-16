package com.brunos3d.wearosclaude.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/**
 * AMOLED-safe palette: pure-black canvas, warm Claude-Code orange as the
 * primary accent, red reserved for `overloaded` and connection failures.
 */
object RetroPalette {
    val Bg = Color(0xFF000000)
    val Orange = Color(0xFFD77757)
    val OrangeDim = Color(0xFF7A3C2B)
    val Red = Color(0xFFFF5560)
    val Cyan = Color(0xFF55E6FF)
    val Mid = Color(0xFF9AA0A6)
    val Faint = Color(0xFF3A3F45)
}

val LocalAccent = compositionLocalOf { RetroPalette.Orange }
val LocalMonoFont = compositionLocalOf<FontFamily> { FontFamily.Monospace }

@Composable
fun WearOsClaudeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = RetroPalette.Orange,
            primaryVariant = RetroPalette.OrangeDim,
            secondary = RetroPalette.Cyan,
            background = RetroPalette.Bg,
            surface = RetroPalette.Bg,
            onPrimary = RetroPalette.Bg,
            onSecondary = RetroPalette.Bg,
            onBackground = RetroPalette.Orange,
            onSurface = RetroPalette.Orange,
            error = RetroPalette.Red,
            onError = RetroPalette.Bg,
        ),
        content = content,
    )
}
