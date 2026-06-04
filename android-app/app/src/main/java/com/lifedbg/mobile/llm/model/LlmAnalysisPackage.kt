package com.lifedbg.mobile.llm.model

data class LlmAnalysisPackage(
    val packageId: String,
    val date: String,
    val timezone: String,
    val deviceId: String,
    val deviceType: String,
    val events: List<LlmEventItem>,
    val privacyNote: String,
)

data class LlmEventItem(
    val start: String,
    val end: String?,
    val category: String?,
    val activityType: String?,
    val appLabel: String?,
    val packageName: String?,
    val summary: String,
)
