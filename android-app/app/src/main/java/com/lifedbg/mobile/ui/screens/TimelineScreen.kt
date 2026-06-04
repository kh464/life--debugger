package com.lifedbg.mobile.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import com.lifedbg.mobile.LifeDbgApp
import com.lifedbg.mobile.core.time.TimeUtils
import com.lifedbg.mobile.db.entity.SummaryEventEntity
import com.lifedbg.mobile.summarize.SummaryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SummaryRepository((application as LifeDbgApp).database.summaryEventDao())
    private val start = TimeUtils.startOfTodayMillis()
    private val end = start + 24L * 60L * 60L * 1000L - 1L
    val events: StateFlow<List<SummaryEventEntity>> = repository.observeEvents(start, end)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setLlmAllowed(eventId: String, allowed: Boolean) {
        viewModelScope.launch { repository.setLlmAllowed(eventId, allowed) }
    }

    fun delete(eventId: String) {
        viewModelScope.launch { repository.delete(eventId) }
    }
}

@Composable
fun TimelineScreen(viewModel: TimelineViewModel = viewModel()) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "时间线",
        subtitle = "按时间排序展示 SummaryEvent，可排除单条事件进入 LLM。",
    ) {
        if (events.isEmpty()) {
            InfoCard("空状态", "还没有摘要。请在首页授权并刷新今日数据。")
        } else {
            events.forEach { event ->
                InfoCard(
                    title = "${formatClock(event.startTime)} - ${event.endTime?.let(::formatClock) ?: "进行中"}",
                    body = "${event.appLabel ?: event.category ?: "未知"} · ${event.durationSeconds?.let { "${it / 60} 分钟" } ?: "手动意图"}\n${event.summary}",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("进入 LLM")
                    Switch(
                        checked = event.llmAllowed,
                        onCheckedChange = { viewModel.setLlmAllowed(event.eventId, it) },
                    )
                    Button(onClick = { viewModel.delete(event.eventId) }) {
                        Text("删除")
                    }
                }
            }
        }
    }
}

private fun formatClock(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
