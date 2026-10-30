package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppFontStyle
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nokshiDataStore by preferencesDataStore(name = "nokshi_theme_prefs")

data class UserThemeSettings(
    val colorTheme: AppColorTheme = AppColorTheme.NOKSHI_CRIMSON,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customPrimaryArgb: Int = 0xFF8E1D2A.toInt(),
    val fontStyle: AppFontStyle = AppFontStyle.HIND_SILIGURI
)

class ThemePreferencesRepository(private val context: Context) {
    private val keyColorTheme = stringPreferencesKey("color_theme")
    private val keyThemeMode = stringPreferencesKey("theme_mode")
    private val keyCustomColor = intPreferencesKey("custom_primary_argb")
    private val keyFontStyle = stringPreferencesKey("font_style")

    val settingsFlow: Flow<UserThemeSettings> = context.nokshiDataStore.data.map { prefs ->
        val theme = prefs[keyColorTheme]?.let {
            runCatching { AppColorTheme.valueOf(it) }.getOrNull()
        } ?: AppColorTheme.NOKSHI_CRIMSON

        val mode = prefs[keyThemeMode]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrNull()
        } ?: ThemeMode.SYSTEM

        val customColor = prefs[keyCustomColor] ?: 0xFF8E1D2A.toInt()

        val font = prefs[keyFontStyle]?.let {
            runCatching { AppFontStyle.valueOf(it) }.getOrNull()
        } ?: AppFontStyle.HIND_SILIGURI

        UserThemeSettings(
            colorTheme = theme,
            themeMode = mode,
            customPrimaryArgb = customColor,
            fontStyle = font
        )
    }

    suspend fun updateColorTheme(theme: AppColorTheme) {
        context.nokshiDataStore.edit { it[keyColorTheme] = theme.name }
    }

    suspend fun updateCustomColor(argb: Int) {
        context.nokshiDataStore.edit {
            it[keyCustomColor] = argb
            it[keyColorTheme] = AppColorTheme.CUSTOM.name
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.nokshiDataStore.edit { it[keyThemeMode] = mode.name }
    }

    suspend fun updateFontStyle(style: AppFontStyle) {
        context.nokshiDataStore.edit { it[keyFontStyle] = style.name }
    }
}
