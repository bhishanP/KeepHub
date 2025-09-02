package com.keephub.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val duplicateId: Long? = null,
    val canSave: Boolean = false
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
        debounceJob = viewModelScope.launch {
            delay(200)
            val existing = repo.checkDuplicateForTerm(term)
            _ui.update { it.copy(duplicateId = existing?.id) }
        }
    }

    fun save(onSaved: (Long) -> Unit) {
        val state = _ui.value
        val term = state.termField.trim()
        if (term.isBlank()) return

        viewModelScope.launch {
            val tags = state.tagsField.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val notes = state.notesField.ifBlank { null }
            when (val res = repo.addWord(term = term, notes = notes, tags = tags)) {
                is AddWordResult.Success -> onSaved(res.id)
                is AddWordResult.Duplicate -> _ui.update { it.copy(duplicateId = res.existingId) }
            }
        }
    }
}
