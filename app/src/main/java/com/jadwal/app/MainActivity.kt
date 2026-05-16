package com.jadwal

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.appcompat.app.AppCompatActivity
import com.jadwal.ui.navigation.JadwalApp
import com.jadwal.ui.screens.settings.SettingsViewModel
import com.jadwal.ui.theme.JadwalTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.ComponentActivity

/**
 * MainActivity — مهم: يجب أن يرث من AppCompatActivity وليس ComponentActivity
 * هذا ضروري لكي يعمل AppCompatDelegate.setApplicationLocales() بشكل صحيح
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash Screen — يجب أن يكون أول شيء
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // جعل التطبيق يمتد خلف شريط الحالة وشريط التنقل
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // SettingsViewModel يوفر الثيم المختار من DataStore
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

            // تحديد هل الوضع داكن أم لا
            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme() // SYSTEM = تبع الجهاز
            }

            JadwalTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JadwalApp()
                }
            }
        }
    }
}
