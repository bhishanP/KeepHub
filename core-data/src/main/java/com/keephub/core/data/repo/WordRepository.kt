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

    suspend fun resetAllReviews(): Boolean

    suspend fun deleteWord(wordId: Long): Boolean
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
    private val quizResultDao: QuizResultDao
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

    override suspend fun resetAllReviews(): Boolean = withContext(Dispatchers.IO) {
        srsDao.clearAll()
        quizResultDao.clearAll()
        true
    }
}

