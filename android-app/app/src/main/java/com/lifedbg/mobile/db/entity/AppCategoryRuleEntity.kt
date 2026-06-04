package com.lifedbg.mobile.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_category_rules")
data class AppCategoryRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "app_label") val appLabel: String?,
    val category: String,
    @ColumnInfo(name = "activity_type") val activityType: String,
    @ColumnInfo(name = "is_sensitive") val isSensitive: Boolean = false,
    @ColumnInfo(name = "user_override") val userOverride: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
