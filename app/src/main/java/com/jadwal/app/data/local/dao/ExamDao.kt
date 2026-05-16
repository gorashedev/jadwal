package com.jadwal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jadwal.data.local.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    // ─── Insert / Update / Delete ───────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE subjectId = :subjectId")
    suspend fun deleteExamsBySubject(subjectId: String)

    // ─── Queries ─────────────────────────────────────────────

    @Query("SELECT * FROM exams ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE subjectId = :subjectId LIMIT 1")
    suspend fun getExamBySubject(subjectId: String): ExamEntity?

    @Query("""
        SELECT * FROM exams 
        WHERE examDate > :now 
        ORDER BY examDate ASC 
        LIMIT 1
    """)
    suspend fun getNextExam(now: Long = System.currentTimeMillis()): ExamEntity?

    @Query("""
        SELECT * FROM exams 
        WHERE examDate > :now 
        ORDER BY examDate ASC
    """)
    fun getUpcomingExams(now: Long = System.currentTimeMillis()): Flow<List<ExamEntity>>

    @Query("""
        SELECT * FROM exams 
        WHERE examDate BETWEEN :from AND :to
        ORDER BY examDate ASC
    """)
    suspend fun getExamsBetween(from: Long, to: Long): List<ExamEntity>
}
