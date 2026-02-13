package com.beeregg2001.komorebi.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

fun appTypographyForPreset(preset: ThemePreset): Typography {
    val headingWeight = if (preset == ThemePreset.HighContrast) FontWeight.ExtraBold else FontWeight.Bold

    return Typography(
        displayLarge = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Normal),
        displayMedium = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Normal),
        displaySmall = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Normal),
        headlineLarge = TextStyle(fontFamily = NotoSansJP, fontWeight = headingWeight),
        headlineMedium = TextStyle(fontFamily = NotoSansJP, fontWeight = headingWeight),
        headlineSmall = TextStyle(fontFamily = NotoSansJP, fontWeight = headingWeight),
        titleLarge = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Bold, fontSize = if (preset == ThemePreset.HighContrast) 24.sp else 22.sp),
        titleMedium = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Medium, fontSize = if (preset == ThemePreset.HighContrast) 18.sp else 16.sp),
        titleSmall = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        bodyLarge = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Normal, fontSize = if (preset == ThemePreset.HighContrast) 18.sp else 16.sp),
        bodyMedium = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Normal, fontSize = if (preset == ThemePreset.HighContrast) 16.sp else 14.sp),
        bodySmall = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        labelLarge = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall = TextStyle(fontFamily = NotoSansJP, fontWeight = FontWeight.Medium, fontSize = 11.sp)
    )
}
