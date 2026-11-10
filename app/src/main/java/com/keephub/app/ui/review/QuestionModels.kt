package com.keephub.app.ui.review

sealed class Question(
    open val wordId: Long,
    open val prompt: String,
    open val term: String,
    open val audioUrl: String? = null,
    open val pos: String? = null
) {
    data class MCQ(
        override val wordId: Long,
        override val prompt: String,
        override val term: String,
        override val audioUrl: String? = null,
        override val pos: String? = null,
        val options: List<String>,
        val correctIndex: Int,
        val correctText: String
    ) : Question(wordId, prompt, term, audioUrl, pos)

    data class TYPE(
        override val wordId: Long,
        override val prompt: String,
        override val term: String,
        override val audioUrl: String? = null,
        override val pos: String? = null,
        val correctText: String
    ) : Question(wordId, prompt, term, audioUrl, pos)

    data class CLOZE(
        override val wordId: Long,
        override val prompt: String,
        override val term: String,
        override val audioUrl: String? = null,
        override val pos: String? = null,
        val correctText: String
    ) : Question(wordId, prompt, term, audioUrl, pos)

    data class AUDIO(
        override val wordId: Long,
        override val prompt: String,
        override val term: String,
        override val audioUrl: String? = null,
        override val pos: String? = null,
        val options: List<String>,
        val correctIndex: Int,
        val correctText: String
    ) : Question(wordId, prompt, term, audioUrl, pos)
}

enum class QuizMode { MCQ, TYPE, CLOZE, AUDIO }

data class AnswerFeedback(
    val correct: Boolean,
    val userAnswer: String,
    val correctAnswer: String,
    val term: String,
    val audioUrl: String?,
    val chosenIndex: Int? = null,
    val correctIndex: Int? = null,
    val semanticNote: String? = null,
    val explanation: String? = null
)

data class AnswerRow(
    val index: Int,
    val wordId: Long,
    val mode: QuizMode,
    val prompt: String,
    val userAnswer: String,
    val correctAnswer: String,
    val correct: Boolean,
    val timeMs: Long,
    val term: String = "",
    val semanticNote: String? = null,
    val explanation: String? = null
)