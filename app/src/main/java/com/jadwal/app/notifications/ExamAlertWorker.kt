package com.jadwal.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jadwal.data.repository.ExamRepository
import com.jadwal.app.notifications.JadwalNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ExamAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: JadwalNotificationManager,
    private val examRepository: ExamRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val upcomingExams = examRepository.getUpcomingExams(withinDays = 7)
            upcomingExams.forEach { exam ->
                notificationManager.showExamAlert(
                    subjectName = exam.subjectName,
                    daysUntil = exam.daysUntil,
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun getUpcomingExams(withinDays: Int) {}

    companion object {
        const val WORK_NAME = "jadwal_exam_alert"
        const val TAG = "exam_alert"
    }
}
