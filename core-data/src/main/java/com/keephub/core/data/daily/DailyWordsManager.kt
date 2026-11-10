package com.keephub.core.data.daily

import com.keephub.core.data.repo.WordRepository
import com.keephub.core.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
    private val settings: SettingsStore
) {

    fun observeSelectedLevel(): Flow<VocabularyLevel> {
        return settings.dailyWordsLevel.map { VocabularyLevel.fromCode(it) }
    }

    suspend fun setLevel(level: VocabularyLevel) {
        settings.setDailyWordsLevel(level.code)
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
        val allSaved = repo.observeAll().first().map { it.normalizedTerm }.toSet()
        val dailyWords = DailyWordBank.getWordsForDate(level, date, allSaved)

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
     * and today has not yet been processed. Fulfills the requirement:
     * "default will be yes (if they don't choose or don't open app for that day)"
     */
    suspend fun ensureDailyWordsAutoAdded(today: LocalDate = LocalDate.now()): List<Long> = withContext(Dispatchers.IO) {
        val autoAddEnabled = settings.dailyWordsAutoAdd.first()
        if (!autoAddEnabled) return@withContext emptyList()

        val lastDateStr = settings.lastDailyWordsDate.first()
        val level = observeSelectedLevel().first()

        val allSaved = repo.observeAll().first().map { it.normalizedTerm }.toSet()
        val dailyWords = DailyWordBank.getWordsForDate(level, today, allSaved)

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

        if (lastDateStr != today.toString()) {
            settings.setLastDailyWordsDate(today.toString())
        }
        addedIds
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
