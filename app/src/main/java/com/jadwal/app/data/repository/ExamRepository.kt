package com.jadwal.data.repository

import com.jadwal.data.local.dao.ExamDao
import com.jadwal.data.local.dao.SubjectDao
import com.jadwal.data.local.entity.toDomain
import com.jadwal.data.local.entity.toEntity
import com.jadwal.domain.model.Exam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val examDao: ExamDao,
    private val subjectDao: SubjectDao,
) {
    fun getAllExams(): Flow<List<Exam>> =
        examDao.getAllExams().map { list ->
            list.map { entity ->
                val subject = subjectDao.getSubjectById(entity.subjectId)
                entity.toDomain().copy(subjectName = subject?.name ?: "")
            }
        }

    suspend fun getUpcomingExams(withinDays: Int): List<Exam> {
        val now = System.currentTimeMillis()
        val to = now + (withinDays * 24L * 60 * 60 * 1000)
        
        return examDao.getExamsBetween(now, to).map { entity ->
            val subject = subjectDao.getSubjectById(entity.subjectId)
            entity.toDomain().copy(subjectName = subject?.name ?: "")
        }
    }

    suspend fun getExamBySubject(subjectId: String): Exam? {
        val entity = examDao.getExamBySubject(subjectId) ?: return null
        val subject = subjectDao.getSubjectById(subjectId)
        return entity.toDomain().copy(subjectName = subject?.name ?: "")
    }

    suspend fun insertExam(exam: Exam) = examDao.insertExam(exam.toEntity())

    suspend fun updateExam(exam: Exam) = examDao.updateExam(exam.toEntity())

    suspend fun deleteExam(exam: Exam) = examDao.deleteExam(exam.toEntity())
}
