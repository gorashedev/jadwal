package com.jadwal.data.repository

import com.jadwal.data.local.dao.SubjectDao
import com.jadwal.data.local.entity.toDomain
import com.jadwal.data.local.entity.toEntity
import com.jadwal.domain.model.Subject
import com.jadwal.ui.screens.analytics.SubjectStat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepository @Inject constructor(
    private val subjectDao: SubjectDao,
) {
    fun getAllSubjects(): Flow<List<Subject>> =
        subjectDao.getAllSubjects().map { list -> list.map { it.toDomain() } }

    suspend fun getSubjectById(id: String): Subject? =
        subjectDao.getSubjectById(id)?.toDomain()

    suspend fun insertSubject(subject: Subject) =
        subjectDao.insertSubject(subject.toEntity())

    suspend fun insertSubjects(subjects: List<Subject>) =
        subjectDao.insertSubjects(subjects.map { it.toEntity() })

    suspend fun updateSubject(subject: Subject) =
        subjectDao.updateSubject(subject.toEntity())

    suspend fun deleteSubject(subject: Subject) =
        subjectDao.deleteSubject(subject.toEntity())

    suspend fun deleteSubjectById(id: String) =
        subjectDao.deleteSubjectById(id)

    suspend fun getSubjectCount(): Int =
        subjectDao.getSubjectCount()

    suspend fun getSubjectStats(): List<SubjectStat> = emptyList()
}
