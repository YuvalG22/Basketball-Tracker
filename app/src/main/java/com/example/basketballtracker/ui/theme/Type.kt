package com.example.basketballtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.basketballtracker.R

val inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_black, FontWeight.Black),
    Font(R.font.inter_semi, FontWeight.SemiBold),
    Font(R.font.inter_medium, FontWeight.Medium),
)

val commonSettings = "tnum"
private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = inter, fontFeatureSettings = commonSettings),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = inter, fontFeatureSettings = commonSettings),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = inter, fontFeatureSettings = commonSettings),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = inter, fontFeatureSettings = commonSettings),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = inter, fontFeatureSettings = commonSettings),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = inter, fontFeatureSettings = commonSettings),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = inter, fontFeatureSettings = commonSettings),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = inter, fontFeatureSettings = commonSettings),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = inter, fontFeatureSettings = commonSettings),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = inter, fontFeatureSettings = commonSettings),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = inter, fontFeatureSettings = commonSettings),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = inter, fontFeatureSettings = commonSettings),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = inter, fontFeatureSettings = commonSettings),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = inter, fontFeatureSettings = commonSettings),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = inter, fontFeatureSettings = commonSettings)
)