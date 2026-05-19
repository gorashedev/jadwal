package com.jadwal

import android.content.Intent
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

    // ─── إصلاح #3: حقن DeepLinkManager لمعالجة روابط إعادة تعيين كلمة المرور ───
    @Inject
    lateinit var deepLinkManager: DeepLinkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // إصلاح #4: تطبيق اللغة المحفوظة قبل رسم أي محتوى
        // نقرأ من SharedPreferences بشكل متزامن لأن DataStore لا يدعم القراءة المتزامنة
        val savedLang = prefs.getLanguageSync()
        if (savedLang.isNotBlank()) {
            val desired = LocaleListCompat.forLanguageTags(savedLang)
            val current = AppCompatDelegate.getApplicationLocales()
            if (current.toLanguageTags() != desired.toLanguageTags()) {
                AppCompatDelegate.setApplicationLocales(desired)
                return // ستُعاد بناء الـ Activity فوراً بالـ locale الصحيح
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // معالجة الـ Deep Link إذا جاء التطبيق مفتوحاً عبره
        intent?.data?.let { uri ->
            if (uri.scheme == "com.jadwal.app") {
                deepLinkManager.handleUri(uri)
            }
        }

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

    // ─── إصلاح #3: معالجة الـ Deep Link عند فتح التطبيق وهو يعمل بالفعل ───
    // هذا يحدث عندما يضغط المستخدم على رابط البريد الإلكتروني والتطبيق في الخلفية
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // ضروري لـ Compose لاستقبال الـ Intent الجديد

        intent.data?.let { uri ->
            if (uri.scheme == "com.jadwal.app") {
                deepLinkManager.handleUri(uri)
            }
        }
    }
}