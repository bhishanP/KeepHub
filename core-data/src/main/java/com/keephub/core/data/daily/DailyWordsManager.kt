package com.keephub.core.data.daily

import com.keephub.core.data.net.GeminiApiService
import com.keephub.core.data.repo.WordRepository
import com.keephub.core.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class DailyWordItem(
    val word: LeveledWord,
    val isEnrolled: Boolean,
    val wordId: Long? = null
)

@Singleton
class DailyWordsManager @Inject constructor(
    private val repo: WordRepository,
    private val settings: SettingsStore,
    private val gemini: GeminiApiService
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun observeSelectedLevel(): Flow<VocabularyLevel> {
        return settings.dailyWordsLevel.map { VocabularyLevel.fromCode(it) }
    }

    suspend fun setLevel(level: VocabularyLevel) {
        settings.setDailyWordsLevel(level.code)
    }

    /**
     * Resolves today's 5 words:
     * - If AI is ON: Generates dynamically with Gemini (or retrieves today's cached generation).
     * - If AI is OFF or call fails: Gracefully falls back to the curated offline bank.
     */
    suspend fun resolveWordsForDate(
        level: VocabularyLevel,
        date: LocalDate = LocalDate.now()
    ): List<LeveledWord> = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.first()
        val fallbackWords = DailyWordBank.getWordsForDate(level, date)

        // If AI is off (no API key configured), return the curated offline bank immediately
        if (apiKey.isBlank()) {
            return@withContext fallbackWords
        }

        val cachedDate = settings.aiDailyWordsDate.first()
        val cachedLevel = settings.aiDailyWordsLevel.first()
        val cachedJson = settings.aiDailyWordsCache.first()

        // Return today's cached AI words if already generated for this date and level
        if (cachedDate == date.toString() && cachedLevel == level.code && cachedJson.isNotBlank()) {
            val cached = runCatching {
                json.decodeFromString<List<LeveledWord>>(cachedJson)
            }.getOrNull()
            if (!cached.isNullOrEmpty() && cached.size >= 5) {
                return@withContext cached.take(5)
            }
        }

        // Dynamically generate 5 new leveled words using Gemini
        val allSaved = runCatching {
            repo.observeAll().first().map { it.normalizedTerm }
        }.getOrDefault(emptyList())

        val aiWords = runCatching {
            gemini.generateDailyWords(
                apiKey = apiKey,
                level = "${level.displayName} (${level.cefr})",
                excludedTerms = allSaved
            )
        }.getOrNull()

        if (!aiWords.isNullOrEmpty() && aiWords.size >= 5) {
            val generated = aiWords.take(5).map { item ->
                LeveledWord(
                    term = item.term.lowercase().trim(),
                    pos = item.pos.lowercase().trim(),
                    ipa = item.ipa.trim(),
                    definition = item.definition.trim(),
                    example = item.example.trim(),
                    mnemonic = item.mnemonic.trim(),
                    level = level
                )
            }
            val serialized = json.encodeToString(generated)
            settings.setAiDailyWords(serialized, date.toString(), level.code)
            return@withContext generated
        }

        // Seamless fallback to curated offline bank if Gemini call failed or offline
        fallbackWords
    }

    suspend fun isAiGeneratedForToday(
        level: VocabularyLevel,
        date: LocalDate = LocalDate.now()
    ): Boolean = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.first()
        if (apiKey.isBlank()) return@withContext false
        val cachedDate = settings.aiDailyWordsDate.first()
        val cachedLevel = settings.aiDailyWordsLevel.first()
        val cachedJson = settings.aiDailyWordsCache.first()
        cachedDate == date.toString() && cachedLevel == level.code && cachedJson.isNotBlank()
    }

    /**
     * Retrieves the 5 words for a given date and level, checking whether each word
     * is currently enrolled in the user's vocabulary.
     */
    suspend fun getDailyWords(
        date: LocalDate = LocalDate.now(),
        levelOverride: VocabularyLevel? = null
    ): List<DailyWordItem> = withContext(Dispatchers.IO) {
        val level = levelOverride ?: observeSelectedLevel().first()
        val dailyWords = resolveWordsForDate(level, date)

        dailyWords.map { w ->
            val isSaved = repo.isWordSaved(w.term)
            DailyWordItem(
                word = w,
                isEnrolled = isSaved
            )
        }
    }

    /**
     * Automatically adds today's 5 words into the user's vocabulary if auto-add is enabled
     * and today has not yet been processed.
     */
    suspend fun ensureDailyWordsAutoAdded(today: LocalDate = LocalDate.now()): List<Long> = withContext(Dispatchers.IO) {
        val autoAddEnabled = settings.dailyWordsAutoAdd.first()
        if (!autoAddEnabled) return@withContext emptyList()

        val lastDateStr = settings.lastDailyWordsDate.first()
        if (lastDateStr == today.toString()) {
            return@withContext emptyList()
        }

        val level = observeSelectedLevel().first()
        val dailyWords = resolveWordsForDate(level, today)

        val addedIds = mutableListOf<Long>()
        for (w in dailyWords) {
            val isAlreadySaved = repo.isWordSaved(w.term)
            if (!isAlreadySaved) {
                val id = repo.addLeveledWord(w, level)
                if (id > 0) {
                    addedIds.add(id)
                }
            }
        }

        settings.setLastDailyWordsDate(today.toString())
        addedIds
    }

    /**
     * Observe whether today's 5 words have already been reviewed by the user.
     */
    fun observeIsTodayReviewed(today: LocalDate = LocalDate.now()): Flow<Boolean> {
        return settings.lastDailyWordsReviewedDate.map { it == today.toString() }
    }

    /**
     * Marks today's 5 words as reviewed, hiding the banner on the main word list screen.
     */
    suspend fun markTodayReviewed(today: LocalDate = LocalDate.now()) {
        settings.setLastDailyWordsReviewedDate(today.toString())
    }

    /**
     * Set today's reviewed state explicitly.
     */
    suspend fun setTodayReviewed(reviewed: Boolean, today: LocalDate = LocalDate.now()) {
        settings.setLastDailyWordsReviewedDate(if (reviewed) today.toString() else "")
    }

    /**
     * Toggles a word's enrollment status in the user's vocabulary.
     * If user explicitly opts out, the word is removed from the list.
     */
    suspend fun setWordEnrolled(word: LeveledWord, enrolled: Boolean, level: VocabularyLevel): Boolean = withContext(Dispatchers.IO) {
        if (enrolled) {
            val id = repo.addLeveledWord(word, level)
            id > 0
        } else {
            repo.removeWordByTerm(word.term)
        }
    }

    /**
     * Enrolls all 5 words for the given list.
     */
    suspend fun enrollAll(words: List<LeveledWord>, level: VocabularyLevel): List<Long> = withContext(Dispatchers.IO) {
        val added = mutableListOf<Long>()
        for (w in words) {
            val id = repo.addLeveledWord(w, level)
            if (id > 0) added.add(id)
        }
        added
    }
}
