package com.lifedbg.mobile.ui.screens

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifedbg.mobile.LifeDbgApp
import com.lifedbg.mobile.core.device.DeviceIdProvider
import com.lifedbg.mobile.llm.prompt.LlmPromptBuilder
import com.lifedbg.mobile.llm.provider.LlmConfigRepository
import com.lifedbg.mobile.llm.provider.OpenAICompatibleClient
import com.lifedbg.mobile.llm.review.LlmOutputParser
import com.lifedbg.mobile.llm.review.LlmReviewEngine
import com.lifedbg.mobile.llm.review.LlmReviewRepository
import com.lifedbg.mobile.llm.secure.ApiKeyStore
import com.lifedbg.mobile.privacy.LlmPayloadPreviewBuilder
import com.lifedbg.mobile.privacy.PrivacySettingsRepository
import com.lifedbg.mobile.summarize.SummaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PreviewUiState(
    val json: String = "",
    val date: String = "",
    val deviceId: String = "",
    val generating: Boolean = false,
    val message: String? = null,
)

class LlmPayloadPreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LifeDbgApp
    private val summaryRepository = SummaryRepository(app.database.summaryEventDao())
    private val privacyRepository = PrivacySettingsRepository(application)
    private val builder = LlmPayloadPreviewBuilder(summaryRepository, privacyRepository, DeviceIdProvider(application))
    private val configRepository = LlmConfigRepository(app.database.llmConfigDao(), ApiKeyStore(application))
    private val reviewRepository = LlmReviewRepository(app.database.llmReviewDao())
    private val engine = LlmReviewEngine(OpenAICompatibleClient(), LlmPromptBuilder(), LlmOutputParser())
    private val _state = MutableStateFlow(PreviewUiState())
    val state: StateFlow<PreviewUiState> = _state.asStateFlow()

    init {
        refreshPreview()
    }

    fun refreshPreview() {
        viewModelScope.launch {
            runCatching { builder.buildForToday() }
                .onSuccess { payload ->
                    _state.value = PreviewUiState(
                        json = builder.toPrettyJson(payload),
                        date = payload.date,
                        deviceId = payload.deviceId,
                    )
                }
                .onFailure { _state.value = _state.value.copy(message = it.message) }
        }
    }

    fun generateReview() {
        viewModelScope.launch {
            val current = _state.value
            if (current.json.isBlank()) {
                _state.value = current.copy(message = "没有可发送的摘要包。")
                return@launch
            }
            val config = configRepository.getEnabledProviderConfig()
            if (config == null) {
                _state.value = current.copy(message = "请先配置 LLM。")
                return@launch
            }
            _state.value = current.copy(generating = true, message = null)
            engine.generate(config, current.date, current.deviceId, current.json)
                .onSuccess {
                    reviewRepository.save(it)
                    _state.value = _state.value.copy(generating = false, message = "复盘生成成功，请到复盘页查看。")
                }
                .onFailure {
                    _state.value = _state.value.copy(generating = false, message = "复盘生成失败：${it.message}")
                }
        }
    }
}

@Composable
fun LlmPayloadPreviewScreen(viewModel: LlmPayloadPreviewViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "LLM 输入预览",
        subtitle = "确认下面的摘要 JSON 后，才会发送给你配置的模型服务商。",
    ) {
        Row {
            Button(onClick = viewModel::refreshPreview) { Text("刷新预览") }
            Button(enabled = !state.generating, onClick = viewModel::generateReview) {
                Text(if (state.generating) "生成中" else "确认发送并生成复盘")
            }
        }
        state.message?.let { InfoCard("状态", it) }
        InfoCard(
            title = "摘要 JSON",
            body = state.json.ifBlank { "暂无摘要。请先在首页刷新今日数据。" },
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        )
    }
}
