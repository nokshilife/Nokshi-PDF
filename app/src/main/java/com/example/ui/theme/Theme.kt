package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val NokshiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NokshiPdfTheme(
    colorTheme: AppColorTheme = AppColorTheme.NOKSHI_CRIMSON,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    customPrimaryArgb: Int = 0xFF8E1D2A.toInt(),
    fontStyle: AppFontStyle = AppFontStyle.HIND_SILIGURI,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = buildColorScheme(
        theme = colorTheme,
        darkTheme = isDark,
        customPrimaryArgb = customPrimaryArgb
    )
    val typography = buildAppTypography(fontStyle)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = NokshiShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    NokshiPdfTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        content = content
    )
}
