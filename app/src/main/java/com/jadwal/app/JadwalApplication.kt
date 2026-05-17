package com.jadwal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jadwal.app.notifications.JadwalNotificationManager
import com.jadwal.app.notifications.NotificationScheduler
import com.jadwal.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * JadwalApplication — إصلاح #6
 *
 * المشكلة الأصلية:
 * - createNotificationChannels() كانت تُستدعى لكن WorkManager لم يكن يعمل صح
 * - السبب: WorkManager يحتاج HiltWorkerFactory لكي يحقن الـ dependencies في الـ Workers
 * - بدونه: DailyReminderWorker يفشل لأن scheduleRepository = null
 *
 * الإصلاح:
 * - implement Configuration.Provider وتوفير HiltWorkerFactory
 * - جدولة الإشعارات بعد التأكد من الإعدادات
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

    // ─── Scope للعمليات في الخلفية ───────────────────────────
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ─── إصلاح: WorkManager يستخدم HiltWorkerFactory ────────
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // إنشاء قنوات الإشعارات (مطلوب على Android 8+)
        notificationManager.createNotificationChannels()

        // جدولة الإشعارات بناءً على الإعدادات المحفوظة
        appScope.launch {
            try {
                val notificationsEnabled = prefs.notificationsEnabled.first()
                if (notificationsEnabled) {
                    val hour = prefs.notificationHour.first()
                    val minute = prefs.notificationMinute.first()
                    notificationScheduler.scheduleDailyReminder(hour, minute)
                    notificationScheduler.scheduleExamAlerts()
                }
            } catch (_: Exception) {
                // إذا فشل القراءة، جدول بالوقت الافتراضي (8 صباحاً)
                notificationScheduler.scheduleDailyReminder(8, 0)
            }
        }
    }
}
