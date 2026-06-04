package com.lifedbg.mobile.llm.review

import com.lifedbg.mobile.db.dao.LlmReviewDao
import com.lifedbg.mobile.db.entity.LlmReviewEntity
import kotlinx.coroutines.flow.Flow

class LlmReviewRepository(private val dao: LlmReviewDao) {
    fun observeLatest(date: String): Flow<LlmReviewEntity?> = dao.observeLatestForDate(date)

    suspend fun save(review: LlmReviewEntity) {
        dao.upsert(review)
    }
}
