package com.keephub.core.data.db.dao

import androidx.room.*
import com.keephub.core.data.db.entity.ReviewSessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ReviewSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ReviewSessionEntity): Long

    @Update
    suspend fun update(session: ReviewSessionEntity)

    @Query("SELECT * FROM review_sessions ORDER BY completedAt DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int = 10): Flow<List<ReviewSessionEntity>>

    @Query("SELECT * FROM review_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): ReviewSessionEntity?

    @Query("DELETE FROM review_sessions WHERE completedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Instant)

    @Query("DELETE FROM review_sessions WHERE id NOT IN (SELECT id FROM review_sessions ORDER BY completedAt DESC LIMIT :keepCount)")
    suspend fun pruneOldSessions(keepCount: Int = 10)

    @Query("DELETE FROM review_sessions")
    suspend fun clearAll()
}
