package com.jadwal.data.repository

import com.jadwal.app.data.ai.GeminiService
import com.jadwal.app.util.LocaleHelper
import com.jadwal.domain.model.StudentSummary
import javax.inject.Inject
import javax.inject.Singleton

interface AIRepository {
    suspend fun getSmartSuggestion(summary: StudentSummary): String
}

@Singleton
class AIRepositoryImpl @Inject constructor(
    private val geminiService: GeminiService,
) : AIRepository {

    override suspend fun getSmartSuggestion(summary: StudentSummary): String {
        val isEn = LocaleHelper.isEnglish()

        val prompt = if (isEn) """
            You are an educational expert. Based on the student's summary, provide one short, encouraging tip (max 20 words).
            Summary:
            - Subjects: ${summary.subjects.joinToString()}
            - Weekly study hours: ${summary.weeklyHours}
            - Hardest subject: ${summary.hardestSubject}
            - Days until next exam: ${summary.daysUntilNextExam}
            - Average understanding: ${summary.averageUnderstanding}/4
        """.trimIndent()
        else """
            أنت خبير تعليمي. بناءً على ملخص الطالب التالي، قدم نصيحة واحدة قصيرة ومشجعة (لا تزيد عن 20 كلمة).
            الملخص:
            - المواد: ${summary.subjects.joinToString()}
            - ساعات الدراسة الأسبوعية: ${summary.weeklyHours}
            - أصعب مادة: ${summary.hardestSubject}
            - الأيام المتبقية لأقرب امتحان: ${summary.daysUntilNextExam}
            - متوسط مستوى الفهم: ${summary.averageUnderstanding}/4
        """.trimIndent()

        return try {
            geminiService.generateText(prompt).ifBlank { fallbackSuggestion() }
        } catch (_: Exception) {
            fallbackSuggestion()
        }
    }

    private fun fallbackSuggestion(): String =
        if (LocaleHelper.isEnglish()) "Stay focused on your goals; success requires consistency."
        else "ركز على أهدافك اليوم، النجاح يتطلب الاستمرارية."
}
