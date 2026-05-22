package com.jadwal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.appcompat.app.AppCompatActivity
import com.jadwal.ui.navigation.JadwalApp
import com.jadwal.ui.screens.settings.SettingsViewModel
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.ui.theme.JadwalTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var prefs: UserPreferencesDataStore

    // وسيط معالجة الـ Deep Link
    @Inject
    lateinit var deepLinkManager: DeepLinkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Capture password-reset deep links before locale recreation can skip the rest of onCreate
        handleDeepLinkIntent(intent)

        // تطبيق اللغة المحفوظة قبل رسم أي محتوى
        val savedLang = prefs.getLanguageSync()
        if (savedLang.isNotBlank()) {
            val desired = LocaleListCompat.forLanguageTags(savedLang)
            val current = AppCompatDelegate.getApplicationLocales()
            if (current.toLanguageTags() != desired.toLanguageTags()) {
                AppCompatDelegate.setApplicationLocales(desired)
                return
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            JadwalTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JadwalApp(
                        prefs = prefs,
                        deepLinkManager = deepLinkManager,
                    )
                }
            }
        }
    }

    // معالجة الـ Deep Link عند فتح التطبيق وهو يعمل بالفعل في الخلفية
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // ضروري لـ Compose لاستقبال الـ Intent الجديد

        // إصلاح #1: التقاط الرابط الخام بالكامل هنا أيضاً لضمان الحفاظ على الـ Fragment
        handleDeepLinkIntent(intent)
    }

    /**
     * دالة مساعدة لاستخراج الرابط الكامل كنص (DataString) وتحويله يدوياً إلى Uri
     * ليتخطى قيام أندرويد بحذف الجزء الذي يلي علامة #
     */
    private fun handleDeepLinkIntent(intent: Intent?) {
        val dataString = intent?.dataString ?: return
        val parsedUri = Uri.parse(dataString)
        if (parsedUri.scheme == "com.jadwal.app") {
            deepLinkManager.handleUri(parsedUri)
        }
    }
}