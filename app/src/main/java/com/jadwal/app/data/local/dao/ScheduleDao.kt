package com.jadwal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jadwal.data.local.entity.ScheduleItemEntity
import com.jadwal.data.local.entity.ScheduleWithSubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    // ─── Insert / Update / Delete ───────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ScheduleItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ScheduleItemEntity>)

    @Update
    suspend fun updateItem(item: ScheduleItemEntity)

    @Query("DELETE FROM schedule_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM schedule_items WHERE subjectId = :subjectId")
    suspend fun deleteItemsBySubject(subjectId: String)

    @Query("DELETE FROM schedule_items")
    suspend fun deleteAllItems()

    // ─── Queries اليومية ─────────────────────────────────────

    /**
     * جلسات يوم محدد (من بداية اليوم لنهايته)
     * startOfDay / endOfDay = Unix timestamps
     */
    @Query("""
        SELECT * FROM schedule_items 
        WHERE scheduledDate BETWEEN :startOfDay AND :endOfDay
        ORDER BY priority DESC
    """)
    fun getScheduleForDay(startOfDay: Long, endOfDay: Long): Flow<List<ScheduleItemEntity>>

    @Query("""
        SELECT * FROM schedule_items 
        WHERE scheduledDate BETWEEN :startOfDay AND :endOfDay
        ORDER BY priority DESC
    """)
    suspend fun getScheduleForDaySync(startOfDay: Long, endOfDay: Long): List<ScheduleItemEntity>

    // ─── Queries مع بيانات المواد ─────────────────────────────

    @Query("""
        SELECT 
            s.id, s.subjectId, sub.name as subjectName, sub.nameEn as subjectNameEn, sub.icon as subjectIcon, sub.colorHex as subjectColor,
            s.scheduledDate, s.allocatedMinutes, s.actualMinutes, s.isCompleted, s.priority, s.studyPhase
        FROM schedule_items s
        JOIN subjects sub ON s.subjectId = sub.id
        WHERE s.scheduledDate BETWEEN :start AND :end
        ORDER BY s.scheduledDate ASC, s.priority DESC
    """)
    suspend fun getScheduleWithSubjectsSync(start: Long, end: Long): List<ScheduleWithSubjectEntity>

    @Query("""
        SELECT 
            s.id, s.subjectId, sub.name as subjectName, sub.nameEn as subjectNameEn, sub.icon as subjectIcon, sub.colorHex as subjectColor,
            s.scheduledDate, s.allocatedMinutes, s.actualMinutes, s.isCompleted, s.priority, s.studyPhase
        FROM schedule_items s
        JOIN subjects sub ON s.subjectId = sub.id
        WHERE s.scheduledDate BETWEEN :start AND :end
        ORDER BY s.scheduledDate ASC, s.priority DESC
    """)
    fun getScheduleWithSubjects(start: Long, end: Long): Flow<List<ScheduleWithSubjectEntity>>

    @Query("""
        SELECT 
            s.id, s.subjectId, sub.name as subjectName, sub.nameEn as subjectNameEn, sub.icon as subjectIcon, sub.colorHex as subjectColor,
            s.scheduledDate, s.allocatedMinutes, s.actualMinutes, s.isCompleted, s.priority, s.studyPhase
        FROM schedule_items s
        JOIN subjects sub ON s.subjectId = sub.id
        WHERE s.id = :id
    """)
    suspend fun getScheduleWithSubjectById(id: String): ScheduleWithSubjectEntity?

    // ─── Queries الأسبوعية ────────────────────────────────────

    @Query("""
        SELECT * FROM schedule_items 
        WHERE scheduledDate BETWEEN :startOfWeek AND :endOfWeek
        ORDER BY scheduledDate ASC, priority DESC
    """)
    fun getScheduleForWeek(startOfWeek: Long, endOfWeek: Long): Flow<List<ScheduleItemEntity>>

    // ─── Queries حسب المادة ───────────────────────────────────

    @Query("""
        SELECT * FROM schedule_items 
        WHERE subjectId = :subjectId 
        AND scheduledDate > :fromDate
        ORDER BY scheduledDate ASC
    """)
    suspend fun getItemsBySubjectAfterDate(subjectId: String, fromDate: Long): List<ScheduleItemEntity>

    // ─── Queries الحالة ──────────────────────────────────────

    @Query("SELECT * FROM schedule_items WHERE id = :id")
    suspend fun getItemById(id: String): ScheduleItemEntity?

    @Query("""
        SELECT * FROM schedule_items 
        WHERE isCompleted = 0 AND isMissed = 0
        AND scheduledDate < :now
        ORDER BY scheduledDate ASC
    """)
    suspend fun getMissedItems(now: Long = System.currentTimeMillis()): List<ScheduleItemEntity>

    // ─── Updates مباشرة ──────────────────────────────────────

    @Query("""
        UPDATE schedule_items 
        SET isCompleted = 1, actualMinutes = :actualMinutes, understandingLevel = :level
        WHERE id = :id
    """)
    suspend fun markCompleted(id: String, actualMinutes: Int, level: Int)

    @Query("UPDATE schedule_items SET isMissed = 1 WHERE id = :id")
    suspend fun markMissed(id: String)

    @Query("""
        UPDATE schedule_items 
        SET allocatedMinutes = :newMinutes 
        WHERE id = :id
    """)
    suspend fun updateAllocatedMinutes(id: String, newMinutes: Int)

    // ─── إحصائيات ────────────────────────────────────────────

    @Query("""
        SELECT COUNT(*) FROM schedule_items 
        WHERE isCompleted = 1 
        AND scheduledDate BETWEEN :startOfWeek AND :endOfWeek
    """)
    suspend fun getCompletedCountForWeek(startOfWeek: Long, endOfWeek: Long): Int

    @Query("""
        SELECT SUM(actualMinutes) FROM schedule_items 
        WHERE isCompleted = 1 
        AND scheduledDate BETWEEN :start AND :end
    """)
    suspend fun getTotalStudiedMinutes(start: Long, end: Long): Int?

    @Query("""
        SELECT scheduledDate, SUM(actualMinutes) as totalMinutes
        FROM schedule_items
        WHERE isCompleted = 1
        AND scheduledDate BETWEEN :start AND :end
        GROUP BY scheduledDate
        ORDER BY scheduledDate ASC
    """)
    suspend fun getDailyMinutesInRange(start: Long, end: Long): List<DailyMinutes>
}

/** نتيجة query الدقائق اليومية */
data class DailyMinutes(
    val scheduledDate: Long,
    val totalMinutes: Int,
)
