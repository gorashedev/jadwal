package com.jadwal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jadwal.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    // ─── Insert / Update ────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    // ─── Queries ─────────────────────────────────────────────

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: String): SessionEntity?

    @Query("""
        SELECT * FROM sessions 
        WHERE scheduleItemId = :scheduleItemId 
        LIMIT 1
    """)
    suspend fun getSessionByScheduleItem(scheduleItemId: String): SessionEntity?

    @Query("""
        SELECT * FROM sessions 
        WHERE subjectId = :subjectId 
        ORDER BY startTime DESC
    """)
    fun getSessionsForSubject(subjectId: String): Flow<List<SessionEntity>>

    @Query("""
        SELECT * FROM sessions 
        WHERE startTime BETWEEN :start AND :end
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsInRange(start: Long, end: Long): List<SessionEntity>

    // ─── إحصائيات ────────────────────────────────────────────

    @Query("""
        SELECT SUM(durationMinutes) FROM sessions 
        WHERE startTime BETWEEN :start AND :end
    """)
    suspend fun getTotalDurationInRange(start: Long, end: Long): Int?

    @Query("""
        SELECT COUNT(*) FROM sessions 
        WHERE startTime BETWEEN :start AND :end
    """)
    suspend fun getSessionCountInRange(start: Long, end: Long): Int

    @Query("""
        SELECT SUM(pomodorosCompleted) FROM sessions 
        WHERE startTime BETWEEN :start AND :end
    """)
    suspend fun getTotalPomodorosInRange(start: Long, end: Long): Int?

    /** متوسط مستوى الفهم لمادة معينة (يستبعد NOT_RATED = 0) */
    @Query("""
        SELECT AVG(understandingLevel) FROM sessions 
        WHERE subjectId = :subjectId 
        AND understandingLevel > 0
    """)
    suspend fun getAverageUnderstandingForSubject(subjectId: String): Float?

    /** عدد أيام السلسلة (streak) — أيام متواصلة فيها جلسة */
    @Query("""
        SELECT COUNT(DISTINCT date(startTime / 1000, 'unixepoch')) 
        FROM sessions 
        WHERE startTime >= :fromTimestamp
    """)
    suspend fun getStreakDays(fromTimestamp: Long): Int
}
