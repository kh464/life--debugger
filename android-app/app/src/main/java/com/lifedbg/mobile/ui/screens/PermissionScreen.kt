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
import com.lifedbg.mobile.usage.UsageAccessChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PermissionViewModel(application: Application) : AndroidViewModel(application) {
    private val checker = UsageAccessChecker(application)
    private val _hasAccess = MutableStateFlow(checker.hasUsageAccess())
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    fun refresh() {
        _hasAccess.value = checker.hasUsageAccess()
    }

    fun openSettings() {
        viewModelScope.launch {
            getApplication<Application>().startActivity(checker.usageAccessSettingsIntent())
        }
    }
}

@Composable
fun PermissionScreen(viewModel: PermissionViewModel = viewModel()) {
    val hasAccess by viewModel.hasAccess.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "使用情况访问权限",
        subtitle = "Life Debugger 需要读取 App 使用时长，用于生成本地行为摘要。",
    ) {
        InfoCard(
            title = if (hasAccess) "已授权" else "未授权",
            body = "它不会读取聊天内容，不会录屏，不会使用无障碍权限。",
        )
        Button(onClick = viewModel::openSettings) {
            Text("去开启权限")
        }
        Button(onClick = viewModel::refresh) {
            Text("我已开启，重新检测")
        }
    }
}
