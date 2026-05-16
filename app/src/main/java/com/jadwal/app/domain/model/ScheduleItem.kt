package com.jadwal.domain.model

/**
 * ScheduleItem — عنصر واحد في الجدول (جلسة مخططة)
 */
data class ScheduleItem(
    val id: String,
    val subjectId: String,
    val scheduledDate: Long,        // Unix timestamp
    val allocatedMinutes: Int,      // الوقت المخطط
    val actualMinutes: Int = 0,     // الوقت الفعلي بعد الجلسة
    val understandingLevel: UnderstandingLevel = UnderstandingLevel.NOT_RATED,
    val isCompleted: Boolean = false,
    val isMissed: Boolean = false,
    val priority: Int = 0,          // أعلى = أهم
    val studyPhase: StudyPhase = StudyPhase.WORK,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class StudyPhase {
    WORK,       // مذاكرة عادية
    REVIEW,     // مراجعة
    EXAM_PREP;  // استعداد للامتحان

    companion object {
        fun fromString(value: String): StudyPhase =
            entries.find { it.name == value } ?: WORK
    }
}
