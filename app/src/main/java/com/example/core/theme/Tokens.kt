package com.example.core.theme

import androidx.compose.ui.graphics.Color

// Core monochrome tokens
val ColorBlack      = Color(0xFF000000)
val ColorInk900     = Color(0xFF0A0A0A)
val ColorInk700     = Color(0xFF1C1C1C)
val ColorInk500     = Color(0xFF3D3D3D)
val ColorAsh        = Color(0xFF8A8A8A)   // Secondary text / inactive dots
val ColorFog        = Color(0xFFD4D4D4)
val ColorPaper      = Color(0xFFF2F2F2)
val ColorWhite      = Color(0xFFFFFFFF)

// Accent colours - user selectable, Red is default
val AccentRed       = Color(0xFFE02B2B)   // Default
val AccentAmber     = Color(0xFFF2A516)
val AccentLime      = Color(0xFFB8E000)
val AccentCyan      = Color(0xFF00C2D1)
val AccentPure      = Color(0xFFFFFFFF)   // Monochrome mode

enum class AccentOption(val displayName: String, val color: Color, val hex: Int) {
    RED("Red", AccentRed, 0xFFE02B2B.toInt()),
    AMBER("Amber", AccentAmber, 0xFFF2A516.toInt()),
    LIME("Lime", AccentLime, 0xFFB8E000.toInt()),
    CYAN("Cyan", AccentCyan, 0xFF00C2D1.toInt()),
    PURE("White", AccentPure, 0xFFFFFFFF.toInt()),

    /** Material You: follows the wallpaper. [color]/[hex] are the red fallback used where a static colour is required. */
    DYNAMIC("Wallpaper", AccentRed, 0xFFE02B2B.toInt());

    companion object {
        fun fromName(name: String?): AccentOption {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: RED
        }
    }
}

enum class BackgroundMode(val displayName: String) {
    SOLID("Solid"),
    TRANSPARENT("Transparent"),
    SCRIM("Scrim");

    companion object {
        fun fromName(name: String?): BackgroundMode {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: SOLID
        }
    }
}

enum class ThemeMode(val displayName: String) {
    AUTO("System"),
    FORCE_DARK("Dark"),
    FORCE_LIGHT("Light");

    companion object {
        fun fromName(name: String?): ThemeMode {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: AUTO
        }
    }
}
