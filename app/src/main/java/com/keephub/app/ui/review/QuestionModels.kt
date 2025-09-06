package com.keephub.app.ui.review

sealed class Question(open val wordId: Long, open val prompt: String) {
    data class MCQ(
        override val wordId: Long,
        override val prompt: String,           // e.g., “Pick the correct definition for: TERM”
        val options: List<String>,             // includes correct + distractors (shuffled)
        val correctIndex: Int,
        val correctText: String
    ) : Question(wordId, prompt)

    data class TYPE(
        override val wordId: Long,
        override val prompt: String,            // e.g., “Type the word for this definition: ...”
        val correctText: String
    ) : Question(wordId, prompt)

    data class CLOZE(
        override val wordId: Long,
        override val prompt: String,            // sentence with ____ in place of the word
        val correctText: String
    ) : Question(wordId, prompt)
}

enum class QuizMode { MCQ, TYPE, CLOZE }

data class AnswerRow(
    val index: Int,
    val mode: QuizMode,
    val prompt: String,
    val userAnswer: String,
    val correctAnswer: String,
    val correct: Boolean,
    val timeMs: Long
)