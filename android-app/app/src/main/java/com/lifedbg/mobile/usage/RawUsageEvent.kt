package com.lifedbg.mobile.usage

data class RawUsageEvent(
    val id: String,
    val packageName: String,
    val eventType: String,
    val timestamp: Long,
    val createdAt: Long,
)
