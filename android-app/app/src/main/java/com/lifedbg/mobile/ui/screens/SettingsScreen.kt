package com.lifedbg.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onOpenOnboarding: () -> Unit,
    onOpenPermission: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenLlm: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenData: () -> Unit,
) {
    ScreenScaffold(
        title = "设置",
        subtitle = "第一阶段的所有关键入口都会从这里进入。",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onOpenOnboarding) { Text("查看引导") }
            OutlinedButton(onClick = onOpenPermission) { Text("Usage Access 权限") }
            OutlinedButton(onClick = onOpenDevices) { Text("设备与同步") }
            OutlinedButton(onClick = onOpenLlm) { Text("LLM 配置") }
            OutlinedButton(onClick = onOpenPrivacy) { Text("隐私设置") }
            OutlinedButton(onClick = onOpenData) { Text("数据管理") }
        }
    }
}
