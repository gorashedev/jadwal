package com.jadwal.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.notifications.NotificationScheduler
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
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    // ===== يُستخدم في MainActivity لتحديد الثيم مباشرةً =====
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
            ) { arr ->
                SettingsUiState(
                    userName = arr[0] as String,
                    themeMode = arr[1] as String,
                    languageCode = arr[2] as String,
                    notificationsEnabled = arr[3] as Boolean,
                    notificationHour = arr[4] as Int,
                )
            }.collect { _uiState.value = it }
        }
    }

    // ===== تغيير الثيم =====
    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    // ===== تغيير اللغة — يُعيد بناء الـ Activity تلقائياً =====
    fun setLanguage(code: String) {
        viewModelScope.launch {
            prefs.setLanguageCode(code)
            LanguageManager.setAppLocale(code)
        }
    }

    // ===== تفعيل/إيقاف الإشعارات =====
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            prefs.setNotificationSettings(enabled, state.notificationHour, state.notificationMinute)

            if (enabled) {
                // إعادة جدولة الإشعارات
                notificationScheduler.scheduleDailyReminder(
                    state.notificationHour,
                    state.notificationMinute,
                )
                notificationScheduler.scheduleExamAlerts()
            } else {
                // إلغاء جميع الإشعارات
                notificationScheduler.cancelAll()
            }
        }
    }

    // ===== تغيير وقت التذكير =====
    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val enabled = _uiState.value.notificationsEnabled
            prefs.setNotificationSettings(enabled, hour, minute)

            if (enabled) {
                // تحديث الجدول بالوقت الجديد
                notificationScheduler.updateReminderTime(hour, minute)
            }
        }
    }
}
