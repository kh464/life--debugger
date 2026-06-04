package com.lifedbg.mobile.llm.model

data class LlmProviderConfig(
    val providerId: String,
    val providerName: String,
    val baseUrl: String,
    val model: String,
    val apiKey: String,
    val temperature: Double,
    val maxTokens: Int,
)
