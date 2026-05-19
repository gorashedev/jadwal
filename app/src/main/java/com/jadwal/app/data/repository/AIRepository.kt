package com.jadwal.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.jadwal.domain.model.StudentSummary
import javax.inject.Inject
import javax.inject.Singleton

interface AIRepository {
    suspend fun getSmartSuggestion(summary: StudentSummary): String
}

@Singleton
class AIRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel
) : AIRepository {
    override suspend fun getSmartSuggestion(summary: StudentSummary): String {
        val isEn = java.util.Locale.getDefault().language == "en"
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
            val response = generativeModel.generateContent(content { text(prompt) })
            response.text?.trim()
                ?: if (isEn) "Keep going, you're doing great!" else "استمر في التقدم، أنت تبلي بلاءً حسناً!"
        } catch (e: Exception) {
            if (isEn) "Stay focused on your goals; success requires consistency." else "ركز على أهدافك اليوم، النجاح يتطلب الاستمرارية."
        }
    }
}