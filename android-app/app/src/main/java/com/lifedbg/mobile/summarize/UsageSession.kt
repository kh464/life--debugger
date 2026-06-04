package com.lifedbg.mobile.summarize

data class UsageSession(
    val packageName: String,
    val appLabel: String?,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
)
