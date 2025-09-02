package com.keephub.core.data.db.dao

import androidx.room.*
import com.keephub.core.data.db.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity)

    @Delete
    suspend fun delete(word: WordEntity)

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id")
    fun observeWord(id: Long): Flow<WordWithDetails?>

    @Query("SELECT * FROM words ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE normalizedTerm = :normalized LIMIT 1")
    suspend fun findByNormalized(normalized: String): WordEntity?

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun findById(id: Long): WordEntity?

    @Query("""
    SELECT * FROM words 
    WHERE id NOT IN (SELECT wordId FROM srs_state) 
    ORDER BY createdAt ASC 
    LIMIT :limit
  """)
    suspend fun newWords(limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<Long>): List<WordEntity>

    @Query("SELECT term FROM words WHERE id != :excludeWordId ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomTerms(excludeWordId: Long, limit: Int): List<String>

}
data class WordWithDetails(
    @Embedded val word: WordEntity,
    @Relation(parentColumn = "id", entityColumn = "wordId") val senses: List<SenseEntity>,
    @Relation(parentColumn = "id", entityColumn = "wordId") val translations: List<TranslationEntity>,
    @Relation(parentColumn = "id", entityColumn = "wordId") val srs: SrsStateEntity?
)
