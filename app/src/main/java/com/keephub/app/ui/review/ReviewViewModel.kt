package com.keephub.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keephub.core.data.net.GeminiApiService
import com.keephub.core.data.repo.WordRepository
import com.keephub.core.data.settings.SettingsStore
import com.keephub.core.data.util.WordNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.max
import kotlin.random.Random

sealed interface StoryUiState {
    object Idle : StoryUiState
    object Loading : StoryUiState
    data class Success(val story: String, val words: List<String>) : StoryUiState
    data class Error(val message: String) : StoryUiState
}

data class ReviewUi(
    val stage: Stage = Stage.Idle,
    val total: Int = 0,
    val index: Int = 0,
    val current: Question? = null,
    val correctCount: Int = 0,
    val elapsedMs: Long = 0L,
    val summary: List<AnswerRow> = emptyList(),
    val error: String? = null,
    val dueCount: Int = 0,
    val isPractice: Boolean = false,
    val feedback: AnswerFeedback? = null
) {
    enum class Stage { Idle, InProgress, Finished, Empty }
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repo: WordRepository,
    private val settings: SettingsStore,
    private val gemini: GeminiApiService,
    private val normalizer: WordNormalizer
) : ViewModel() {

    private val _ui = MutableStateFlow(ReviewUi())
    val ui: StateFlow<ReviewUi> = _ui.asStateFlow()

    private val _explaining = MutableStateFlow(false)
    val explaining: StateFlow<Boolean> = _explaining.asStateFlow()

    private val _storyState = MutableStateFlow<StoryUiState>(StoryUiState.Idle)
    val storyState: StateFlow<StoryUiState> = _storyState.asStateFlow()

    private var queue: List<Long> = emptyList()
    private var questions: List<Question> = emptyList()
    private var questionStartMs: Long = 0L
    private var enabledModes: List<QuizMode> = listOf(QuizMode.MCQ, QuizMode.TYPE, QuizMode.CLOZE, QuizMode.AUDIO)
    private var targetGoal: Int = 10

    private val collected = mutableListOf<AnswerRow>()

    init {
        viewModelScope.launch {
            combine(settings.quizModes, settings.dailyGoal) { modes, goal ->
                enabledModes = modes.mapNotNull {
                    when (it.uppercase()) {
                        "MCQ" -> QuizMode.MCQ
                        "TYPE" -> QuizMode.TYPE
                        "CLOZE" -> QuizMode.CLOZE
                        "AUDIO" -> QuizMode.AUDIO
                        else -> null
                    }
                }.ifEmpty { listOf(QuizMode.MCQ) }
                targetGoal = max(1, goal)
            }.collect {
                refreshDueCount()
            }
        }
    }

    fun refreshDueCount(today: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            try {
                val q = repo.generateDailyQueue(targetGoal, today)
                _ui.update { it.copy(dueCount = q.size) }
            } catch (_: Exception) {}
        }
    }

    fun start(today: LocalDate = LocalDate.now(), practice: Boolean = false) {
        viewModelScope.launch {
            try {
                collected.clear()
                queue = if (practice) {
                    repo.generatePracticeQueue(targetGoal)
                } else {
                    repo.generateDailyQueue(targetGoal, today)
                }
                questions = buildQuestions(queue)
                if (questions.isEmpty()) {
                    _ui.value = ReviewUi(
                        stage = ReviewUi.Stage.Empty,
                        isPractice = practice,
                        dueCount = 0
                    )
                } else {
                    _ui.value = ReviewUi(
                        stage = ReviewUi.Stage.InProgress,
                        total = questions.size,
                        index = 0,
                        current = questions.firstOrNull(),
                        isPractice = practice,
                        dueCount = queue.size,
                        feedback = null
                    )
                    questionStartMs = System.currentTimeMillis()
                }
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
            val pos = details.senses.firstOrNull()?.pos
            val audioUrl = details.senses.firstOrNull { it.audioUrls.isNotEmpty() }?.audioUrls?.firstOrNull()
            val examples = details.senses.firstOrNull()?.examples ?: emptyList()
            val sentence = examples.firstOrNull() ?: "The word ____ means: $def"

            val mode = enabledModes[modeIdx % enabledModes.size]
            modeIdx++

            when (mode) {
                QuizMode.MCQ -> {
                    val distractors = repo.randomDefinitionsExcept(id, 3).ifEmpty {
                        listOf("Unrelated definition A", "Unrelated definition B", "Unrelated definition C")
                    }
                    val options = (distractors + def).shuffled(Random(System.nanoTime()))
                    val correctIndex = options.indexOf(def).coerceAtLeast(0)
                    out += Question.MCQ(
                        wordId = id,
                        prompt = "Pick the correct definition for: $term",
                        term = term,
                        audioUrl = audioUrl,
                        pos = pos,
                        options = options,
                        correctIndex = correctIndex,
                        correctText = def
                    )
                }
                QuizMode.TYPE -> {
                    out += Question.TYPE(
                        wordId = id,
                        prompt = "Type the word for this definition:\n$def",
                        term = term,
                        audioUrl = audioUrl,
                        pos = pos,
                        correctText = term
                    )
                }
                QuizMode.CLOZE -> {
                    val blankPrompt = if (sentence.contains(term, ignoreCase = true)) {
                        sentence.replace(term, "____", ignoreCase = true)
                    } else {
                        "Complete the sentence:\n\"____: $def\""
                    }
                    out += Question.CLOZE(
                        wordId = id,
                        prompt = blankPrompt,
                        term = term,
                        audioUrl = audioUrl,
                        pos = pos,
                        correctText = term
                    )
                }
                QuizMode.AUDIO -> {
                    val distractors = repo.randomTermsExcept(id, 3).ifEmpty {
                        listOf("Word A", "Word B", "Word C")
                    }
                    val options = (distractors + term).shuffled(Random(System.nanoTime()))
                    val correctIndex = options.indexOf(term).coerceAtLeast(0)
                    out += Question.AUDIO(
                        wordId = id,
                        prompt = "Listen to pronunciation and choose the matching word:",
                        term = term,
                        audioUrl = audioUrl,
                        pos = pos,
                        options = options,
                        correctIndex = correctIndex,
                        correctText = term
                    )
                }
            }
        }
        return out
    }

    fun answerMcq(chosenIndex: Int) {
        val q = ui.value.current as? Question.MCQ ?: return
        if (ui.value.feedback != null) return
        val correct = (chosenIndex == q.correctIndex)
        val user = q.options.getOrNull(chosenIndex) ?: ""
        recordAndShowFeedback(
            wordId = q.wordId,
            mode = QuizMode.MCQ,
            correct = correct,
            grade = if (correct) 5 else 2,
            userAnswer = user,
            correctAnswer = q.correctText,
            term = q.term,
            audioUrl = q.audioUrl,
            chosenIndex = chosenIndex,
            correctIndex = q.correctIndex
        )
    }

    fun answerAudio(chosenIndex: Int) {
        val q = ui.value.current as? Question.AUDIO ?: return
        if (ui.value.feedback != null) return
        val correct = (chosenIndex == q.correctIndex)
        val user = q.options.getOrNull(chosenIndex) ?: ""
        recordAndShowFeedback(
            wordId = q.wordId,
            mode = QuizMode.AUDIO,
            correct = correct,
            grade = if (correct) 5 else 2,
            userAnswer = user,
            correctAnswer = q.correctText,
            term = q.term,
            audioUrl = q.audioUrl,
            chosenIndex = chosenIndex,
            correctIndex = q.correctIndex
        )
    }

    fun answerType(input: String) {
        val q = ui.value.current as? Question.TYPE ?: return
        if (ui.value.feedback != null) return
        evaluateAndRecordInput(q.wordId, QuizMode.TYPE, input, q.correctText, q.prompt, q.term, q.audioUrl)
    }

    fun answerCloze(input: String) {
        val q = ui.value.current as? Question.CLOZE ?: return
        if (ui.value.feedback != null) return
        evaluateAndRecordInput(q.wordId, QuizMode.CLOZE, input, q.correctText, q.prompt, q.term, q.audioUrl)
    }

    private fun evaluateAndRecordInput(
        wordId: Long,
        mode: QuizMode,
        input: String,
        correctText: String,
        prompt: String,
        term: String,
        audioUrl: String?
    ) {
        val trimmed = input.trim()
        val exactMatch = normalized(trimmed) == normalized(correctText)
        if (exactMatch) {
            recordAndShowFeedback(
                wordId = wordId,
                mode = mode,
                correct = true,
                grade = 5,
                userAnswer = trimmed,
                correctAnswer = correctText,
                term = term,
                audioUrl = audioUrl
            )
            return
        }

        viewModelScope.launch {
            var isAcceptable = false
            var semanticNote: String? = null
            var grade = 2

            // Fast offline typo check via normalizer Levenshtein distance
            if (normalizer.isMinorTypo(trimmed, correctText)) {
                isAcceptable = true
                semanticNote = "Close enough! Accepted as minor spelling typo."
                grade = 4
            } else {
                // Check with Gemini if key is configured
                val apiKey = settings.geminiApiKey.first()
                if (apiKey.isNotBlank()) {
                    val eval = runCatching {
                        gemini.evaluateSemanticAnswer(
                            apiKey = apiKey,
                            targetTerm = correctText,
                            targetDefinition = prompt,
                            userTyped = trimmed
                        )
                    }.getOrNull()

                    if (eval != null && eval.isAcceptable) {
                        isAcceptable = true
                        semanticNote = if (eval.feedback.isNotBlank()) eval.feedback else "Accepted as a valid synonym!"
                        grade = 4
                    }
                }
            }

            recordAndShowFeedback(
                wordId = wordId,
                mode = mode,
                correct = isAcceptable,
                grade = if (isAcceptable) grade else 2,
                userAnswer = trimmed,
                correctAnswer = correctText,
                term = term,
                audioUrl = audioUrl,
                semanticNote = semanticNote
            )
        }
    }

    private fun normalized(s: String) = s.trim().lowercase()

    private fun recordAndShowFeedback(
        wordId: Long,
        mode: QuizMode,
        correct: Boolean,
        grade: Int,
        userAnswer: String,
        correctAnswer: String,
        term: String,
        audioUrl: String?,
        chosenIndex: Int? = null,
        correctIndex: Int? = null,
        semanticNote: String? = null
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
                wordId = wordId,
                mode = mode,
                prompt = ui.value.current?.prompt.orEmpty(),
                userAnswer = userAnswer,
                correctAnswer = correctAnswer,
                correct = correct,
                timeMs = elapsed
            )
            collected += row

            val newCorrect = ui.value.correctCount + if (correct) 1 else 0

            // 3) show feedback banner
            _ui.value = ui.value.copy(
                correctCount = newCorrect,
                elapsedMs = ui.value.elapsedMs + elapsed,
                summary = collected.toList(),
                feedback = AnswerFeedback(
                    correct = correct,
                    userAnswer = userAnswer,
                    correctAnswer = correctAnswer,
                    term = term,
                    audioUrl = audioUrl,
                    chosenIndex = chosenIndex,
                    correctIndex = correctIndex,
                    semanticNote = semanticNote
                )
            )
        }
    }

    fun requestMistakeExplanation() {
        val fb = ui.value.feedback ?: return
        if (fb.correct || !fb.explanation.isNullOrBlank()) return
        val cur = ui.value.current ?: return

        viewModelScope.launch {
            _explaining.value = true
            try {
                val apiKey = settings.geminiApiKey.first()
                if (apiKey.isNotBlank()) {
                    val explanation = gemini.explainMistake(
                        apiKey = apiKey,
                        term = fb.term,
                        definition = fb.correctAnswer,
                        userAnswer = fb.userAnswer,
                        contextSentence = cur.prompt
                    )
                    if (!explanation.isNullOrBlank()) {
                        _ui.update { it.copy(feedback = fb.copy(explanation = explanation)) }
                    }
                }
            } catch (_: Exception) {
            } finally {
                _explaining.value = false
            }
        }
    }

    fun generateStoryFromReview() {
        val words = questions.map { it.term }.distinct()
        if (words.isEmpty()) return
        viewModelScope.launch {
            _storyState.value = StoryUiState.Loading
            try {
                val apiKey = settings.geminiApiKey.first()
                if (apiKey.isBlank()) {
                    _storyState.value = StoryUiState.Error("Please set your Gemini API key in Settings to generate AI stories.")
                    return@launch
                }
                val story = gemini.generateStory(apiKey, words)
                if (!story.isNullOrBlank()) {
                    _storyState.value = StoryUiState.Success(story, words)
                } else {
                    _storyState.value = StoryUiState.Error("Could not generate story. Please check your network connection.")
                }
            } catch (e: Exception) {
                _storyState.value = StoryUiState.Error(e.message ?: "Failed to generate story")
            }
        }
    }

    fun dismissStory() {
        _storyState.value = StoryUiState.Idle
    }

    fun advanceAfterFeedback() {
        val nextIndex = ui.value.index + 1
        if (nextIndex >= questions.size) {
            _ui.value = ui.value.copy(
                stage = ReviewUi.Stage.Finished,
                index = questions.size,
                current = null,
                feedback = null
            )
        } else {
            _ui.value = ui.value.copy(
                index = nextIndex,
                current = questions[nextIndex],
                feedback = null
            )
            questionStartMs = System.currentTimeMillis()
        }
    }

    fun startMissedWords() {
        val missed = collected.filter { !it.correct }.map { it.wordId }.distinct()
        if (missed.isEmpty()) return
        viewModelScope.launch {
            try {
                collected.clear()
                queue = missed
                questions = buildQuestions(queue)
                _ui.value = ReviewUi(
                    stage = ReviewUi.Stage.InProgress,
                    total = questions.size,
                    index = 0,
                    current = questions.firstOrNull(),
                    isPractice = true,
                    dueCount = queue.size,
                    feedback = null
                )
                questionStartMs = System.currentTimeMillis()
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Failed to start review") }
            }
        }
    }

    fun restart() {
        _ui.value = ReviewUi()
        queue = emptyList()
        questions = emptyList()
        collected.clear()
        refreshDueCount()
    }
}
