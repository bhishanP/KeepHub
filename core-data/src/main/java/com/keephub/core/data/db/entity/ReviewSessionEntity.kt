package com.keephub.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "review_sessions",
    indices = [Index("completedAt")]
)
data class ReviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val completedAt: Instant = Instant.now(),
    val totalQuestions: Int,
    val correctCount: Int,
    val elapsedMs: Long,
    val isPractice: Boolean,
    val story: String? = null,
    val storyWordsJson: String = "[]",
    val itemsJson: String = "[]"
)
