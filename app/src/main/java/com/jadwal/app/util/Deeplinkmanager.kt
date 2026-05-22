package com.jadwal

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepLinkManager — bridges MainActivity intents and Compose navigation.
 *
 * Password-reset tokens live in the URL hash (#access_token=…). Passing them as a
 * NavHost path segment breaks routing; we store the fragment here instead.
 */
@Singleton
class DeepLinkManager @Inject constructor() {

    private val _pendingUri = MutableStateFlow<Uri?>(null)
    val pendingUri: StateFlow<Uri?> = _pendingUri.asStateFlow()

    private val _authFragment = MutableStateFlow<String?>(null)

    fun handleUri(uri: Uri) {
        _pendingUri.value = uri
        extractAuthFragment(uri)?.takeIf { it.isNotBlank() }?.let { fragment ->
            _authFragment.value = fragment
        }
    }

    fun hasPendingPasswordReset(): Boolean {
        val uri = _pendingUri.value
        if (uri != null && isPasswordResetUri(uri)) {
            val fragment = extractAuthFragment(uri)
            if (!fragment.isNullOrBlank()) return true
        }
        return !_authFragment.value.isNullOrBlank()
    }

    fun consumeUri() {
        _pendingUri.value = null
    }

    /** Returns and clears the stored Supabase auth hash fragment. */
    fun consumeAuthFragment(): String? {
        val uri = _pendingUri.value
        val fromUri = uri?.let { extractAuthFragment(it) }?.takeIf { it.isNotBlank() }
        val fragment = fromUri ?: _authFragment.value
        _authFragment.value = null
        return fragment
    }

    companion object {
        fun isPasswordResetUri(uri: Uri): Boolean {
            val host = uri.host.orEmpty()
            val raw = uri.toString()
            return host.equals("reset-password", ignoreCase = true) ||
                raw.contains("reset-password", ignoreCase = true) ||
                raw.contains("reset_password", ignoreCase = true)
        }

        /**
         * Extracts Supabase session params from hash (#…) or query (?…) segments.
         */
        fun extractAuthFragment(uri: Uri): String? {
            val raw = uri.toString()

            val fromHash = uri.fragment?.takeIf { it.isNotBlank() }
                ?: raw.substringAfter("#", "").takeIf { it.isNotBlank() }
            if (!fromHash.isNullOrBlank()) return fromHash

            val accessToken = uri.getQueryParameter("access_token")
            if (accessToken.isNullOrBlank()) return null

            return buildString {
                append("access_token=").append(accessToken)
                uri.getQueryParameter("refresh_token")?.takeIf { it.isNotBlank() }?.let {
                    append("&refresh_token=").append(it)
                }
                uri.getQueryParameter("expires_in")?.takeIf { it.isNotBlank() }?.let {
                    append("&expires_in=").append(it)
                }
                uri.getQueryParameter("token_type")?.takeIf { it.isNotBlank() }?.let {
                    append("&token_type=").append(it)
                }
                append("&type=").append(uri.getQueryParameter("type") ?: "recovery")
            }
        }
    }
}
