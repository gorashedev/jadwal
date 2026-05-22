package com.jadwal.data.repository

import com.jadwal.data.local.dao.ScheduleDao
import com.jadwal.data.local.entity.toDomain
import com.jadwal.data.local.entity.toEntity
import com.jadwal.domain.model.ScheduleItem
import com.jadwal.domain.model.ScheduleSlot
import com.jadwal.domain.model.ScheduleWithSubject
import com.jadwal.domain.model.StudyPhase
import com.jadwal.domain.model.UnderstandingLevel
import com.jadwal.ui.screens.schedule.ExamBadge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao,
) {
    // ─── Read ────────────────────────────────────────────────

    fun getScheduleForDay(date: Long = System.currentTimeMillis()): Flow<List<ScheduleItem>> {
        val (start, end) = dayRange(date)
        return scheduleDao.getScheduleForDay(start, end)
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun getTodaySchedule(): List<ScheduleItem> {
        val (start, end) = dayRange(System.currentTimeMillis())
        return scheduleDao.getScheduleForDaySync(start, end).map { it.toDomain() }
    }

    suspend fun getTodayScheduleWithSubjects(): List<ScheduleWithSubject> {
        val (start, end) = dayRange(System.currentTimeMillis())
        return scheduleDao.getScheduleWithSubjectsSync(start, end).map { it.toDomain() }
    }

    /** Reactive stream for today's schedule — updates when subjects or schedule items change. */
    fun observeTodayScheduleWithSubjects(): Flow<List<ScheduleWithSubject>> {
        val (start, end) = dayRange(System.currentTimeMillis())
        return scheduleDao.getScheduleWithSubjects(start, end)
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun getWeekScheduleWithSubjects(): List<ScheduleWithSubject> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        val end = start + 7L * 24 * 60 * 60 * 1000 - 1
        return scheduleDao.getScheduleWithSubjectsSync(start, end).map { it.toDomain() }
    }

    fun getScheduleForWeek(weekStart: Long): Flow<List<ScheduleItem>> {
        val end = weekStart + 7L * 24 * 60 * 60 * 1000
        return scheduleDao.getScheduleForWeek(weekStart, end)
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun getItemById(id: String): ScheduleItem? =
        scheduleDao.getItemById(id)?.toDomain()

    suspend fun getMissedItems(): List<ScheduleItem> =
        scheduleDao.getMissedItems().map { it.toDomain() }

    suspend fun getItemsBySubjectAfterDate(subjectId: String, fromDate: Long): List<ScheduleItem> =
        scheduleDao.getItemsBySubjectAfterDate(subjectId, fromDate).map { it.toDomain() }

    suspend fun getScheduleItemWithSubject(id: String): ScheduleWithSubject? =
        scheduleDao.getScheduleWithSubjectById(id)?.toDomain()

    suspend fun getUpcomingExamBadges(): List<ExamBadge> = emptyList()

    suspend fun getWeekSessions(): List<ScheduleWithSubject> = emptyList()

    suspend fun getMonthSessions(): List<ScheduleWithSubject> = emptyList()

    // ─── Write ───────────────────────────────────────────────

    suspend fun insertItem(item: ScheduleItem) =
        scheduleDao.insertItem(item.toEntity())

    suspend fun insertItems(items: List<ScheduleItem>) =
        scheduleDao.insertItems(items.map { it.toEntity() })

    suspend fun saveScheduleSlots(slots: List<ScheduleSlot>) {
        val items = slots.map { slot ->
            ScheduleItem(
                id = UUID.randomUUID().toString(),
                subjectId = slot.subjectId,
                scheduledDate = calculateTimestamp(slot.dayOfWeek, slot.startHour, slot.startMinute),
                allocatedMinutes = slot.durationMinutes,
                actualMinutes = 0,
                understandingLevel = UnderstandingLevel.NOT_RATED,
                isCompleted = false,
                isMissed = false,
                priority = slot.priority,
                studyPhase = StudyPhase.WORK,
                createdAt = System.currentTimeMillis()
            )
        }
        insertItems(items)
    }

    suspend fun updateItem(item: ScheduleItem) =
        scheduleDao.updateItem(item.toEntity())

    suspend fun markCompleted(id: String, actualMinutes: Int, level: UnderstandingLevel) =
        scheduleDao.markCompleted(id, actualMinutes, level.value)

    suspend fun markMissed(id: String) =
        scheduleDao.markMissed(id)

    suspend fun updateAllocatedMinutes(id: String, newMinutes: Int) =
        scheduleDao.updateAllocatedMinutes(id, newMinutes)

    suspend fun deleteItemsBySubject(subjectId: String) =
        scheduleDao.deleteItemsBySubject(subjectId)

    suspend fun deleteAllItems() =
        scheduleDao.deleteAllItems()

    // ─── Analytics ───────────────────────────────────────────

    suspend fun getTotalStudiedMinutes(start: Long, end: Long): Int =
        scheduleDao.getTotalStudiedMinutes(start, end) ?: 0

    suspend fun getCompletedCountForWeek(start: Long, end: Long): Int =
        scheduleDao.getCompletedCountForWeek(start, end)

    // ─── Helpers ─────────────────────────────────────────────

    private fun calculateTimestamp(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        // ضبط التقويم على بداية الأسبوع الحالي (الأحد)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // إضافة عدد الأيام (dayOfWeek: 0=الأحد)
        calendar.add(Calendar.DAY_OF_WEEK, dayOfWeek)
        
        return calendar.timeInMillis
    }

    private fun dayRange(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 24L * 60 * 60 * 1000 - 1
        return Pair(start, end)
    }
}
