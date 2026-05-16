package com.jadwal.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val emailSent: Boolean = false, // لشاشة نسيت كلمة السر
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabase: SupabaseClient,
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
                supabase.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("Invalid login credentials") == true ->
                        "البريد الإلكتروني أو كلمة المرور غير صحيحة"
                    e.message?.contains("Email not confirmed") == true ->
                        "يرجى تأكيد بريدك الإلكتروني أولاً"
                    e.message?.contains("network") == true ->
                        "تأكد من اتصالك بالإنترنت"
                    else -> "حدث خطأ، حاول مجدداً"
                }
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
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
                supabase.auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                    data = buildJsonObject {
                        put("full_name", name.trim())
                    }
                }
                // حفظ اسم المستخدم محلياً
                prefs.setUserName(name.trim())
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("already registered") == true ->
                        "هذا البريد الإلكتروني مسجل بالفعل"
                    e.message?.contains("invalid email") == true ->
                        "صيغة البريد الإلكتروني غير صحيحة"
                    else -> "حدث خطأ أثناء إنشاء الحساب"
                }
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
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
                supabase.auth.resetPasswordForEmail(email.trim())
                _uiState.update { it.copy(isLoading = false, emailSent = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "تعذر إرسال البريد، تحقق من العنوان"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// دالة مساعدة لبناء JSON بدون import إضافي
private fun buildJsonObject(block: JsonObjectBuilder.() -> Unit): kotlinx.serialization.json.JsonObject {
    return kotlinx.serialization.json.buildJsonObject(block)
}

private typealias JsonObjectBuilder = kotlinx.serialization.json.JsonObjectBuilder
