package com.keephub.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val lang by vm.translationLang.collectAsState(initial = "en")
    val goal by vm.dailyGoal.collectAsState(initial = 20)
    val modes by vm.quizModes.collectAsState(initial = setOf("MCQ","TYPE","CLOZE"))
    val hour by vm.notifyHour.collectAsState(initial = 19)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            // Gradient header card
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = .18f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = .14f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text("Tune your study experience", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Pill(text = "Goal: $goal/day")
                        Pill(text = "Reminder: ${hour.toString().padStart(2,'0')}:00")
                        Pill(text = "Modes: ${modes.joinToString("·")}")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Study Goal ---
            SectionCard(
                title = "Daily review goal",
                subtitle = "How many words to review per day",
                icon = { Icon(Icons.Outlined.Flag, contentDescription = null) }
            ) {
                var sliderVal by remember(goal) { mutableStateOf(goal.toFloat()) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${sliderVal.toInt()} / day", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(0.3f))
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it.coerceIn(1f, 50f) },
                        valueRange = 1f..50f,
                        modifier = Modifier.weight(0.7f),
                        onValueChangeFinished = {
                            val newGoal = sliderVal.toInt()
                            scope.launch {
                                vm.setGoal(newGoal)
                                snackbarHostState.showSnackbar("Saved goal: $newGoal / day")
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Quiz Modes ---
            SectionCard(
                title = "Quiz modes",
                subtitle = "Choose how you want to practice",
                icon = { Icon(Icons.AutoMirrored.Outlined.FormatListBulleted, contentDescription = null) }
            ) {
                FlowChipsRow(
                    chips = listOf(
                        ChipData("MCQ", Icons.AutoMirrored.Outlined.FormatListBulleted, modes.contains("MCQ")),
                        ChipData("TYPE", Icons.Outlined.Keyboard, modes.contains("TYPE")),
                        ChipData("CLOZE", Icons.Outlined.TextFields, modes.contains("CLOZE")),
                    ),
                    onToggle = { label, newState ->
                        scope.launch {
                            vm.toggleMode(label, newState)
                            snackbarHostState.showSnackbar(
                                if (newState) "Enabled $label" else "Disabled $label"
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // --- Reminder Time ---
            SectionCard(
                title = "Daily reminder time",
                subtitle = "We’ll notify you to review at this time",
                icon = { Icon(Icons.Outlined.AccessTime, contentDescription = null) }
            ) {
                var h by remember(hour) { mutableStateOf(hour.toFloat()) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${h.toInt().toString().padStart(2,'0')}:00", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(0.3f))
                    Slider(
                        value = h,
                        onValueChange = { h = it.coerceIn(0f, 23f) },
                        valueRange = 0f..23f,
                        modifier = Modifier.weight(0.7f),
                        onValueChangeFinished = {
                            val newHour = h.toInt()
                            scope.launch {
                                vm.setHour(newHour)
                                snackbarHostState.showSnackbar("Reminder set to ${newHour.toString().padStart(2,'0')}:00")
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Translation (hidden if feature off) ---
            AnimatedVisibility(visible = com.keephub.core.data.BuildConfig.ENABLE_TRANSLATION) {
                SectionCard(
                    title = "Translation language",
                    subtitle = "Target language for headword translation",
                    icon = { Icon(Icons.Outlined.Translate, contentDescription = null) }
                ) {
                    var edit by remember(lang) { mutableStateOf(lang) }
                    OutlinedTextField(
                        value = edit,
                        onValueChange = { edit = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Translate, contentDescription = null) },
                        supportingText = { Text("Use ISO code like “hi”, “es”, “fr”.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                vm.setLang(edit.trim().lowercase())
                                snackbarHostState.showSnackbar("Saved translation language: ${edit.trim().lowercase()}")
                            }
                        }
                    ) { Text("Save language") }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ---------- Small reusable bits ---------- */

@Composable
private fun Pill(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .6f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) { Text(text, style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: @Composable (() -> Unit),
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = .15f)),
                    contentAlignment = Alignment.Center
                ) { icon() }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

data class ChipData(val label: String, val icon: ImageVector, val checked: Boolean)

@Composable
private fun FlowChipsRow(
    chips: List<ChipData>,
    onToggle: (label: String, newState: Boolean) -> Unit
) {
    // Simple wrap layout using rows; avoids extra deps
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var row = mutableListOf<ChipData>()
        var widthUsed = 0
        val maxCharsPerRow = 28

        chips.forEach { c ->
            val len = c.label.length + 6
            if (widthUsed + len > maxCharsPerRow) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { chip -> ModeChip(chip.label, chip.checked, chip.icon) { newChecked -> onToggle(chip.label, newChecked) } }
                }
                row = mutableListOf()
                widthUsed = 0
            }
            row += c; widthUsed += len
        }
        if (row.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { chip -> ModeChip(chip.label, chip.checked, chip.icon) {newChecked -> onToggle(chip.label, newChecked) } }
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    checked: Boolean,
    icon: ImageVector,
    onCheckedChange: (Boolean) -> Unit
) {
    FilterChip(
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}
