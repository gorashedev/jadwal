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
        val prompt = """
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
            response.text?.trim() ?: "استمر في التقدم، أنت تبلي بلاءً حسناً!"
        } catch (e: Exception) {
            "ركز على أهدافك اليوم، النجاح يتطلب الاستمرارية."
        }
    }
}
