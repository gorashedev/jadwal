package com.jadwal.domain.model

/**
 * UnderstandingLevel — مستوى فهم الطالب بعد كل جلسة
 *
 * يؤثر على الخوارزمية:
 * - POOR    → +30% وقت + مراجعة غداً
 * - PARTIAL → نفس الوقت + مراجعة بعد 3 أيام
 * - GREAT   → -20% وقت + مراجعة بعد أسبوع
 */
enum class UnderstandingLevel(val value: Int) {
    NOT_RATED(0),
    POOR(1),
    PARTIAL(2),
    GREAT(3),
    EXCELLENT(4);

    companion object {
        fun fromValue(value: Int): UnderstandingLevel =
            entries.find { it.value == value } ?: NOT_RATED
    }
}
