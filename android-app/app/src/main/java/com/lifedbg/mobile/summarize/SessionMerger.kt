package com.lifedbg.mobile.summarize

import com.lifedbg.mobile.usage.RawUsageEvent
import com.lifedbg.mobile.usage.UsageEventNormalizer
import kotlin.math.max

class SessionMerger {
    fun merge(events: List<RawUsageEvent>, endBoundary: Long): List<UsageSession> {
        val sorted = events.sortedBy { it.timestamp }
        val sessions = mutableListOf<UsageSession>()
        var activePackage: String? = null
        var activeStart: Long? = null

        for (event in sorted) {
            when (event.eventType) {
                UsageEventNormalizer.ACTIVITY_RESUMED,
                UsageEventNormalizer.MOVE_TO_FOREGROUND -> {
                    val currentPackage = activePackage
                    val currentStart = activeStart
                    if (currentPackage != null && currentStart != null && currentPackage != event.packageName) {
                        addSession(sessions, currentPackage, currentStart, event.timestamp)
                    }
                    activePackage = event.packageName
                    activeStart = event.timestamp
                }

                UsageEventNormalizer.ACTIVITY_PAUSED,
                UsageEventNormalizer.ACTIVITY_STOPPED,
                UsageEventNormalizer.MOVE_TO_BACKGROUND -> {
                    val currentPackage = activePackage
                    val currentStart = activeStart
                    if (currentPackage == event.packageName && currentStart != null) {
                        addSession(sessions, currentPackage, currentStart, event.timestamp)
                        activePackage = null
                        activeStart = null
                    }
                }
            }
        }

        val currentPackage = activePackage
        val currentStart = activeStart
        if (currentPackage != null && currentStart != null) {
            addSession(sessions, currentPackage, currentStart, endBoundary)
        }

        return mergeAdjacent(sessions)
    }

    private fun addSession(target: MutableList<UsageSession>, packageName: String, start: Long, end: Long) {
        val duration = max(0L, (end - start) / 1000L)
        if (duration < MIN_SESSION_SECONDS) return
        val cappedDuration = duration.coerceAtMost(MAX_SESSION_SECONDS)
        target += UsageSession(
            packageName = packageName,
            appLabel = null,
            startTime = start,
            endTime = start + cappedDuration * 1000L,
            durationSeconds = cappedDuration,
        )
    }

    private fun mergeAdjacent(sessions: List<UsageSession>): List<UsageSession> {
        val result = mutableListOf<UsageSession>()
        for (session in sessions.sortedBy { it.startTime }) {
            val last = result.lastOrNull()
            if (last != null &&
                last.packageName == session.packageName &&
                session.startTime - last.endTime <= ADJACENT_GAP_MILLIS
            ) {
                result[result.lastIndex] = last.copy(
                    endTime = session.endTime,
                    durationSeconds = (session.endTime - last.startTime) / 1000L,
                )
            } else {
                result += session
            }
        }
        return result
    }

    private companion object {
        const val MIN_SESSION_SECONDS = 15L
        const val MAX_SESSION_SECONDS = 6L * 60L * 60L
        const val ADJACENT_GAP_MILLIS = 60_000L
    }
}
