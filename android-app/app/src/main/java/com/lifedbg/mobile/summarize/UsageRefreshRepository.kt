package com.lifedbg.mobile.summarize

import android.content.Context
import com.lifedbg.mobile.core.device.DeviceIdProvider
import com.lifedbg.mobile.core.time.TimeUtils
import com.lifedbg.mobile.db.LifeDbgDatabase
import com.lifedbg.mobile.usage.UsageRepository
import com.lifedbg.mobile.usage.UsageStatsReader

class UsageRefreshRepository(context: Context, private val database: LifeDbgDatabase) {
    private val appContext = context.applicationContext
    private val usageRepository = UsageRepository(database.rawUsageEventDao())
    private val reader = UsageStatsReader(appContext)
    private val sessionMerger = SessionMerger()
    private val classifier = AppClassifier(appContext, database.appCategoryRuleDao())
    private val generator = SummaryGenerator(DeviceIdProvider(appContext), classifier)
    private val summaryRepository = SummaryRepository(database.summaryEventDao())

    suspend fun refreshToday(): RefreshResult {
        val start = TimeUtils.startOfTodayMillis()
        val end = TimeUtils.nowMillis()
        val rawEvents = reader.readEvents(start, end)
        usageRepository.insertEvents(rawEvents)
        val unprocessed = usageRepository.getUnprocessedEvents(start, end)
        val sessions = sessionMerger.merge(unprocessed, end)
        val summaries = generator.generate(sessions)
        summaryRepository.save(summaries)
        usageRepository.markProcessed(unprocessed.map { it.id })
        return RefreshResult(
            rawEventCount = rawEvents.size,
            sessionCount = sessions.size,
            summaryCount = summaries.size,
        )
    }
}

data class RefreshResult(
    val rawEventCount: Int,
    val sessionCount: Int,
    val summaryCount: Int,
)
