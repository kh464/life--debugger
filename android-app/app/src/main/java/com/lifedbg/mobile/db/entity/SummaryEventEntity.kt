package com.lifedbg.mobile.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summary_events")
data class SummaryEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "device_type") val deviceType: String,
    val source: String,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long?,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long?,
    val category: String?,
    @ColumnInfo(name = "activity_type") val activityType: String?,
    @ColumnInfo(name = "app_label") val appLabel: String?,
    @ColumnInfo(name = "package_name") val packageName: String?,
    val summary: String,
    val confidence: Double = 0.0,
    @ColumnInfo(name = "privacy_level") val privacyLevel: String = "summary_only",
    @ColumnInfo(name = "app_label_mode") val appLabelMode: String = "real",
    @ColumnInfo(name = "llm_allowed") val llmAllowed: Boolean = true,
    @ColumnInfo(name = "user_corrected") val userCorrected: Boolean = false,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "local",
    @ColumnInfo(name = "metadata_json") val metadataJson: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
