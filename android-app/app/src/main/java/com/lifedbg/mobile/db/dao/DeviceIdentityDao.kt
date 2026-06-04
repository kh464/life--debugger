package com.lifedbg.mobile.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifedbg.mobile.db.entity.DeviceIdentityEntity

@Dao
interface DeviceIdentityDao {
    @Query("SELECT * FROM device_identity LIMIT 1")
    suspend fun getIdentity(): DeviceIdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(identity: DeviceIdentityEntity)

    @Query("UPDATE device_identity SET device_name = :deviceName, updated_at = :updatedAt WHERE device_id = :deviceId")
    suspend fun updateName(deviceId: String, deviceName: String, updatedAt: Long)

    @Query("DELETE FROM device_identity")
    suspend fun clear()
}
