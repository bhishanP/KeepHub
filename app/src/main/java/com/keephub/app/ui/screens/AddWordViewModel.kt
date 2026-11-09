package com.keephub.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keephub.core.data.db.entity.WordEntity
import com.keephub.core.data.repo.AddWordResult
import com.keephub.core.data.repo.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddUiState(
    val termField: String = "",
    val tagsField: String = "",
    val notesField: String = "",
    val duplicateWord: WordEntity? = null,
    val similarWords: List<WordEntity> = emptyList(),
    val canSave: Boolean = false,
    val isMerging: Boolean = false
)

@HiltViewModel
class AddWordViewModel @Inject constructor(
    private val repo: WordRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _ui = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _ui.asStateFlow()

    private var debounceJob: Job? = null

    fun setInitial(term: String) {
        if (_ui.value.termField.isBlank() && term.isNotBlank()) {
            _ui.update { it.copy(termField = term, canSave = true) }
            checkDuplicateDebounced(term)
        }
    }

    fun updateTerm(s: String) {
        _ui.update { it.copy(termField = s, canSave = s.isNotBlank()) }
        checkDuplicateDebounced(s)
    }

    fun updateTags(s: String) { _ui.update { it.copy(tagsField = s) } }
    fun updateNotes(s: String) { _ui.update { it.copy(notesField = s) } }

    private fun checkDuplicateDebounced(term: String) {
        debounceJob?.cancel()
        val trimmed = term.trim()
        if (trimmed.isBlank()) {
            _ui.update { it.copy(duplicateWord = null, similarWords = emptyList()) }
            return
        }

        debounceJob = viewModelScope.launch {
            delay(150)
            val existing = repo.checkDuplicateForTerm(trimmed)
            val similar = if (existing == null && trimmed.length >= 3) {
                repo.findSimilarWords(trimmed)
            } else {
                emptyList()
            }
            _ui.update { it.copy(duplicateWord = existing, similarWords = similar) }
        }
    }

    fun save(onSaved: (Long) -> Unit) {
        val state = _ui.value
        val term = state.termField.trim()
        if (term.isBlank()) return

        viewModelScope.launch {
            // Immediate check before insert (guards against fast typing race conditions)
            val existing = repo.checkDuplicateForTerm(term)
            if (existing != null) {
                _ui.update { it.copy(duplicateWord = existing) }
                return@launch
            }

            val tags = parseTags(state.tagsField)
            val notes = state.notesField.ifBlank { null }
            when (val res = repo.addWord(term = term, notes = notes, tags = tags)) {
                is AddWordResult.Success -> onSaved(res.id)
                is AddWordResult.Duplicate -> {
                    val dupWord = repo.checkDuplicateForTerm(term)
                    _ui.update { it.copy(duplicateWord = dupWord) }
                }
            }
        }
    }

    fun mergeIntoExisting(onMerged: (Long) -> Unit) {
        val state = _ui.value
        val existing = state.duplicateWord ?: return
        val tags = parseTags(state.tagsField)
        val notes = state.notesField.ifBlank { null }

        _ui.update { it.copy(isMerging = true) }
        viewModelScope.launch {
            val success = repo.mergeWord(existing.id, newNotes = notes, newTags = tags)
            _ui.update { it.copy(isMerging = false) }
            if (success) {
                onMerged(existing.id)
            }
        }
    }

    private fun parseTags(raw: String): List<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
