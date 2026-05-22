package com.jadwal.app.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.parseSessionFromFragment
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Registers a new user and classifies the Supabase response.
     * Clears any stale session first so duplicate sign-ups cannot appear as success.
     */
    suspend fun signUp(email: String, password: String, data: JsonObject? = null): SignUpOutcome {
        runCatching { supabase.auth.signOut() }

        try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = data
            }
        } catch (e: Exception) {
            if (isDuplicateSignUpError(e)) {
                runCatching { supabase.auth.signOut() }
                return SignUpOutcome.EmailAlreadyRegistered
            }
            throw e
        }

        val session = supabase.auth.currentSessionOrNull()
        val user = supabase.auth.currentUserOrNull()

        if (user != null && user.identities.isNullOrEmpty()) {
            runCatching { supabase.auth.signOut() }
            return SignUpOutcome.EmailAlreadyRegistered
        }

        if (session != null) {
            return SignUpOutcome.SuccessWithSession
        }

        if (user == null) {
            return SignUpOutcome.EmailAlreadyRegistered
        }

        // user present, no session — distinguish new (needs confirmation) vs existing email
        return when (probeAccountAfterSignUp(email, password)) {
            AccountProbe.NewUserNeedsConfirmation -> SignUpOutcome.EmailConfirmationRequired
            AccountProbe.ExistingAccount -> {
                runCatching { supabase.auth.signOut() }
                SignUpOutcome.EmailAlreadyRegistered
            }
        }
    }

    fun hasActiveSession(): Boolean = supabase.auth.currentSessionOrNull() != null

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    suspend fun resetPassword(email: String) {
        supabase.auth.resetPasswordForEmail(
            email = email,
            redirectUrl = "com.jadwal.app://reset-password",
        )
    }

    suspend fun importSessionFromFragment(fragment: String) {
        val session = supabase.auth.parseSessionFromFragment(fragment)
        supabase.auth.importSession(session)
    }

    suspend fun updatePassword(newPassword: String) {
        supabase.auth.updateUser {
            password = newPassword
        }
    }

    fun getCurrentUser(): UserInfo? = supabase.auth.currentUserOrNull()

    /**
     * After sign-up without a session, attempt sign-in to tell apart:
     * - brand-new unconfirmed user ("Email not confirmed")
     * - existing account (successful sign-in or invalid credentials)
     */
    private suspend fun probeAccountAfterSignUp(email: String, password: String): AccountProbe {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            if (supabase.auth.currentSessionOrNull() != null) {
                AccountProbe.ExistingAccount
            } else {
                AccountProbe.NewUserNeedsConfirmation
            }
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            when {
                msg.contains("Email not confirmed", ignoreCase = true) ->
                    AccountProbe.NewUserNeedsConfirmation
                msg.contains("Invalid login credentials", ignoreCase = true) ->
                    AccountProbe.ExistingAccount
                isDuplicateSignUpError(e) ->
                    AccountProbe.ExistingAccount
                else -> AccountProbe.NewUserNeedsConfirmation
            }
        }
    }

    private fun isDuplicateSignUpError(e: Throwable): Boolean {
        val msg = buildThrowableMessages(e).joinToString(" ")
        return msg.contains("already registered", ignoreCase = true) ||
            msg.contains("User already registered", ignoreCase = true) ||
            msg.contains("already exists", ignoreCase = true) ||
            msg.contains("duplicate", ignoreCase = true) ||
            msg.contains("email address is already", ignoreCase = true) ||
            msg.contains("user_already_exists", ignoreCase = true)
    }

    private fun buildThrowableMessages(e: Throwable): List<String> {
        val messages = mutableListOf<String>()
        var current: Throwable? = e
        while (current != null) {
            current.message?.takeIf { it.isNotBlank() }?.let { messages.add(it) }
            current = current.cause
        }
        return messages
    }

    private enum class AccountProbe {
        NewUserNeedsConfirmation,
        ExistingAccount,
    }
}
