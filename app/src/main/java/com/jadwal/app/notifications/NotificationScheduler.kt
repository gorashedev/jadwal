package com.jadwal.notifications

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationScheduler — نقطة التحكم الموحّدة لجدولة جميع الإشعارات.
 *
 * استخدام:
 *   scheduler.scheduleDailyReminder(hour = 8, minute = 0)
 *   scheduler.scheduleExamAlerts()
 *   scheduler.cancelAll()
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    // ===== جدولة التذكير اليومي =====
    fun scheduleDailyReminder(hour: Int = 8, minute: Int = 0) {
        // حساب الوقت المتبقي حتى الوقت المحدد
        val delayMillis = calculateDelayMillis(hour, minute)

        // قيود التشغيل: الشبكة غير مطلوبة
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        // العمل الدوري كل 24 ساعة
        val periodicWork = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 30,
            flexTimeIntervalUnit = TimeUnit.MINUTES,
        )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(DailyReminderWorker.TAG)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            DailyReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // تحديث الجدول إذا تغيّر الوقت
            periodicWork,
        )
    }

    // ===== جدولة تنبيهات الامتحانات =====
    fun scheduleExamAlerts() {
        // تعمل كل يوم في الصباح (6 ص) وتتحقق إذا كان هناك امتحان خلال 7 أيام
        val delayMillis = calculateDelayMillis(hour = 7, minute = 0)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<ExamAlertWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(ExamAlertWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ExamAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork,
        )
    }

    // ===== إلغاء التذكير اليومي فقط =====
    fun cancelDailyReminder() {
        workManager.cancelUniqueWork(DailyReminderWorker.WORK_NAME)
    }

    // ===== إلغاء جميع الإشعارات =====
    fun cancelAll() {
        workManager.cancelAllWork()
    }

    // ===== تحديث وقت التذكير =====
    fun updateReminderTime(hour: Int, minute: Int) {
        // إلغاء القديم وإعادة جدولة بالوقت الجديد
        cancelDailyReminder()
        scheduleDailyReminder(hour, minute)
    }

    // ===== حساب التأخير حتى وقت محدد =====
    private fun calculateDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // إذا مرّ الوقت المحدد اليوم، ابدأ غداً
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
