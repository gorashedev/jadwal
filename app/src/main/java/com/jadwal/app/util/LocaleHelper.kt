package com.jadwal.app.util

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

object LocaleHelper {
    fun isEnglish(): Boolean {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) locales[0]?.language == "en"
        else Locale.getDefault().language == "en"
    }

    fun localeLanguageTag(): String = if (isEnglish()) "en" else "ar"
}
