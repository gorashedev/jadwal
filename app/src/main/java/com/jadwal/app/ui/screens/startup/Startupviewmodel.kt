package com.jadwal.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.DeepLinkManager
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val authRepository: AuthRepository,
    private val deepLinkManager: DeepLinkManager,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            // Brief wait so MainActivity.handleDeepLinkIntent() can run before we decide
            repeat(10) {
                if (deepLinkManager.hasPendingPasswordReset()) return@repeat
                delay(30)
            }

            if (routeToPasswordReset()) return@launch

            val onboardingDone = prefs.onboardingDone.first()
            if (!onboardingDone) {
                _startDestination.value = Screen.Onboarding.route
                return@launch
            }

            val loggedIn = try {
                prefs.isLoggedIn.first()
            } catch (_: Exception) {
                false
            }

            _startDestination.value = if (loggedIn) Screen.Home.route else Screen.Login.route
        }
    }

    private fun routeToPasswordReset(): Boolean {
        if (!deepLinkManager.hasPendingPasswordReset()) return false
        _startDestination.value = Screen.ResetPassword.route
        deepLinkManager.consumeUri()
        return true
    }
}
