package com.lifedbg.mobile.usage

import android.app.usage.UsageEvents

object UsageEventNormalizer {
    const val ACTIVITY_RESUMED = "ACTIVITY_RESUMED"
    const val ACTIVITY_PAUSED = "ACTIVITY_PAUSED"
    const val ACTIVITY_STOPPED = "ACTIVITY_STOPPED"
    const val MOVE_TO_FOREGROUND = "MOVE_TO_FOREGROUND"
    const val MOVE_TO_BACKGROUND = "MOVE_TO_BACKGROUND"
    const val UNKNOWN = "UNKNOWN"

    fun normalize(eventType: Int): String {
        return when (eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> ACTIVITY_RESUMED
            UsageEvents.Event.ACTIVITY_PAUSED -> ACTIVITY_PAUSED
            UsageEvents.Event.ACTIVITY_STOPPED -> ACTIVITY_STOPPED
            UsageEvents.Event.MOVE_TO_FOREGROUND -> MOVE_TO_FOREGROUND
            UsageEvents.Event.MOVE_TO_BACKGROUND -> MOVE_TO_BACKGROUND
            else -> UNKNOWN
        }
    }
}
