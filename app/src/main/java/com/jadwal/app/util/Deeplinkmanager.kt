package com.jadwal

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepLinkManager — وسيط بين MainActivity والـ Compose
 *
 * إصلاح #3: عندما يضغط المستخدم على رابط إعادة تعيين كلمة المرور
 * في البريد الإلكتروني، يُعيد التوجيه للتطبيق عبر:
 * com.jadwal.app://reset-password#access_token=xxx&type=recovery
 *
 * MainActivity تستقبل الـ URI وترسله هنا.
 * JadwalApp يستمع ويوجه المستخدم لشاشة ResetPassword.
 */
@Singleton
class DeepLinkManager @Inject constructor() {

    private val _pendingUri = MutableSharedFlow<Uri>(
        extraBufferCapacity = 1,
        replay = 1, // يحتفظ بآخر قيمة حتى يُجمع عليها
    )
    val pendingUri: SharedFlow<Uri> = _pendingUri

    fun handleUri(uri: Uri) {
        _pendingUri.tryEmit(uri)
    }
}