package com.lifedbg.mobile.llm.review

import com.lifedbg.mobile.core.time.TimeUtils
import com.lifedbg.mobile.db.entity.LlmReviewEntity
import com.lifedbg.mobile.llm.model.LlmProviderConfig
import com.lifedbg.mobile.llm.prompt.LlmPromptBuilder
import com.lifedbg.mobile.llm.provider.OpenAICompatibleClient
import java.security.MessageDigest
import java.util.UUID

class LlmReviewEngine(
    private val client: OpenAICompatibleClient,
    private val promptBuilder: LlmPromptBuilder,
    private val parser: LlmOutputParser,
) {
    suspend fun generate(
        config: LlmProviderConfig,
        date: String,
        primaryDeviceId: String,
        payloadJson: String,
    ): Result<LlmReviewEntity> {
        val systemPrompt = promptBuilder.systemPrompt()
        val userPrompt = promptBuilder.userPrompt(payloadJson)
        return client.generateReview(config, systemPrompt, userPrompt).mapCatching { raw ->
            val outputJson = parser.parseToJson(raw).getOrThrow()
            LlmReviewEntity(
                reviewId = UUID.randomUUID().toString(),
                date = date,
                primaryDeviceId = primaryDeviceId,
                providerId = config.providerId,
                model = config.model,
                inputPackageHash = sha256(payloadJson),
                outputJson = outputJson,
                createdAt = TimeUtils.nowMillis(),
            )
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
