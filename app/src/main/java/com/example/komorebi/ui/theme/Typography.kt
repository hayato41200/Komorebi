package com.example.komorebi.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList // これを追加
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        // ★ 日本語ロケールを明示的に指定することで、中華フォント化を防ぎます
        localeList = LocaleList("ja-JP")
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        localeList = LocaleList("ja-JP")
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        localeList = LocaleList("ja-JP")
    )
)