package com.lifedbg.mobile.summarize

import com.lifedbg.mobile.db.dao.SummaryEventDao
import com.lifedbg.mobile.db.entity.SummaryEventEntity
import kotlinx.coroutines.flow.Flow

class SummaryRepository(private val dao: SummaryEventDao) {
    fun observeEvents(startTime: Long, endTime: Long): Flow<List<SummaryEventEntity>> {
        return dao.observeBetween(startTime, endTime)
    }

    suspend fun getEvents(startTime: Long, endTime: Long): List<SummaryEventEntity> {
        return dao.getBetween(startTime, endTime)
    }

    suspend fun save(events: List<SummaryEventEntity>) {
        if (events.isNotEmpty()) dao.upsertAll(events)
    }

    suspend fun setLlmAllowed(eventId: String, allowed: Boolean) {
        dao.updateLlmAllowed(eventId, allowed)
    }

    suspend fun delete(eventId: String) {
        dao.deleteById(eventId)
    }
}
