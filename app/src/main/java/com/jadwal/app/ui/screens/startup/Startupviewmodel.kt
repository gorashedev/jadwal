package com.jadwal.ui.navigation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.DeepLinkManager
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * StartupViewModel — يحدد شاشة البداية الصحيحة عند فتح التطبيق
 *
 * إصلاح #1 (Deep Link):
 * نتحقق من وجود رابط reset-password أولاً وإذا وُجد
 * نجعله startDestination مباشرةً بدلاً من محاولة التنقل
 * بعد رسم NavGraph (وهو ما كان يسبب الفشل الصامت).
 *
 * منطق التوجيه:
 * 0. deep link reset-password موجود → ResetPassword مباشرة
 * 1. onboardingDone = false → Onboarding
 * 2. loggedIn = false → Login
 * 3. else → Home
 */
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
            // ─── إصلاح #1: تحقق من Deep Link reset-password أولاً ──────────
            // المشكلة السابقة: كنا نحاول التنقل بعد رسم NavGraph
            //   → فكان navController لا يجد الـ destination لأن
            //     NavHost لم يُكمل compose شجرته بعد.
            // الحل: نجعل ResetPassword هو startDestination نفسه
            //   → لا يوجد navigate() ولا timing issue.
            val pendingUri = deepLinkManager.pendingUri.value
            if (pendingUri != null
                && pendingUri.scheme == "com.jadwal.app"
                && pendingUri.host == "reset-password"
            ) {
                val fragment = pendingUri.fragment ?: ""
                if (fragment.isNotBlank()) {
                    _startDestination.value = Screen.ResetPassword.createRoute(
                        Uri.encode(fragment)
                    )
                    deepLinkManager.consumeUri()
                    return@launch
                }
            }

            // ─── الدفق الطبيعي ────────────────────────────────────────────
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
}