package com.jadwal.util

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * LanguageManager — يعمل على جميع إصدارات Android بشكل صحيح
 *
 * الطريقة الصحيحة لتغيير اللغة في التطبيق هي AppCompatDelegate
 * وليس LocaleManager (Android 13+) لأن AppCompatDelegate يعمل
 * على جميع الإصدارات ويُعيد تشغيل الـ Activity تلقائياً
 */
object LanguageManager {

    const val LANG_ARABIC = "ar"
    const val LANG_ENGLISH = "en"
    const val LANG_SYSTEM = "" // فارغ = اتبع الجهاز

    /**
     * تغيير لغة التطبيق — يُعيد بناء الـ Activity تلقائياً
     *
     * @param languageCode "ar" أو "en" أو "" للمتابعة مع الجهاز
     */
    fun setAppLocale(languageCode: String) {
        val localeList = if (languageCode.isEmpty()) {
            // إعادة للغة الجهاز
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        // هذا يُعيد تشغيل الـ Activity تلقائياً ويُطبّق اللغة فوراً
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * الحصول على اللغة الحالية المختارة من المستخدم
     * تُعيد "" إذا كانت تتبع الجهاز
     */
    fun getCurrentLanguageCode(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) LANG_SYSTEM
        else locales[0]?.language ?: LANG_SYSTEM
    }

    /**
     * هل اللغة الحالية عربية؟
     */
    fun isCurrentlyArabic(context: Context): Boolean {
        return context.resources.configuration.locales[0].language == LANG_ARABIC
    }
}
