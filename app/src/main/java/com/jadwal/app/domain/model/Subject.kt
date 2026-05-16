package com.jadwal.domain.model

/**
 * Subject — موديل المادة الدراسية في طبقة الـ Domain
 * نظيف من أي تبعيات Android أو Room
 */
data class Subject(
    val id: String,
    val name: String,           // الاسم بالعربية
    val nameEn: String,         // الاسم بالإنجليزية
    val difficulty: Difficulty,
    val colorHex: String,       // "#5C6BC0"
    val icon: String,           // إيموجي المادة مثل "📐"
    val totalChapters: Int,
    val completedChapters: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
