package com.lifedbg.mobile.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.lifedbg.mobile.core.time.TimeUtils
import java.security.MessageDigest

class UsageStatsReader(private val context: Context) {
    fun readEvents(startTime: Long, endTime: Long): List<RawUsageEvent> {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        val result = mutableListOf<RawUsageEvent>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            val normalized = UsageEventNormalizer.normalize(event.eventType)
            if (normalized == UsageEventNormalizer.UNKNOWN) continue

            result += RawUsageEvent(
                id = stableId(packageName, normalized, event.timeStamp),
                packageName = packageName,
                eventType = normalized,
                timestamp = event.timeStamp,
                createdAt = TimeUtils.nowMillis(),
            )
        }

        return result
    }

    private fun stableId(packageName: String, eventType: String, timestamp: Long): String {
        val input = "$packageName|$eventType|$timestamp"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
