package com.jadwal.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.jadwal.data.repository.ScheduleRepository
import java.util.concurrent.TimeUnit

/**
 * DailyReminderWorker — يعمل في الخلفية حتى عندما يكون التطبيق مغلقاً.
 * يتحقق من مهام اليوم ثم يُرسل إشعار التذكير.
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: JadwalNotificationManager,
    private val scheduleRepository: ScheduleRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // جلب مهام اليوم من قاعدة البيانات
            val todayTasks = scheduleRepository.getTodayScheduleWithSubjects()
            val pendingTasks = todayTasks.filter { !it.isCompleted }

            val firstSubject = pendingTasks.firstOrNull()?.subjectName ?: ""
            val tasksCount = pendingTasks.size

            // إرسال إشعار التذكير اليومي
            notificationManager.showDailyReminder(
                subjectName = firstSubject,
                tasksCount = tasksCount,
            )
            Result.success()
        } catch (e: Exception) {
            // إعادة المحاولة مرة واحدة فقط عند الفشل
            if (runAttemptCount < 1) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "jadwal_daily_reminder"
        const val TAG = "daily_reminder"
    }
}
