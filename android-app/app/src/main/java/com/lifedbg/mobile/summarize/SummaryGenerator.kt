package com.lifedbg.mobile.summarize

import com.lifedbg.mobile.core.device.DeviceIdProvider
import com.lifedbg.mobile.core.time.TimeUtils
import com.lifedbg.mobile.db.entity.SummaryEventEntity
import java.util.UUID

class SummaryGenerator(
    private val deviceIdProvider: DeviceIdProvider,
    private val classifier: AppClassifier,
) {
    suspend fun generate(sessions: List<UsageSession>): List<SummaryEventEntity> {
        val deviceId = deviceIdProvider.getOrCreateDeviceId()
        return sessions.map { session ->
            val classification = classifier.classify(session.packageName)
            val appLabel = classification.appLabel ?: session.packageName
            val durationText = formatDuration(session.durationSeconds)
            val llmAllowed = !classification.isSensitive
            SummaryEventEntity(
                eventId = UUID.randomUUID().toString(),
                deviceId = deviceId,
                deviceType = "android",
                source = "android_usage_stats",
                startTime = session.startTime,
                endTime = session.endTime,
                durationSeconds = session.durationSeconds,
                category = classification.category,
                activityType = classification.activityType,
                appLabel = appLabel,
                packageName = session.packageName,
                summary = "连续使用 $appLabel 约 $durationText。",
                confidence = classification.confidence,
                privacyLevel = if (classification.isSensitive) "sensitive_masked" else "summary_only",
                appLabelMode = "real",
                llmAllowed = llmAllowed,
                userCorrected = false,
                syncStatus = "local",
                metadataJson = null,
                createdAt = TimeUtils.nowMillis(),
            )
        }
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = (seconds / 60L).coerceAtLeast(1L)
        val hours = minutes / 60L
        val remainMinutes = minutes % 60L
        return if (hours > 0) "${hours}小时${remainMinutes}分钟" else "${minutes}分钟"
    }
}
