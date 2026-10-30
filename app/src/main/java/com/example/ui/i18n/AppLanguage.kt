package com.example.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(
    val labelNative: String,
    val code: String
) {
    ENGLISH("English", "en"),
    BENGALI("বাংলা", "bn");

    fun pick(en: String, bn: String): String = when (this) {
        ENGLISH -> en
        BENGALI -> bn
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

@Composable
@ReadOnlyComposable
fun tr(en: String, bn: String): String {
    return LocalAppLanguage.current.pick(en, bn)
}
