package com.jadwal.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.notifications.NotificationScheduler
import com.jadwal.util.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val themeMode: String = "SYSTEM",
    val languageCode: String = "",
    val notificationsEnabled: Boolean = true,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,
    val showLogoutDialog: Boolean = false,
    val isLoggingOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val notificationScheduler: NotificationScheduler,
    private val supabase: SupabaseClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent = _logoutEvent.asSharedFlow()

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

    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            prefs.setLanguageCode(code)
            LanguageManager.setAppLocale(code)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            prefs.setNotificationSettings(enabled, state.notificationHour, state.notificationMinute)
            if (enabled) {
                notificationScheduler.scheduleDailyReminder(state.notificationHour, state.notificationMinute)
                notificationScheduler.scheduleExamAlerts()
            } else {
                notificationScheduler.cancelAll()
            }
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val enabled = _uiState.value.notificationsEnabled
            prefs.setNotificationSettings(enabled, hour, minute)
            if (enabled) notificationScheduler.updateReminderTime(hour, minute)
        }
    }

    // ===== تسجيل الخروج =====

    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun dismissLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, showLogoutDialog = false) }
            try {
                supabase.auth.signOut()
            } catch (_: Exception) {
                // تجاهل الأخطاء — نُعيد التوجيه في جميع الأحوال
            } finally {
                _uiState.update { it.copy(isLoggingOut = false) }
                _logoutEvent.emit(Unit)
            }
        }
    }
}
