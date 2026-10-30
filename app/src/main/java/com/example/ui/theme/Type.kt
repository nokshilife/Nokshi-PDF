package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.i18n.AppLanguage

val HindSiliguriFamily = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal),
    Font(R.font.hind_siliguri, FontWeight.Medium),
    Font(R.font.hind_siliguri, FontWeight.SemiBold),
    Font(R.font.hind_siliguri, FontWeight.Bold)
)

val NotoSerifBengaliFamily = FontFamily(
    Font(R.font.noto_serif_bengali, FontWeight.Normal),
    Font(R.font.noto_serif_bengali, FontWeight.Medium),
    Font(R.font.noto_serif_bengali, FontWeight.Bold)
)

val AtmaFamily = FontFamily(
    Font(R.font.atma, FontWeight.Normal),
    Font(R.font.atma, FontWeight.Medium),
    Font(R.font.atma, FontWeight.Bold)
)

val PlusJakartaFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val LoraFamily = FontFamily(
    Font(R.font.lora, FontWeight.Normal),
    Font(R.font.lora, FontWeight.Medium),
    Font(R.font.lora, FontWeight.Bold)
)

val PoppinsFamily = FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
    Font(R.font.poppins, FontWeight.Medium),
    Font(R.font.poppins, FontWeight.Bold)
)

val RobotoMonoFamily = FontFamily(
    Font(R.font.roboto_mono, FontWeight.Normal),
    Font(R.font.roboto_mono, FontWeight.Medium),
    Font(R.font.roboto_mono, FontWeight.Bold)
)

enum class AppFontStyle(
    val titleEn: String,
    val titleBn: String,
    val sampleText: String
) {
    HIND_SILIGURI(
        titleEn = "Hind Siliguri (Modern Bilingual)",
        titleBn = "হিন্দ শিলিগুড়ি (আধুনিক বাংলা)",
        sampleText = "Nokshi PDF Studio • নকশি পিডিএফ"
    ),
    PLUS_JAKARTA(
        titleEn = "Plus Jakarta Sans (Clean UI)",
        titleBn = "প্লাস জাকার্তা স্যান্স",
        sampleText = "Clean Geometric • আধুনিক ডকুমেন্ট"
    ),
    NOTO_SERIF_BENGALI(
        titleEn = "Noto Serif (Editorial Print)",
        titleBn = "নোটো সেরিফ (সাহিত্যিক ছাপা)",
        sampleText = "Editorial Serif • নকশি পিডিএফ"
    ),
    LORA_EDITORIAL(
        titleEn = "Lora Classic Book",
        titleBn = "লোরা ক্ল্যাসিক বুক",
        sampleText = "Classic Book Typography • বইয়ের ছাঁদ"
    ),
    ATMA_ARTISAN(
        titleEn = "Atma Nokshi (Handcrafted)",
        titleBn = "আত্মা নকশি (নান্দনিক স্টাইল)",
        sampleText = "Artisan Handcrafted • নকশি পিডিএফ"
    ),
    ROBOTO_MONO(
        titleEn = "Roboto Monospace",
        titleBn = "রোবোটো মনোস্পেস",
        sampleText = "Technical Precision • 0123456789"
    );

    fun label(lang: AppLanguage): String = lang.pick(titleEn, titleBn)

    val displayFamily: FontFamily
        get() = when (this) {
            HIND_SILIGURI -> HindSiliguriFamily
            NOTO_SERIF_BENGALI -> NotoSerifBengaliFamily
            ATMA_ARTISAN -> AtmaFamily
            PLUS_JAKARTA -> PlusJakartaFamily
            LORA_EDITORIAL -> LoraFamily
            ROBOTO_MONO -> RobotoMonoFamily
        }

    val bodyFamily: FontFamily
        get() = when (this) {
            HIND_SILIGURI -> HindSiliguriFamily
            NOTO_SERIF_BENGALI -> NotoSerifBengaliFamily
            ATMA_ARTISAN -> HindSiliguriFamily
            PLUS_JAKARTA -> PlusJakartaFamily
            LORA_EDITORIAL -> LoraFamily
            ROBOTO_MONO -> RobotoMonoFamily
        }
}

fun buildAppTypography(style: AppFontStyle): Typography {
    val display = style.displayFamily
    val body = style.bodyFamily
    return Typography(
        displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
        displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
        headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
        headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 26.sp),
        titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 25.sp),
        titleMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp)
    )
}

val Typography: Typography by lazy { buildAppTypography(AppFontStyle.HIND_SILIGURI) }
