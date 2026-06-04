package com.lifedbg.mobile.intent

import com.lifedbg.mobile.core.device.DeviceIdProvider
import com.lifedbg.mobile.core.time.TimeUtils
import com.lifedbg.mobile.db.dao.ManualIntentDao
import com.lifedbg.mobile.db.dao.SummaryEventDao
import com.lifedbg.mobile.db.entity.ManualIntentEntity
import com.lifedbg.mobile.db.entity.SummaryEventEntity
import java.util.UUID

class ManualIntentRepository(
    private val intentDao: ManualIntentDao,
    private val summaryEventDao: SummaryEventDao,
    private val deviceIdProvider: DeviceIdProvider,
) {
    suspend fun record(category: String, activityType: String, label: String) {
        val now = TimeUtils.nowMillis()
        val deviceId = deviceIdProvider.getOrCreateDeviceId()
        val eventId = UUID.randomUUID().toString()
        intentDao.upsert(
            ManualIntentEntity(
                eventId = eventId,
                deviceId = deviceId,
                startTime = now,
                endTime = null,
                category = category,
                activityType = activityType,
                summary = "用户手动记录：准备$label。",
                createdAt = now,
            ),
        )
        summaryEventDao.upsertAll(
            listOf(
                SummaryEventEntity(
                    eventId = eventId,
                    deviceId = deviceId,
                    deviceType = "android",
                    source = "manual_intent",
                    startTime = now,
                    endTime = null,
                    durationSeconds = null,
                    category = category,
                    activityType = activityType,
                    appLabel = null,
                    packageName = null,
                    summary = "用户手动记录：准备$label。",
                    confidence = 1.0,
                    privacyLevel = "user_input",
                    appLabelMode = "hidden",
                    llmAllowed = true,
                    userCorrected = true,
                    syncStatus = "local",
                    createdAt = now,
                ),
            ),
        )
    }
}
