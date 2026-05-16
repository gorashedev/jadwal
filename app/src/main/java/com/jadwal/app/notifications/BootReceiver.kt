package com.jadwal.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jadwal.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BootReceiver — يُعيد جدولة الإشعارات بعد إعادة تشغيل الجهاز.
 * WorkManager يُعيد الجدولة تلقائياً في Android 12+، لكن هذا Receiver
 * يضمن التوافق مع الإصدارات الأقدم.
 *
 * يجب إضافته في AndroidManifest.xml:
 * <receiver
 *     android:name=".notifications.BootReceiver"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.BOOT_COMPLETED"/>
 *         <action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>
 *     </intent-filter>
 * </receiver>
 *
 * والإذن في AndroidManifest.xml:
 * <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: NotificationScheduler

    @Inject
    lateinit var prefs: UserPreferencesDataStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            scope.launch {
                val notificationsEnabled = prefs.notificationsEnabled.first()
                if (notificationsEnabled) {
                    val hour = prefs.notificationHour.first()
                    val minute = prefs.notificationMinute.first()
                    scheduler.scheduleDailyReminder(hour, minute)
                    scheduler.scheduleExamAlerts()
                }
            }
        }
    }
}
