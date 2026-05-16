package com.jadwal.domain.usecase

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.jadwal.data.repository.SubjectRepository
import com.jadwal.domain.model.AISuggestion
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerateAISuggestionUseCase @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val subjectRepository: SubjectRepository,
) {
    private val gson = Gson()

    suspend operator fun invoke(): AISuggestion {
        val subjects = subjectRepository.getAllSubjects().first()
        if (subjects.isEmpty()) {
            throw Exception("لا توجد مواد دراسية مضافة. يرجى إضافة مواد أولاً.")
        }

        val subjectsInfo = subjects.joinToString("\n") { 
            "- ${it.name} (الصعوبة: ${it.difficulty}, الفصول المكتملة: ${it.completedChapters}/${it.totalChapters}, الأيقونة: ${it.icon}, اللون: ${it.colorHex})"
        }

        val prompt = """
            أنت خبير في تنظيم الوقت والدراسة. بناءً على المواد الدراسية التالية، اقترح جدولاً دراسياً مثالياً للأسبوع القادم.
            المواد المتاحة:
            $subjectsInfo
            
            المطلوب:
            توزيع جلسات المذاكرة على مدار الأسبوع (0 للأحد إلى 6 للسبت).
            يجب أن يكون الرد بتنسيق JSON حصراً كالتالي (بدون أي نص إضافي):
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
            2. توزيع الجلسات في أوقات منطقية (صباحاً ومساءً).
            3. يوم الجمعة (5) يجب أن يكون خفيفاً.
            4. الالتزام التام بتنسيق الـ JSON.
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(content { text(prompt) })
            val responseText = response.text ?: throw Exception("لم يتم استلام رد من الذكاء الاصطناعي")
            
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
