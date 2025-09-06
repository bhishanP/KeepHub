package com.keephub.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keephub.core.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore
) : ViewModel() {

    // Expose flows to the UI
    val translationLang = store.translationLang
    val dailyGoal = store.dailyGoal
    val quizModes = store.quizModes
    val notifyHour = store.notifyHour

    // Mutators
    fun setLang(code: String) = launch { store.setTranslationLang(code.lowercase()) }
    fun setGoal(goal: Int)   = launch { store.setDailyGoal(goal) }
    fun setHour(h: Int)      = launch { store.setNotifyHour(h) }

    fun toggleMode(label: String, enabled: Boolean) = launch {
        val cur = quizModes.first().toMutableSet()
        if (enabled) cur += label else cur -= label
        if (cur.isEmpty()) cur += "MCQ"          // always keep at least one mode
        store.setQuizModes(cur)
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }
}
