package com.lifedbg.mobile.usage

import com.lifedbg.mobile.db.dao.RawUsageEventDao
import com.lifedbg.mobile.db.entity.RawUsageEventEntity

class UsageRepository(private val dao: RawUsageEventDao) {
    suspend fun insertEvents(events: List<RawUsageEvent>) {
        dao.insertAll(events.map { it.toEntity() })
    }

    suspend fun getUnprocessedEvents(startTime: Long, endTime: Long): List<RawUsageEvent> {
        return dao.getUnprocessed(startTime, endTime).map { it.toModel() }
    }

    suspend fun markProcessed(ids: List<String>) {
        if (ids.isNotEmpty()) dao.markProcessed(ids)
    }
}

private fun RawUsageEvent.toEntity(): RawUsageEventEntity {
    return RawUsageEventEntity(
        id = id,
        packageName = packageName,
        eventType = eventType,
        timestamp = timestamp,
        createdAt = createdAt,
    )
}

private fun RawUsageEventEntity.toModel(): RawUsageEvent {
    return RawUsageEvent(
        id = id,
        packageName = packageName,
        eventType = eventType,
        timestamp = timestamp,
        createdAt = createdAt,
    )
}
