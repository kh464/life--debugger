package com.lifedbg.mobile.ui.screens

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifedbg.mobile.LifeDbgApp
import com.lifedbg.mobile.llm.review.LlmReviewRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONObject
import java.time.LocalDate

data class ReviewUiState(
    val summary: String? = null,
    val outputJson: String? = null,
)

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LlmReviewRepository((application as LifeDbgApp).database.llmReviewDao())
    val state: StateFlow<ReviewUiState> = repository.observeLatest(LocalDate.now().toString())
        .map { review ->
            if (review == null) {
                ReviewUiState()
            } else {
                val obj = JSONObject(review.outputJson)
                ReviewUiState(
                    summary = obj.optString("daily_summary", "模型未返回 daily_summary"),
                    outputJson = review.outputJson,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())
}

@Composable
fun ReviewScreen(viewModel: ReviewViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "每日复盘",
        subtitle = "MVP 复盘必须来自用户配置的云端 OpenAI-compatible LLM。",
    ) {
        if (state.outputJson == null) {
            InfoCard("未生成复盘", "请先在 LLM 输入预览页确认发送摘要包。LLM 失败时不会生成规则伪复盘。")
        } else {
            InfoCard("今日总结", state.summary.orEmpty())
            InfoCard("完整 JSON", state.outputJson.orEmpty())
        }
    }
}
