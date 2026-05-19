package com.jadwal.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
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
    private val authRepository: AuthRepository,
) : ViewModel() {

    val themeMode = prefs.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "SYSTEM"
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _logoutEvent = Channel<Unit>(Channel.BUFFERED)
    val logoutEvent = _logoutEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.themeMode,
                prefs.languageCode,
                prefs.notificationsEnabled,
                prefs.notificationHour,
                prefs.notificationMinute,
            ) { theme, lang, notif, hour, minute ->
                SettingsUiState(
                    themeMode = theme,
                    languageCode = lang,
                    notificationsEnabled = notif,
                    notificationHour = hour,
                    notificationMinute = minute,
                )
            }.collect { state ->
                _uiState.update { it.copy(
                    themeMode = state.themeMode,
                    languageCode = state.languageCode,
                    notificationsEnabled = state.notificationsEnabled,
                    notificationHour = state.notificationHour,
                    notificationMinute = state.notificationMinute,
                ) }
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    /**
     * تغيير لغة التطبيق عبر AppCompatDelegate.setApplicationLocales()
     * هذا يتسبب في إعادة تشغيل الـ Activity تلقائياً فيُعيد بناء
     * كل الـ Composables بالـ locale الجديد — بما في ذلك stringResource().
     */
    fun setLanguage(code: String) {
        // حفظ متزامن في SharedPreferences حتى يُطبَّق الـ locale عند بدء التطبيق
        prefs.saveLanguageSync(code)
        viewModelScope.launch {
            prefs.setLanguageCode(code)
        }
        val localeList = when (code) {
            "ar" -> LocaleListCompat.forLanguageTags("ar")
            "en" -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            prefs.setNotificationSettings(enabled, state.notificationHour, state.notificationMinute)
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            prefs.setNotificationSettings(state.notificationsEnabled, hour, minute)
        }
    }

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
                authRepository.signOut()
            } catch (_: Exception) { }
            prefs.setLoggedIn(false)
            _uiState.update { it.copy(isLoggingOut = false) }
            _logoutEvent.send(Unit)
        }
    }
}
