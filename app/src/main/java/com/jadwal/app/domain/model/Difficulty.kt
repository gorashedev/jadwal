package com.jadwal.domain.model

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD;

    companion object {
        fun fromString(value: String): Difficulty {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
