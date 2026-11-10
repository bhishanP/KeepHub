package com.keephub.core.data.daily

import kotlinx.serialization.Serializable

@Serializable
enum class VocabularyLevel(
    val code: String,
    val displayName: String,
    val cefr: String,
    val description: String
) {
    BEGINNER(
        code = "BEGINNER",
        displayName = "Beginner",
        cefr = "A1 - A2",
        description = "Essential everyday foundational words for daily fluency"
    ),
    INTERMEDIATE(
        code = "INTERMEDIATE",
        displayName = "Intermediate",
        cefr = "B1 - B2",
        description = "High-frequency academic and professional vocabulary"
    ),
    ADVANCED(
        code = "ADVANCED",
        displayName = "Advanced",
        cefr = "C1 - C2 / GRE",
        description = "Sophisticated literary, competitive exam, and GRE words"
    );

    companion object {
        fun fromCode(code: String?): VocabularyLevel =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INTERMEDIATE
    }
}
