package com.mapmate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mapmate.R

// navx design tokens, pulled 1:1 from the team design sample (00-design-sample/NavX App.html)
// dark theme only -> the design ships dark, that's what we match
object NavX {
    // surfaces
    val bg = Color(0xFF0C0C14)
    val surface = Color(0xFF13131F)
    val card = Color(0xFF1A1A2E)
    val border = Color(0xFF2A2A42)
    val borderSoft = Color(0x8C2A2A42)

    // text
    val text = Color(0xFFF0EEFF)
    val muted = Color(0xFF7C7A99)

    // accent (the purple)
    val accent = Color(0xFF7C3AED)
    val accentLight = Color(0xFF9D6EF5)
    val accentGlow = Color(0x617C3AED)
    val accentSoft = Color(0x247C3AED)

    // status
    val success = Color(0xFF34D399)
    val danger = Color(0xFFF87171)

    // map canvas
    val mapBg = Color(0xFF141D33)
    val mapRoad = Color(0x0FFFFFFF)
    val mapGrid = Color(0x08FFFFFF)
    val mapPark = Color(0x0D34D399)
    val mapWater = Color(0x173878C8)
    val mapScrim = Color(0x6B080812)

    // glass + chips
    val glass = Color(0xB313131F)
    val glassBorder = Color(0x1AFFFFFF)
    val chipBg = Color(0xFFFFFFFF)
    val chipText = Color(0xFF15151F)
    val shadow = Color(0x80000000)
}

// fonts -> the real design fonts, bundled as variable ttf in res/font
// one variable file per family, compose instances each weight off the wght axis
val Syne = FontFamily(
    Font(R.font.syne_variable, FontWeight.Normal),
    Font(R.font.syne_variable, FontWeight.Medium),
    Font(R.font.syne_variable, FontWeight.SemiBold),
    Font(R.font.syne_variable, FontWeight.Bold),
    Font(R.font.syne_variable, FontWeight.ExtraBold),
)
val DMSans = FontFamily(
    Font(R.font.dmsans_variable, FontWeight.Light),
    Font(R.font.dmsans_variable, FontWeight.Normal),
    Font(R.font.dmsans_variable, FontWeight.Medium),
    Font(R.font.dmsans_variable, FontWeight.SemiBold),
)

private val navXTypography = Typography(
    // headline -> screen titles, syne extrabold
    headlineLarge = TextStyle(fontFamily = Syne, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp),
    headlineMedium = TextStyle(fontFamily = Syne, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp),
    titleLarge = TextStyle(fontFamily = Syne, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = Syne, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    // body -> dm sans
    bodyLarge = TextStyle(fontFamily = DMSans, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = DMSans, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = DMSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Syne, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = Syne, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
)

// map navx tokens onto a material3 dark scheme so stock components inherit the look
private val navXColorScheme = darkColorScheme(
    primary = NavX.accent,
    onPrimary = Color.White,
    secondary = NavX.accentLight,
    background = NavX.bg,
    onBackground = NavX.text,
    surface = NavX.surface,
    onSurface = NavX.text,
    surfaceVariant = NavX.card,
    onSurfaceVariant = NavX.muted,
    outline = NavX.border,
    error = NavX.danger,
)

@Composable
fun MapMateTheme(content: @Composable () -> Unit) {
    // always dark, the design is dark. isSystemInDarkTheme kept for future light support
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = navXColorScheme,
        typography = navXTypography,
        content = content,
    )
}
