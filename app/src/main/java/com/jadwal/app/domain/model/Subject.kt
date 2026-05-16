package com.jadwal.domain.model

data class Subject(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val color: Int = 0,
    val difficulty: Difficulty = Difficulty.MEDIUM
)
