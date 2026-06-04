package com.lifedbg.mobile.summarize

data class AppClassification(
    val packageName: String,
    val appLabel: String?,
    val category: String,
    val activityType: String,
    val isSensitive: Boolean,
    val confidence: Double,
)
