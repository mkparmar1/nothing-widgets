package com.example.ui.theme

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

val DarkDotColors = DotColors(
    background = Color(0xFF101010),
    surface = Color(0xFF181818),
    surfaceSecondary = Color(0xFF222222),
    surfaceElevated = Color(0xFF292929),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFA5A5A5),
    textMuted = Color(0xFF8C8C8C),
    border = Color(0xFF333333),
    divider = Color(0xFF292929),
    dotPattern = Color(0x0FFFFFFF),
    success = Color(0xFF72A88C),
    info = Color(0xFF779BC2),
    warning = Color(0xFFD8AA60),
    danger = Color(0xFFE5484D)
)

val LightDotColors = DotColors(
    background = Color(0xFFF5F5F3),
    surface = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFECECEA),
    surfaceElevated = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF5F5F5F),
    textMuted = Color(0xFF747474),
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
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val xLarge = RoundedCornerShape(24.dp)
}

object DotIconSize {
    val small = 16.dp
    val medium = 20.dp
    val large = 24.dp
}
