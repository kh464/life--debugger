# 第一阶段验收记录

## 当前状态

第一阶段 Android 单端闭环代码已完成，并通过 Debug 构建验证。

已修复 LLM 配置页交互：点击“测试连接”时，如果输入框中有新的 API Key，会先加密保存配置再测试，避免填写后未保存导致后续复盘读取不到配置。

已生成 APK：

```text
android-app/app/build/outputs/apk/debug/app-debug.apk
```

已通过命令：

```powershell
cd android-app
.\gradlew.bat :app:assembleDebug --no-daemon
```

## 尚未验收

以下事项需要真机或模拟器配合，当前未完成手动验收：

- 安装 APK 并启动 App。
- 在系统设置中开启 Usage Access 权限。
- 点击首页“刷新今日数据”，确认能读取真实 UsageEvents。
- 确认 `raw_usage_events` 与 `summary_events` 产生真实数据。
- 在 Timeline 查看今日手机使用摘要。
- 配置 DeepSeek / Qwen / Custom Provider API Key。
- 测试 LLM 连接。
- 预览即将发送给 LLM 的摘要 JSON。
- 确认发送并生成每日复盘。
- 检查隐私设置变更后 LLM 预览内容是否变化。
- 检查金融类、健康类、关闭 `llm_allowed` 的事件不会进入 LLM Payload。

## 验收注意

- 不应出现 Accessibility Service、截图、录屏、麦克风、摄像头、聊天内容、键盘输入等能力。
- LLM 调用失败时只能展示错误，不能生成规则伪复盘。
- API Key 不能明文写入 Room 或日志。
