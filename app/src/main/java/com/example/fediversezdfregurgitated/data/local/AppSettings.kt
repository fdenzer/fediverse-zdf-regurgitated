package com.example.fediversezdfregurgitated.data.local

import android.content.Context
import android.content.SharedPreferences

enum class AppTheme(val preferenceValue: String) {
    SYSTEM_DEFAULT("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromPreferenceValue(value: String?): AppTheme {
            return entries.find { it.preferenceValue == value } ?: SYSTEM_DEFAULT
        }
    }
}

enum class FontSize(val scaleFactor: Float, val preferenceValue: String) {
    SMALL(0.85f, "small"),
    MEDIUM(1.0f, "medium"),
    LARGE(1.15f, "large");

    companion object {
        fun fromPreferenceValue(value: String?): FontSize {
            return entries.find { it.preferenceValue == value } ?: MEDIUM
        }
    }
}


class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_FONT_SIZE = "font_size"
    }

    var currentTheme: AppTheme
        get() = AppTheme.fromPreferenceValue(prefs.getString(KEY_APP_THEME, null))
        set(value) = prefs.edit().putString(KEY_APP_THEME, value.preferenceValue).apply()

    var fontSize: FontSize
        get() = FontSize.fromPreferenceValue(prefs.getString(KEY_FONT_SIZE, null))
        set(value) = prefs.edit().putString(KEY_FONT_SIZE, value.preferenceValue).apply()

    fun isDarkThemeEnabled(isSystemDark: Boolean): Boolean {
        return when (currentTheme) {
            AppTheme.SYSTEM_DEFAULT -> isSystemDark
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
        }
    }
}
