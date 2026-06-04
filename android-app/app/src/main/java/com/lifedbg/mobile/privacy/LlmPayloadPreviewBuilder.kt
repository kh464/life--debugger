package com.lifedbg.mobile.privacy

import com.lifedbg.mobile.core.device.DeviceIdProvider
import com.lifedbg.mobile.db.entity.SummaryEventEntity
import com.lifedbg.mobile.llm.model.LlmAnalysisPackage
import com.lifedbg.mobile.llm.model.LlmEventItem
import com.lifedbg.mobile.summarize.SummaryRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.UUID

class LlmPayloadPreviewBuilder(
    private val summaryRepository: SummaryRepository,
    private val privacySettingsRepository: PrivacySettingsRepository,
    private val deviceIdProvider: DeviceIdProvider,
) {
    suspend fun buildForToday(): LlmAnalysisPackage {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val settings = privacySettingsRepository.settings.first()
        val events = summaryRepository.getEvents(start, end)
            .filter { it.llmAllowed }
            .filter { it.allowedByCategory(settings) }
            .sortedBy { it.startTime }
            .map { it.toLlmEvent(settings) }

        return LlmAnalysisPackage(
            packageId = UUID.randomUUID().toString(),
            date = today.toString(),
            timezone = zone.id,
            deviceId = deviceIdProvider.getOrCreateDeviceId(),
            deviceType = "android",
            events = events,
            privacyNote = "All events are local summaries. Raw app content, chat content, screenshots and keystrokes are not included.",
        )
    }

    fun toPrettyJson(payload: LlmAnalysisPackage): String {
        return JSONObject()
            .put("package_id", payload.packageId)
            .put("date", payload.date)
            .put("timezone", payload.timezone)
            .put(
                "device",
                JSONObject()
                    .put("device_id", payload.deviceId)
                    .put("device_type", payload.deviceType)
                    .put("role", "primary_analysis"),
            )
            .put(
                "events",
                JSONArray().apply {
                    payload.events.forEach { event ->
                        put(
                            JSONObject()
                                .put("start", event.start)
                                .put("end", event.end)
                                .put("category", event.category)
                                .put("activity_type", event.activityType)
                                .put("app_label", event.appLabel)
                                .put("package_name", event.packageName)
                                .put("summary", event.summary),
                        )
                    }
                },
            )
            .put("privacy_note", payload.privacyNote)
            .toString(2)
    }

    private fun SummaryEventEntity.allowedByCategory(settings: PrivacySettings): Boolean {
        return when (category) {
            "finance" -> settings.allowFinanceInLlm
            "health" -> settings.allowHealthInLlm
            "unknown" -> settings.allowUnknownInLlm
            else -> true
        }
    }

    private fun SummaryEventEntity.toLlmEvent(settings: PrivacySettings): LlmEventItem {
        return LlmEventItem(
            start = formatClock(startTime),
            end = endTime?.let { formatClock(it) },
            category = category,
            activityType = activityType,
            appLabel = if (settings.allowAppLabelInLlm) appLabel else null,
            packageName = if (settings.allowPackageNameInLlm) packageName else null,
            summary = summary,
        )
    }

    private fun formatClock(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
