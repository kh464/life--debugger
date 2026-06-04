package com.lifedbg.mobile.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    ScreenScaffold(
        title = "Life Debugger",
        subtitle = "本地优先的手机使用复盘工具，先生成摘要，再由你确认是否发送给云端 LLM。",
    ) {
        InfoCard(
            title = "隐私边界",
            body = "不读取聊天内容，不截图，不录屏，不使用无障碍权限。第一阶段只读取 Android UsageEvents 并生成本地摘要。",
        )
        InfoCard(
            title = "第一阶段闭环",
            body = "Usage Access 授权 -> 采集 UsageEvents -> 生成 SummaryEvent -> 预览 LLM 输入 -> 展示每日复盘。",
        )
        Button(onClick = onStart) {
            Text("开始使用")
        }
    }
}
