package com.jadwal.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.app.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NotificationPermissionViewModel
 *
 * إصلاح: حفظ حالة "تم التعامل مع الإذن" في DataStore
 * حتى لا يتكرر طلب الإذن في كل مرة يرجع المستخدم للشاشة الرئيسية
 */
@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val scheduler: NotificationScheduler,
) : ViewModel() {

    private val _permissionHandled = MutableStateFlow(false)
    val permissionHandled = _permissionHandled.asStateFlow()

    init {
        // تحميل الحالة المحفوظة من DataStore عند إنشاء الـ ViewModel
        viewModelScope.launch {
            val alreadyAsked = prefs.notificationPermissionAsked.first()
            if (alreadyAsked) {
                _permissionHandled.value = true
            }
        }
    }

    fun onPermissionGranted() {
        _permissionHandled.value = true
        viewModelScope.launch {
            // حفظ أن الإذن طُلب ومُنح
            prefs.setNotificationPermissionAsked(true)
            val hour = prefs.notificationHour.first()
            val minute = prefs.notificationMinute.first()
            prefs.setNotificationSettings(enabled = true, hour = hour, minute = minute)
            scheduler.scheduleDailyReminder(hour, minute)
            scheduler.scheduleExamAlerts()
        }
    }

    fun onPermissionDenied() {
        _permissionHandled.value = true
        viewModelScope.launch {
            // حفظ أن الإذن طُلب ورُفض — لا نسأل مرة ثانية
            prefs.setNotificationPermissionAsked(true)
            prefs.setNotificationSettings(
                enabled = false,
                hour = prefs.notificationHour.first(),
                minute = prefs.notificationMinute.first(),
            )
        }
    }
}
