package com.lifedbg.mobile.data

import com.lifedbg.mobile.core.time.TimeUtils
import com.lifedbg.mobile.db.LifeDbgDatabase
import com.lifedbg.mobile.llm.provider.LlmConfigRepository
import org.json.JSONArray
import org.json.JSONObject

class DataManagementRepository(
    private val database: LifeDbgDatabase,
    private val llmConfigRepository: LlmConfigRepository,
) {
    suspend fun clearRawUsageEvents() = database.rawUsageEventDao().clear()
    suspend fun clearSummaryEvents() = database.summaryEventDao().clear()
    suspend fun clearReviews() = database.llmReviewDao().clear()
    suspend fun clearLlmConfig() = llmConfigRepository.clear()

    suspend fun exportTodaySummariesJson(): String {
        val start = TimeUtils.startOfTodayMillis()
        val end = start + 24L * 60L * 60L * 1000L - 1L
        val events = database.summaryEventDao().getBetween(start, end)
        return JSONArray().apply {
            events.forEach { event ->
                put(
                    JSONObject()
                        .put("event_id", event.eventId)
                        .put("device_id", event.deviceId)
                        .put("device_type", event.deviceType)
                        .put("source", event.source)
                        .put("start_time", event.startTime)
                        .put("end_time", event.endTime)
                        .put("duration_seconds", event.durationSeconds)
                        .put("category", event.category)
                        .put("activity_type", event.activityType)
                        .put("app_label", event.appLabel)
                        .put("summary", event.summary)
                        .put("privacy_level", event.privacyLevel)
                        .put("llm_allowed", event.llmAllowed),
                )
            }
        }.toString(2)
    }
}
