package com.vellum.notes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.vellum.notes.R

// Brand typefaces: Fraunces 500/600 for display + titles, Inter 400/500/600
// for body/UI. Bundled under app/src/main/res/font/ (Google Fonts, OFL).
val VellumDisplayFontFamily: FontFamily = FontFamily(
    Font(R.font.fraunces_medium, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold)
)
val VellumBodyFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(
        fontFamily = VellumDisplayFontFamily, fontWeight = FontWeight.Medium,
        letterSpacing = (-0.02).em
    ),
    displayMedium = base.displayMedium.copy(
        fontFamily = VellumDisplayFontFamily, fontWeight = FontWeight.Medium,
        letterSpacing = (-0.02).em
    ),
    displaySmall = base.displaySmall.copy(
        fontFamily = VellumDisplayFontFamily, fontWeight = FontWeight.Medium,
        letterSpacing = (-0.02).em
    ),
    headlineLarge = base.headlineLarge.copy(
        fontFamily = VellumDisplayFontFamily, fontWeight = FontWeight.Medium
    ),
    headlineMedium = base.headlineMedium.copy(
        fontFamily = VellumDisplayFontFamily, fontWeight = FontWeight.Medium
    ),
    headlineSmall = base.headlineSmall.copy(
        fontFamily = VellumDisplayFontFamily, fontWeight = FontWeight.Medium
    ),
    titleLarge = base.titleLarge.copy(
        fontFamily = VellumDisplayFontFamily, fontWeight = FontWeight.SemiBold
    ),
    titleMedium = base.titleMedium.copy(
        fontFamily = VellumDisplayFontFamily, fontWeight = FontWeight.SemiBold
    ),
    titleSmall = base.titleSmall.copy(
        fontFamily = VellumDisplayFontFamily, fontWeight = FontWeight.Medium
    ),
    bodyLarge = base.bodyLarge.copy(fontFamily = VellumBodyFontFamily, lineHeight = 1.5.em),
    bodyMedium = base.bodyMedium.copy(fontFamily = VellumBodyFontFamily, lineHeight = 1.5.em),
    bodySmall = base.bodySmall.copy(fontFamily = VellumBodyFontFamily, lineHeight = 1.5.em),
    labelLarge = base.labelLarge.copy(fontFamily = VellumBodyFontFamily),
    labelMedium = base.labelMedium.copy(fontFamily = VellumBodyFontFamily),
    labelSmall = base.labelSmall.copy(fontFamily = VellumBodyFontFamily),
)
