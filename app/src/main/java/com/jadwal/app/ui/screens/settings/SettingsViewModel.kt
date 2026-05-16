package com.jadwal.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.util.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val themeMode: String = "SYSTEM",     // "LIGHT", "DARK", "SYSTEM"
    val languageCode: String = "",        // "ar", "en", "" = تبع الجهاز
    val notificationsEnabled: Boolean = true,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    // ===== يُستخدم في MainActivity لتحديد الثيم =====
    val themeMode: StateFlow<String> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")

    init {
        viewModelScope.launch {
            combine(
                prefs.userName,
                prefs.themeMode,
                prefs.languageCode,
                prefs.notificationsEnabled,
                prefs.notificationHour,
            ) { values ->
                SettingsUiState(
                    userName = values[0] as String,
                    themeMode = values[1] as String,
                    languageCode = values[2] as String,
                    notificationsEnabled = values[3] as Boolean,
                    notificationHour = values[4] as Int,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    // ===== تغيير الثيم =====
    fun setThemeMode(mode: String) {
        // mode: "LIGHT", "DARK", "SYSTEM"
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    // ===== تغيير اللغة — يُعيد بناء الـ Activity تلقائياً =====
    fun setLanguage(code: String) {
        // code: "ar", "en", "" = تبع الجهاز
        viewModelScope.launch {
            prefs.setLanguageCode(code)
            // هذا يُعيد تشغيل الـ Activity ويُطبّق اللغة فوراً
            LanguageManager.setAppLocale(code)
        }
    }

    // ===== تغيير الإشعارات =====
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setNotificationSettings(
                enabled = enabled,
                hour = _uiState.value.notificationHour,
                minute = _uiState.value.notificationMinute,
            )
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.setNotificationSettings(
                enabled = _uiState.value.notificationsEnabled,
                hour = hour,
                minute = minute,
            )
        }
    }
}
