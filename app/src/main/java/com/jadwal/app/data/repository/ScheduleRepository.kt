package com.jadwal.data.repository

import com.jadwal.data.local.dao.ScheduleDao
import com.jadwal.data.local.entity.toDomain
import com.jadwal.data.local.entity.toEntity
import com.jadwal.domain.model.ScheduleItem
import com.jadwal.domain.model.UnderstandingLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
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

    // ─── Write ───────────────────────────────────────────────

    suspend fun insertItem(item: ScheduleItem) =
        scheduleDao.insertItem(item.toEntity())

    suspend fun insertItems(items: List<ScheduleItem>) =
        scheduleDao.insertItems(items.map { it.toEntity() })

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
