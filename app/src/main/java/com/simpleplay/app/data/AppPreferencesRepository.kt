package com.simpleplay.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AccentTheme {
    CORAL,
    MANGO,
    MINT,
    SKY,
    PURPLE,
    BERRY,
    CUSTOM,
}

enum class WidgetButtonSize {
    SMALL,
    MEDIUM,
    LARGE,
}

enum class WidgetBackgroundStyle {
    THEME,
    TRANSPARENT,
}

val DefaultCustomAccent: Int = 0xFF00A884.toInt()

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentTheme: AccentTheme = AccentTheme.CORAL,
    val selectedMediaPackage: String? = null,
    val selectedMediaSessionId: String? = null,
    val customAccentArgb: Int = DefaultCustomAccent,
    val widgetButtonSize: WidgetButtonSize = WidgetButtonSize.LARGE,
    val widgetThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val widgetAccentTheme: AccentTheme = AccentTheme.CORAL,
    val widgetCustomAccentArgb: Int = DefaultCustomAccent,
    val widgetBackgroundStyle: WidgetBackgroundStyle = WidgetBackgroundStyle.THEME,
)

private val Context.preferencesDataStore by preferencesDataStore(name = "app_preferences")

class AppPreferencesRepository(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val accentTheme = stringPreferencesKey("accent_theme")
        val selectedMediaPackage = stringPreferencesKey("selected_media_package")
        val selectedMediaSessionId = stringPreferencesKey("selected_media_session_id")
        val customAccentArgb = intPreferencesKey("custom_accent_argb")
        val widgetButtonSize = stringPreferencesKey("widget_button_size")
        val widgetThemeMode = stringPreferencesKey("widget_theme_mode")
        val widgetAccentTheme = stringPreferencesKey("widget_accent_theme")
        val widgetCustomAccentArgb = intPreferencesKey("widget_custom_accent_argb")
        val widgetBackgroundStyle = stringPreferencesKey("widget_background_style")
    }

    val preferences: Flow<AppPreferences> = context.preferencesDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { values ->
            AppPreferences(
                themeMode = values[Keys.themeMode].toEnumOrDefault(ThemeMode.SYSTEM),
                accentTheme = values[Keys.accentTheme].toEnumOrDefault(AccentTheme.CORAL),
                selectedMediaPackage = values[Keys.selectedMediaPackage],
                selectedMediaSessionId = values[Keys.selectedMediaSessionId],
                customAccentArgb = values[Keys.customAccentArgb] ?: DefaultCustomAccent,
                widgetButtonSize = values[Keys.widgetButtonSize]
                    .toEnumOrDefault(WidgetButtonSize.LARGE),
                widgetThemeMode = values[Keys.widgetThemeMode]
                    .toEnumOrDefault(ThemeMode.SYSTEM),
                widgetAccentTheme = values[Keys.widgetAccentTheme]
                    .toEnumOrDefault(AccentTheme.CORAL),
                widgetCustomAccentArgb = values[Keys.widgetCustomAccentArgb]
                    ?: DefaultCustomAccent,
                widgetBackgroundStyle = values[Keys.widgetBackgroundStyle]
                    .toEnumOrDefault(WidgetBackgroundStyle.THEME),
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.preferencesDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setAccentTheme(theme: AccentTheme) {
        context.preferencesDataStore.edit { it[Keys.accentTheme] = theme.name }
    }

    suspend fun setCustomAccent(argb: Int) {
        context.preferencesDataStore.edit {
            it[Keys.customAccentArgb] = argb
            it[Keys.accentTheme] = AccentTheme.CUSTOM.name
        }
    }

    suspend fun setWidgetButtonSize(size: WidgetButtonSize) {
        context.preferencesDataStore.edit { it[Keys.widgetButtonSize] = size.name }
    }

    suspend fun setWidgetThemeMode(mode: ThemeMode) {
        context.preferencesDataStore.edit { it[Keys.widgetThemeMode] = mode.name }
    }

    suspend fun setWidgetAccentTheme(theme: AccentTheme) {
        context.preferencesDataStore.edit { it[Keys.widgetAccentTheme] = theme.name }
    }

    suspend fun setWidgetCustomAccent(argb: Int) {
        context.preferencesDataStore.edit {
            it[Keys.widgetCustomAccentArgb] = argb
            it[Keys.widgetAccentTheme] = AccentTheme.CUSTOM.name
        }
    }

    suspend fun setWidgetBackgroundStyle(style: WidgetBackgroundStyle) {
        context.preferencesDataStore.edit { it[Keys.widgetBackgroundStyle] = style.name }
    }

    suspend fun setSelectedMediaSession(packageName: String, sessionId: String) {
        context.preferencesDataStore.edit {
            it[Keys.selectedMediaPackage] = packageName
            it[Keys.selectedMediaSessionId] = sessionId
        }
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
