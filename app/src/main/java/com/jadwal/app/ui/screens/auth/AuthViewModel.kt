package com.jadwal.ui.screens.auth

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.R
import com.jadwal.DeepLinkManager
import com.jadwal.app.data.repository.AuthRepository
import com.jadwal.app.data.repository.SignUpOutcome
import com.jadwal.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val emailAlreadyExists: Boolean = false,
    val pendingEmailConfirmation: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: UserPreferencesDataStore,
    private val deepLinkManager: DeepLinkManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    fun consumeRecoveryFragment(): String? = deepLinkManager.consumeAuthFragment()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_fill_all_fields)) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null, emailAlreadyExists = false, pendingEmailConfirmation = false)
            }
            try {
                authRepository.login(email.trim(), password)
                prefs.setLoggedIn(true)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mapLoginError(e)) }
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        when {
            name.isBlank() || email.isBlank() || password.isBlank() -> {
                _uiState.update {
                    it.copy(
                        error = context.getString(R.string.error_fill_all_fields),
                        emailAlreadyExists = false,
                        pendingEmailConfirmation = false,
                    )
                }
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                _uiState.update {
                    it.copy(
                        error = context.getString(R.string.error_invalid_email),
                        emailAlreadyExists = false,
                        pendingEmailConfirmation = false,
                    )
                }
                return
            }
            password != confirmPassword -> {
                _uiState.update {
                    it.copy(
                        error = context.getString(R.string.error_passwords_mismatch),
                        emailAlreadyExists = false,
                        pendingEmailConfirmation = false,
                    )
                }
                return
            }
            password.length < 6 -> {
                _uiState.update {
                    it.copy(
                        error = context.getString(R.string.error_password_too_short),
                        emailAlreadyExists = false,
                        pendingEmailConfirmation = false,
                    )
                }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    isSuccess = false,
                    emailAlreadyExists = false,
                    pendingEmailConfirmation = false,
                )
            }
            try {
                val userData = buildJsonObject { put("full_name", JsonPrimitive(name.trim())) }
                when (authRepository.signUp(email.trim(), password, userData)) {
                    SignUpOutcome.SuccessWithSession -> {
                        prefs.setUserName(name.trim())
                        prefs.setLoggedIn(true)
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    }
                    SignUpOutcome.EmailConfirmationRequired -> {
                        prefs.setUserName(name.trim())
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingEmailConfirmation = true,
                                error = context.getString(R.string.error_email_confirmation_required),
                            )
                        }
                    }
                    SignUpOutcome.EmailAlreadyRegistered -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = context.getString(R.string.error_email_already_registered),
                                emailAlreadyExists = true,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                val isDuplicate = isDuplicateRegisterError(e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (isDuplicate) {
                            context.getString(R.string.error_email_already_registered)
                        } else {
                            mapRegisterError(e)
                        },
                        emailAlreadyExists = isDuplicate,
                    )
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_enter_email)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.resetPassword(email.trim())
                _uiState.update { it.copy(isLoading = false, emailSent = true) }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = context.getString(R.string.error_reset_email_failed))
                }
            }
        }
    }

    fun importSessionFromDeepLink(fragment: String) {
        viewModelScope.launch {
            try {
                authRepository.importSessionFromFragment(fragment)
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(error = context.getString(R.string.error_reset_link_invalid))
                }
            }
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.updatePassword(newPassword)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = context.getString(R.string.error_update_password_failed))
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, emailAlreadyExists = false, pendingEmailConfirmation = false) }
    }

    private fun mapLoginError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("Invalid login credentials") ->
                context.getString(R.string.error_invalid_credentials)
            msg.contains("Email not confirmed") ->
                context.getString(R.string.error_email_not_confirmed)
            msg.contains("network", ignoreCase = true) ->
                context.getString(R.string.error_no_internet)
            else -> context.getString(R.string.error_generic)
        }
    }

    private fun mapRegisterError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("invalid email", ignoreCase = true) ->
                context.getString(R.string.error_invalid_email)
            else -> context.getString(R.string.error_register_generic)
        }
    }

    private fun isDuplicateRegisterError(e: Exception): Boolean {
        val msg = e.message.orEmpty()
        return msg.contains("already registered", ignoreCase = true) ||
            msg.contains("already exists", ignoreCase = true) ||
            msg.contains("duplicate", ignoreCase = true) ||
            msg.contains("email address is already", ignoreCase = true) ||
            msg.contains("user_already_exists", ignoreCase = true)
    }
}

private fun buildJsonObject(block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) =
    kotlinx.serialization.json.buildJsonObject(block)
