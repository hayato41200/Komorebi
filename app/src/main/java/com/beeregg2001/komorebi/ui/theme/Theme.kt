package com.beeregg2001.komorebi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.beeregg2001.komorebi.data.SettingsRepository

enum class ThemePreset { Dark, Light, HighContrast, Custom }

private fun String.toThemePreset(): ThemePreset =
    ThemePreset.entries.find { it.name.equals(this, ignoreCase = true) } ?: ThemePreset.Dark

private fun parseColorOrDefault(hex: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { fallback }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun KomorebiTheme(content: @Composable () -> Unit) {
    val repository = SettingsRepository(LocalContext.current)
    val presetName by repository.themePreset.collectAsState(initial = ThemePreset.Dark.name)
    val customAccentHex by repository.themeCustomAccent.collectAsState(initial = "#7C4DFF")

    val preset = presetName.toThemePreset()
    val palette = when (preset) {
        ThemePreset.Dark -> DarkPalette
        ThemePreset.Light -> LightPalette
        ThemePreset.HighContrast -> HighContrastPalette
        ThemePreset.Custom -> customPalette(parseColorOrDefault(customAccentHex, DarkPalette.primary))
    }

    val colorScheme = darkColorScheme(
        primary = palette.primary,
        secondary = palette.secondary,
        tertiary = palette.tertiary,
        background = palette.background,
        surface = palette.surface,
        onSurface = palette.onSurface
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypographyForPreset(preset),
        content = content
    )
}
