package com.keephub.core.data.repo

import android.util.Log
import com.keephub.core.data.db.dao.*
import com.keephub.core.data.db.entity.*
import com.keephub.core.data.net.GeminiApiService
import com.keephub.core.data.settings.SettingsStore
import com.keephub.core.data.util.WordNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate
import com.keephub.core.data.srs.SrsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.keephub.core.data.db.dao.WordWithDetails
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

sealed interface AddWordResult {
    data class Success(val id: Long): AddWordResult
    data class Duplicate(val existingId: Long): AddWordResult
}

interface WordRepository {
    fun observeAll(): Flow<List<WordEntity>>
    fun observeWord(id: Long): Flow<WordWithDetails?>
    suspend fun checkDuplicateForTerm(term: String): WordEntity?
    suspend fun addWord(
        term: String,
        baseLang: String = "en",
        notes: String?,
        tags: List<String>
    ): AddWordResult

    suspend fun ensureEnriched(wordId: Long, targetLang: String, forceRefresh: Boolean = false): Boolean

    /** Returns word IDs to review today, preferring due items then filling with new words. */
    suspend fun generateDailyQueue(targetSize: Int, today: LocalDate): List<Long>

    /** Returns random word IDs for practice review regardless of due date. */
    suspend fun generatePracticeQueue(limit: Int): List<Long>

    /** Applies a review grade (0..5) and records QuizResult. Returns the new SRS state. */
    suspend fun recordReview(wordId: Long, mode: String, correct: Boolean, timeMs: Long, grade: Int, today: LocalDate): SrsStateEntity

    /** Helpers for quiz generation **/
    suspend fun getDetails(wordId: Long): WordWithDetails?
    suspend fun randomDefinitionsExcept(wordId: Long, count: Int): List<String>
    suspend fun randomTermsExcept(wordId: Long, count: Int): List<String>

    suspend fun findSimilarWords(term: String, limit: Int = 3): List<WordEntity>
    suspend fun mergeWord(existingId: Long, newNotes: String?, newTags: List<String>): Boolean

    fun observeReviewTimestamps(): Flow<List<Instant>>
    fun observeDifficultWordIds(): Flow<List<Long>>

    suspend fun saveReviewSession(
        totalQuestions: Int,
        correctCount: Int,
        elapsedMs: Long,
        isPractice: Boolean,
        story: String?,
        storyWords: List<String>,
        items: List<SessionReviewItem>
    ): Long

    suspend fun updateSessionStory(sessionId: Long, story: String, words: List<String>)
    suspend fun updateSessionItemExplanation(sessionId: Long, wordId: Long, explanation: String)
    fun observeRecentReviewSessions(limit: Int = 10): Flow<List<ReviewSessionRecord>>
    suspend fun getReviewSession(id: Long): ReviewSessionRecord?

    suspend fun resetAllReviews(): Boolean

    suspend fun deleteWord(wordId: Long): Boolean

    suspend fun addLeveledWord(word: com.keephub.core.data.daily.LeveledWord, level: com.keephub.core.data.daily.VocabularyLevel): Long
    suspend fun removeWordByTerm(term: String): Boolean
    suspend fun isWordSaved(term: String): Boolean
}

@Singleton
class WordRepositoryImpl @Inject constructor(
    private val wordDao: WordDao,
    private val senseDao: SenseDao,
    private val translationDao: TranslationDao,
    private val normalizer: WordNormalizer,
    private val dict: DictionaryService,
    private val gemini: GeminiApiService,
    private val settings: SettingsStore,
    private val translator: TranslateService,
    private val srsDao: SrsDao,
    private val quizResultDao: QuizResultDao,
    private val reviewSessionDao: ReviewSessionDao,
    private val json: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
) : WordRepository {

    override fun observeAll() = wordDao.observeAll()
    override fun observeWord(id: Long) = wordDao.observeWord(id)
    override fun observeReviewTimestamps(): Flow<List<Instant>> = quizResultDao.observeReviewTimestamps()
    override fun observeDifficultWordIds(): Flow<List<Long>> = srsDao.observeDifficultWordIds()

    override suspend fun checkDuplicateForTerm(term: String): WordEntity? {
        val candidates = normalizer.candidateKeys(term)
        if (candidates.isEmpty()) return null
        return wordDao.findByNormalizedIn(candidates)
    }

    override suspend fun findSimilarWords(term: String, limit: Int): List<WordEntity> {
        val normalized = normalizer.normalizeForKey(term)
        if (normalized.length < 2) return emptyList()
        val prefix = normalized.take(4)
        return wordDao.findSimilar(prefix, limit)
            .filter { it.normalizedTerm != normalized }
    }

    override suspend fun mergeWord(existingId: Long, newNotes: String?, newTags: List<String>): Boolean = withContext(Dispatchers.IO) {
        val existing = wordDao.findById(existingId) ?: return@withContext false
        val mergedTags = (existing.tags + newTags.filter { it.isNotBlank() }.map { it.trim() }).distinct()
        val mergedNotes = when {
            existing.notes.isNullOrBlank() -> newNotes?.ifBlank { null }
            newNotes.isNullOrBlank() -> existing.notes
            newNotes.trim().let { existing.notes.contains(it) } -> existing.notes
            else -> "${existing.notes}\n\n${newNotes.trim()}"
        }
        wordDao.update(
            existing.copy(
                tags = mergedTags,
                notes = mergedNotes,
                updatedAt = Instant.now()
            )
        )
        true
    }

    override suspend fun addWord(term: String, baseLang: String, notes: String?, tags: List<String>): AddWordResult {
        val trimmed = term.trim()
        if (trimmed.isBlank()) return AddWordResult.Duplicate(-1)
        val existing = checkDuplicateForTerm(trimmed)
        if (existing != null) return AddWordResult.Duplicate(existing.id)

        val normalized = normalizer.normalizeForKey(trimmed)
        val now = Instant.now()
        val id = wordDao.insert(
            WordEntity(
                term = trimmed,
                normalizedTerm = normalized,
                baseLang = baseLang,
                notes = notes?.ifBlank { null },
                tags = tags.filter { it.isNotBlank() }.map { it.trim() }.distinct(),
                createdAt = now,
                updatedAt = now
            )
        )
        return AddWordResult.Success(id)
    }

    override suspend fun ensureEnriched(wordId: Long, targetLang: String, forceRefresh: Boolean): Boolean = withContext(Dispatchers.IO) {
        val word = wordDao.findById(wordId) ?: return@withContext false
        var changed = false

        if (forceRefresh) {
            senseDao.deleteByWord(wordId)
        }

        // dictionary / AI enrichment
        if (senseDao.countForWord(wordId) == 0) {
            val geminiKey = runCatching { settings.geminiApiKey.first() }.getOrDefault("")
            var enrichedWithAi = false

            // Tier 1: Gemini AI enrichment if BYOK key is configured
            if (geminiKey.isNotBlank()) {
                val aiResult = runCatching {
                    gemini.enrichWord(
                        apiKey = geminiKey,
                        term = word.term,
                        contextSentence = word.notes,
                        baseLang = word.baseLang
                    )
                }.getOrNull()

                if (aiResult != null && aiResult.lookup.senses.isNotEmpty()) {
                    val senses = aiResult.lookup.senses.map {
                        SenseEntity(
                            wordId = wordId,
                            pos = it.pos,
                            definition = it.definition,
                            ipa = it.ipa,
                            examples = it.examples,
                            synonyms = it.synonyms,
                            antonyms = it.antonyms,
                            audioUrls = emptyList()
                        )
                    }
                    senseDao.insertAll(senses)

                    if (word.notes.isNullOrBlank() && !aiResult.mnemonic.isNullOrBlank()) {
                        wordDao.update(word.copy(notes = "💡 Mnemonic: ${aiResult.mnemonic}", updatedAt = Instant.now()))
                    }
                    changed = true
                    enrichedWithAi = true
                }
            }

            // Tier 2: Standard FreeDictionaryApi fallback
            if (!enrichedWithAi) {
                val look = runCatching { dict.lookup(word.term, word.baseLang) }.getOrNull()
                if (look != null && look.senses.isNotEmpty()) {
                    val senses = look.senses.map {
                        SenseEntity(
                            wordId = wordId,
                            pos = it.pos,
                            definition = it.definition,
                            ipa = it.ipa ?: look.ipa,
                            examples = it.examples,
                            synonyms = it.synonyms,
                            antonyms = it.antonyms,
                            audioUrls = it.audioUrls
                        )
                    }
                    senseDao.insertAll(senses)
                    changed = true
                }
            }
        }
        // translation
        if (com.keephub.core.data.BuildConfig.ENABLE_TRANSLATION && targetLang.isNotBlank()) {
            val translated = runCatching { translator.translate(word.term, targetLang, word.baseLang) }
                .onFailure { Log.e("KeepHub", "translate failed", it) }
                .getOrNull()
            Log.i("KeepHub", "translated='$translated' lang=$targetLang")

            if (!translated.isNullOrBlank()) {
                translationDao.upsertAll(listOf(TranslationEntity(wordId = wordId, languageCode = targetLang, text = translated)))
                changed = true
            }
        }
        changed
    }

    override suspend fun generateDailyQueue(targetSize: Int, today: LocalDate): List<Long> = withContext(Dispatchers.IO) {
        val due = srsDao.dueIds(today.toString(), targetSize)
        if (due.size >= targetSize) return@withContext due

        val remaining = targetSize - due.size
        val newOnes = wordDao.newWords(remaining).map { it.id }
        return@withContext (due + newOnes)
    }

    override suspend fun generatePracticeQueue(limit: Int): List<Long> = withContext(Dispatchers.IO) {
        wordDao.randomWordIds(limit)
    }

    override suspend fun recordReview(
        wordId: Long,
        mode: String,
        correct: Boolean,
        timeMs: Long,
        grade: Int,
        today: LocalDate
    ): SrsStateEntity = withContext(Dispatchers.IO) {
        val current = srsDao.get(wordId)?.copy(wordId = wordId)
        val res = SrsEngine.review(today, current, grade)
        val next = res.next.copy(wordId = wordId)
        srsDao.upsert(next)
        quizResultDao.insert(
            QuizResultEntity(
                wordId = wordId,
                mode = mode,
                correct = correct,
                timeMs = timeMs
            )
        )
        next
    }

    override suspend fun getDetails(wordId: Long): WordWithDetails? =
        wordDao.observeWord(wordId).firstOrNull() // quick one-shot;

    override suspend fun randomDefinitionsExcept(wordId: Long, count: Int): List<String> =
        senseDao.randomDefinitions(excludeWordId = wordId, limit = count)

    override suspend fun randomTermsExcept(wordId: Long, count: Int): List<String> =
        wordDao.randomTerms(excludeWordId = wordId, limit = count)

    override suspend fun deleteWord(wordId: Long): Boolean = withContext(Dispatchers.IO) {
        // Remove dependents first (defensive; don’t rely on FK cascades)
        quizResultDao.deleteByWord(wordId)
        srsDao.deleteByWord(wordId)
        senseDao.deleteByWord(wordId)
        translationDao.deleteByWord(wordId)
        val rows = wordDao.delete(wordId)
        rows > 0
    }

    override suspend fun saveReviewSession(
        totalQuestions: Int,
        correctCount: Int,
        elapsedMs: Long,
        isPractice: Boolean,
        story: String?,
        storyWords: List<String>,
        items: List<SessionReviewItem>
    ): Long = withContext(Dispatchers.IO) {
        val itemsJson = json.encodeToString(ListSerializer(SessionReviewItem.serializer()), items)
        val storyWordsJson = json.encodeToString(ListSerializer(String.serializer()), storyWords)
        val entity = ReviewSessionEntity(
            completedAt = Instant.now(),
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            elapsedMs = elapsedMs,
            isPractice = isPractice,
            story = story,
            storyWordsJson = storyWordsJson,
            itemsJson = itemsJson
        )
        val id = reviewSessionDao.insert(entity)
        // Keep limited to recent 10 sessions / 10 days
        reviewSessionDao.pruneOldSessions(keepCount = 10)
        id
    }

    override suspend fun updateSessionStory(
        sessionId: Long,
        story: String,
        words: List<String>
    ): Unit = withContext(Dispatchers.IO) {
        val existing = reviewSessionDao.getSessionById(sessionId) ?: return@withContext
        val wordsJson = json.encodeToString(ListSerializer(String.serializer()), words)
        reviewSessionDao.update(existing.copy(story = story, storyWordsJson = wordsJson))
    }

    override suspend fun updateSessionItemExplanation(
        sessionId: Long,
        wordId: Long,
        explanation: String
    ): Unit = withContext(Dispatchers.IO) {
        val existing = reviewSessionDao.getSessionById(sessionId) ?: return@withContext
        val currentItems: List<SessionReviewItem> = runCatching {
            json.decodeFromString(ListSerializer(SessionReviewItem.serializer()), existing.itemsJson)
        }.getOrDefault(emptyList())
        val updated = currentItems.map {
            if (it.wordId == wordId) it.copy(explanation = explanation) else it
        }
        val updatedJson = json.encodeToString(ListSerializer(SessionReviewItem.serializer()), updated)
        reviewSessionDao.update(existing.copy(itemsJson = updatedJson))
    }

    override fun observeRecentReviewSessions(limit: Int): Flow<List<ReviewSessionRecord>> =
        reviewSessionDao.observeRecentSessions(limit).map { list ->
            list.map { it.toRecord(json) }
        }

    override suspend fun getReviewSession(id: Long): ReviewSessionRecord? = withContext(Dispatchers.IO) {
        reviewSessionDao.getSessionById(id)?.toRecord(json)
    }

    override suspend fun resetAllReviews(): Boolean = withContext(Dispatchers.IO) {
        srsDao.clearAll()
        quizResultDao.clearAll()
        reviewSessionDao.clearAll()
        true
    }

    override suspend fun addLeveledWord(
        word: com.keephub.core.data.daily.LeveledWord,
        level: com.keephub.core.data.daily.VocabularyLevel
    ): Long = withContext(Dispatchers.IO) {
        val trimmed = word.term.trim()
        val normalized = normalizer.normalizeForKey(trimmed)
        val existing = wordDao.findByNormalized(normalized)
        if (existing != null) {
            return@withContext existing.id
        }

        val now = Instant.now()
        val wordId = wordDao.insert(
            WordEntity(
                term = trimmed,
                normalizedTerm = normalized,
                baseLang = "en",
                notes = "💡 Memory Tip: ${word.mnemonic}",
                tags = listOf("Daily5", level.displayName),
                createdAt = now,
                updatedAt = now
            )
        )

        val sense = SenseEntity(
            wordId = wordId,
            pos = word.pos,
            definition = word.definition,
            ipa = word.ipa,
            examples = listOf(word.example),
            synonyms = emptyList(),
            antonyms = emptyList(),
            audioUrls = emptyList()
        )
        senseDao.insertAll(listOf(sense))
        wordId
    }

    override suspend fun removeWordByTerm(term: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = normalizer.normalizeForKey(term.trim())
        val existing = wordDao.findByNormalized(normalized) ?: return@withContext false
        wordDao.delete(existing.id)
        senseDao.deleteByWord(existing.id)
        srsDao.deleteByWord(existing.id)
        true
    }

    override suspend fun isWordSaved(term: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = normalizer.normalizeForKey(term.trim())
        wordDao.findByNormalized(normalized) != null
    }
}

private fun ReviewSessionEntity.toRecord(json: Json): ReviewSessionRecord {
    val items: List<SessionReviewItem> = runCatching {
        json.decodeFromString(ListSerializer(SessionReviewItem.serializer()), itemsJson)
    }.getOrDefault(emptyList())
    val words: List<String> = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), storyWordsJson)
    }.getOrDefault(emptyList())
    return ReviewSessionRecord(
        id = id,
        completedAt = completedAt,
        totalQuestions = totalQuestions,
        correctCount = correctCount,
        elapsedMs = elapsedMs,
        isPractice = isPractice,
        story = story,
        storyWords = words,
        items = items
    )
}

