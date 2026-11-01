package com.example.basketballtracker.ui.theme

import androidx.compose.ui.graphics.Color

/** Light sports palette with separate brand and shot-result colors. */
object SportsColors {
    val Background = Color(0xFFF2F6FC)
    val Surface = Color(0xFFFFFFFF)
    val Raised = Color(0xFFE8F0FF)
    val Line = Color(0xFFD2DEEF)
    val Text = Color(0xFF142442)
    val Muted = Color(0xFF536781)
    val Accent = Color(0xFF165DDB)
    val OnAccent = Color.White
    val Blue = Accent
    val Orange = Color(0xFFC44F08)
    val Green = Color(0xFF00843D)
    val Red = Color(0xFFCC253D)
}

val Primary = SportsColors.Accent
val OnPrimary = SportsColors.OnAccent
val PrimaryContainer = SportsColors.Raised
val OnPrimaryContainer = SportsColors.Accent
val Background = SportsColors.Background
val OnBackground = SportsColors.Text
val Surface = SportsColors.Surface
val OnSurface = SportsColors.Text
val SurfaceVariant = SportsColors.Raised
val OnSurfaceVariant = SportsColors.Muted
val Outline = SportsColors.Line
val OutlineVariant = SportsColors.Line
val Success = SportsColors.Green
val Warning = SportsColors.Accent
val Error = SportsColors.Red
val OnError = SportsColors.OnAccent

val LightPrimary = SportsColors.Accent
val LightOnPrimary = Color.White
val LightPrimaryContainer = SportsColors.Raised
val LightOnPrimaryContainer = SportsColors.Accent
val LightBackground = SportsColors.Background
val LightOnBackground = SportsColors.Text
val LightSurface = Color.White
val LightOnSurface = SportsColors.Text
val LightSurfaceVariant = SportsColors.Raised
val LightOnSurfaceVariant = SportsColors.Muted
