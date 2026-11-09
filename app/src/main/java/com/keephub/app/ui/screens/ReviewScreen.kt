package com.keephub.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.keephub.app.ui.components.MarkdownText
import com.keephub.app.ui.review.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onBack: () -> Unit = {},
    vm: ReviewViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val storyState by vm.storyState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Review") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Exit review"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp) // M3 adaptive constraint
            ) {
                when (ui.stage) {
                    ReviewUi.Stage.Idle -> IdleView(
                        ui = ui,
                        onStart = { vm.start(practice = false) },
                        onPractice = { vm.start(practice = true) }
                    )
                    ReviewUi.Stage.InProgress -> InProgressView(ui = ui, vm = vm)
                    ReviewUi.Stage.Finished -> SummaryView(
                        ui = ui,
                        onRestart = { vm.restart() },
                        onPracticeMissed = { vm.startMissedWords() },
                        onReadStory = { vm.generateStoryFromReview() }
                    )
                    ReviewUi.Stage.Empty -> EmptyReviewView(
                        onPractice = { vm.start(practice = true) },
                        onBack = onBack
                    )
                }

                if (storyState !is StoryUiState.Idle) {
                    StoryBottomSheet(
                        state = storyState,
                        onDismiss = { vm.dismissStory() }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleView(
    ui: ReviewUi,
    onStart: () -> Unit,
    onPractice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val hasDue = ui.dueCount > 0
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (hasDue) Icons.Outlined.School else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (hasDue) "Ready to review today's words?" else "All caught up for today! 🎉",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (hasDue) {
                        "You have ${ui.dueCount} words scheduled for review today. Spaced repetition sharpens your retention over time."
                    } else {
                        "You have no words due for review right now. Keep your streak sharp with a quick practice session!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                if (hasDue) {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Start Review (${ui.dueCount} words)", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onPractice,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Practice Any Words", fontWeight = FontWeight.Medium)
                    }
                } else {
                    Button(
                        onClick = onPractice,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Start Practice Session", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReviewView(
    onPractice: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "No words to review",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Your daily review queue is empty. Save more words or start a practice session to continue learning.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onPractice,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Try Practice Mode", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Back to Vocabulary", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun InProgressView(ui: ReviewUi, vm: ReviewViewModel) {
    val current = ui.current ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Question ${ui.index + 1} of ${ui.total}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${((ui.index + 1f) / ui.total.coerceAtLeast(1) * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (ui.index + 1f) / (ui.total.coerceAtLeast(1)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Box(Modifier.padding(20.dp)) {
                when (current) {
                    is Question.MCQ -> McqCard(current, ui.feedback) { vm.answerMcq(it) }
                    is Question.TYPE -> TypeCard(current, ui.feedback, onSubmit = { vm.answerType(it) })
                    is Question.CLOZE -> ClozeCard(current, ui.feedback, onSubmit = { vm.answerCloze(it) })
                    is Question.AUDIO -> AudioCard(current, ui.feedback) { vm.answerAudio(it) }
                }
            }
        }

        val isExplaining by vm.explaining.collectAsState()

        // Immediate Answer Feedback Panel
        AnimatedVisibility(visible = ui.feedback != null) {
            ui.feedback?.let { fb ->
                Spacer(Modifier.height(16.dp))
                FeedbackCard(
                    feedback = fb,
                    isExplaining = isExplaining,
                    onExplainMistake = { vm.requestMistakeExplanation() },
                    onContinue = { vm.advanceAfterFeedback() }
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun McqCard(
    q: Question.MCQ,
    feedback: AnswerFeedback?,
    onChoose: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = q.prompt,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(16.dp))

        val optionLetters = listOf("A", "B", "C", "D", "E")
        q.options.forEachIndexed { idx, opt ->
            val isSelected = feedback?.chosenIndex == idx
            val isCorrect = feedback?.correctIndex == idx
            val containerColor = when {
                feedback == null -> MaterialTheme.colorScheme.surface
                isCorrect -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }

            OutlinedCard(
                onClick = { if (feedback == null) onChoose(idx) },
                enabled = feedback == null,
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = optionLetters.getOrElse(idx) { "${idx + 1}" },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (feedback != null) {
                        if (isCorrect) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Correct answer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = "Your answer",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioCard(
    q: Question.AUDIO,
    feedback: AnswerFeedback?,
    onChoose: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = q.prompt,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(16.dp))

        // Large Listening Speaker Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                PronunciationButton(
                    term = q.term,
                    audioUrl = q.audioUrl
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Tap to listen to pronunciation",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val optionLetters = listOf("A", "B", "C", "D")
        q.options.forEachIndexed { idx, opt ->
            val isSelected = feedback?.chosenIndex == idx
            val isCorrect = feedback?.correctIndex == idx
            val containerColor = when {
                feedback == null -> MaterialTheme.colorScheme.surface
                isCorrect -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }

            OutlinedCard(
                onClick = { if (feedback == null) onChoose(idx) },
                enabled = feedback == null,
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = optionLetters.getOrElse(idx) { "${idx + 1}" },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (feedback != null) {
                        if (isCorrect) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Correct answer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = "Your answer",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TypeCard(
    q: Question.TYPE,
    feedback: AnswerFeedback?,
    onSubmit: (String) -> Unit
) {
    var text by remember(q) { mutableStateOf("") }
    var showHint by remember(q) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val requester = remember { BringIntoViewRequester() }

    Column(
        Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
    ) {
        Text(
            text = q.prompt,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Smart Hint Chip
        val hintClue = remember(q) {
            val letters = "${q.correctText.first().uppercase()}${q.correctText.drop(1).map { if (it == ' ' || it == '-') it else '_' }.joinToString(" ")}"
            val posStr = q.pos?.let { "[${it.uppercase()}] " } ?: ""
            "$posStr$letters"
        }

        if (!showHint && feedback == null) {
            AssistChip(
                onClick = { showHint = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                label = { Text("Need a hint?", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.padding(top = 8.dp)
            )
        } else if (showHint) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Hint: $hintClue",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Your answer") },
            placeholder = { Text("Type the term here...") },
            singleLine = true,
            enabled = feedback == null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank() && feedback == null) {
                        onSubmit(text.trim())
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        scope.launch {
                            delay(150)
                            requester.bringIntoView()
                        }
                    }
                }
        )

        Spacer(Modifier.height(16.dp))

        if (feedback == null) {
            Button(
                onClick = { onSubmit(text.trim()) },
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Submit", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClozeCard(
    q: Question.CLOZE,
    feedback: AnswerFeedback?,
    onSubmit: (String) -> Unit
) {
    var text by remember(q) { mutableStateOf("") }
    var showHint by remember(q) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val requester = remember { BringIntoViewRequester() }

    Column(
        Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
    ) {
        Text(
            text = q.prompt,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Smart Hint Chip
        val hintClue = remember(q) {
            val letters = "${q.correctText.first().uppercase()}${q.correctText.drop(1).map { if (it == ' ' || it == '-') it else '_' }.joinToString(" ")}"
            val posStr = q.pos?.let { "[${it.uppercase()}] " } ?: ""
            "$posStr$letters"
        }

        if (!showHint && feedback == null) {
            AssistChip(
                onClick = { showHint = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                label = { Text("Need a hint?", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.padding(top = 8.dp)
            )
        } else if (showHint) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Hint: $hintClue",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Fill the blank") },
            placeholder = { Text("Type missing word...") },
            singleLine = true,
            enabled = feedback == null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank() && feedback == null) {
                        onSubmit(text.trim())
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        scope.launch {
                            delay(150)
                            requester.bringIntoView()
                        }
                    }
                }
        )

        Spacer(Modifier.height(16.dp))

        if (feedback == null) {
            Button(
                onClick = { onSubmit(text.trim()) },
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Submit", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    feedback: AnswerFeedback,
    isExplaining: Boolean = false,
    onExplainMistake: () -> Unit = {},
    onContinue: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (feedback.correct) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (feedback.correct) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                        contentDescription = null,
                        tint = if (feedback.correct) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (feedback.correct) "Correct! 🎉" else "Incorrect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (feedback.correct) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                // Pronunciation Audio Button with TTS Fallback
                PronunciationButton(
                    term = feedback.term,
                    audioUrl = feedback.audioUrl
                )
            }

            // Semantic / Typo Note (e.g. accepted synonym or minor typo)
            feedback.semanticNote?.let { note ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (!feedback.correct) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Correct answer: ${feedback.correctAnswer}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(Modifier.height(10.dp))

                // AI Tutor Explanation Box or Trigger Button
                if (!feedback.explanation.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "AI Tutor Explanation",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            MarkdownText(
                                text = feedback.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                boldColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onExplainMistake,
                        enabled = !isExplaining,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isExplaining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Asking AI Tutor...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Explain My Mistake with AI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (feedback.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    contentColor = if (feedback.correct) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SummaryView(
    ui: ReviewUi,
    onRestart: () -> Unit,
    onPracticeMissed: () -> Unit,
    onReadStory: () -> Unit
) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ui.elapsedMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ui.elapsedMs) % 60
    val missedCount = ui.summary.count { !it.correct }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary Score Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "Review completed!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        icon = {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Score: ${ui.correctCount}/${ui.total}") }
                    )
                    SuggestionChip(
                        onClick = {},
                        icon = {
                            Icon(
                                Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Time: ${minutes}m ${seconds}s") }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Question Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        if (ui.summary.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items = ui.summary, key = { it.index }) { row ->
                    SummaryRow(row)
                }
            }
        } else {
            Text(
                text = "No questions answered.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        // Practice Missed Words Button (if any missed)
        if (missedCount > 0) {
            FilledTonalButton(
                onClick = onPracticeMissed,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Practice Missed Words ($missedCount)", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
        }

        // AI Story Generator Button
        OutlinedButton(
            onClick = onReadStory,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Read AI Story with Reviewed Words", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onRestart,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Outlined.Replay, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Practice Again", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SummaryRow(row: AnswerRow) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${row.index}. ${row.mode}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (row.correct) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                    contentDescription = if (row.correct) "Correct" else "Incorrect",
                    tint = if (row.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = row.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Your answer: ${row.userAnswer.ifBlank { "—" }}",
                style = MaterialTheme.typography.bodySmall,
                color = if (row.correct) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
            )
            if (!row.correct) {
                Text(
                    text = "Correct answer: ${row.correctAnswer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StoryBottomSheet(
    state: StoryUiState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Vocabulary Story",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(14.dp))

            when (state) {
                is StoryUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Weaving your vocabulary words into a story...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is StoryUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Close")
                    }
                }
                is StoryUiState.Success -> {
                    Text(
                        text = "Featured Vocabulary:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.words.forEach { w ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(w, fontWeight = FontWeight.Medium) }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MarkdownText(
                            text = state.story,
                            style = MaterialTheme.typography.bodyMedium,
                            boldColor = MaterialTheme.colorScheme.primary,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Done", fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> {}
            }
        }
    }
}
