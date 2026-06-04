package com.lifedbg.mobile.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifedbg.mobile.LifeDbgApp
import com.lifedbg.mobile.llm.model.LlmProviderConfig
import com.lifedbg.mobile.llm.provider.LlmConfigRepository
import com.lifedbg.mobile.llm.provider.OpenAICompatibleClient
import com.lifedbg.mobile.llm.secure.ApiKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LlmSettingsUiState(
    val providerId: String = "deepseek",
    val providerName: String = "DeepSeek",
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-chat",
    val apiKey: String = "",
    val temperature: String = "0.3",
    val maxTokens: String = "2048",
    val testing: Boolean = false,
    val message: String? = null,
)

class LlmSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LifeDbgApp
    private val repository = LlmConfigRepository(app.database.llmConfigDao(), ApiKeyStore(application))
    private val client = OpenAICompatibleClient()
    private val _state = MutableStateFlow(LlmSettingsUiState())
    val state: StateFlow<LlmSettingsUiState> = _state.asStateFlow()

    fun selectPreset(providerId: String) {
        _state.value = when (providerId) {
            "qwen" -> _state.value.copy(
                providerId = "qwen",
                providerName = "Qwen",
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
                model = "qwen-plus",
                message = null,
            )
            "custom" -> _state.value.copy(
                providerId = "custom",
                providerName = "Custom",
                baseUrl = "",
                model = "",
                message = null,
            )
            else -> _state.value.copy(
                providerId = "deepseek",
                providerName = "DeepSeek",
                baseUrl = "https://api.deepseek.com",
                model = "deepseek-chat",
                message = null,
            )
        }
    }

    fun update(transform: (LlmSettingsUiState) -> LlmSettingsUiState) {
        _state.value = transform(_state.value)
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value
            saveCurrentConfig(current)
            _state.value = current.copy(message = "LLM 配置已保存，API Key 已加密存储。", apiKey = "")
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val current = _state.value
            val apiKey = current.apiKey.ifBlank {
                repository.getEnabledProviderConfig()?.apiKey.orEmpty()
            }
            if (apiKey.isBlank()) {
                _state.value = current.copy(message = "请先输入 API Key。")
                return@launch
            }
            _state.value = current.copy(testing = true, message = null)
            if (current.apiKey.isNotBlank()) {
                saveCurrentConfig(current)
            }
            val config = LlmProviderConfig(
                providerId = current.providerId,
                providerName = current.providerName,
                baseUrl = current.baseUrl,
                model = current.model,
                apiKey = apiKey,
                temperature = current.temperature.toDoubleOrNull() ?: 0.3,
                maxTokens = current.maxTokens.toIntOrNull() ?: 2048,
            )
            val message = client.testConnection(config).fold(
                onSuccess = { "LLM 连接成功。" },
                onFailure = { "LLM 连接失败：${it.message}" },
            )
            _state.value = _state.value.copy(testing = false, message = message)
        }
    }

    private suspend fun saveCurrentConfig(current: LlmSettingsUiState) {
        repository.save(
            providerId = current.providerId,
            providerName = current.providerName,
            baseUrl = current.baseUrl,
            model = current.model,
            apiKey = current.apiKey,
            temperature = current.temperature.toDoubleOrNull() ?: 0.3,
            maxTokens = current.maxTokens.toIntOrNull() ?: 2048,
        )
    }
}

@Composable
fun LlmSettingsScreen(viewModel: LlmSettingsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "LLM 配置",
        subtitle = "支持 DeepSeek、Qwen 和自定义 OpenAI-compatible Provider。",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.selectPreset("deepseek") }) { Text("DeepSeek") }
            Button(onClick = { viewModel.selectPreset("qwen") }) { Text("Qwen") }
            Button(onClick = { viewModel.selectPreset("custom") }) { Text("Custom") }
        }
        OutlinedTextField(value = state.providerName, onValueChange = { v -> viewModel.update { it.copy(providerName = v) } }, label = { Text("Provider") })
        OutlinedTextField(value = state.baseUrl, onValueChange = { v -> viewModel.update { it.copy(baseUrl = v) } }, label = { Text("Base URL") })
        OutlinedTextField(value = state.model, onValueChange = { v -> viewModel.update { it.copy(model = v) } }, label = { Text("Model") })
        OutlinedTextField(value = state.apiKey, onValueChange = { v -> viewModel.update { it.copy(apiKey = v) } }, label = { Text("API Key") })
        OutlinedTextField(value = state.temperature, onValueChange = { v -> viewModel.update { it.copy(temperature = v) } }, label = { Text("Temperature") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(value = state.maxTokens, onValueChange = { v -> viewModel.update { it.copy(maxTokens = v) } }, label = { Text("Max Tokens") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::save) { Text("保存") }
            Button(enabled = !state.testing, onClick = viewModel::testConnection) { Text(if (state.testing) "测试中" else "测试连接") }
        }
        state.message?.let { InfoCard("状态", it) }
    }
}
