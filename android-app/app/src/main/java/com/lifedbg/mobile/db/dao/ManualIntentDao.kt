package com.lifedbg.mobile.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifedbg.mobile.db.entity.ManualIntentEntity

@Dao
interface ManualIntentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(intent: ManualIntentEntity)

    @Query("SELECT * FROM manual_intent_events WHERE start_time BETWEEN :startTime AND :endTime ORDER BY start_time ASC")
    suspend fun getBetween(startTime: Long, endTime: Long): List<ManualIntentEntity>
}
