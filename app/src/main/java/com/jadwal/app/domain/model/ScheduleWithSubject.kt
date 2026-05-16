package com.jadwal.domain.model

import java.util.Calendar

/**
 * ScheduleWithSubject — يجمع بيانات الجلسة مع بيانات المادة
 */
data class ScheduleWithSubject(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val subjectIcon: String,
    val subjectColor: String,
    val scheduledDate: Long,
    val allocatedMinutes: Int,
    val actualMinutes: Int,
    val isCompleted: Boolean,
    val priority: Int,
    val studyPhase: StudyPhase
) {
    val dayOfWeek: Int
        get() {
            val cal = Calendar.getInstance()
            cal.timeInMillis = scheduledDate
            return cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
        }

    val startHour: Int
        get() {
            val cal = Calendar.getInstance()
            cal.timeInMillis = scheduledDate
            return cal.get(Calendar.HOUR_OF_DAY)
        }

    val startMinute: Int
        get() {
            val cal = Calendar.getInstance()
            cal.timeInMillis = scheduledDate
            return cal.get(Calendar.MINUTE)
        }

    val completedMinutes: Int
        get() = if (isCompleted) actualMinutes else 0

    val weekOfMonth: Int
        get() {
            val cal = Calendar.getInstance()
            cal.timeInMillis = scheduledDate
            return cal.get(Calendar.WEEK_OF_MONTH)
        }
}
