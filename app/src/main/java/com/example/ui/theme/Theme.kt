package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.core.theme.AccentOption

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

@Composable
fun DotWidgetsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentOption: AccentOption = AccentOption.RED,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = baseScheme.copy(
        primary = accentOption.color
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
