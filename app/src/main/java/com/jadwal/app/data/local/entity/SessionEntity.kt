package com.jadwal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jadwal.domain.model.Session
import com.jadwal.domain.model.UnderstandingLevel

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("subjectId"),
        Index("scheduleItemId"),
        Index("startTime"),
    ]
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val scheduleItemId: String,
    val subjectId: String,
    val startTime: Long,
    val endTime: Long = 0,
    val durationMinutes: Int = 0,
    val pomodorosCompleted: Int = 0,
    val understandingLevel: Int = 0,
    val notes: String = "",
)

// ===== Mappers =====

fun SessionEntity.toDomain(): Session = Session(
    id = id,
    scheduleItemId = scheduleItemId,
    subjectId = subjectId,
    startTime = startTime,
    endTime = endTime,
    durationMinutes = durationMinutes,
    pomodorosCompleted = pomodorosCompleted,
    understandingLevel = UnderstandingLevel.fromValue(understandingLevel),
    notes = notes,
)

fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id,
    scheduleItemId = scheduleItemId,
    subjectId = subjectId,
    startTime = startTime,
    endTime = endTime,
    durationMinutes = durationMinutes,
    pomodorosCompleted = pomodorosCompleted,
    understandingLevel = understandingLevel.value,
    notes = notes,
)
