package com.jadwal.app.data.repository

import android.net.Uri
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
    private val supabase: SupabaseClient
) {
    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String, data: JsonObject? = null) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = data
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    // ─── إصلاح #3: إضافة redirectUrl لتوجيه المستخدم للتطبيق بعد الضغط على الرابط ───
    suspend fun resetPassword(email: String) {
        supabase.auth.resetPasswordForEmail(
            email = email,
            redirectUrl = "com.jadwal.app://reset-password",
        )
    }

    // ─── إصلاح #3: استيراد الجلسة من الـ fragment في الـ Deep Link ───
    // Supabase يُرسل: com.jadwal.app://reset-password#access_token=xxx&type=recovery
    // هذه الدالة تأخذ الـ fragment (بعد #) وتُعيد بناء الجلسة
    suspend fun importSessionFromFragment(fragment: String) {
        val session = supabase.auth.parseSessionFromFragment(fragment)
        supabase.auth.importSession(session)
    }

    // ─── إصلاح #3: تحديث كلمة المرور بعد التحقق من الـ Deep Link ───
    suspend fun updatePassword(newPassword: String) {
        supabase.auth.updateUser {
            password = newPassword
        }
    }

    fun getCurrentUser(): UserInfo? = supabase.auth.currentUserOrNull()
}