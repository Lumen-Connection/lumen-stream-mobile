package com.lumenconnection.stream.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LumenAmber = Color(0xFFFFD866)
private val LumenBlue = Color(0xFF7AC7FF)
private val LumenDarkBg = Color(0xFF0F1420)
private val LumenDarkSurface = Color(0xFF1A2130)

private val DarkColors = darkColorScheme(
    primary = LumenAmber,
    onPrimary = Color(0xFF201A00),
    secondary = LumenBlue,
    onSecondary = Color(0xFF00131F),
    background = LumenDarkBg,
    onBackground = Color(0xFFE6E9F0),
    surface = LumenDarkSurface,
    onSurface = Color(0xFFE6E9F0),
    surfaceVariant = Color(0xFF232B3D),
    onSurfaceVariant = Color(0xFFAAB2C4),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A5C00),
    secondary = Color(0xFF00639A),
    background = Color(0xFFFBF8F2),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun LumenTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
