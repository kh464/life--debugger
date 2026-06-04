package com.lifedbg.mobile.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "raw_usage_events")
data class RawUsageEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    val timestamp: Long,
    val processed: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
