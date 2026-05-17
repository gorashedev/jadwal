package com.jadwal.app.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    // ─── جدولة التذكير اليومي ─────────────────────────────────
    fun scheduleDailyReminder(hour: Int = 8, minute: Int = 0) {
        val delayMillis = calculateDelayMillis(hour, minute)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(false)
            .setRequiresDeviceIdle(false)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24L, TimeUnit.HOURS
        )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(DailyReminderWorker.TAG)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15L,
                TimeUnit.MINUTES,
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            DailyReminderWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork,
        )
    }

    // ─── تشغيل فوري للاختبار ──────────────────────────────────
    fun runReminderNow() {
        val oneTimeWork = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .addTag(DailyReminderWorker.TAG)
            .build()
        workManager.enqueue(oneTimeWork)
    }

    // ─── جدولة تنبيهات الامتحانات ────────────────────────────
    fun scheduleExamAlerts() {
        val delayMillis = calculateDelayMillis(hour = 7, minute = 0)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(false)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<ExamAlertWorker>(
            24L, TimeUnit.HOURS
        )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(ExamAlertWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ExamAlertWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork,
        )
    }

    fun cancelDailyReminder() {
        workManager.cancelUniqueWork(DailyReminderWorker.WORK_NAME)
    }

    fun cancelAll() {
        workManager.cancelAllWork()
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        cancelDailyReminder()
        scheduleDailyReminder(hour, minute)
    }

    private fun calculateDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
