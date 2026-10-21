package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.example.core.theme.AccentOption
import com.example.core.theme.MaterialYouPalette

private val DarkColorScheme = darkColorScheme(
    primary = AccentRed,
    onPrimary = White,
    primaryContainer = Ink700,
    onPrimaryContainer = White,
    secondary = Ash,
    onSecondary = Black,
    background = Black,
    onBackground = White,
    surface = Ink900,
    onSurface = White,
    surfaceVariant = Ink700,
    onSurfaceVariant = Fog,
    outline = Ink500
)

private val LightColorScheme = lightColorScheme(
    primary = AccentRed,
    onPrimary = White,
    primaryContainer = Fog,
    onPrimaryContainer = Black,
    secondary = Ash,
    onSecondary = White,
    background = Paper,
    onBackground = Ink900,
    surface = White,
    onSurface = Ink900,
    surfaceVariant = Fog,
    onSurfaceVariant = Ink700,
    outline = Fog
)

/** WCAG contrast ratio between two colours (1..21). */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/**
 * [color] adjusted to stay readable as text/icons on [background]: blended toward black on light
 * backgrounds (or white on dark ones) until it reaches [minContrast]. Red on either theme is unchanged.
 */
fun readableAgainst(color: Color, background: Color, minContrast: Float = 4.5f): Color {
    if (contrastRatio(color, background) >= minContrast) return color
    val target = if (background.luminance() > 0.5f) Color.Black else Color.White
    var t = 0.1f
    while (t < 1f) {
        val mixed = lerp(color, target, t)
        if (contrastRatio(mixed, background) >= minContrast) return mixed
        t += 0.1f
    }
    return target
}

/** A per-item accent (a setup's, a Look's) made readable on the current surface. */
@Composable
fun readableAccent(color: Color): Color = readableAgainst(color, MaterialTheme.colorScheme.surface)

/** Readable text/icon colour on an accent fill: black on light accents (white, lime, amber, cyan). */
fun contentColorOn(background: Color): Color = if (background.luminance() > 0.4f) Color.Black else Color.White

@Composable
fun DotWidgetsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentOption: AccentOption = AccentOption.RED,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accent = remember(accentOption, darkTheme) {
        if (accentOption == AccentOption.DYNAMIC) {
            Color(MaterialYouPalette.resolve(context))
        } else {
            accentOption.color
        }
    }
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    // The accent is used for text, icons and selection on the theme background, so on the light
    // theme light accents (white, lime, amber, cyan) are darkened until they read.
    val readable = readableAgainst(accent, baseScheme.background)
    val colorScheme = baseScheme.copy(
        primary = readable,
        onPrimary = contentColorOn(readable)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
