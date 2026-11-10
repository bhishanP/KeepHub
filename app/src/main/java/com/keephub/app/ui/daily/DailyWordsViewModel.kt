package com.keephub.app.ui.daily

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keephub.core.data.daily.DailyWordItem
import com.keephub.core.data.daily.DailyWordsManager
import com.keephub.core.data.daily.VocabularyLevel
import com.keephub.core.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

data class DailyWordsUiState(
    val level: VocabularyLevel = VocabularyLevel.INTERMEDIATE,
    val words: List<DailyWordItem> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val autoAddEnabled: Boolean = true,
    val isAiGenerated: Boolean = false
)

@HiltViewModel
class DailyWordsViewModel @Inject constructor(
    private val manager: DailyWordsManager,
    private val settings: SettingsStore,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyWordsUiState(isLoading = true))
    val uiState: StateFlow<DailyWordsUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        initTts()
        viewModelScope.launch {
            // Observe level changes from settings
            combine(
                manager.observeSelectedLevel(),
                settings.dailyWordsAutoAdd
            ) { level, autoAdd ->
                Pair(level, autoAdd)
            }.collect { (level, autoAdd) ->
                _uiState.update { it.copy(level = level, autoAddEnabled = autoAdd) }
                loadWordsForLevel(level)
            }
        }
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
    }

    fun loadWords() {
        viewModelScope.launch {
            loadWordsForLevel(_uiState.value.level)
        }
    }

    private suspend fun loadWordsForLevel(level: VocabularyLevel) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val today = LocalDate.now()
            // First ensure today's 5 words are auto-enrolled (default is YES)
            manager.ensureDailyWordsAutoAdded(today)
            var words = manager.getDailyWords(today, levelOverride = level)

            // If none are enrolled yet (e.g. first time user opens Study Words or auto-add was off),
            // enroll all 5 words by default as required.
            if (words.isNotEmpty() && words.none { it.isEnrolled }) {
                manager.enrollAll(words.map { it.word }, level)
                words = manager.getDailyWords(today, levelOverride = level)
            }

            val isAi = manager.isAiGeneratedForToday(level, today)
            _uiState.update {
                it.copy(
                    words = words,
                    isLoading = false,
                    isAiGenerated = isAi,
                    currentIndex = it.currentIndex.coerceIn(0, (words.size - 1).coerceAtLeast(0))
                )
            }
        } catch (_: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun changeLevel(level: VocabularyLevel) {
        viewModelScope.launch {
            manager.setLevel(level)
            val today = LocalDate.now()
            val newWords = manager.resolveWordsForDate(level, today)
            manager.enrollAll(newWords, level)
            _uiState.update { it.copy(level = level, currentIndex = 0) }
            loadWordsForLevel(level)
        }
    }

    fun markTodayReviewed() {
        viewModelScope.launch {
            manager.markTodayReviewed()
        }
    }

    fun toggleWord(item: DailyWordItem) {
        viewModelScope.launch {
            val targetState = !item.isEnrolled
            manager.setWordEnrolled(item.word, targetState, _uiState.value.level)
            // Update local state reactively
            _uiState.update { current ->
                val updated = current.words.map {
                    if (it.word.term.equals(item.word.term, ignoreCase = true)) {
                        it.copy(isEnrolled = targetState)
                    } else it
                }
                current.copy(words = updated)
            }
        }
    }

    fun enrollAll() {
        viewModelScope.launch {
            val level = _uiState.value.level
            val words = _uiState.value.words.map { it.word }
            manager.enrollAll(words, level)
            _uiState.update { current ->
                current.copy(words = current.words.map { it.copy(isEnrolled = true) })
            }
        }
    }

    fun selectIndex(index: Int) {
        if (index in _uiState.value.words.indices) {
            _uiState.update { it.copy(currentIndex = index) }
        }
    }

    fun nextWord() {
        val next = _uiState.value.currentIndex + 1
        if (next < _uiState.value.words.size) {
            _uiState.update { it.copy(currentIndex = next) }
        }
    }

    fun prevWord() {
        val prev = _uiState.value.currentIndex - 1
        if (prev >= 0) {
            _uiState.update { it.copy(currentIndex = prev) }
        }
    }

    fun speakWord(term: String) {
        if (ttsReady) {
            tts?.speak(term, TextToSpeech.QUEUE_FLUSH, null, "daily_word_speak")
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
