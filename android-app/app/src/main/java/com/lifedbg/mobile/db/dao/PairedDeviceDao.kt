package com.lifedbg.mobile.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifedbg.mobile.db.entity.PairedDeviceEntity

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_devices ORDER BY paired_at DESC")
    suspend fun getAll(): List<PairedDeviceEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM paired_devices WHERE paired_device_id = :deviceId)")
    suspend fun isPaired(deviceId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: PairedDeviceEntity)

    @Query("DELETE FROM paired_devices WHERE paired_device_id = :deviceId")
    suspend fun delete(deviceId: String)
}
