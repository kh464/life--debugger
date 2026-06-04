package com.lifedbg.mobile.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifedbg.mobile.db.entity.LlmReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LlmReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: LlmReviewEntity)

    @Query("SELECT * FROM llm_reviews WHERE date = :date ORDER BY created_at DESC LIMIT 1")
    fun observeLatestForDate(date: String): Flow<LlmReviewEntity?>

    @Query("DELETE FROM llm_reviews")
    suspend fun clear()
}
