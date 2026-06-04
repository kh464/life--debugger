package com.lifedbg.mobile.llm.provider

import com.lifedbg.mobile.core.time.TimeUtils
import com.lifedbg.mobile.db.dao.LlmConfigDao
import com.lifedbg.mobile.db.entity.LlmConfigEntity
import com.lifedbg.mobile.llm.model.LlmProviderConfig
import com.lifedbg.mobile.llm.secure.ApiKeyStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LlmConfigRepository(
    private val dao: LlmConfigDao,
    private val apiKeyStore: ApiKeyStore,
) {
    fun observeEnabledConfig(): Flow<LlmConfigEntity?> = dao.observeEnabled()

    suspend fun save(
        providerId: String,
        providerName: String,
        baseUrl: String,
        model: String,
        apiKey: String,
        temperature: Double,
        maxTokens: Int,
    ) {
        val now = TimeUtils.nowMillis()
        if (apiKey.isNotBlank()) apiKeyStore.saveApiKey(providerId, apiKey)
        dao.upsert(
            LlmConfigEntity(
                providerId = providerId,
                providerName = providerName,
                baseUrl = baseUrl.trimEnd('/'),
                model = model,
                apiKeyRef = "keystore:$providerId",
                temperature = temperature,
                maxTokens = maxTokens,
                enabled = true,
                createdAt = dao.getByProviderId(providerId)?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    suspend fun getEnabledProviderConfig(): LlmProviderConfig? {
        val entity = dao.getEnabled() ?: return null
        val apiKey = apiKeyStore.getApiKey(entity.providerId) ?: return null
        return LlmProviderConfig(
            providerId = entity.providerId,
            providerName = entity.providerName,
            baseUrl = entity.baseUrl,
            model = entity.model,
            apiKey = apiKey,
            temperature = entity.temperature,
            maxTokens = entity.maxTokens,
        )
    }

    suspend fun clear() {
        dao.clear()
        apiKeyStore.clearAll()
    }
}
