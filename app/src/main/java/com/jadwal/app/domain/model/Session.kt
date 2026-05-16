package com.jadwal.domain.model

/**
 * Session — جلسة مذاكرة فعلية منتهية
 */
data class Session(
    val id: String,
    val scheduleItemId: String,
    val subjectId: String,
    val startTime: Long,
    val endTime: Long = 0,
    val durationMinutes: Int = 0,
    val pomodorosCompleted: Int = 0,
    val understandingLevel: UnderstandingLevel = UnderstandingLevel.NOT_RATED,
    val notes: String = "",
) {
    val date: Long get() = startTime
    val isCompleted: Boolean get() = endTime > 0
}
