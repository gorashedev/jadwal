package com.jadwal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jadwal.domain.model.ScheduleItem
import com.jadwal.domain.model.ScheduleWithSubject
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

/** POJO for JOIN query results */
data class ScheduleWithSubjectEntity(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val subjectIcon: String,
    val subjectColor: String,
    val scheduledDate: Long,
    val allocatedMinutes: Int,
    val actualMinutes: Int,
    val isCompleted: Boolean,
    val priority: Int,
    val studyPhase: String
)

fun ScheduleWithSubjectEntity.toDomain(): ScheduleWithSubject = ScheduleWithSubject(
    id = id,
    subjectId = subjectId,
    subjectName = subjectName,
    subjectIcon = subjectIcon,
    subjectColor = subjectColor,
    scheduledDate = scheduledDate,
    allocatedMinutes = allocatedMinutes,
    actualMinutes = actualMinutes,
    isCompleted = isCompleted,
    priority = priority,
    studyPhase = StudyPhase.fromString(studyPhase)
)
