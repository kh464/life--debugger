package com.lifedbg.mobile.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manual_intent_events")
data class ManualIntentEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long?,
    val category: String,
    @ColumnInfo(name = "activity_type") val activityType: String,
    val summary: String,
    @ColumnInfo(name = "llm_allowed") val llmAllowed: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
