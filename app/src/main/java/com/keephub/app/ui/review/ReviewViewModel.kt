package com.keephub.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keephub.core.data.repo.WordRepository
import com.keephub.core.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.max
import kotlin.random.Random
import com.keephub.app.ui.review.AnswerRow
import com.keephub.app.ui.review.Question
import com.keephub.app.ui.review.QuizMode

data class ReviewUi(
    val stage: Stage = Stage.Idle,
    val total: Int = 0,
    val index: Int = 0,
    val current: Question? = null,
    val correctCount: Int = 0,
    val elapsedMs: Long = 0L,
    val summary: List<AnswerRow> = emptyList(),
    val error: String? = null
) {
    enum class Stage { Idle, InProgress, Finished }
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repo: WordRepository,
    private val settings: SettingsStore
) : ViewModel() {

    private val _ui = MutableStateFlow(ReviewUi())
    val ui: StateFlow<ReviewUi> = _ui.asStateFlow()

    private var queue: List<Long> = emptyList()
    private var questions: List<Question> = emptyList()
    private var questionStartMs: Long = 0L
    private var enabledModes: List<QuizMode> = listOf(QuizMode.MCQ, QuizMode.TYPE, QuizMode.CLOZE)
    private var targetGoal: Int = 10

    private val collected = mutableListOf<AnswerRow>()  // ← NEW: accumulate answers

    init {
        viewModelScope.launch {
            combine(settings.quizModes, settings.dailyGoal) { modes, goal ->
                enabledModes = modes.mapNotNull {
                    when (it.uppercase()) { "MCQ" -> QuizMode.MCQ; "TYPE" -> QuizMode.TYPE; "CLOZE" -> QuizMode.CLOZE; else -> null }
                }.ifEmpty { listOf(QuizMode.MCQ) }
                targetGoal = max(1, goal)
            }.collect()
        }
    }

    fun start(today: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            try {
                collected.clear()                            // ← NEW
                queue = repo.generateDailyQueue(targetGoal, today)
                questions = buildQuestions(queue)
                _ui.value = ReviewUi(stage = ReviewUi.Stage.InProgress, total = questions.size, index = 0, current = questions.getOrNull(0))
                questionStartMs = System.currentTimeMillis()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Failed to start review") }
            }
        }
    }

    private suspend fun buildQuestions(ids: List<Long>): List<Question> {
        val out = mutableListOf<Question>()
        var modeIdx = 0
        for (id in ids) {
            val details = repo.getDetails(id) ?: continue
            val term = details.word.term
            val def = details.senses.firstOrNull()?.definition ?: "No definition cached yet."
            val examples = details.senses.firstOrNull()?.examples ?: emptyList()
            val sentence = examples.firstOrNull() ?: "The word ____ means: $def"

            val mode = enabledModes[modeIdx % enabledModes.size]
            modeIdx++

            when (mode) {
                QuizMode.MCQ -> {
                    val distractors = repo.randomDefinitionsExcept(id, 3).ifEmpty { listOf("Unrelated meaning A", "Unrelated meaning B", "Unrelated meaning C") }
                    val options = (distractors + def).shuffled(Random(System.nanoTime()))
                    val correctIndex = options.indexOf(def).coerceAtLeast(0)
                    out += Question.MCQ(
                        id,
                        prompt = "Pick the correct definition for: $term",
                        options = options,
                        correctIndex = correctIndex,
                        correctText = def
                    )
                }
                QuizMode.TYPE -> {
                    out += Question.TYPE(
                        id,
                        prompt = "Type the word for this definition:\n$def",
                        correctText = term
                    )
                }
                QuizMode.CLOZE -> {
                    out += Question.CLOZE(
                        id,
                        prompt = sentence.replace(term, "____", ignoreCase = true),
                        correctText = term
                    )
                }
            }
        }
        return out
    }

    fun answerMcq(chosenIndex: Int) {
        val q = ui.value.current as? Question.MCQ ?: return
        val correct = (chosenIndex == q.correctIndex)
        val user = q.options.getOrNull(chosenIndex) ?: ""
        recordAndAdvance(q.wordId, mode = QuizMode.MCQ, correct = correct, grade = if (correct) 5 else 2, userAnswer = user, correctAnswer = q.correctText)
    }

    fun answerType(input: String) {
        val q = ui.value.current as? Question.TYPE ?: return
        val correct = normalized(input) == normalized(q.correctText)
        recordAndAdvance(q.wordId, mode = QuizMode.TYPE, correct = correct, grade = if (correct) 5 else 2, userAnswer = input, correctAnswer = q.correctText)
    }

    fun answerCloze(input: String) {
        val q = ui.value.current as? Question.CLOZE ?: return
        val correct = normalized(input) == normalized(q.correctText)
        recordAndAdvance(q.wordId, mode = QuizMode.CLOZE, correct = correct, grade = if (correct) 5 else 2, userAnswer = input, correctAnswer = q.correctText)
    }

    private fun normalized(s: String) = s.trim().lowercase()

    private fun recordAndAdvance(
        wordId: Long,
        mode: QuizMode,
        correct: Boolean,
        grade: Int,
        userAnswer: String,
        correctAnswer: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val elapsed = now - questionStartMs
            questionStartMs = now

            // 1) persist SRS result
            runCatching {
                repo.recordReview(
                    wordId = wordId,
                    mode = mode.name,
                    correct = correct,
                    timeMs = elapsed,
                    grade = grade,
                    today = LocalDate.now()
                )
            }

            // 2) collect for summary
            val row = AnswerRow(
                index = ui.value.index + 1,
                mode = mode,
                prompt = ui.value.current?.prompt.orEmpty(),
                userAnswer = userAnswer,
                correctAnswer = correctAnswer,
                correct = correct,
                timeMs = elapsed
            )
            collected += row

            // 3) advance UI
            val nextIndex = ui.value.index + 1
            val newCorrect = ui.value.correctCount + if (correct) 1 else 0
            if (nextIndex >= questions.size) {
                _ui.value = ui.value.copy(
                    stage = ReviewUi.Stage.Finished,
                    index = questions.size,
                    current = null,
                    correctCount = newCorrect,
                    elapsedMs = ui.value.elapsedMs + elapsed,
                    summary = collected.toList()
                )
            } else {
                _ui.value = ui.value.copy(
                    index = nextIndex,
                    current = questions[nextIndex],
                    correctCount = newCorrect,
                    elapsedMs = ui.value.elapsedMs + elapsed
                )
            }
        }
    }

    fun restart() {
        _ui.value = ReviewUi()
        queue = emptyList()
        questions = emptyList()
        collected.clear()
    }
}
