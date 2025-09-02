package com.keephub.core.data.db.dao

import androidx.room.*
import com.keephub.core.data.db.entity.QuizResultEntity

@Dao
interface QuizResultDao {
    @Insert suspend fun insert(result: QuizResultEntity)

    @Query("DELETE FROM quiz_results WHERE wordId = :wordId")
    suspend fun deleteByWord(wordId: Long)
}
