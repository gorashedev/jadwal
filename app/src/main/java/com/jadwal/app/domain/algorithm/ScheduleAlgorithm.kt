package com.jadwal.domain.algorithm

import com.jadwal.domain.model.Difficulty
import com.jadwal.domain.model.ScheduleSlot
import com.jadwal.domain.model.StudyTime
import com.jadwal.domain.model.Subject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface ScheduleAlgorithm {
    fun generateSchedule(
        subjects: List<Subject>,
        dailyHours: Int,
        preferredTime: StudyTime
    ): List<ScheduleSlot>
}

/**
 * DefaultScheduleAlgorithm — خوارزمية توليد الجدول الأسبوعي
 *
 * تعمل بالطريقة التالية:
 * - أيام الدراسة: الأحد إلى الخميس (0-4)
 * - توزيع الوقت نسبياً بحسب صعوبة المادة (صعبة = وقت أكثر)
 * - وقت البدء حسب تفضيل المستخدم (صباح = 8ص، مساء = 7م)
 * - كل جلسة بين 30 و 120 دقيقة
 * - استراحة 15 دقيقة بين الجلسات
 */
@Singleton
class DefaultScheduleAlgorithm @Inject constructor() : ScheduleAlgorithm {

    override fun generateSchedule(
        subjects: List<Subject>,
        dailyHours: Int,
        preferredTime: StudyTime
    ): List<ScheduleSlot> {
        if (subjects.isEmpty()) return emptyList()

        val startHour = when (preferredTime) {
            StudyTime.MORNING -> 8
            StudyTime.EVENING -> 19
            StudyTime.NIGHT -> 21
        }

        // أيام الدراسة: الأحد (0) إلى الخميس (4)
        val studyDays = listOf(0, 1, 2, 3, 4)
        val dailyMinutes = (dailyHours * 60).coerceAtLeast(60)

        // أوزان الصعوبة: صعبة=3، متوسطة=2، سهلة=1
        fun Subject.weight(): Int = when (difficulty) {
            Difficulty.HARD   -> 3
            Difficulty.MEDIUM -> 2
            Difficulty.EASY   -> 1
        }
        val totalWeight = subjects.sumOf { it.weight() }.coerceAtLeast(1)

        val slots = mutableListOf<ScheduleSlot>()

        for (dayOfWeek in studyDays) {
            var currentTotalMinutes = startHour * 60  // نقطة البداية بالدقائق

            subjects.forEach { subject ->
                val proportion = subject.weight().toFloat() / totalWeight
                val duration = (dailyMinutes * proportion)
                    .toInt()
                    .coerceIn(30, 120)

                val slotHour = currentTotalMinutes / 60
                val slotMinute = currentTotalMinutes % 60

                // لا نضيف جلسات بعد منتصف الليل
                if (slotHour < 24) {
                    slots.add(
                        ScheduleSlot(
                            id = UUID.randomUUID().toString(),
                            subjectId = subject.id,
                            subjectName = subject.name,
                            subjectIcon = subject.icon,
                            colorHex = subject.colorHex,
                            dayOfWeek = dayOfWeek,
                            startHour = slotHour,
                            startMinute = slotMinute,
                            durationMinutes = duration,
                            priority = subject.weight(),
                        )
                    )
                }

                // تحريك مؤشر الوقت (المدة + 15 دقيقة استراحة)
                currentTotalMinutes += duration + 15
            }
        }

        return slots
    }
}
