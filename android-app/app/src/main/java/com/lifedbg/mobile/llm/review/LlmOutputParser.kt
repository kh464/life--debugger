package com.lifedbg.mobile.llm.review

import org.json.JSONObject

class LlmOutputParser {
    fun parseToJson(rawText: String): Result<String> {
        return runCatching {
            val trimmed = rawText.trim()
            val jsonText = when {
                trimmed.startsWith("{") -> trimmed
                "```" in trimmed -> extractFromFence(trimmed)
                else -> extractFirstObject(trimmed)
            }
            val obj = JSONObject(jsonText)
            require(obj.has("daily_summary")) { "Missing daily_summary" }
            obj.toString(2)
        }
    }

    private fun extractFromFence(text: String): String {
        val withoutFence = text
            .replace("```json", "```", ignoreCase = true)
            .substringAfter("```")
            .substringBeforeLast("```")
            .trim()
        return if (withoutFence.startsWith("{")) withoutFence else extractFirstObject(withoutFence)
    }

    private fun extractFirstObject(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "No JSON object found" }
        return text.substring(start, end + 1)
    }
}
