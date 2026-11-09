package com.keephub.app.ui.screens

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.keephub.core.data.net.GeminiTestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lang by vm.translationLang.collectAsState(initial = "en")
    val goal by vm.dailyGoal.collectAsState(initial = 20)
    val modes by vm.quizModes.collectAsState(initial = setOf("MCQ", "TYPE", "CLOZE", "AUDIO"))
    val hour by vm.notifyHour.collectAsState(initial = 19)
    val isReminderActive by vm.isReminderActive.collectAsState()
    val geminiApiKey by vm.geminiApiKey.collectAsState(initial = "")

    val areNotificationsEnabled = remember(context) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        val scrollState = rememberScrollState()
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(text = "Goal: $goal/day")
                        Pill(text = "Reminder: ${hour.toString().padStart(2, '0')}:00")
                        Pill(text = if (geminiApiKey.isNotBlank()) "AI: Active" else "AI: Off")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Section 1: Study Goal ---
            SectionCard(
                title = "Daily review goal",
                subtitle = "How many words to practice each day",
                icon = { Icon(Icons.Outlined.Flag, contentDescription = null) }
            ) {
                // Tactile Stepper Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val newG = maxOf(1, goal - 5)
                                vm.setGoal(newG)
                            },
                            enabled = goal > 1
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Decrease goal by 5")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$goal words",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "daily practice target",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                val newG = minOf(100, goal + 5)
                                vm.setGoal(newG)
                            },
                            enabled = goal < 100
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Increase goal by 5")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick Pick Presets
                Text(
                    text = "Quick presets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val goalPresets = listOf(
                        Pair(5, "5 (Light)"),
                        Pair(10, "10 (Casual)"),
                        Pair(20, "20 (Standard)"),
                        Pair(30, "30 (Active)"),
                        Pair(50, "50 (Intense)")
                    )
                    goalPresets.forEach { (presetGoal, label) ->
                        FilterChip(
                            selected = goal == presetGoal,
                            onClick = {
                                vm.setGoal(presetGoal)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Daily goal set to $presetGoal words")
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Section 2: Quiz Modes ---
            SectionCard(
                title = "Quiz modes",
                subtitle = "Choose question types in your reviews",
                icon = { Icon(Icons.AutoMirrored.Outlined.FormatListBulleted, contentDescription = null) }
            ) {
                FlowChipsRow(
                    chips = listOf(
                        ChipData("MCQ", Icons.AutoMirrored.Outlined.FormatListBulleted, modes.contains("MCQ")),
                        ChipData("TYPE", Icons.Outlined.Keyboard, modes.contains("TYPE")),
                        ChipData("CLOZE", Icons.Outlined.TextFields, modes.contains("CLOZE")),
                        ChipData("AUDIO", Icons.Outlined.Headphones, modes.contains("AUDIO")),
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

            // --- Section 3: Daily Reminder Time ---
            SectionCard(
                title = "Daily reminder",
                subtitle = "Pick notification time to protect your streak",
                icon = { Icon(Icons.Outlined.AccessTime, contentDescription = null) }
            ) {
                if (!areNotificationsEnabled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notifications are disabled",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Tap here to allow notifications in Android settings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Tactile Hour Stepper Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val newH = if (hour == 0) 23 else hour - 1
                                vm.setHour(newH)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Reminder scheduled for ${newH.toString().padStart(2, '0')}:00")
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Step 1 hour earlier")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${hour.toString().padStart(2, '0')}:00",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = when (hour) {
                                    in 5..11 -> "Morning reminder"
                                    in 12..16 -> "Afternoon reminder"
                                    in 17..20 -> "Evening reminder"
                                    else -> "Night reminder"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                val newH = if (hour == 23) 0 else hour + 1
                                vm.setHour(newH)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Reminder scheduled for ${newH.toString().padStart(2, '0')}:00")
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Step 1 hour later")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Time-of-Day Quick Presets
                Text(
                    text = "Time-of-day presets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val timePresets = listOf(
                        Triple(8, "Morning", "08:00"),
                        Triple(12, "Noon", "12:00"),
                        Triple(18, "Evening", "18:00"),
                        Triple(21, "Night", "21:00")
                    )
                    timePresets.forEach { (presetH, label, timeStr) ->
                        FilterChip(
                            selected = hour == presetH,
                            onClick = {
                                vm.setHour(presetH)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Reminder scheduled for $timeStr")
                                }
                            },
                            label = { Text("$label ($timeStr)") }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isReminderActive) Icons.Outlined.Check else Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isReminderActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (isReminderActive) "Reminder active" else "Scheduling...",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isReminderActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val sent = vm.sendTestNotification()
                            scope.launch {
                                if (sent) {
                                    snackbarHostState.showSnackbar("Test notification sent! Check your notification bar.")
                                } else {
                                    snackbarHostState.showSnackbar("Notifications are disabled in Android Settings.")
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Send Test", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Section 4: Gemini AI (BYOK) ---
            val geminiRequester = remember { BringIntoViewRequester() }
            val imeVisible = WindowInsets.isImeVisible
            LaunchedEffect(imeVisible) {
                if (imeVisible) {
                    geminiRequester.bringIntoView()
                }
            }

            SectionCard(
                title = "Gemini AI (BYOK)",
                subtitle = "Smart mnemonics & CEFR examples with your free API key",
                icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                var keyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
                var showKey by remember { mutableStateOf(false) }
                var isTesting by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxWidth().bringIntoViewRequester(geminiRequester)) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        singleLine = true,
                        label = { Text("Google Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    imageVector = if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (showKey) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        supportingText = {
                            Text("Get a free key at aistudio.google.com. Falls back to offline dictionary if blank.")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch {
                                        delay(200)
                                        geminiRequester.bringIntoView()
                                    }
                                }
                            }
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (geminiApiKey.isNotBlank())
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (geminiApiKey.isNotBlank()) Icons.Outlined.Check else Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (geminiApiKey.isNotBlank())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (geminiApiKey.isNotBlank()) "AI Active" else "Fallback Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (geminiApiKey.isNotBlank())
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (keyInput.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isTesting = true
                                            when (val result = vm.testGeminiConnectionDetailed(keyInput)) {
                                                is GeminiTestResult.Success -> {
                                                    snackbarHostState.showSnackbar(
                                                        "Gemini connected successfully (${result.modelUsed})!"
                                                    )
                                                }
                                                is GeminiTestResult.Failure -> {
                                                    snackbarHostState.showSnackbar(
                                                        result.message,
                                                        duration = SnackbarDuration.Long
                                                    )
                                                }
                                            }
                                            isTesting = false
                                        }
                                    },
                                    enabled = !isTesting,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (isTesting) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text("Test Key", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        val cleaned = vm.sanitizeApiKey(keyInput)
                                        vm.setGeminiApiKey(cleaned)
                                        snackbarHostState.showSnackbar(
                                            if (cleaned.isNotBlank()) "Gemini API key saved!" else "Gemini API key removed. Using fallback."
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Save", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Section 5: Translation (hidden if feature off) ---
            AnimatedVisibility(visible = com.keephub.core.data.BuildConfig.ENABLE_TRANSLATION) {
                val translationRequester = remember { BringIntoViewRequester() }
                SectionCard(
                    title = "Translation language",
                    subtitle = "Target language for headword translation",
                    icon = { Icon(Icons.Outlined.Translate, contentDescription = null) }
                ) {
                    var edit by remember(lang) { mutableStateOf(lang) }
                    Column(modifier = Modifier.fillMaxWidth().bringIntoViewRequester(translationRequester)) {
                        OutlinedTextField(
                            value = edit,
                            onValueChange = { edit = it },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Outlined.Translate, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Done
                            ),
                            supportingText = { Text("Use ISO code like “hi”, “es”, “fr”.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        scope.launch {
                                            delay(200)
                                            translationRequester.bringIntoView()
                                        }
                                    }
                                }
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
            }

            Spacer(Modifier.height(80.dp))
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
    val maxCharsPerRow = 28
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var row = mutableListOf<ChipData>()
        var widthUsed = 0
        for (c in chips) {
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
                row.forEach { chip -> ModeChip(chip.label, chip.checked, chip.icon) { newChecked -> onToggle(chip.label, newChecked) } }
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
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        }
    )
}
