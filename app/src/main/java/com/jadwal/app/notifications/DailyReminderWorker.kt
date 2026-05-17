package com.jadwal.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jadwal.data.repository.ScheduleRepository
import com.jadwal.app.notifications.JadwalNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: JadwalNotificationManager,
    private val scheduleRepository: ScheduleRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val todayTasks = scheduleRepository.getTodayScheduleWithSubjects()
            val pendingTasks = todayTasks.filter { !it.isCompleted }
            val firstSubject = pendingTasks.firstOrNull()?.subjectName ?: ""
            val tasksCount = pendingTasks.size

            notificationManager.showDailyReminder(
                subjectName = firstSubject,
                tasksCount = tasksCount,
            )
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 1) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "jadwal_daily_reminder"
        const val TAG = "daily_reminder"
    }
}
