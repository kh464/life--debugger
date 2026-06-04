package com.lifedbg.mobile.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llm_reviews")
data class LlmReviewEntity(
    @PrimaryKey
    @ColumnInfo(name = "review_id")
    val reviewId: String,
    val date: String,
    @ColumnInfo(name = "primary_device_id") val primaryDeviceId: String,
    @ColumnInfo(name = "provider_id") val providerId: String,
    val model: String,
    @ColumnInfo(name = "input_package_hash") val inputPackageHash: String,
    @ColumnInfo(name = "output_json") val outputJson: String,
    @ColumnInfo(name = "user_feedback_json") val userFeedbackJson: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
