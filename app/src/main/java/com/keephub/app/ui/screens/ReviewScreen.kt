package com.keephub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.keephub.app.ui.review.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(vm: ReviewViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Review") }) }
    ) { padding ->
        when (ui.stage) {
            ReviewUi.Stage.Idle -> IdleView(modifier = Modifier.padding(padding), onStart = { vm.start() })
            ReviewUi.Stage.InProgress -> InProgressView(modifier = Modifier.padding(padding), ui = ui, vm = vm)
            ReviewUi.Stage.Finished -> SummaryView(modifier = Modifier.padding(padding), ui = ui, onRestart = { vm.restart() })
        }
    }
}

@Composable
private fun IdleView(modifier: Modifier = Modifier, onStart: () -> Unit) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Ready to review today’s words?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start") }
    }
}

@Composable
private fun InProgressView(modifier: Modifier, ui: ReviewUi, vm: ReviewViewModel) {
    val current = ui.current ?: return
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Question ${ui.index + 1} of ${ui.total}", fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(
            progress = { (ui.index + 1f) / (ui.total.coerceAtLeast(1)) },
            modifier = Modifier.fillMaxWidth(),
            color = ProgressIndicatorDefaults.linearColor,
            trackColor = ProgressIndicatorDefaults.linearTrackColor,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        Spacer(Modifier.height(12.dp))
        when (current) {
            is Question.MCQ -> McqCard(current) { vm.answerMcq(it) }
            is Question.TYPE -> TypeCard(current, onSubmit = { vm.answerType(it) })
            is Question.CLOZE -> ClozeCard(current, onSubmit = { vm.answerCloze(it) })
        }
    }
}

@Composable
private fun SummaryView(modifier: Modifier, ui: ReviewUi, onRestart: () -> Unit) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ui.elapsedMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ui.elapsedMs) % 60
    Column(modifier.fillMaxSize().padding(24.dp)) {
        Text("All done!", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("Score: ${ui.correctCount}/${ui.total}")
        Text("Time: ${minutes}m ${seconds}s")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRestart) { Text("Restart") }
    }
}

@Composable
private fun McqCard(q: Question.MCQ, onChoose: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(q.prompt, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        q.options.forEachIndexed { idx, opt ->
            OutlinedButton(
                onClick = { onChoose(idx) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) { Text(opt) }
        }
    }
}

@Composable
private fun TypeCard(q: Question.TYPE, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth()) {
        Text(q.prompt, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Your answer") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onSubmit(text) }, enabled = text.isNotBlank()) { Text("Submit") }
    }
}

@Composable
private fun ClozeCard(q: Question.CLOZE, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth()) {
        Text(q.prompt, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Fill the blank") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onSubmit(text) }, enabled = text.isNotBlank()) { Text("Submit") }
    }
}
