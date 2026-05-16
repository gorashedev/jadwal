package com.jadwal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jadwal.notifications.JadwalNotificationManager
import com.jadwal.notifications.NotificationScheduler
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.util.LanguageManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * JadwalApplication — نقطة بدء التطبيق.
 *
 * مهام التهيئة:
 * 1. استعادة اللغة المحفوظة وتطبيقها فوراً قبل أي Activity
 * 2. إنشاء قنوات الإشعارات (مطلوب لـ Android 8+)
 * 3. تمرير HiltWorkerFactory إلى WorkManager (مطلوب لـ @HiltWorker)
 * 4. جدولة الإشعارات الدورية إذا كانت مُفعَّلة
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // ===== استعادة اللغة قبل كل شيء =====
        // runBlocking مقبول هنا لأنه في onCreate() ويجب تطبيق اللغة قبل أي نشاط
        restoreLanguage()

        notificationManager.createNotificationChannels()
        initNotifications()
    }

    /**
     * يقرأ اللغة المحفوظة من DataStore ويطبّقها عبر AppCompatDelegate.
     * هذا يضمن أن اللغة المختارة تُستعاد في كل مرة يُفتح فيها التطبيق.
     */
    private fun restoreLanguage() {
        try {
            val savedCode = runBlocking(Dispatchers.IO) { prefs.languageCode.first() }
            if (savedCode.isNotEmpty()) {
                LanguageManager.setAppLocale(savedCode)
            }
        } catch (_: Exception) {
            // تجاهل الخطأ — يبقى التطبيق على اللغة الافتراضية
        }
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
