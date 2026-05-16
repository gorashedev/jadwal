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

/**
 * MainActivity — يرث من AppCompatActivity وليس ComponentActivity
 * هذا ضروري لكي يعمل AppCompatDelegate.setApplicationLocales() بشكل صحيح
 * وبالتالي تغيير اللغة يعمل فوراً
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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
                    JadwalApp()
                }
            }
        }
    }
}
