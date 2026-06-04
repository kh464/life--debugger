package com.lifedbg.mobile.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifedbg.mobile.db.entity.SummaryEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<SummaryEventEntity>)

    @Query("SELECT * FROM summary_events WHERE start_time BETWEEN :startTime AND :endTime ORDER BY start_time ASC")
    fun observeBetween(startTime: Long, endTime: Long): Flow<List<SummaryEventEntity>>

    @Query("SELECT * FROM summary_events WHERE start_time BETWEEN :startTime AND :endTime ORDER BY start_time ASC")
    suspend fun getBetween(startTime: Long, endTime: Long): List<SummaryEventEntity>

    @Query("UPDATE summary_events SET llm_allowed = :allowed WHERE event_id = :eventId")
    suspend fun updateLlmAllowed(eventId: String, allowed: Boolean)

    @Query("DELETE FROM summary_events WHERE event_id = :eventId")
    suspend fun deleteById(eventId: String)

    @Query("DELETE FROM summary_events")
    suspend fun clear()
}
