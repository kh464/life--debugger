package com.lifedbg.mobile.ui.screens

import android.app.Application
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifedbg.mobile.LifeDbgApp
import com.lifedbg.mobile.data.DataManagementRepository
import com.lifedbg.mobile.llm.provider.LlmConfigRepository
import com.lifedbg.mobile.llm.secure.ApiKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LifeDbgApp
    private val repository = DataManagementRepository(
        app.database,
        LlmConfigRepository(app.database.llmConfigDao(), ApiKeyStore(application)),
    )
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _exportJson = MutableStateFlow("")
    val exportJson: StateFlow<String> = _exportJson.asStateFlow()

    fun clearRaw() = runAction("已清空原始使用事件。") { repository.clearRawUsageEvents() }
    fun clearSummaries() = runAction("已清空摘要事件。") { repository.clearSummaryEvents() }
    fun clearReviews() = runAction("已清空复盘历史。") { repository.clearReviews() }
    fun clearLlm() = runAction("已清空 LLM 配置和 API Key。") { repository.clearLlmConfig() }
    fun exportToday() = runAction("已生成今日摘要 JSON。") {
        _exportJson.value = repository.exportTodaySummariesJson()
    }

    private fun runAction(message: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            action()
            _message.value = message
        }
    }
}

@Composable
fun DataManagementScreen(viewModel: DataManagementViewModel = viewModel()) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val exportJson by viewModel.exportJson.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "数据管理",
        subtitle = "清空本地数据、清空 LLM 配置。第一阶段不连接开发者服务器。",
    ) {
        Button(onClick = viewModel::clearRaw) { Text("清空 raw_usage_events") }
        Button(onClick = viewModel::clearSummaries) { Text("清空 summary_events") }
        Button(onClick = viewModel::clearReviews) { Text("清空 llm_reviews") }
        Button(onClick = viewModel::clearLlm) { Text("清空 LLM 配置") }
        Button(onClick = viewModel::exportToday) { Text("导出今日摘要 JSON") }
        message?.let { InfoCard("状态", it) }
        if (exportJson.isNotBlank()) {
            InfoCard("导出内容", exportJson)
        }
    }
}
