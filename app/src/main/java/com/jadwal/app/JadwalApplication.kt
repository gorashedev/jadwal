package com.jadwal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jadwal.notifications.JadwalNotificationManager
import com.jadwal.notifications.NotificationScheduler
import com.jadwal.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * JadwalApplication — نقطة بدء التطبيق.
 *
 * مهام التهيئة:
 * 1. إنشاء قنوات الإشعارات (مطلوب لـ Android 8+)
 * 2. تمرير HiltWorkerFactory إلى WorkManager (مطلوب لـ @HiltWorker)
 * 3. جدولة الإشعارات الدورية إذا كانت مُفعَّلة
 *
 * تأكد في build.gradle.kts أن اسم هذا الكلاس مُعيَّن في AndroidManifest.xml:
 * <application android:name=".JadwalApplication" ...>
 */
@HiltAndroidApp
class JadwalApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationManager: JadwalNotificationManager

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var prefs: UserPreferencesDataStore

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ===== تمرير HiltWorkerFactory — مطلوب لكي يعمل @HiltWorker =====
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // إنشاء قنوات الإشعارات مرة واحدة عند بدء التطبيق
        notificationManager.createNotificationChannels()
        // جدولة الإشعارات بناءً على تفضيلات المستخدم
        initNotifications()
    }

    private fun initNotifications() {
        applicationScope.launch {
            val enabled = prefs.notificationsEnabled.first()
            if (enabled) {
                val hour = prefs.notificationHour.first()
                val minute = prefs.notificationMinute.first()
                notificationScheduler.scheduleDailyReminder(hour, minute)
                notificationScheduler.scheduleExamAlerts()
            }
        }
    }
}
