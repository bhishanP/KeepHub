package com.keephub.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.keephub.app.nav.Routes
import com.keephub.app.ui.components.EmptyState
import com.keephub.core.data.daily.DailyWordBank
import com.keephub.core.data.daily.DailyWordsManager
import com.keephub.core.data.daily.LeveledWord
import com.keephub.core.data.daily.VocabularyLevel
import com.keephub.core.data.db.entity.WordEntity
import com.keephub.core.data.repo.WordRepository
import com.keephub.core.data.settings.SettingsStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyWordsSummary(
    val level: VocabularyLevel = VocabularyLevel.INTERMEDIATE,
    val words: List<LeveledWord> = emptyList(),
    val enrolledCount: Int = 0,
    val allEnrolled: Boolean = false,
    val isReviewedToday: Boolean = false,
    val isAiGenerated: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    navController: NavController,
    vm: WordListViewModel = hiltViewModel()
) {
    val words by vm.items.collectAsState()
    val streak by vm.streak.collectAsState()
    val difficultIds by vm.difficultIds.collectAsState()
    val dailySummary by vm.dailyWordsState.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var showOnlyDifficult by rememberSaveable { mutableStateOf(false) }

    val allTags = remember(words) {
        words.flatMap { it.tags }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredWords = remember(words, searchQuery, selectedTag, showOnlyDifficult, difficultIds) {
        var list = words
        if (showOnlyDifficult) {
            list = list.filter { difficultIds.contains(it.id) }
        }
        if (!selectedTag.isNullOrBlank()) {
            list = list.filter { it.tags.contains(selectedTag) }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            list = list.filter {
                it.term.contains(q, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(q, ignoreCase = true) }
            }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Vocabulary",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (words.isNotEmpty()) {
                                Text(
                                    text = "${words.size} ${if (words.size == 1) "word" else "words"} saved",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (streak > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = if (streak > 0) "🔥 $streak" else "🔥 0",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (streak > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = if (isSearchActive) "Close search" else "Search words"
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate(Routes.REVIEW) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = "Start daily quiz review"
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate(Routes.SETTINGS) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Open settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add word", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    .widthIn(max = 720.dp) // Adaptive constraint for wide windows / tablets
            ) {
                // Collapsible M3 Search Bar
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search words or tags...") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Today's 5 Words Banner (hidden when reviewed today or searching)
                if (!isSearchActive && searchQuery.isBlank() && dailySummary.words.isNotEmpty() && !dailySummary.isReviewedToday) {
                    DailyWordsBannerCard(
                        summary = dailySummary,
                        onOpenDailyWords = { navController.navigate(Routes.DAILY_WORDS) },
                        onDismiss = { vm.markDailyWordsReviewed() }
                    )
                }

                // Interactive Tag & Difficulty Filter Chips
                if (allTags.isNotEmpty() || difficultIds.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTag == null && !showOnlyDifficult,
                                onClick = {
                                    selectedTag = null
                                    showOnlyDifficult = false
                                },
                                label = { Text("All (${words.size})") }
                            )
                        }
                        if (difficultIds.isNotEmpty()) {
                            item {
                                FilterChip(
                                    selected = showOnlyDifficult,
                                    onClick = {
                                        showOnlyDifficult = !showOnlyDifficult
                                        if (showOnlyDifficult) selectedTag = null
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.WarningAmber,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (showOnlyDifficult) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    label = { Text("Difficult (${difficultIds.size})") }
                                )
                            }
                        }
                        items(allTags) { tag ->
                            val count = words.count { it.tags.contains(tag) }
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = {
                                    selectedTag = if (selectedTag == tag) null else tag
                                    showOnlyDifficult = false
                                },
                                label = { Text("#$tag ($count)") }
                            )
                        }
                    }
                }

                if (words.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            title = "No words yet",
                            subtitle = "Add your first word or share text into KeepHub to start studying.",
                            actionLabel = "Add word",
                            onAction = onAdd
                        )
                    }
                } else if (filteredWords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No words matching \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 96.dp // Room for Extended FAB
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filteredWords, key = { _, w -> w.id }) { index, word ->
                            WordListItem(
                                word = word,
                                onClick = { onOpen(word.id) },
                                onTagClick = { tag -> selectedTag = tag }
                            )
                            if (index < filteredWords.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordListItem(
    word: WordEntity,
    onClick: () -> Unit,
    onTagClick: (String) -> Unit = {}
) {
    ListItem(
        headlineContent = {
            Text(
                text = word.term,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            if (word.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    word.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { onTagClick(tag) }
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (word.tags.size > 3) {
                        Text(
                            text = "+${word.tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClickLabel = "Open details for ${word.term}",
                onClick = onClick
            )
            .padding(horizontal = 4.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyWordsBannerCard(
    summary: DailyWordsSummary,
    onOpenDailyWords: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Tappable header & preview area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        onClickLabel = "Open daily words lesson",
                        onClick = onOpenDailyWords
                    )
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Today's 5 Words",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (summary.isAiGenerated) {
                                    "✨ AI Curated • ${summary.level.displayName} (${summary.level.cefr})"
                                } else {
                                    "${summary.level.displayName} • ${summary.level.cefr}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (summary.allEnrolled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (summary.allEnrolled) "5/5 Added" else "${summary.enrolledCount}/5 Added",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.allEnrolled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 5 Word chips preview
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    summary.words.forEach { w ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = w.term,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // M3 Action Row: Supporting hint on left, clear action buttons on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enrolled by default",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onOpenDailyWords,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Study Words",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private data class DailyWordsCombinedInput(
    val level: VocabularyLevel,
    val isReviewed: Boolean,
    val allWords: List<WordEntity>,
    val apiKey: String
)

@HiltViewModel
class WordListViewModel @Inject constructor(
    repo: WordRepository,
    private val dailyWordsManager: DailyWordsManager,
    private val settings: SettingsStore
) : ViewModel() {
    val items: StateFlow<List<WordEntity>> =
        repo.observeAll()
            .map { it.sortedByDescending { w -> w.updatedAt } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val difficultIds: StateFlow<Set<Long>> =
        repo.observeDifficultWordIds()
            .map { it.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val streak: StateFlow<Int> =
        repo.observeReviewTimestamps()
            .map { timestamps -> calculateStreak(timestamps) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val dailyWordsState: StateFlow<DailyWordsSummary> =
        combine(
            dailyWordsManager.observeSelectedLevel(),
            dailyWordsManager.observeIsTodayReviewed(),
            repo.observeAll(),
            settings.geminiApiKey
        ) { level, isReviewed, allWords, apiKey ->
            DailyWordsCombinedInput(level, isReviewed, allWords, apiKey)
        }.map { input ->
            val allSavedTerms = input.allWords.map { it.normalizedTerm }.toSet()
            val today = LocalDate.now()
            val words = dailyWordsManager.resolveWordsForDate(input.level, today)
            val enrolledCount = words.count { it.term.lowercase() in allSavedTerms }
            val isAi = input.apiKey.isNotBlank() && dailyWordsManager.isAiGeneratedForToday(input.level, today)
            DailyWordsSummary(
                level = input.level,
                words = words,
                enrolledCount = enrolledCount,
                allEnrolled = enrolledCount == words.size,
                isReviewedToday = input.isReviewed,
                isAiGenerated = isAi
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyWordsSummary())

    init {
        viewModelScope.launch {
            runCatching { dailyWordsManager.ensureDailyWordsAutoAdded() }
        }
    }

    fun markDailyWordsReviewed() {
        viewModelScope.launch {
            dailyWordsManager.markTodayReviewed()
        }
    }

    companion object {
        fun calculateStreak(
            timestamps: List<Instant>,
            today: LocalDate = LocalDate.now(ZoneId.systemDefault())
        ): Int {
            if (timestamps.isEmpty()) return 0
            val zone = ZoneId.systemDefault()
            val reviewDates = timestamps.map { it.atZone(zone).toLocalDate() }.toSet()
            if (reviewDates.isEmpty()) return 0
            var count = 0
            var checkDate = if (reviewDates.contains(today)) today else today.minusDays(1)
            while (reviewDates.contains(checkDate)) {
                count++
                checkDate = checkDate.minusDays(1)
            }
            return count
        }
    }
}
