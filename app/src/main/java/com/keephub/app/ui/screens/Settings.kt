package com.keephub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.keephub.core.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val lang by vm.translationLang.collectAsState(initial = "en")
    val goal by vm.dailyGoal.collectAsState(initial = 20)
    val modes by vm.quizModes.collectAsState(initial = setOf("MCQ","TYPE","CLOZE"))
    val hour by vm.notifyHour.collectAsState(initial = 19)

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Translation language (ISO code)", style = MaterialTheme.typography.titleMedium)
            var editLang by remember(lang) { mutableStateOf(lang) }
            OutlinedTextField(value = editLang, onValueChange = { editLang = it }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.setLang(editLang) }) { Text("Save language") }

            Spacer(Modifier.height(16.dp))
            Text("Daily review goal", style = MaterialTheme.typography.titleMedium)
            var editGoal by remember(goal) { mutableStateOf(goal.toString()) }
            OutlinedTextField(value = editGoal, onValueChange = { editGoal = it.filter { ch -> ch.isDigit() } }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.setGoal(editGoal.toIntOrNull() ?: goal) }) { Text("Save goal") }

            Spacer(Modifier.height(16.dp))
            Text("Quiz modes", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth()) {
                ModeChip("MCQ", modes.contains("MCQ")) { vm.toggleMode("MCQ", it) }
                Spacer(Modifier.width(8.dp))
                ModeChip("TYPE", modes.contains("TYPE")) { vm.toggleMode("TYPE", it) }
                Spacer(Modifier.width(8.dp))
                ModeChip("CLOZE", modes.contains("CLOZE")) { vm.toggleMode("CLOZE", it) }
            }

            Spacer(Modifier.height(16.dp))
            Text("Daily notification time (24h)", style = MaterialTheme.typography.titleMedium)
            var editHour by remember(hour) { mutableStateOf(hour.toString()) }
            OutlinedTextField(value = editHour, onValueChange = { editHour = it.filter { ch -> ch.isDigit() } }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.setHour(editHour.toIntOrNull() ?: hour) }) { Text("Save time") }
        }
    }
}

@Composable
private fun ModeChip(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    FilterChip(
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        label = { Text(label) }
    )
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore
) : ViewModel() {
    val translationLang = store.translationLang
    val dailyGoal = store.dailyGoal
    val quizModes = store.quizModes
    val notifyHour = store.notifyHour

    fun setLang(code: String) = launch { store.setTranslationLang(code.lowercase()) }
    fun setGoal(goal: Int) = launch { store.setDailyGoal(goal) }
    fun setHour(h: Int) = launch { store.setNotifyHour(h) }
    fun toggleMode(label: String, enabled: Boolean) = launch {
        val cur = quizModes.first().toMutableSet()
        if (enabled) cur += label else cur -= label
        if (cur.isEmpty()) cur += "MCQ"
        store.setQuizModes(cur)
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }
}
