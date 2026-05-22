package com.jadwal.domain.usecase

import com.google.gson.Gson
import com.jadwal.app.data.ai.GeminiService
import com.jadwal.app.util.LocaleHelper
import com.jadwal.data.repository.SubjectRepository
import com.jadwal.domain.model.AISuggestion
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerateAISuggestionUseCase @Inject constructor(
    private val geminiService: GeminiService,
    private val subjectRepository: SubjectRepository,
) {
    private val gson = Gson()

    suspend operator fun invoke(): AISuggestion {
        val subjects = subjectRepository.getAllSubjects().first()
        if (subjects.isEmpty()) {
            throw Exception("لا توجد مواد دراسية مضافة. يرجى إضافة مواد أولاً.")
        }

        val isEn = LocaleHelper.isEnglish()
        val subjectsInfo = subjects.joinToString("\n") {
            if (isEn) {
                "- ${it.nameEn.ifBlank { it.name }} (difficulty: ${it.difficulty}, chapters: ${it.completedChapters}/${it.totalChapters})"
            } else {
                "- ${it.name} (الصعوبة: ${it.difficulty}, الفصول: ${it.completedChapters}/${it.totalChapters})"
            }
        }

        val prompt = if (isEn) """
            You are a study scheduling expert. Based on these subjects, suggest an ideal schedule for the coming week.
            Subjects:
            $subjectsInfo

            Return JSON only (no markdown), exactly:
            {
              "schedule": [
                {
                  "id": "generate_random_uuid",
                  "subjectId": "id_of_subject",
                  "subjectName": "name_of_subject",
                  "subjectIcon": "icon_of_subject",
                  "colorHex": "color_of_subject",
                  "dayOfWeek": 0,
                  "startHour": 14,
                  "startMinute": 0,
                  "durationMinutes": 60,
                  "priority": 1
                }
              ],
              "motivationMessage": "short encouraging message in English",
              "notes": "general study tip based on subjects",
              "totalHours": 20,
              "totalPomodoros": 40
            }

            Rules:
            1. Prioritize HARD subjects.
            2. Spread sessions morning and evening.
            3. Keep Friday (day 5) light.
            4. dayOfWeek: 0=Sunday through 6=Saturday.
        """.trimIndent()
        else """
            أنت خبير في تنظيم الوقت والدراسة. بناءً على المواد الدراسية التالية، اقترح جدولاً دراسياً مثالياً للأسبوع القادم.
            المواد المتاحة:
            $subjectsInfo

            المطلوب: الرد بتنسيق JSON حصراً (بدون markdown):
            {
              "schedule": [
                {
                  "id": "generate_random_uuid",
                  "subjectId": "id_of_subject",
                  "subjectName": "name_of_subject",
                  "subjectIcon": "icon_of_subject",
                  "colorHex": "color_of_subject",
                  "dayOfWeek": 0,
                  "startHour": 14,
                  "startMinute": 0,
                  "durationMinutes": 60,
                  "priority": 1
                }
              ],
              "motivationMessage": "رسالة تشجيعية قصيرة بالعربية",
              "notes": "نصيحة دراسية عامة بناءً على المواد",
              "totalHours": 20,
              "totalPomodoros": 40
            }

            قواعد:
            1. التركيز أكثر على المواد ذات الصعوبة العالية (HARD).
            2. توزيع الجلسات في أوقات منطقية.
            3. يوم الجمعة (5) يجب أن يكون خفيفاً.
        """.trimIndent()

        try {
            val responseText = geminiService.generateText(prompt)
                .ifBlank { throw Exception("لم يتم استلام رد من الذكاء الاصطناعي") }
            
            // تنظيف النص من markdown إذا وجد
            val jsonText = responseText.replace("```json", "")
                                     .replace("```", "")
                                     .trim()
            
            return gson.fromJson(jsonText, AISuggestion::class.java)
        } catch (e: Exception) {
            throw Exception("خطأ في توليد الاقتراح: ${e.localizedMessage}")
        }
    }
}
