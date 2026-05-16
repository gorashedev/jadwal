package com.jadwal.data.local

import androidx.room.TypeConverter

/**
 * Converters — محولات أنواع البيانات لـ Room
 *
 * Room يعرف فقط: Int, Long, Float, Double, String, ByteArray
 * هذه المحولات تساعده على حفظ أنواع أخرى
 */
class Converters {

    // List<String> ↔ String (مفصولة بـ ,)
    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        value?.joinToString(",")

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        value?.split(",")?.filter { it.isNotBlank() }

    // Boolean ↔ Int (Room يدعم Boolean مباشرة لكن للوضوح)
    @TypeConverter
    fun fromBoolean(value: Boolean): Int = if (value) 1 else 0

    @TypeConverter
    fun toBoolean(value: Int): Boolean = value == 1
}
