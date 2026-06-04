# 第二阶段实现记录

## 当前状态

第二阶段 Desktop 单端闭环已完成到可构建、可运行的 MVP 状态：

```text
Window Activity
  -> RawDesktopEvent 本地 SQLite
  -> Desktop SummaryEvent
  -> LLM Preview
  -> OpenAI-compatible LLM Review
```

已完成：

- Tauri + React + TypeScript 桌面工程。
- Windows 当前活动窗口采集：应用名、窗口标题、进程名。
- Windows idle time 采集：`GetLastInputInfo`。
- 后台采样线程：用户在 Onboarding 进入首页后开启，立即采样一次，之后每 10 秒采样一次。
- 手动刷新今日数据：立即采集一条 raw event 并生成 SummaryEvent。
- SQLite 本地表：`raw_desktop_events`、`summary_events`、`llm_configs`、`llm_reviews`、`manual_intent_events`、`privacy_settings`。
- Dashboard、Timeline、Collector、Intent、Data 页面已接入真实 Tauri commands。
- LLM Provider 配置：Provider、Base URL、Model。
- API Key 使用系统凭据存储，数据库只保存 `api_key_stored` 标记。
- LLM 输入预览只包含用户可预览的 `SummaryEvent` 摘要包，不包含 raw events。
- OpenAI-compatible `/chat/completions` 连接测试与每日复盘生成。
- LLM 输出 JSON 校验：非 JSON 直接报错，不生成规则伪复盘。
- Review 页面读取最近一次真实 LLM 复盘。
- Tauri 图标已补充：`desktop-app/src-tauri/icons/icon.ico`。
- 已修复 LLM 配置页交互：输入新的 API Key 后可直接测试连接或生成复盘，执行前会自动保存到系统凭据存储。
- 已增强 LLM 调用反馈：点击测试/生成会立即显示进行中状态，请求设置 120 秒超时，复盘页支持手动刷新。
- API Key 存储增加 Windows DPAPI fallback：优先写系统凭据存储，若无法稳定读回，则使用 DPAPI 加密保存到本地文件，不明文写入数据库。
- 已修复 Desktop API Key 传参链路：前端改用 JSON 字符串传递 LLM 配置，后端支持 `apiKey` / `api_key` 解析，并增加无密钥单元测试。
- 已增加 API Key 保存链路测试：使用临时 SQLite 与临时 DPAPI 文件验证 `save_llm_config_json` 保存后 `apiKeyStored=true`。
- 已修复 Credential Manager 瞬时验证但后续读不到的问题：API Key 现在会同时写入 DPAPI 加密备份，避免 `apiKeyStored=false`。
- 已增强每日复盘 LLM 请求：生成复盘时优先使用 OpenAI-compatible JSON mode，并提供普通请求 fallback；系统提示要求只返回 JSON 对象。
- 已调整复盘入口：LLM 配置页只负责配置/测试/预览，生成今日复盘移动到 Review 页面；Review 页面改为结构化友好展示，不默认暴露原始 JSON。

## 已通过命令

```powershell
cd desktop-app
npm run build

cd src-tauri
cargo check

cd ..
npx tauri build --no-bundle
npx tauri build --bundles nsis
```

已生成可执行文件：

```text
desktop-app/src-tauri/target/release/life-debugger-desktop.exe
```

已生成 NSIS 安装包：

```text
desktop-app/src-tauri/target/release/bundle/nsis/Life Debugger Desktop_0.1.0_x64-setup.exe
```

## 未验收 / 剩余风险

- 尚未在真实桌面交互中长时间运行验证后台采样稳定性。
- 尚未使用用户自己的 DeepSeek / Qwen / Custom API Key 验证真实 LLM 调用。
- NSIS 安装包已生成；MSI 打包仍依赖 WiX 下载，可后续按需处理。
- SummaryEvent session merge 目前是 MVP 级：按相邻采样估算持续时间，后续可优化为更严格的会话合并。
- 隐私设置页面目前是说明页，后续可继续做成可配置规则。

## 下一步建议

1. 运行 `life-debugger-desktop.exe` 做人工验收：进入首页、刷新今日数据、查看 Timeline。
2. 在 LLM 页面保存 API Key，测试连接，生成每日复盘。
3. 使用 NSIS 安装包做安装/卸载验收；如必须提供 MSI，再处理 WiX 下载。
