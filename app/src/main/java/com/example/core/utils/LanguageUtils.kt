package com.example.core.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

data class Language(val code: String, val displayName: String, val isRtl: Boolean)

val SUPPORTED_LANGUAGES = listOf(
    Language("ar", "العربية", isRtl = true),
    Language("en", "English", isRtl = false),
    Language("fr", "Français", isRtl = false)
)

object LanguageUtils {
    private const val PREFS_NAME = "tariki_languages"
    private const val KEY_LANG = "selected_lang"

    fun setLocale(context: Context, languageCode: String): Context {
        saveLanguage(context, languageCode)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun isRtl(languageCode: String): Boolean {
        return languageCode == "ar"
    }

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "ar") ?: "ar"
    }

    fun saveLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, languageCode).apply()
    }
}
