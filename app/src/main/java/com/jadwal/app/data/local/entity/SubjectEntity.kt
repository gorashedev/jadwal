package com.jadwal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jadwal.domain.model.Difficulty
import com.jadwal.domain.model.Subject

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameEn: String,
    val difficulty: String,         // "EASY" | "MEDIUM" | "HARD"
    val colorHex: String,           // "#5C6BC0"
    val icon: String,               // "📐"
    val totalChapters: Int,
    val completedChapters: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

// ===== Mappers =====

fun SubjectEntity.toDomain(): Subject = Subject(
    id = id,
    name = name,
    nameEn = nameEn,
    difficulty = Difficulty.fromString(difficulty),
    colorHex = colorHex,
    icon = icon,
    totalChapters = totalChapters,
    completedChapters = completedChapters,
    createdAt = createdAt,
)

fun Subject.toEntity(): SubjectEntity = SubjectEntity(
    id = id,
    name = name,
    nameEn = nameEn,
    difficulty = difficulty.name,
    colorHex = colorHex,
    icon = icon,
    totalChapters = totalChapters,
    completedChapters = completedChapters,
    createdAt = createdAt,
)
