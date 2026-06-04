package com.lifedbg.mobile.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifedbg.mobile.db.entity.RawUsageEventEntity

@Dao
interface RawUsageEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<RawUsageEventEntity>)

    @Query("SELECT * FROM raw_usage_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getBetween(startTime: Long, endTime: Long): List<RawUsageEventEntity>

    @Query("SELECT * FROM raw_usage_events WHERE processed = 0 AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getUnprocessed(startTime: Long, endTime: Long): List<RawUsageEventEntity>

    @Query("UPDATE raw_usage_events SET processed = 1 WHERE id IN (:ids)")
    suspend fun markProcessed(ids: List<String>)

    @Query("DELETE FROM raw_usage_events")
    suspend fun clear()
}
