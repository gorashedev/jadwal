package com.jadwal.domain.model

/**
 * النموذج الكامل لاقتراح الذكاء الاصطناعي
 */
data class AISuggestion(
    val schedule: List<ScheduleSlot>,
    val motivationMessage: String,
    val notes: String,
    val totalHours: Int,
    val totalPomodoros: Int,
)

/**
 * جلسة مذاكرة مقترحة من الـ AI
 */
data class ScheduleSlot(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val subjectIcon: String,
    val colorHex: String,
    val dayOfWeek: Int,          // 0=الأحد .. 6=السبت
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val priority: Int,           // 1=عالي، 2=متوسط، 3=منخفض
)

/**
 * ملخص حالة الطالب لطلب اقتراح ذكي
 */
data class StudentSummary(
    val subjects: List<String>,
    val weeklyHours: Int,
    val hardestSubject: String,
    val daysUntilNextExam: Int,
    val averageUnderstanding: Float
)
