package com.keephub.core.data.repo

import android.util.Log
import com.keephub.core.data.db.dao.*
import com.keephub.core.data.db.entity.*
import com.keephub.core.data.util.WordNormalizer
import kotlinx.coroutines.flow.Flow
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

    suspend fun ensureEnriched(wordId: Long, targetLang: String): Boolean

    /** Returns word IDs to review today, preferring due items then filling with new words. */
    suspend fun generateDailyQueue(targetSize: Int, today: LocalDate): List<Long>

    /** Applies a review grade (0..5) and records QuizResult. Returns the new SRS state. */
    suspend fun recordReview(wordId: Long, mode: String, correct: Boolean, timeMs: Long, grade: Int, today: LocalDate): SrsStateEntity

    /** Helpers for quiz generation **/
    suspend fun getDetails(wordId: Long): WordWithDetails?
    suspend fun randomDefinitionsExcept(wordId: Long, count: Int): List<String>
    suspend fun randomTermsExcept(wordId: Long, count: Int): List<String>

    suspend fun deleteWord(wordId: Long): Boolean
}

@Singleton
class WordRepositoryImpl @Inject constructor(
    private val wordDao: WordDao,
    private val senseDao: SenseDao,
    private val translationDao: TranslationDao,
    private val normalizer: WordNormalizer,
    private val dict: DictionaryService,
    private val translator: TranslateService,
    private val srsDao: SrsDao,
    private val quizResultDao: QuizResultDao
) : WordRepository {

    override fun observeAll() = wordDao.observeAll()
    override fun observeWord(id: Long) = wordDao.observeWord(id)
    override suspend fun checkDuplicateForTerm(term: String) =
        wordDao.findByNormalized(normalizer.normalizeForKey(term))

    override suspend fun addWord(term: String, baseLang: String, notes: String?, tags: List<String>): AddWordResult {
        val normalized = normalizer.normalizeForKey(term)
        val existing = wordDao.findByNormalized(normalized)
        if (existing != null) return AddWordResult.Duplicate(existing.id)
        val now = Instant.now()
        val id = wordDao.insert(
            WordEntity(
                term = term.trim(),
                normalizedTerm = normalized,
                baseLang = baseLang,
                notes = notes?.ifBlank { null },
                tags = tags.filter { it.isNotBlank() }.map { it.trim() },
                createdAt = now, updatedAt = now
            )
        )
        return AddWordResult.Success(id)
    }

    override suspend fun ensureEnriched(wordId: Long, targetLang: String): Boolean = withContext(Dispatchers.IO) {
        val word = wordDao.findById(wordId) ?: return@withContext false
        var changed = false
        // dictionary
        if (senseDao.countForWord(wordId) == 0) {
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
}

