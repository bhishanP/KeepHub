package com.keephub.core.data.db.dao

import androidx.room.*
import com.keephub.core.data.db.entity.SenseEntity

@Dao
interface SenseDao {
    @Insert
    suspend fun insertAll(items: List<SenseEntity>)

    @Query("DELETE FROM senses WHERE wordId = :wordId")
    suspend fun deleteByWord(wordId: Long)

    @Query("SELECT COUNT(*) FROM senses WHERE wordId = :wordId")
    suspend fun countForWord(wordId: Long): Int

    @Query("SELECT * FROM senses WHERE wordId = :wordId")
    suspend fun forWord(wordId: Long): List<SenseEntity>

    @Query("SELECT definition FROM senses WHERE wordId != :excludeWordId ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomDefinitions(excludeWordId: Long, limit: Int): List<String>

}
