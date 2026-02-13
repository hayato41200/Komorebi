package com.beeregg2001.komorebi.ui.theme

import androidx.compose.ui.graphics.Color

data class ThemePalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color
)

val DarkPalette = ThemePalette(
    primary = Color(0xFF8E7DFF),
    secondary = Color(0xFFC8C2FF),
    tertiary = Color(0xFFFFB3D1),
    background = Color(0xFF111217),
    surface = Color(0xFF1A1B21),
    onSurface = Color.White
)

val LightPalette = ThemePalette(
    primary = Color(0xFF5A43CC),
    secondary = Color(0xFF5D4A90),
    tertiary = Color(0xFF9B3F65),
    background = Color(0xFFF5F6FC),
    surface = Color.White,
    onSurface = Color(0xFF181A20)
)

val HighContrastPalette = ThemePalette(
    primary = Color(0xFFFFFF00),
    secondary = Color(0xFF00E5FF),
    tertiary = Color(0xFFFF6E40),
    background = Color(0xFF000000),
    surface = Color(0xFF111111),
    onSurface = Color.White
)

fun customPalette(accent: Color): ThemePalette = ThemePalette(
    primary = accent,
    secondary = accent.copy(alpha = 0.8f),
    tertiary = accent.copy(alpha = 0.65f),
    background = Color(0xFF0F1118),
    surface = Color(0xFF181C26),
    onSurface = Color.White
)
