package com.keephub.core.data.db.dao

import androidx.room.*
import com.keephub.core.data.db.entity.QuizResultEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface QuizResultDao {
    @Insert suspend fun insert(result: QuizResultEntity)

    @Query("DELETE FROM quiz_results WHERE wordId = :wordId")
    suspend fun deleteByWord(wordId: Long)

    @Query("SELECT timestamp FROM quiz_results ORDER BY timestamp DESC")
    fun observeReviewTimestamps(): Flow<List<Instant>>

    @Query("DELETE FROM quiz_results")
    suspend fun clearAll()
}
