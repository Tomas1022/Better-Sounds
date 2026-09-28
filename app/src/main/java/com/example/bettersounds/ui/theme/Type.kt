package com.example.bettersounds.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.bettersounds.R

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold)
)

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Manrope),
    displayMedium = base.displayMedium.copy(fontFamily = Manrope),
    displaySmall = base.displaySmall.copy(fontFamily = Manrope),
    headlineLarge = base.headlineLarge.copy(fontFamily = Manrope),
    headlineMedium = base.headlineMedium.copy(fontFamily = Manrope),
    headlineSmall = base.headlineSmall.copy(fontFamily = Manrope),
    titleLarge = base.titleLarge.copy(fontFamily = Manrope),
    titleMedium = base.titleMedium.copy(fontFamily = Manrope),
    titleSmall = base.titleSmall.copy(fontFamily = Manrope),
    bodyLarge = base.bodyLarge.copy(fontFamily = Manrope),
    bodyMedium = base.bodyMedium.copy(fontFamily = Manrope),
    bodySmall = base.bodySmall.copy(fontFamily = Manrope),
    labelLarge = base.labelLarge.copy(fontFamily = Manrope),
    labelMedium = base.labelMedium.copy(fontFamily = Manrope),
    labelSmall = base.labelSmall.copy(fontFamily = Manrope)
)