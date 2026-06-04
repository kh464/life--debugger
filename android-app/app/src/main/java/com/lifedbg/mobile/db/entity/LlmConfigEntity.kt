package com.lifedbg.mobile.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llm_configs")
data class LlmConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    @ColumnInfo(name = "provider_name") val providerName: String,
    @ColumnInfo(name = "base_url") val baseUrl: String,
    val model: String,
    @ColumnInfo(name = "api_key_ref") val apiKeyRef: String,
    val temperature: Double = 0.3,
    @ColumnInfo(name = "max_tokens") val maxTokens: Int = 2048,
    val enabled: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
