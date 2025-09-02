package com.keephub.app.ui.review

sealed class Question(open val wordId: Long, open val prompt: String) {
    data class MCQ(
        override val wordId: Long,
        override val prompt: String,           // e.g., “Pick the correct definition for: TERM”
        val options: List<String>,             // includes correct + distractors (shuffled)
        val correctIndex: Int
    ) : Question(wordId, prompt)

    data class TYPE(
        override val wordId: Long,
        override val prompt: String            // e.g., “Type the word for this definition: ...”
    ) : Question(wordId, prompt)

    data class CLOZE(
        override val wordId: Long,
        override val prompt: String            // sentence with ____ in place of the word
    ) : Question(wordId, prompt)
}

enum class QuizMode { MCQ, TYPE, CLOZE }
