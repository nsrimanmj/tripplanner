package com.example.tripchecker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(
    size: Int,
    line: Int,
    weight: FontWeight,
    tracking: Float,
    family: FontFamily = FontFamily.Default,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

/** Material 3 type scale overridden with the design's sizes and weights (system font). */
val Typography = Typography(
    displaySmall = style(44, 50, FontWeight.Bold, -1f),
    headlineLarge = style(34, 42, FontWeight.SemiBold, -0.5f),
    headlineMedium = style(32, 40, FontWeight.Bold, -0.5f),
    headlineSmall = style(28, 36, FontWeight.SemiBold, 0f),
    titleLarge = style(22, 28, FontWeight.SemiBold, 0f),
    titleMedium = style(16, 22, FontWeight.SemiBold, 0f),
    titleSmall = style(14, 20, FontWeight.SemiBold, 0f),
    bodyLarge = style(16, 24, FontWeight.Normal, 0.5f),
    bodyMedium = style(14, 20, FontWeight.Normal, 0.25f),
    bodySmall = style(13, 18, FontWeight.Normal, 0.2f),
    labelLarge = style(14, 20, FontWeight.SemiBold, 0.1f),
    labelMedium = style(12, 16, FontWeight.Bold, 0.6f),
    labelSmall = style(11, 16, FontWeight.Bold, 0.5f),
)

/** Extra text styles from the design that have no Material 3 role. Colour is applied by callers. */
object TripTypography {
    /** Hero "in 2d 4h". */
    val countdown: TextStyle = style(44, 50, FontWeight.Bold, -1f)

    /** Hero station codes. */
    val stationCodeHero: TextStyle = style(32, 36, FontWeight.ExtraBold, 0f)

    /** Timeline station codes. */
    val stationCode: TextStyle = style(22, 26, FontWeight.ExtraBold, 0f)

    /** Hero Coach/Berth/Type values. */
    val seatValue: TextStyle = style(30, 36, FontWeight.Bold, 0f)

    /** Monospace PNR block. */
    val pnr: TextStyle = style(22, 30, FontWeight.SemiBold, 1f, FontFamily.Monospace)
}
