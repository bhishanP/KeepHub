package com.keephub.core.data.db.dao

import androidx.room.*
import com.keephub.core.data.db.entity.SrsStateEntity

@Dao
interface SrsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(state: SrsStateEntity)
    @Query("SELECT * FROM srs_state WHERE wordId = :wordId") suspend fun get(wordId: Long): SrsStateEntity?

    @Query("SELECT wordId FROM srs_state WHERE dueDate <= :today ORDER BY dueDate ASC LIMIT :limit")
    suspend fun dueIds(today: String, limit: Int): List<Long>

    @Query("SELECT COUNT(*) FROM srs_state WHERE wordId = :wordId")
    suspend fun countForWord(wordId: Long): Int

    @Query("DELETE FROM srs_state WHERE wordId = :wordId")
    suspend fun deleteByWord(wordId: Long)
}