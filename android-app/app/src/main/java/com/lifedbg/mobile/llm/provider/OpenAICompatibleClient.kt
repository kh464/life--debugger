package com.lifedbg.mobile.llm.provider

import com.lifedbg.mobile.llm.model.LlmProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAICompatibleClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun testConnection(config: LlmProviderConfig): Result<Unit> {
        return generate(config, "You are a connection tester.", "Return JSON: {\"ok\": true}", 64)
            .map { Unit }
    }

    suspend fun generateReview(
        config: LlmProviderConfig,
        systemPrompt: String,
        userPrompt: String,
    ): Result<String> {
        return generate(config, systemPrompt, userPrompt, config.maxTokens)
    }

    private suspend fun generate(
        config: LlmProviderConfig,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val requestJson = buildRequest(config, systemPrompt, userPrompt, maxTokens, useJsonMode = true)
            execute(config, requestJson)
        }.recoverCatching {
            val fallbackJson = buildRequest(config, systemPrompt, userPrompt, maxTokens, useJsonMode = false)
            execute(config, fallbackJson)
        }
    }

    private fun execute(config: LlmProviderConfig, bodyJson: JSONObject): String {
        val body = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("LLM HTTP ${response.code}: ${responseBody.take(240)}")
            }
            val root = JSONObject(responseBody)
            return root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    private fun buildRequest(
        config: LlmProviderConfig,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        useJsonMode: Boolean,
    ): JSONObject {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", userPrompt))
        return JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("temperature", config.temperature)
            .put("max_tokens", maxTokens)
            .apply {
                if (useJsonMode) put("response_format", JSONObject().put("type", "json_object"))
            }
    }
}
