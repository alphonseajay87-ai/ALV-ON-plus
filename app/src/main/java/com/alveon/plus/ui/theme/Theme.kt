package com.alveon.plus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF101012)
    val SurfaceHigh = Color(0xFF232327)
    val Selected = Color(0xFF34343A)
    val Silver = Color(0xFFDCDCE1)
    val Muted = Color(0xFF8E8E95)
    val Danger = Color(0xFFFF7A7A)
}

private val Sans = FontFamily.SansSerif

private val AlveonTypography = Typography(
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 44.sp, letterSpacing = 8.sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 22.sp, letterSpacing = 0.3.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp),
)

@Composable
fun AlveonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Silver,
            onPrimary = Palette.Black,
            background = Palette.Black,
            onBackground = Palette.Silver,
            surface = Palette.Surface,
            onSurface = Palette.Silver,
            surfaceVariant = Palette.SurfaceHigh,
            onSurfaceVariant = Palette.Muted,
            error = Palette.Danger,
        ),
        typography = AlveonTypography,
    ) {
        // Pure black canvas; also sets the default content colour for Text.
        Surface(color = Palette.Black, contentColor = Palette.Silver, content = content)
    }
}
