package com.lumenconnection.stream.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Paleta portada 1:1 de `src/ui/theme.rs` do Lumen Stream Desktop.
 * Os valores são os mesmos literais do egui, para que as duas versões do app
 * fiquem visualmente idênticas.
 */
@Immutable
data class LumenColors(
    val bgApp: Color,
    val bgSidebar: Color,
    val bgCard: Color,
    val bgCardHover: Color,
    val bgInput: Color,
    val border: Color,
    val accent: Color,
    val accentSoft: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val danger: Color,
    val isLight: Boolean,
)

/** accent() do desktop: o laranja da marca é fixo, independente do tema. */
val LumenAccent = Color(0xFFFF5722)

private fun darkColors(highContrast: Boolean) = LumenColors(
    bgApp = Color(0xFF0A0E12),
    bgSidebar = Color(0xFF070A0D),
    bgCard = Color(0xFF121821),
    bgCardHover = Color(0xFF1C2530),
    bgInput = Color(0xFF161D27),
    border = if (highContrast) Color(0xFFC8D0D6) else Color(0xFF263240),
    accent = LumenAccent,
    // accent_soft() no escuro: accent.linear_multiply(0.22)
    accentSoft = LumenAccent.copy(alpha = 0.22f),
    text = if (highContrast) Color(0xFFFFFFFF) else Color(0xFFEEF3F6),
    textMuted = if (highContrast) Color(0xFFE0E6EA) else Color(0xFF93A1AD),
    textFaint = if (highContrast) Color(0xFFC0C6CC) else Color(0xFF5A6670),
    danger = Color(0xFFFF4D4D),
    isLight = false,
)

private fun lightColors(highContrast: Boolean) = LumenColors(
    bgApp = Color(0xFFF3F6F8),
    bgSidebar = Color(0xFFE7EDF1),
    bgCard = Color(0xFFFFFFFF),
    bgCardHover = Color(0xFFE4EBEF),
    bgInput = Color(0xFFFFFFFF),
    border = if (highContrast) Color(0xFF000000) else Color(0xFFD2DBE1),
    accent = LumenAccent,
    // accent_soft() no claro: blend(accent, WHITE, 0.85)
    accentSoft = Color(0xFFFFE4DB),
    text = if (highContrast) Color(0xFF000000) else Color(0xFF16202A),
    textMuted = if (highContrast) Color(0xFF202024) else Color(0xFF54616C),
    textFaint = if (highContrast) Color(0xFF404046) else Color(0xFF94A2AC),
    danger = Color(0xFFD32F2F),
    isLight = true,
)

val LocalLumenColors: ProvidableCompositionLocal<LumenColors> =
    staticCompositionLocalOf { darkColors(false) }

/** Espaçamentos do desktop (style.spacing), com o modo compacto. */
@Immutable
data class LumenDimens(
    val cardRounding: Int = 10,
    val widgetRounding: Int = 8,
    val cardMargin: Int = 18,
    val itemSpacing: Int = 10,
)

val LocalLumenDimens: ProvidableCompositionLocal<LumenDimens> =
    staticCompositionLocalOf { LumenDimens() }

object Lumen {
    val colors: LumenColors
        @Composable get() = LocalLumenColors.current
    val dimens: LumenDimens
        @Composable get() = LocalLumenDimens.current
}

private val LumenShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),   // widgets
    medium = RoundedCornerShape(10.dp), // CARD_ROUNDING
    large = RoundedCornerShape(12.dp),  // window_rounding
)

private val LumenTypography = Typography(
    // page_header do desktop: título 30 strong, subtítulo 14 muted
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 12.5.sp),
    labelSmall = TextStyle(fontSize = 11.5.sp),
)

@Composable
fun LumenTheme(
    light: Boolean = !isSystemInDarkTheme(),
    highContrast: Boolean = false,
    compact: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (light) lightColors(highContrast) else darkColors(highContrast)
    val dimens = if (compact) {
        LumenDimens(cardMargin = 12, itemSpacing = 7)
    } else {
        LumenDimens()
    }

    val scheme = if (light) {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            secondary = colors.accent,
            background = colors.bgApp,
            onBackground = colors.text,
            surface = colors.bgCard,
            onSurface = colors.text,
            surfaceVariant = colors.bgInput,
            onSurfaceVariant = colors.textMuted,
            outline = colors.border,
            error = colors.danger,
        )
    } else {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            secondary = colors.accent,
            background = colors.bgApp,
            onBackground = colors.text,
            surface = colors.bgCard,
            onSurface = colors.text,
            surfaceVariant = colors.bgInput,
            onSurfaceVariant = colors.textMuted,
            outline = colors.border,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(
        LocalLumenColors provides colors,
        LocalLumenDimens provides dimens,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = LumenShapes,
            typography = LumenTypography,
            content = content,
        )
    }
}
