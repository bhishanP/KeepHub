package com.keephub.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "quiz_results",
    indices = [Index("wordId"), Index("timestamp")]
)
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long,
    val mode: String,            // MCQ | TYPE | CLOZE
    val correct: Boolean,
    val timeMs: Long,
    val timestamp: Instant = Instant.now()
)
