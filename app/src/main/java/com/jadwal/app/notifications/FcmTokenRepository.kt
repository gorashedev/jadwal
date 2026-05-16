package com.jadwal.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FcmTokenRepository — يحفظ رمز FCM في Supabase ليتمكن الـ Server
 * من إرسال إشعارات مخصصة لهذا المستخدم.
 */
@Singleton
class FcmTokenRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ===== حفظ رمز FCM في Supabase =====
    fun saveToken(token: String) {
        scope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                supabase.postgrest["fcm_tokens"].upsert(
                    mapOf(
                        "user_id" to userId,
                        "token" to token,
                        "platform" to "android",
                    )
                )
                Log.d("FcmToken", "✅ Token saved: ${token.take(20)}...")
            } catch (e: Exception) {
                Log.e("FcmToken", "❌ Failed to save token: ${e.message}")
            }
        }
    }

    // ===== جلب رمز FCM الحالي وحفظه =====
    fun refreshAndSaveToken() {
        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                saveToken(token)
            } catch (e: Exception) {
                Log.e("FcmToken", "❌ Failed to get FCM token: ${e.message}")
            }
        }
    }
}
