package com.jadwal.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NotificationPermissionViewModel — يُدير حالة إذن الإشعارات ويوفّر
 * وصولاً مريحاً للجدولة بعد المنح.
 *
 * يُستخدم مع NotificationPermissionHandler في HomeScreen.
 */
@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    // هل تم التعامل مع الإذن في هذه الجلسة؟
    private val _permissionHandled = MutableStateFlow(false)
    val permissionHandled = _permissionHandled.asStateFlow()

    // ===== يُستدعى عند منح المستخدم الإذن =====
    fun onPermissionGranted() {
        _permissionHandled.value = true
        viewModelScope.launch {
            // حفظ أن الإشعارات مُفعَّلة
            val hour = prefs.notificationHour.first()
            val minute = prefs.notificationMinute.first()
            prefs.setNotificationSettings(
                enabled = true,
                hour = hour,
                minute = minute,
            )
            // جدولة التذكير اليومي وتنبيهات الامتحانات
            scheduler.scheduleDailyReminder(hour, minute)
            scheduler.scheduleExamAlerts()
        }
    }

    // ===== يُستدعى عند رفض المستخدم =====
    fun onPermissionDenied() {
        _permissionHandled.value = true
        viewModelScope.launch {
            prefs.setNotificationSettings(
                enabled = false,
                hour = prefs.notificationHour.first(),
                minute = prefs.notificationMinute.first(),
            )
        }
    }
}
