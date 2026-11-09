package com.keephub.app.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keephub.app.ui.components.MarkdownText
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keephub.app.ui.components.Shimmer
import com.keephub.core.data.db.dao.WordWithDetails
import com.keephub.core.data.db.entity.SrsStateEntity
import com.keephub.core.data.repo.WordRepository
import com.keephub.core.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordDetailScreen(
    id: Long,
    onBack: () -> Unit,
    vm: WordDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(id) { vm.setId(id) }
    val ui by vm.ui.collectAsState()
    val deleted by vm.deleted.collectAsState()

    LaunchedEffect(deleted) { if (deleted) onBack() }

    var showConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ui.word?.word?.term ?: "Word details",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.enrichNow() },
                        enabled = !ui.loading,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh definitions and pronunciation"
                        )
                    }
                    IconButton(
                        onClick = { showConfirm = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete word",
                            tint = MaterialTheme.colorScheme.error
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp) // M3 Adaptive readable bound
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val w = ui.word
                if (w == null) {
                    Shimmer(height = 24)
                    Spacer(Modifier.height(12.dp))
                    Shimmer(height = 16, widthFraction = 0.5f)
                    Spacer(Modifier.height(24.dp))
                    Shimmer(height = 120)
                    return@Column
                }

                // Header Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = w.word.term,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                val topIpa = w.senses.firstOrNull { !it.ipa.isNullOrBlank() }?.ipa
                                if (!topIpa.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = topIpa,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            PronunciationButton(
                                term = w.word.term,
                                audioUrl = w.senses.firstOrNull { it.audioUrls.isNotEmpty() }?.audioUrls?.firstOrNull()
                            )
                        }

                        if (w.word.tags.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                w.word.tags.forEach { tag ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(tag) }
                                    )
                                }
                            }
                        }

                        w.word.notes?.let { notes ->
                            val isMnemonic = notes.startsWith("💡 Mnemonic:")
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isMnemonic)
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isMnemonic) {
                                            Icon(
                                                imageVector = Icons.Outlined.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        Text(
                                            text = if (isMnemonic) "AI Memory Anchor" else "Personal notes",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isMnemonic)
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    MarkdownText(
                                        text = if (isMnemonic) notes.removePrefix("💡 Mnemonic:").trim() else notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isMnemonic)
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                        boldColor = if (isMnemonic) MaterialTheme.colorScheme.tertiary else null
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                SrsMasteryCard(srs = w.srs)

                if (ui.error != null) {
                    Spacer(Modifier.height(16.dp))
                    ErrorState(message = ui.error, actionLabel = "Retry") { vm.enrichNow() }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Meanings & Definitions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))

                if (ui.loading && w.senses.isEmpty()) {
                    Shimmer(height = 100)
                    Spacer(Modifier.height(8.dp))
                    Shimmer(height = 100)
                } else if (w.senses.isEmpty()) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No definitions cached yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(onClick = { vm.enrichNow() }) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Fetch from dictionary")
                            }
                        }
                    }
                } else {
                    w.senses.forEachIndexed { i, s ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "${i + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        s.pos?.let { pos ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = pos.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (s.audioUrls.isNotEmpty()) {
                                        AudioChip(url = s.audioUrls.first())
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = s.definition,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                ExamplesBlock(s.examples)

                                if (s.synonyms.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "Synonyms",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        s.synonyms.take(6).forEach { syn ->
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(syn, style = MaterialTheme.typography.bodySmall) }
                                            )
                                        }
                                    }
                                }

                                if (s.antonyms.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "Antonyms",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        s.antonyms.take(6).forEach { ant ->
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(ant, style = MaterialTheme.typography.bodySmall) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (com.keephub.core.data.BuildConfig.ENABLE_TRANSLATION) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Translations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    if (w.translations.isEmpty()) {
                        Text(
                            text = "No translations available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        w.translations.forEach { t ->
                            Text(
                                text = "${t.languageCode.uppercase()}: ${t.text}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete word?") },
            text = {
                Text("This permanently removes this word and its review history from your vocabulary.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        vm.deleteWord()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ErrorState(
    message: String?,
    actionLabel: String,
    onRetry: (() -> Unit)? = null
) {
    if (message != null) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Error: $message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onRetry?.invoke() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun ExamplesBlock(examples: List<String>) {
    if (examples.isEmpty()) return
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Examples",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary
    )
    Spacer(Modifier.height(4.dp))
    examples.forEach { ex ->
        Text(
            text = "“$ex”",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}

@Composable
fun PronunciationButton(
    term: String,
    audioUrl: String? = null
) {
    val context = LocalContext.current
    var isSpeaking by remember { mutableStateOf(false) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(context) {
        val speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // speech ready
            }
        }
        speech.language = Locale.US
        speech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { isSpeaking = true }
            override fun onDone(utteranceId: String?) { isSpeaking = false }
            override fun onError(utteranceId: String?) { isSpeaking = false }
        })
        tts = speech
        onDispose {
            speech.stop()
            speech.shutdown()
        }
    }

    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var mediaPrepared by remember { mutableStateOf(false) }

    DisposableEffect(audioUrl) {
        if (!audioUrl.isNullOrBlank()) {
            val player = MediaPlayer()
            try {
                val src = if (audioUrl.startsWith("//")) "https:$audioUrl" else audioUrl
                val attrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
                player.setAudioAttributes(attrs)
                player.setDataSource(src)
                player.setOnPreparedListener { mediaPrepared = true }
                player.setOnCompletionListener { isSpeaking = false }
                player.setOnErrorListener { _, _, _ ->
                    mediaPrepared = false
                    true
                }
                player.prepareAsync()
                mediaPlayer = player
            } catch (_: Exception) {
                mediaPrepared = false
            }
        }
        onDispose {
            runCatching {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
            mediaPlayer = null
            mediaPrepared = false
        }
    }

    FilledTonalIconButton(
        onClick = {
            if (isSpeaking) {
                runCatching { mediaPlayer?.pause() }
                tts?.stop()
                isSpeaking = false
            } else if (mediaPrepared && mediaPlayer != null) {
                try {
                    mediaPlayer?.seekTo(0)
                    mediaPlayer?.start()
                    isSpeaking = true
                } catch (_: Exception) {
                    tts?.speak(term, TextToSpeech.QUEUE_FLUSH, null, "keep_hub_pronounce")
                }
            } else {
                tts?.speak(term, TextToSpeech.QUEUE_FLUSH, null, "keep_hub_pronounce")
            }
        },
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = if (isSpeaking) Icons.Outlined.Pause else Icons.AutoMirrored.Outlined.VolumeUp,
            contentDescription = "Pronounce $term",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun AudioChip(url: String) {
    PronunciationButton(term = "", audioUrl = url)
}

data class DetailUi(
    val word: WordWithDetails? = null,
    val targetLang: String = "en",
    val error: String? = null,
    val loading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WordDetailViewModel @Inject constructor(
    private val repo: WordRepository,
    private val settings: SettingsStore
) : ViewModel() {

    private val id = MutableStateFlow<Long>(-1)
    private val refreshTrigger = MutableStateFlow(0)
    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _deleted = MutableStateFlow(false)

    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    val ui: StateFlow<DetailUi> =
        combine(id, refreshTrigger, _loading, _error) { wordId, _, loading, err ->
            Triple(wordId, loading, err)
        }.flatMapLatest { (wordId, loading, err) ->
            if (wordId <= 0) flowOf(DetailUi(loading = loading, error = err))
            else repo.observeWord(wordId).map { details ->
                DetailUi(word = details, loading = loading, error = err)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUi())

    fun setId(wordId: Long) {
        id.value = wordId
    }

    fun enrichNow() {
        val wordId = id.value
        if (wordId <= 0) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val lang = settings.translationLang.first()
                repo.ensureEnriched(wordId, lang, forceRefresh = true)
                refreshTrigger.value++
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to refresh"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteWord() {
        val wordId = id.value
        if (wordId <= 0) return
        viewModelScope.launch {
            val ok = repo.deleteWord(wordId)
            if (ok) _deleted.value = true
        }
    }
}

@Composable
private fun SrsMasteryCard(srs: SrsStateEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Spaced Repetition Mastery",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val stageLabel = when {
                    srs == null || srs.repetitions == 0 -> "New"
                    srs.intervalDays < 7 -> "Learning"
                    else -> "Mastered"
                }
                val stageContainerColor = when {
                    srs == null || srs.repetitions == 0 -> MaterialTheme.colorScheme.secondaryContainer
                    srs.intervalDays < 7 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                val stageContentColor = when {
                    srs == null || srs.repetitions == 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                    srs.intervalDays < 7 -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stageContainerColor
                ) {
                    Text(
                        text = stageLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = stageContentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val progress = when {
                srs == null || srs.repetitions == 0 -> 0.1f
                srs.intervalDays < 3 -> 0.35f
                srs.intervalDays < 7 -> 0.65f
                else -> 1.0f
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(12.dp))

            val today = java.time.LocalDate.now()
            val dueText = when {
                srs == null -> "Queued for first quiz"
                srs.dueDate <= today -> "Due today"
                srs.dueDate == today.plusDays(1) -> "Due tomorrow"
                else -> {
                    val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, srs.dueDate)
                    "In $daysUntil days (${srs.dueDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${srs.dueDate.dayOfMonth})"
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Next review",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dueText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (srs != null && srs.dueDate <= today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Repetitions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${srs?.repetitions ?: 0} times",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Interval",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${srs?.intervalDays ?: 0} days",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
