package com.lifedbg.mobile.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifedbg.mobile.LifeDbgApp
import com.lifedbg.mobile.core.time.TimeUtils
import com.lifedbg.mobile.db.entity.SummaryEventEntity
import com.lifedbg.mobile.llm.secure.ApiKeyStore
import com.lifedbg.mobile.llm.provider.LlmConfigRepository
import com.lifedbg.mobile.summarize.SummaryRepository
import com.lifedbg.mobile.summarize.UsageRefreshRepository
import com.lifedbg.mobile.usage.UsageAccessChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class DashboardUiState(
    val hasUsageAccess: Boolean = false,
    val totalUsageMinutes: Long = 0,
    val summaryEventCount: Int = 0,
    val topCategories: String = "暂无",
    val llmConfigured: Boolean = false,
    val latestMessage: String? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LifeDbgApp
    private val checker = UsageAccessChecker(application)
    private val refreshRepository = UsageRefreshRepository(application, app.database)
    private val summaryRepository = SummaryRepository(app.database.summaryEventDao())
    private val llmConfigRepository = LlmConfigRepository(app.database.llmConfigDao(), ApiKeyStore(application))
    private val _state = MutableStateFlow(DashboardUiState(hasUsageAccess = checker.hasUsageAccess()))
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        val start = TimeUtils.startOfTodayMillis()
        val end = start + 24L * 60L * 60L * 1000L - 1L
        viewModelScope.launch {
            combine(
                summaryRepository.observeEvents(start, end),
                llmConfigRepository.observeEnabledConfig(),
            ) { events, config ->
                statsFor(events, config != null)
            }.collect { next ->
                _state.value = _state.value.copy(
                    totalUsageMinutes = next.totalUsageMinutes,
                    summaryEventCount = next.summaryEventCount,
                    topCategories = next.topCategories,
                    llmConfigured = next.llmConfigured,
                    hasUsageAccess = checker.hasUsageAccess(),
                )
            }
        }
    }

    fun refreshPermissionState() {
        _state.value = _state.value.copy(hasUsageAccess = checker.hasUsageAccess())
    }

    fun refreshToday() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, errorMessage = null)
            runCatching { refreshRepository.refreshToday() }
                .onSuccess { result ->
                    _state.value = _state.value.copy(
                        isRefreshing = false,
                        latestMessage = "读取 ${result.rawEventCount} 条事件，生成 ${result.summaryCount} 条摘要。",
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isRefreshing = false,
                        errorMessage = error.message ?: "刷新失败",
                    )
                }
        }
    }

    private fun statsFor(events: List<SummaryEventEntity>, llmConfigured: Boolean): DashboardUiState {
        val totalMinutes = events.mapNotNull { it.durationSeconds }.sum() / 60L
        val topCategories = events
            .groupBy { it.category ?: "unknown" }
            .mapValues { entry -> entry.value.mapNotNull { it.durationSeconds }.sum() / 60L }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .joinToString { "${it.first} ${it.second}m" }
            .ifBlank { "暂无" }
        return DashboardUiState(
            hasUsageAccess = checker.hasUsageAccess(),
            totalUsageMinutes = totalMinutes,
            summaryEventCount = events.size,
            topCategories = topCategories,
            llmConfigured = llmConfigured,
        )
    }
}

@Composable
fun DashboardScreen(
    onOpenPermission: () -> Unit,
    onOpenPreview: () -> Unit,
    onOpenIntent: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "今日概览",
        subtitle = "刷新今日数据后，系统会从 UsageEvents 生成本地 SummaryEvent。",
    ) {
        InfoCard("Usage Access", if (state.hasUsageAccess) "已授权，可以读取使用情况。" else "未授权，请先开启使用情况访问权限。")
        InfoCard("今日统计", "总时长 ${state.totalUsageMinutes} 分钟\n摘要事件 ${state.summaryEventCount} 条\n主要类别：${state.topCategories}")
        InfoCard("LLM", if (state.llmConfigured) "已配置 Provider，可以预览摘要并生成复盘。" else "尚未配置 LLM，请在设置中填写 Provider 和 API Key。")
        state.latestMessage?.let { InfoCard("最近操作", it) }
        state.errorMessage?.let { InfoCard("错误", it) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = {
                viewModel.refreshPermissionState()
                if (!state.hasUsageAccess) onOpenPermission()
            }) { Text("权限状态") }
            Button(
                enabled = state.hasUsageAccess && !state.isRefreshing,
                onClick = viewModel::refreshToday,
            ) { Text(if (state.isRefreshing) "刷新中" else "刷新今日数据") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onOpenPreview) { Text("预览 LLM 输入") }
            OutlinedButton(onClick = onOpenIntent) { Text("记录意图") }
        }
    }
}
