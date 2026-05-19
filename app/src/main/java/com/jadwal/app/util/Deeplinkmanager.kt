package com.jadwal

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepLinkManager — وسيط بين MainActivity والـ Compose
 *
 * إصلاح #3: استخدام StateFlow<Uri?> بدلاً من SharedFlow(replay=1)
 * حتى لا يُعاد إطلاق الـ URI القديم في كل فتح للتطبيق.
 * بعد معالجة الـ URI في JadwalApp تُستدعى consumeUri() لإعادة القيمة إلى null.
 */
@Singleton
class DeepLinkManager @Inject constructor() {

    private val _pendingUri = MutableStateFlow<Uri?>(null)
    val pendingUri: StateFlow<Uri?> = _pendingUri.asStateFlow()

    fun handleUri(uri: Uri) {
        _pendingUri.value = uri
    }

    fun consumeUri() {
        _pendingUri.value = null
    }
}