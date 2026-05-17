package com.jadwal.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * StartupViewModel — يحدد شاشة البداية الصحيحة عند فتح التطبيق
 *
 * إصلاح المشاكل #1 و #2:
 * - المشكلة: كان يتحقق من setupDone في DataStore فقط
 *   وهذا لا يعكس حالة جلسة Supabase الحقيقية
 * - الإصلاح: يتحقق من currentUserOrNull() من Supabase مباشرة
 *
 * منطق التوجيه:
 * 1. onboardingDone = false → شاشة Onboarding (مرة واحدة فقط)
 * 2. Supabase session = null → شاشة Login
 * 3. Supabase session موجود → شاشة Home
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            // خطوة 1: هل أكمل المستخدم Onboarding؟
            val onboardingDone = prefs.onboardingDone.first()
            if (!onboardingDone) {
                _startDestination.value = Screen.Onboarding.route
                return@launch
            }

            // خطوة 2: هل توجد جلسة Supabase حقيقية؟
            val currentUser = try {
                authRepository.getCurrentUser()
            } catch (_: Exception) {
                null
            }

            _startDestination.value = if (currentUser != null) {
                Screen.Home.route
            } else {
                // المستخدم أكمل Onboarding لكن جلسته انتهت أو سجّل خروجاً
                Screen.Login.route
            }
        }
    }
}