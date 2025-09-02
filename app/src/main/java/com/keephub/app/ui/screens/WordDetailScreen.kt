package com.keephub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keephub.app.ui.components.Shimmer
import com.keephub.core.data.db.dao.WordWithDetails
import com.keephub.core.data.repo.WordRepository
import com.keephub.core.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.platform.LocalContext


@OptIn(ExperimentalMaterial3Api::class)
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
                title = { Text(ui.word?.word?.term ?: "Word") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = { vm.enrichNow() }) { Text(if (ui.loading) "Loading…" else "Refresh") }
                    TextButton(
                        onClick = { showConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                    // Alternatively, an icon:
                    // IconButton(onClick = { showConfirm = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete word") }
                }
            )
        }
    ) { padding ->
        Column(
  Modifier
    .padding(padding)
    .verticalScroll(rememberScrollState())  // ← enables scrolling
    .padding(16.dp)
) {

            val w = ui.word
            if (w == null) {
                Shimmer(height = 18); Spacer(Modifier.height(8.dp)); Shimmer(
                    height = 14,
                    widthFraction = .6f
                )
                return@Column
            }

            Text("Language: ${w.word.baseLang}")
            if (w.word.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp)); Text("Tags: ${w.word.tags.joinToString()}")
            }
            w.word.notes?.let { Spacer(Modifier.height(8.dp)); Text("Notes: $it") }

            if (ui.error != null) {
                ErrorState(message = ui.error, actionLabel = "Retry") { vm.enrichNow() }
            }

            Spacer(Modifier.height(12.dp))
            Text("Meanings", style = MaterialTheme.typography.titleMedium)
            if (ui.loading && w.senses.isEmpty()) {
                Spacer(Modifier.height(8.dp)); Shimmer(height = 14); Spacer(Modifier.height(6.dp)); Shimmer(
                    height = 14,
                    widthFraction = .8f
                )
            } else if (w.senses.isEmpty()) {
                Text(
                    "No definitions yet. Tap Refresh.",
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Spacer(Modifier.height(4.dp))
                w.senses.forEachIndexed { i, s ->
                    Text("${i + 1}. ${s.definition}")
                    val meta = listOfNotNull(s.pos, s.ipa).joinToString(" • ")
                    if (meta.isNotBlank()) Text(meta, color = MaterialTheme.colorScheme.secondary)

                    if (s.audioUrls.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            s.audioUrls.take(2).forEach { url -> AudioChip(url) }
                        }
                    }
                }
            }
            val showTranslations = com.keephub.core.data.BuildConfig.ENABLE_TRANSLATION
            if (showTranslations) {
                Spacer(Modifier.height(12.dp))
                Text("Translations", style = MaterialTheme.typography.titleMedium)
                if (ui.loading && w.translations.isEmpty()) {
                    Spacer(Modifier.height(8.dp)); Shimmer(height = 14, widthFraction = .5f)
                } else if (w.translations.isEmpty()) {
                    Text("No translation cached.", color = MaterialTheme.colorScheme.secondary)
                } else {
                    w.translations.forEach { t -> Text("${t.languageCode}: ${t.text}") }
                }
            }
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete this word?") },
            text = { Text("This will permanently remove the word and all its data (definitions, translations, SRS, results). This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        vm.deleteWord()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onError)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
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
        Column {
            Text("Error: $message", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onRetry?.invoke() }) { Text(actionLabel) }
            Spacer(Modifier.height(12.dp))
        }
    }
}
@Composable
fun AudioChip(url: String) {
    var playing by remember { mutableStateOf(false) }
    var prepared by remember { mutableStateOf(false) }
    val mediaPlayer = remember(url) { android.media.MediaPlayer() }

    DisposableEffect(url) {
        try {
            val src = if (url.startsWith("//")) "https:$url" else url
            val attrs = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .build()
            mediaPlayer.setAudioAttributes(attrs)
            mediaPlayer.setDataSource(src)
            mediaPlayer.setOnPreparedListener { prepared = true }
            mediaPlayer.setOnCompletionListener { playing = false }
            mediaPlayer.prepareAsync()
        } catch (_: Exception) { /* ignore */ }
        onDispose {
            runCatching {
                mediaPlayer.reset()
                mediaPlayer.release()
            }
        }
    }

    AssistChip(
        onClick = {
            try {
                if (playing) {
                    mediaPlayer.pause()
                    mediaPlayer.seekTo(0)
                    playing = false
                } else if (prepared) {
                    mediaPlayer.start()
                    playing = true
                }
            } catch (_: Exception) { /* ignore */ }
        },
        label = { Text(if (playing) "Pause audio" else if (prepared) "Play audio" else "Loading…") }
    )
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
    private val _ui = MutableStateFlow(DetailUi())
    private val _deleted = MutableStateFlow(false)
    val ui: StateFlow<DetailUi> = _ui.asStateFlow()
    val deleted: StateFlow<Boolean> = _deleted

    init {
        viewModelScope.launch {
            combine(
                id.flatMapLatest { if (it <= 0) flowOf(null) else repo.observeWord(it) },
                settings.translationLang
            ) { word, lang -> word to lang }.collect { (w, lang) ->
                _ui.update { it.copy(word = w, targetLang = lang) }
                if (w != null && w.translations.none { it.languageCode == lang }) {
                    // silently enrich
                    viewModelScope.launch {runCatching { repo.ensureEnriched(w.word.id, lang) }
                    }
                }

            }

        }
    }


    fun setId(wordId: Long) {
        id.value = wordId
        enrichNow()
    }

    fun enrichNow() {
        val wordId = id.value
        if (wordId <= 0) return
        val target = _ui.value.targetLang
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            runCatching { repo.ensureEnriched(wordId, target) }
                .onFailure { e -> _ui.update { state -> state.copy(error = e.message ?: "Lookup failed") } }
            _ui.update { it.copy(loading = false) }
        }
    }

    fun deleteWord() {
        val wordId = id.value
        if (wordId <= 0) return
        viewModelScope.launch {
            runCatching { repo.deleteWord(wordId) }
                .onSuccess { ok -> if (ok) _deleted.value = true else _ui.update { it.copy(error = "Delete failed") } }
                .onFailure { e -> _ui.update { it.copy(error = e.message ?: "Delete failed") } }
        }
    }
}
