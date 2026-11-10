package com.keephub.core.data.repo

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class SessionReviewItem(
    val wordId: Long,
    val term: String,
    val mode: String, // MCQ, TYPE, CLOZE, AUDIO
    val prompt: String,
    val userAnswer: String,
    val correctAnswer: String,
    val correct: Boolean,
    val semanticNote: String? = null,
    val explanation: String? = null,
    val timeMs: Long = 0L
)

data class ReviewSessionRecord(
    val id: Long = 0,
    val completedAt: Instant = Instant.now(),
    val totalQuestions: Int,
    val correctCount: Int,
    val elapsedMs: Long,
    val isPractice: Boolean,
    val story: String? = null,
    val storyWords: List<String> = emptyList(),
    val items: List<SessionReviewItem> = emptyList()
)
