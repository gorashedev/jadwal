package com.jadwal.domain.model

/**
 * Exam — موديل الامتحان
 */
data class Exam(
    val id: String,
    val subjectId: String,
    val subjectName: String = "",
    val examDate: Long,         // Unix timestamp بالمللي ثانية
    val location: String = "",
    val notes: String = "",
) {
    /** عدد الأيام المتبقية للامتحان من الآن */
    val daysUntil: Int
        get() {
            val now = System.currentTimeMillis()
            val diff = examDate - now
            return if (diff <= 0) 0
            else (diff / (1000L * 60 * 60 * 24)).toInt()
        }

    val isPassed: Boolean
        get() = System.currentTimeMillis() > examDate
}
