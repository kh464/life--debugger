package com.lifedbg.mobile.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifedbg.mobile.LifeDbgApp
import com.lifedbg.mobile.core.device.DeviceIdProvider
import com.lifedbg.mobile.intent.ManualIntentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IntentViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LifeDbgApp
    private val repository = ManualIntentRepository(
        app.database.manualIntentDao(),
        app.database.summaryEventDao(),
        DeviceIdProvider(application),
    )
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun record(option: IntentOption) {
        viewModelScope.launch {
            repository.record(option.category, option.activityType, option.label)
            _message.value = "已记录：准备${option.label}"
        }
    }
}

data class IntentOption(val label: String, val category: String, val activityType: String)

private val intentOptions = listOf(
    IntentOption("工作", "work", "productive"),
    IntentOption("学习", "study", "productive"),
    IntentOption("写作", "writing", "productive"),
    IntentOption("编码", "coding", "productive"),
    IntentOption("查资料", "browser", "productive"),
    IntentOption("放松", "entertainment", "entertainment"),
    IntentOption("通勤", "navigation", "life_service"),
    IntentOption("聊天", "chat", "communication"),
    IntentOption("购物", "shopping", "life_service"),
    IntentOption("休息", "rest", "life_service"),
)

@Composable
fun IntentScreen(viewModel: IntentViewModel = viewModel()) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "手动意图",
        subtitle = "帮助区分计划休息、工作沟通和无意识切换。",
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            intentOptions.forEach { option ->
                Button(onClick = { viewModel.record(option) }) {
                    Text(option.label)
                }
            }
        }
        message?.let { InfoCard("状态", it) }
    }
}
