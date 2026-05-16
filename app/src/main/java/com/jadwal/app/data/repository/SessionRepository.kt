package com.jadwal.data.repository

import com.jadwal.data.local.dao.SessionDao
import com.jadwal.data.local.entity.toDomain
import com.jadwal.data.local.entity.toEntity
import com.jadwal.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
) {
    suspend fun insertSession(session: Session) =
        sessionDao.insertSession(session.toEntity())

    suspend fun updateSession(session: Session) =
        sessionDao.updateSession(session.toEntity())

    suspend fun getSessionById(id: String): Session? =
        sessionDao.getSessionById(id)?.toDomain()

    suspend fun getSessionByScheduleItem(scheduleItemId: String): Session? =
        sessionDao.getSessionByScheduleItem(scheduleItemId)?.toDomain()

    fun getSessionsForSubject(subjectId: String): Flow<List<Session>> =
        sessionDao.getSessionsForSubject(subjectId).map { list -> list.map { it.toDomain() } }

    suspend fun getSessionsInRange(start: Long, end: Long): List<Session> =
        sessionDao.getSessionsInRange(start, end).map { it.toDomain() }

    suspend fun getTotalDurationInRange(start: Long, end: Long): Int =
        sessionDao.getTotalDurationInRange(start, end) ?: 0

    suspend fun getSessionCountInRange(start: Long, end: Long): Int =
        sessionDao.getSessionCountInRange(start, end)

    suspend fun getAverageUnderstanding(subjectId: String): Float =
        sessionDao.getAverageUnderstandingForSubject(subjectId) ?: 0f

    suspend fun getStreakDays(fromTimestamp: Long): Int =
        sessionDao.getStreakDays(fromTimestamp)
}
