package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val GeistSans = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_semibold, FontWeight.Bold)
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
    Font(R.font.geist_mono_semibold, FontWeight.SemiBold),
    Font(R.font.geist_mono_semibold, FontWeight.Bold)
)

private fun sans(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = GeistSans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp
)

private fun mono(size: Int, line: Int, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = GeistMono, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp
)

/** Named styles with no direct Material role. */
object DotType {
    /** Big technical numbers: battery level, temperature, stats. */
    val NumericDisplay = mono(40, 44, FontWeight.Medium, -1.0)
    val NumericSmall = mono(20, 24, FontWeight.Medium, -0.25)
}

/*
 * The type scale. Geist for interface text; Geist Mono for small technical labels and numbers.
 *   Display Large/Medium  displayLarge, displayMedium
 *   Heading Large/Med/Small  headlineLarge, headlineMedium, headlineSmall
 *   Card titles           titleLarge, titleMedium, titleSmall
 *   Body Large/Med/Small  bodyLarge, bodyMedium, bodySmall
 *   Label Large           labelLarge (buttons)
 *   Label Small           labelMedium, labelSmall (mono technical labels)
 * Every Material role is defined so nothing falls back to the platform font.
 */
val Typography = Typography(
    displayLarge = sans(40, 48, FontWeight.SemiBold, -0.8),
    displayMedium = sans(32, 40, FontWeight.SemiBold, -0.6),
    displaySmall = sans(28, 36, FontWeight.SemiBold, -0.4),
    headlineLarge = sans(28, 36, FontWeight.SemiBold, -0.4),
    headlineMedium = sans(24, 32, FontWeight.SemiBold, -0.3),
    headlineSmall = sans(20, 28, FontWeight.SemiBold, -0.2),
    titleLarge = sans(18, 24, FontWeight.SemiBold, -0.1),
    titleMedium = sans(16, 22, FontWeight.Medium),
    titleSmall = sans(14, 20, FontWeight.Medium),
    bodyLarge = sans(16, 24, FontWeight.Normal),
    bodyMedium = sans(14, 20, FontWeight.Normal),
    bodySmall = sans(12, 16, FontWeight.Normal, 0.1),
    labelLarge = sans(14, 20, FontWeight.Medium, 0.1),
    labelMedium = mono(12, 16, FontWeight.Medium, 0.4),
    labelSmall = mono(11, 14, FontWeight.Medium, 0.6)
)
