package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Semantic colours. Screens use these instead of raw values, so both themes stay consistent. */
@Immutable
data class DotColors(
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val divider: Color,
    val dotPattern: Color,
    val success: Color,
    val info: Color,
    val warning: Color,
    val danger: Color
)

/** v2 dark palette: near-black page, two card tones, hairline outlines, white/grey text. */
val DarkDotColors = DotColors(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF141414),
    surfaceSecondary = Color(0xFF1C1C1C),
    surfaceElevated = Color(0xFF1C1C1C),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA3A3A3),
    textMuted = Color(0xFF8C8C8C),
    border = Color(0xFF262626),
    divider = Color(0xFF262626),
    dotPattern = Color(0x0FFFFFFF),
    success = Color(0xFF72A88C),
    info = Color(0xFF779BC2),
    warning = Color(0xFFD8AA60),
    danger = Color(0xFFE5484D)
)

/** Same structure as dark, inverted. Muted stays above 4.5:1 on [background]. */
val LightDotColors = DotColors(
    background = Color(0xFFF5F5F3),
    surface = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFECECEA),
    surfaceElevated = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF5F5F5F),
    textMuted = Color(0xFF6B6B6B),
    border = Color(0xFFDEDEDC),
    divider = Color(0xFFE7E7E5),
    dotPattern = Color(0x12000000),
    success = Color(0xFF3F7A5C),
    info = Color(0xFF466F9C),
    warning = Color(0xFF94702C),
    danger = Color(0xFFD71920)
)

val LocalDotColors = staticCompositionLocalOf { DarkDotColors }

/** `DotTheme.colors.textSecondary` etc., next to `MaterialTheme` in screens. */
object DotTheme {
    val colors: DotColors
        @Composable @ReadOnlyComposable get() = LocalDotColors.current
}

object DotSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object DotShape {
    /** Chips, badges, small technical tags. */
    val small = RoundedCornerShape(8.dp)
    /** Buttons and inputs. */
    val button = RoundedCornerShape(16.dp)
    /** Cards and sheets. */
    val card = RoundedCornerShape(20.dp)
    /** Hero banners and the floating nav bar shell. */
    val hero = RoundedCornerShape(24.dp)
    /** Full pill: filter chips, segment tabs, nav bar. */
    val pill = CircleShape

    // Kept so existing screens inherit the v2 scale without a sweeping rename.
    val medium = button
    val large = card
    val xLarge = hero
}

object DotIconSize {
    val small = 16.dp
    val medium = 20.dp
    val large = 24.dp
}
