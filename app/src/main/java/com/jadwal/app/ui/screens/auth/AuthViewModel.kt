package com.jadwal.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val emailSent: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    // ===== تسجيل الدخول =====
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "يرجى ملء جميع الحقول") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.login(email.trim(), password)
                prefs.setLoggedIn(true)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mapLoginError(e)) }
            }
        }
    }

    // ===== إنشاء حساب جديد =====
    fun register(name: String, email: String, password: String, confirmPassword: String) {
        when {
            name.isBlank() || email.isBlank() || password.isBlank() -> {
                _uiState.update { it.copy(error = "يرجى ملء جميع الحقول") }
                return
            }
            password != confirmPassword -> {
                _uiState.update { it.copy(error = "كلمتا المرور غير متطابقتان") }
                return
            }
            password.length < 6 -> {
                _uiState.update { it.copy(error = "كلمة المرور يجب أن تكون 6 أحرف على الأقل") }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userData = buildJsonObject { put("full_name", JsonPrimitive(name.trim())) }
                authRepository.signUp(email.trim(), password, userData)
                prefs.setUserName(name.trim())
                prefs.setLoggedIn(true)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mapRegisterError(e)) }
            }
        }
    }

    // ===== استعادة كلمة المرور =====
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "يرجى إدخال بريدك الإلكتروني") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.resetPassword(email.trim())
                _uiState.update { it.copy(isLoading = false, emailSent = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "تعذر إرسال البريد، تحقق من العنوان")
                }
            }
        }
    }

    // ─── إصلاح #3: استيراد جلسة Supabase من الـ Deep Link ───────────────
    fun importSessionFromDeepLink(fragment: String) {
        viewModelScope.launch {
            try {
                authRepository.importSessionFromFragment(fragment)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "رابط إعادة التعيين غير صالح أو انتهت صلاحيته")
                }
            }
        }
    }

    // ─── إصلاح #3: تحديث كلمة المرور ─────────────────────────────────────
    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.updatePassword(newPassword)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "تعذر تحديث كلمة المرور. حاول طلب رابط جديد."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ─── دوال مساعدة لرسائل الخطأ ────────────────────────────────────────

    private fun mapLoginError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("Invalid login credentials") -> "البريد الإلكتروني أو كلمة المرور غير صحيحة"
            msg.contains("Email not confirmed")       -> "يرجى تأكيد بريدك الإلكتروني أولاً"
            msg.contains("network")                   -> "تأكد من اتصالك بالإنترنت"
            else                                      -> "حدث خطأ، حاول مجدداً"
        }
    }

    private fun mapRegisterError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("already registered") -> "هذا البريد الإلكتروني مسجل بالفعل"
            msg.contains("invalid email")      -> "صيغة البريد الإلكتروني غير صحيحة"
            else                               -> "حدث خطأ أثناء إنشاء الحساب"
        }
    }
}

private fun buildJsonObject(block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) =
    kotlinx.serialization.json.buildJsonObject(block)