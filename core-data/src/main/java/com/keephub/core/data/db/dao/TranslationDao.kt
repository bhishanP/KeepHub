package com.keephub.core.data.db.dao

import androidx.room.*
import com.keephub.core.data.db.entity.TranslationEntity

@Dao
interface TranslationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TranslationEntity>)

    @Query("SELECT * FROM translations WHERE wordId = :wordId AND languageCode = :lang LIMIT 1")
    suspend fun get(wordId: Long, lang: String): TranslationEntity?

    @Query("DELETE FROM translations WHERE wordId = :wordId")
    suspend fun deleteByWord(wordId: Long)
}

