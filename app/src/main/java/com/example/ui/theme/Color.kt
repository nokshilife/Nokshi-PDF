package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.i18n.AppLanguage

enum class AppColorTheme(
    val titleEn: String,
    val titleBn: String,
    val subtitle: String,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewTertiary: Color
) {
    NOKSHI_CRIMSON(
        titleEn = "Nokshi Maroon (Default)",
        titleBn = "নকশি মেরুন (ডিফল্ট)",
        subtitle = "Nokshi Kantha Heritage",
        previewPrimary = Color(0xFF8E1D2A),
        previewSecondary = Color(0xFF1F3A60),
        previewTertiary = Color(0xFFD49B35)
    ),
    ROYAL_INDIGO(
        titleEn = "Royal Indigo",
        titleBn = "নীলকণ্ঠ ইন্ডিগো",
        subtitle = "Royal Bengal Indigo",
        previewPrimary = Color(0xFF1E3A8A),
        previewSecondary = Color(0xFF0F766E),
        previewTertiary = Color(0xFFD97706)
    ),
    BENGAL_EMERALD(
        titleEn = "Delta Emerald",
        titleBn = "বাংলা পান্না সবুজ",
        subtitle = "Lush Delta Emerald",
        previewPrimary = Color(0xFF0F6E56),
        previewSecondary = Color(0xFF2E4F4F),
        previewTertiary = Color(0xFFC88A2B)
    ),
    SUVARNA_TERRACOTTA(
        titleEn = "Suvarna Terracotta",
        titleBn = "সুবর্ণ টেরাকোটা",
        subtitle = "Paharpur Clay & Gold",
        previewPrimary = Color(0xFFA8431D),
        previewSecondary = Color(0xFF5C3D2E),
        previewTertiary = Color(0xFF2B6CB0)
    ),
    OCEAN_TEAL(
        titleEn = "Bay Ocean Teal",
        titleBn = "সাগর নীলাভ টিল",
        subtitle = "Bay of Bengal Breeze",
        previewPrimary = Color(0xFF0E7490),
        previewSecondary = Color(0xFF334155),
        previewTertiary = Color(0xFFE11D48)
    ),
    CUSTOM(
        titleEn = "Custom Color",
        titleBn = "কাস্টম কালার",
        subtitle = "Your Personalized Palette",
        previewPrimary = Color(0xFF7C3AED),
        previewSecondary = Color(0xFF3730A3),
        previewTertiary = Color(0xFFEC4899)
    );

    fun label(lang: AppLanguage): String = lang.pick(titleEn, titleBn)
}

enum class ThemeMode(val labelEn: String, val labelBn: String) {
    SYSTEM("System", "সিস্টেম ডিফল্ট"),
    LIGHT("Light", "লাইট মোড"),
    DARK("Dark", "ডার্ক মোড");

    fun label(lang: AppLanguage): String = lang.pick(labelEn, labelBn)
}

fun buildColorScheme(
    theme: AppColorTheme,
    darkTheme: Boolean,
    customPrimaryArgb: Int
): ColorScheme {
    if (theme == AppColorTheme.CUSTOM) {
        val seed = Color(customPrimaryArgb)
        return createCustomScheme(seed, darkTheme)
    }

    return when (theme) {
        AppColorTheme.NOKSHI_CRIMSON -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFFFB3B6),
                onPrimary = Color(0xFF56000D),
                primaryContainer = Color(0xFF73101D),
                onPrimaryContainer = Color(0xFFFFDAD9),
                secondary = Color(0xFFAEC6FF),
                onSecondary = Color(0xFF08294E),
                secondaryContainer = Color(0xFF1F3A60),
                onSecondaryContainer = Color(0xFFD8E2FF),
                tertiary = Color(0xFFF6BE58),
                onTertiary = Color(0xFF422C00),
                tertiaryContainer = Color(0xFF5E4100),
                onTertiaryContainer = Color(0xFFFFDEA6),
                background = Color(0xFF181112),
                onBackground = Color(0xFFF0DEDF),
                surface = Color(0xFF211819),
                onSurface = Color(0xFFF0DEDF),
                surfaceVariant = Color(0xFF3A2B2C),
                onSurfaceVariant = Color(0xFFD7C1C2)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF8E1D2A),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDAD9),
                onPrimaryContainer = Color(0xFF3B0007),
                secondary = Color(0xFF1F3A60),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFD8E4FF),
                onSecondaryContainer = Color(0xFF071B36),
                tertiary = Color(0xFFB67D14),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFFFE4AE),
                onTertiaryContainer = Color(0xFF2B1B00),
                background = Color(0xFFFBF7F0),
                onBackground = Color(0xFF22191A),
                surface = Color(0xFFFFFBF7),
                onSurface = Color(0xFF22191A),
                surfaceVariant = Color(0xFFF4E7DF),
                onSurfaceVariant = Color(0xFF524343)
            )
        }

        AppColorTheme.ROYAL_INDIGO -> createCustomScheme(Color(0xFF1E3A8A), darkTheme, Color(0xFF0F766E), Color(0xFFD97706))
        AppColorTheme.BENGAL_EMERALD -> createCustomScheme(Color(0xFF0F6E56), darkTheme, Color(0xFF2C5E50), Color(0xFFC88A2B))
        AppColorTheme.SUVARNA_TERRACOTTA -> createCustomScheme(Color(0xFFA8431D), darkTheme, Color(0xFF5C3D2E), Color(0xFF2B6CB0))
        AppColorTheme.OCEAN_TEAL -> createCustomScheme(Color(0xFF0E7490), darkTheme, Color(0xFF334155), Color(0xFFE11D48))
        AppColorTheme.CUSTOM -> createCustomScheme(Color(customPrimaryArgb), darkTheme)
    }
}

private fun blend(c1: Color, c2: Color, ratio: Float): Color {
    val inv = 1f - ratio
    return Color(
        red = (c1.red * inv + c2.red * ratio).coerceIn(0f, 1f),
        green = (c1.green * inv + c2.green * ratio).coerceIn(0f, 1f),
        blue = (c1.blue * inv + c2.blue * ratio).coerceIn(0f, 1f),
        alpha = 1f
    )
}

private fun createCustomScheme(
    primarySeed: Color,
    darkTheme: Boolean,
    secondarySeed: Color = blend(primarySeed, Color(0xFF1E293B), 0.45f),
    tertiarySeed: Color = Color(0xFFD49B35)
): ColorScheme {
    return if (darkTheme) {
        val lightPrimary = blend(primarySeed, Color.White, 0.45f)
        val lightSecondary = blend(secondarySeed, Color.White, 0.48f)
        val lightTertiary = blend(tertiarySeed, Color.White, 0.35f)
        darkColorScheme(
            primary = lightPrimary,
            onPrimary = if (lightPrimary.luminance() > 0.45f) Color(0xFF161616) else Color.White,
            primaryContainer = blend(primarySeed, Color.Black, 0.35f),
            onPrimaryContainer = blend(primarySeed, Color.White, 0.80f),
            secondary = lightSecondary,
            onSecondary = Color(0xFF121820),
            secondaryContainer = blend(secondarySeed, Color.Black, 0.35f),
            onSecondaryContainer = blend(secondarySeed, Color.White, 0.82f),
            tertiary = lightTertiary,
            onTertiary = Color(0xFF241A00),
            tertiaryContainer = blend(tertiarySeed, Color.Black, 0.40f),
            onTertiaryContainer = blend(tertiarySeed, Color.White, 0.85f),
            background = Color(0xFF141518),
            onBackground = Color(0xFFECEEF2),
            surface = Color(0xFF1B1D22),
            onSurface = Color(0xFFECEEF2),
            surfaceVariant = blend(Color(0xFF272A30), primarySeed, 0.14f),
            onSurfaceVariant = Color(0xFFC8CDD5)
        )
    } else {
        val onPrimary = if (primarySeed.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
        lightColorScheme(
            primary = primarySeed,
            onPrimary = onPrimary,
            primaryContainer = blend(primarySeed, Color.White, 0.82f),
            onPrimaryContainer = blend(primarySeed, Color.Black, 0.55f),
            secondary = secondarySeed,
            onSecondary = Color.White,
            secondaryContainer = blend(secondarySeed, Color.White, 0.84f),
            onSecondaryContainer = blend(secondarySeed, Color.Black, 0.55f),
            tertiary = tertiarySeed,
            onTertiary = Color.White,
            tertiaryContainer = blend(tertiarySeed, Color.White, 0.82f),
            onTertiaryContainer = blend(tertiarySeed, Color.Black, 0.55f),
            background = blend(Color(0xFFFAF8F5), primarySeed, 0.03f),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = blend(Color(0xFFF1EEE9), primarySeed, 0.08f),
            onSurfaceVariant = Color(0xFF47464A)
        )
    }
}
