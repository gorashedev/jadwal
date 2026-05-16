package com.jadwal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jadwal.domain.model.ScheduleItem
import com.jadwal.domain.model.StudyPhase
import com.jadwal.domain.model.UnderstandingLevel

@Entity(
    tableName = "schedule_items",
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
        Index("scheduledDate"),     // سرّع جلب جدول يوم معين
    ]
)
data class ScheduleItemEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val scheduledDate: Long,
    val allocatedMinutes: Int,
    val actualMinutes: Int = 0,
    val understandingLevel: Int = 0,  // UnderstandingLevel.value
    val isCompleted: Boolean = false,
    val isMissed: Boolean = false,
    val priority: Int = 0,
    val studyPhase: String = "WORK",
    val createdAt: Long = System.currentTimeMillis(),
)

// ===== Mappers =====

fun ScheduleItemEntity.toDomain(): ScheduleItem = ScheduleItem(
    id = id,
    subjectId = subjectId,
    scheduledDate = scheduledDate,
    allocatedMinutes = allocatedMinutes,
    actualMinutes = actualMinutes,
    understandingLevel = UnderstandingLevel.fromValue(understandingLevel),
    isCompleted = isCompleted,
    isMissed = isMissed,
    priority = priority,
    studyPhase = StudyPhase.fromString(studyPhase),
    createdAt = createdAt,
)

fun ScheduleItem.toEntity(): ScheduleItemEntity = ScheduleItemEntity(
    id = id,
    subjectId = subjectId,
    scheduledDate = scheduledDate,
    allocatedMinutes = allocatedMinutes,
    actualMinutes = actualMinutes,
    understandingLevel = understandingLevel.value,
    isCompleted = isCompleted,
    isMissed = isMissed,
    priority = priority,
    studyPhase = studyPhase.name,
    createdAt = createdAt,
)
