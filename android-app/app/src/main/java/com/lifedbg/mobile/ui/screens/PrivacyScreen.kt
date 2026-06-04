package com.lifedbg.mobile.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifedbg.mobile.privacy.PrivacySettings
import com.lifedbg.mobile.privacy.PrivacySettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrivacyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PrivacySettingsRepository(application)
    val settings: StateFlow<PrivacySettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacySettings())

    fun update(transform: (PrivacySettings) -> PrivacySettings) {
        viewModelScope.launch { repository.update(transform) }
    }
}

@Composable
fun PrivacyScreen(viewModel: PrivacyViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "隐私设置",
        subtitle = "控制哪些摘要字段和类别可以进入 LLM 输入包。",
    ) {
        PrivacySwitch("允许 App 名称进入 LLM", settings.allowAppLabelInLlm) {
            viewModel.update { s -> s.copy(allowAppLabelInLlm = it) }
        }
        PrivacySwitch("允许 packageName 进入 LLM", settings.allowPackageNameInLlm) {
            viewModel.update { s -> s.copy(allowPackageNameInLlm = it) }
        }
        PrivacySwitch("金融类事件进入 LLM", settings.allowFinanceInLlm) {
            viewModel.update { s -> s.copy(allowFinanceInLlm = it) }
        }
        PrivacySwitch("健康类事件进入 LLM", settings.allowHealthInLlm) {
            viewModel.update { s -> s.copy(allowHealthInLlm = it) }
        }
        PrivacySwitch("未知 App 进入 LLM", settings.allowUnknownInLlm) {
            viewModel.update { s -> s.copy(allowUnknownInLlm = it) }
        }
        PrivacySwitch("每次 LLM 分析前确认", settings.confirmBeforeLlmAnalysis) {
            viewModel.update { s -> s.copy(confirmBeforeLlmAnalysis = it) }
        }
    }
}

@Composable
private fun PrivacySwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
