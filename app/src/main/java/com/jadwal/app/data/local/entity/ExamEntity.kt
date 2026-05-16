package com.jadwal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jadwal.domain.model.Exam

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,  // احذف الامتحان لو حُذفت المادة
        )
    ],
    indices = [Index("subjectId")]
)
data class ExamEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val examDate: Long,
    val location: String = "",
    val notes: String = "",
)

// ===== Mappers =====

fun ExamEntity.toDomain(): Exam = Exam(
    id = id,
    subjectId = subjectId,
    examDate = examDate,
    location = location,
    notes = notes,
)

fun Exam.toEntity(): ExamEntity = ExamEntity(
    id = id,
    subjectId = subjectId,
    examDate = examDate,
    location = location,
    notes = notes,
)
