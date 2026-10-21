package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

/*
 * The whole app uses four sizes and two weights:
 *   24sp  screen titles            (headline*)
 *   16sp  card and section titles  (title*)
 *   14sp  body text and buttons    (body*, labelLarge)
 *   12sp  secondary text, tags     (bodySmall, labelMedium, labelSmall)
 * Space Grotesk for text; JetBrains Mono for small uppercase tags and numbers.
 * Every Material role is defined so nothing falls back to the platform font.
 */
private val Headline = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.5.sp)
private val Title = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp)
private val Body = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp)
private val Small = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.25.sp)
private val Tag = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.sp)

val Typography = Typography(
    displayLarge = Headline,
    displayMedium = Headline,
    displaySmall = Headline,
    headlineLarge = Headline,
    headlineMedium = Headline,
    headlineSmall = Headline,
    titleLarge = Title,
    titleMedium = Title,
    titleSmall = Title,
    bodyLarge = Body,
    bodyMedium = Body,
    bodySmall = Small,
    labelLarge = Body.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
    labelMedium = Tag,
    labelSmall = Tag
)
