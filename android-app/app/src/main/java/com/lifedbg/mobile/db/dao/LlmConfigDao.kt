package com.lifedbg.mobile.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifedbg.mobile.db.entity.LlmConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LlmConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: LlmConfigEntity)

    @Query("SELECT * FROM llm_configs WHERE enabled = 1 LIMIT 1")
    fun observeEnabled(): Flow<LlmConfigEntity?>

    @Query("SELECT * FROM llm_configs WHERE enabled = 1 LIMIT 1")
    suspend fun getEnabled(): LlmConfigEntity?

    @Query("SELECT * FROM llm_configs WHERE provider_id = :providerId LIMIT 1")
    suspend fun getByProviderId(providerId: String): LlmConfigEntity?

    @Query("DELETE FROM llm_configs")
    suspend fun clear()
}
