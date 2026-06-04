# AGENTS.md

## 项目定位

Life Debugger 是本地优先的个人行为调试器，不是监控软件。任何实现都必须优先保护用户控制权、隐私和可解释性。

## 文档入口

- 设计文档：`docs/design/life-debugger-design.md`
- 阶段计划：`docs/plans/phase-*.md`
- 开始编码前，先阅读当前阶段计划；涉及架构取舍时，再查设计文档。

## 必守边界

- 不使用 Accessibility Service。
- 不采集聊天内容、输入内容、截图、录屏、麦克风、摄像头、密码或文件正文。
- 不把原始 UsageEvents、raw desktop events 或敏感原始数据发送给 LLM、同步端或开发者服务器。
- 同步只允许传输 `SummaryEvent` 摘要；LLM 只允许接收用户可预览、可排除的摘要包。
- API Key 必须本地安全存储，不能明文写入普通数据库、日志或提交内容。

## 实现原则

- 按阶段计划递进实现，不提前混入后续阶段能力。
- MVP 每日复盘必须来自用户配置的云端 OpenAI-compatible LLM；LLM 失败时只提示错误，不生成规则伪复盘。
- Android 与 Desktop 都要保留统一抽象：`SummaryEvent`、`device_id`、`device_type`、`source`、`sync_status`、`LlmProvider`、`LlmAnalysisPackage`。
- UI 不能只是后台壳子；关键页面要完整、清晰、可操作。

## 工作习惯

- 修改前先确认相关文档和现有结构，不凭空重构。
- 保持文件命名清晰、目录职责明确；设计文档放 `docs/design/`，计划文档放 `docs/plans/`。
- 代码变更后尽量运行可用的构建、测试或静态检查；无法运行时说明原因。
