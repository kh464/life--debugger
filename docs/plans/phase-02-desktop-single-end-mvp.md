# Life Debugger 第二阶段编程 Plan：Desktop 单端闭环 MVP

## 0. 阶段目标

第二阶段只实现 Desktop 单端闭环。

目标是让用户只安装桌面端应用，也能完成：

```text
桌面端启动
        ↓
采集当前活动应用 / 窗口标题 / 空闲状态
        ↓
生成 Desktop SummaryEvent 摘要事件
        ↓
保存到本地 SQLite
        ↓
配置云端 LLM
        ↓
预览即将发送给 LLM 的摘要包
        ↓
调用 DeepSeek / Qwen / Custom OpenAI-compatible Provider
        ↓
展示 LLM 生成的每日复盘
```

本阶段不实现：

```text
Android 同步
双设备配对
二维码配对
局域网自动发现
mDNS
手机端数据接收
浏览器插件同步
云端账号
远程数据库
键盘输入记录
屏幕录制
麦克风采集
摄像头采集
文件内容读取
```

第一阶段 Android 已经验证的是：

```text
Android 单端：
UsageEvents -> SummaryEvent -> LLM Preview -> LLM Review
```

第二阶段要验证的是：

```text
Desktop 单端：
Window Activity -> SummaryEvent -> LLM Preview -> LLM Review
```

第二阶段成功标准：

> 用户只安装 Desktop App，填写 LLM API Key，就能看到一份基于电脑使用摘要生成的每日复盘。

------

## 1. 推荐技术栈

Desktop：

```text
Tauri
React
TypeScript
SQLite
Rust commands
Material-like UI 或 shadcn 风格组件
React Router
Zustand 或 Redux Toolkit
TanStack Query 可选
```

后端 / 本地层：

```text
Rust
SQLite
serde
reqwest
keyring / 系统凭据存储
active-window 采集能力
idle time 采集能力
```

LLM：

```text
OpenAI-compatible Chat Completions
DeepSeek Provider
Qwen Provider
Custom Provider
JSON schema validation
```

第一版优先支持：

```text
Windows
```

后续再扩展：

```text
macOS
Linux
```

原因：

```text
用户当前主要使用 Windows 11。
Windows 上更容易先验证活动窗口采集、应用名称识别和空闲状态检测。
```

------

## 2. 第二阶段功能范围

### 2.1 必须实现

```text
1. Desktop App 项目骨架
2. 本地 SQLite 数据库
3. 当前活动窗口采集
4. 应用名称采集
5. 窗口标题采集
6. 用户空闲状态采集
7. RawDesktopEvent 本地保存
8. Desktop UsageSession 合并
9. Desktop SummaryEvent 生成
10. 今日时间线展示
11. 今日 Dashboard 统计
12. LLM Provider 配置页面
13. API Key 安全保存
14. LLM 连接测试
15. LLM 输入预览页面
16. LLM 分析调用
17. LLM JSON 输出校验
18. 每日复盘展示页面
19. 隐私设置页面
20. 数据管理页面
```

### 2.2 可以后置实现

```text
1. 浏览器扩展
2. URL 精确采集
3. 文件路径识别
4. IDE 项目结构识别
5. Git 仓库识别
6. 截图 OCR
7. 本地向量搜索
8. Ollama 本地模型
9. 手机端同步
```

### 2.3 明确不实现

```text
1. 不读取键盘输入内容
2. 不记录用户输入了什么
3. 不读取文件内容
4. 不录屏
5. 不截图
6. 不采集麦克风
7. 不采集摄像头
8. 不上传数据到开发者服务器
9. 不实现账号登录
10. 不用规则伪造每日复盘
```

------

## 3. Desktop MVP 产品流程

用户首次使用流程：

```text
1. 用户打开 Life Debugger Desktop。
2. 应用展示隐私说明。
3. 用户进入首页。
4. 应用开始本地采集活动窗口。
5. 用户工作一段时间。
6. 用户点击“刷新今日数据”。
7. 系统生成桌面使用摘要。
8. 用户进入 LLM 设置。
9. 用户填写 DeepSeek / Qwen / Custom API Key。
10. 用户测试连接。
11. 用户预览即将发送给 LLM 的摘要包。
12. 用户确认发送。
13. 应用调用云端 LLM。
14. Review 页面展示每日复盘。
```

------

## 4. 页面结构

Desktop App 需要包含以下页面：

```text
OnboardingPage
DashboardPage
TimelinePage
ReviewPage
LlmSettingsPage
LlmPayloadPreviewPage
PrivacyPage
IntentPage
DataManagementPage
CollectorStatusPage
```

建议左侧导航栏：

```text
首页 Dashboard
时间线 Timeline
复盘 Review
手动意图 Intent
采集状态 Collector
设置 Settings
```

Settings 内包含：

```text
LLM 配置
隐私设置
数据管理
关于应用
```

------

## 5. 推荐项目结构

```text
desktop-app/
  src/
    main.tsx
    App.tsx

    routes/
      AppRouter.tsx

    components/
      layout/
        AppShell.tsx
        Sidebar.tsx
        TopBar.tsx
      common/
        Card.tsx
        Button.tsx
        EmptyState.tsx
        LoadingState.tsx
        ErrorState.tsx
        JsonPreview.tsx
        StatCard.tsx

    pages/
      onboarding/
        OnboardingPage.tsx
      dashboard/
        DashboardPage.tsx
        DashboardViewModel.ts
      timeline/
        TimelinePage.tsx
        TimelineViewModel.ts
      review/
        ReviewPage.tsx
        ReviewViewModel.ts
      llm/
        LlmSettingsPage.tsx
        LlmPayloadPreviewPage.tsx
        LlmSettingsViewModel.ts
      privacy/
        PrivacyPage.tsx
        PrivacyViewModel.ts
      intent/
        IntentPage.tsx
        IntentViewModel.ts
      collector/
        CollectorStatusPage.tsx
      data/
        DataManagementPage.tsx

    api/
      tauriCommands.ts

    types/
      desktop.ts
      summary.ts
      llm.ts
      review.ts
      privacy.ts

    store/
      appStore.ts
      settingsStore.ts

  src-tauri/
    src/
      main.rs

      core/
        time.rs
        device.rs
        result.rs
        hash.rs

      db/
        mod.rs
        migrations.rs
        raw_desktop_event_repository.rs
        summary_event_repository.rs
        llm_config_repository.rs
        llm_review_repository.rs
        manual_intent_repository.rs
        privacy_repository.rs

      collector/
        mod.rs
        active_window.rs
        idle_detector.rs
        desktop_event_collector.rs
        collector_scheduler.rs

      summarize/
        mod.rs
        desktop_session.rs
        desktop_session_merger.rs
        desktop_app_classifier.rs
        desktop_summary_generator.rs

      llm/
        mod.rs
        llm_provider.rs
        openai_compatible_client.rs
        llm_prompt_builder.rs
        llm_review_engine.rs
        llm_output_parser.rs
        api_key_store.rs

      privacy/
        mod.rs
        privacy_settings.rs
        llm_payload_preview_builder.rs
        exclusion_rules.rs

      commands/
        dashboard_commands.rs
        timeline_commands.rs
        review_commands.rs
        llm_commands.rs
        privacy_commands.rs
        collector_commands.rs
        data_commands.rs
```

------

## 6. 数据库设计

第二阶段使用 Desktop 本地 SQLite。

### 6.1 raw_desktop_events

用于保存桌面端原始活动事件。

```sql
CREATE TABLE raw_desktop_events (
  id TEXT PRIMARY KEY,
  app_name TEXT NOT NULL,
  window_title TEXT,
  process_name TEXT,
  start_time INTEGER NOT NULL,
  end_time INTEGER,
  duration_seconds INTEGER,
  idle_seconds INTEGER DEFAULT 0,
  is_idle INTEGER DEFAULT 0,
  processed INTEGER DEFAULT 0,
  created_at INTEGER NOT NULL
);
```

说明：

```text
app_name：应用名称，例如 VS Code、Chrome、Word。
window_title：当前活动窗口标题。
process_name：进程名，例如 Code.exe、chrome.exe。
start_time：事件开始时间，Unix 毫秒。
end_time：事件结束时间。
duration_seconds：持续时间。
idle_seconds：事件期间空闲秒数。
is_idle：是否为空闲事件。
processed：是否已转为 SummaryEvent。
```

注意：

```text
raw_desktop_events 只保存在本地。
不会上传给开发者服务器。
不会直接发送给 LLM。
```

------

### 6.2 summary_events

统一摘要事件表。

```sql
CREATE TABLE summary_events (
  event_id TEXT PRIMARY KEY,
  device_id TEXT NOT NULL,
  device_type TEXT NOT NULL,
  source TEXT NOT NULL,
  start_time INTEGER NOT NULL,
  end_time INTEGER,
  duration_seconds INTEGER,
  category TEXT,
  activity_type TEXT,
  app_label TEXT,
  window_title TEXT,
  process_name TEXT,
  summary TEXT NOT NULL,
  confidence REAL DEFAULT 0,
  privacy_level TEXT DEFAULT 'metadata',
  app_label_mode TEXT DEFAULT 'real',
  llm_allowed INTEGER DEFAULT 1,
  user_corrected INTEGER DEFAULT 0,
  sync_status TEXT DEFAULT 'local',
  metadata_json TEXT,
  created_at INTEGER NOT NULL
);
```

第二阶段策略：

```text
本地 summary_events 默认保存真实 app_label、window_title、process_name。
是否将 app_label、window_title、process_name 发送给 LLM，由隐私设置控制。
默认允许 app_label 进入 LLM。
默认允许泛化 window_title 进入 LLM。
默认不允许 process_name 进入 LLM。
```

------

### 6.3 desktop_app_category_rules

保存桌面应用分类规则。

```sql
CREATE TABLE desktop_app_category_rules (
  rule_id TEXT PRIMARY KEY,
  app_name TEXT,
  process_name TEXT,
  title_keyword TEXT,
  category TEXT NOT NULL,
  activity_type TEXT NOT NULL,
  is_sensitive INTEGER DEFAULT 0,
  user_override INTEGER DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
```

分类规则可以按三层匹配：

```text
1. process_name 精确匹配
2. app_name 精确匹配
3. window_title 关键词匹配
```

------

### 6.4 llm_configs

保存 LLM 配置，不保存明文 API Key。

```sql
CREATE TABLE llm_configs (
  provider_id TEXT PRIMARY KEY,
  provider_name TEXT NOT NULL,
  base_url TEXT NOT NULL,
  model TEXT NOT NULL,
  api_key_ref TEXT NOT NULL,
  temperature REAL DEFAULT 0.3,
  max_tokens INTEGER DEFAULT 2048,
  enabled INTEGER DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
```

------

### 6.5 llm_reviews

保存 LLM 每日复盘结果。

```sql
CREATE TABLE llm_reviews (
  review_id TEXT PRIMARY KEY,
  date TEXT NOT NULL,
  primary_device_id TEXT NOT NULL,
  provider_id TEXT NOT NULL,
  model TEXT NOT NULL,
  input_package_hash TEXT NOT NULL,
  output_json TEXT NOT NULL,
  user_feedback_json TEXT,
  created_at INTEGER NOT NULL
);
```

------

### 6.6 manual_intent_events

保存用户手动意图记录。

```sql
CREATE TABLE manual_intent_events (
  event_id TEXT PRIMARY KEY,
  device_id TEXT NOT NULL,
  device_type TEXT NOT NULL,
  start_time INTEGER NOT NULL,
  end_time INTEGER,
  category TEXT NOT NULL,
  activity_type TEXT NOT NULL,
  summary TEXT NOT NULL,
  llm_allowed INTEGER DEFAULT 1,
  created_at INTEGER NOT NULL
);
```

------

### 6.7 privacy_settings

保存隐私设置。

```sql
CREATE TABLE privacy_settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
```

------

## 7. 核心数据模型

### 7.1 RawDesktopEvent

```typescript
export type RawDesktopEvent = {
  id: string
  appName: string
  windowTitle?: string
  processName?: string
  startTime: number
  endTime?: number
  durationSeconds?: number
  idleSeconds: number
  isIdle: boolean
  processed: boolean
  createdAt: number
}
```

Rust 对应结构：

```rust
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RawDesktopEvent {
    pub id: String,
    pub app_name: String,
    pub window_title: Option<String>,
    pub process_name: Option<String>,
    pub start_time: i64,
    pub end_time: Option<i64>,
    pub duration_seconds: Option<i64>,
    pub idle_seconds: i64,
    pub is_idle: bool,
    pub processed: bool,
    pub created_at: i64,
}
```

------

### 7.2 DesktopUsageSession

```rust
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DesktopUsageSession {
    pub app_name: String,
    pub window_title: Option<String>,
    pub process_name: Option<String>,
    pub start_time: i64,
    pub end_time: i64,
    pub duration_seconds: i64,
    pub idle_seconds: i64,
    pub is_idle: bool,
}
```

------

### 7.3 SummaryEvent

```typescript
export type SummaryEvent = {
  eventId: string
  deviceId: string
  deviceType: 'desktop'
  source: 'desktop_window_tracker' | 'manual_intent'
  startTime: number
  endTime?: number
  durationSeconds?: number
  category?: string
  activityType?: string
  appLabel?: string
  windowTitle?: string
  processName?: string
  summary: string
  confidence: number
  privacyLevel: 'metadata' | 'summary_only' | 'user_input' | 'sensitive_masked'
  appLabelMode: 'real' | 'generic' | 'hidden'
  llmAllowed: boolean
  userCorrected: boolean
  syncStatus: 'local'
  metadataJson?: string
  createdAt: number
}
```

------

### 7.4 LlmAnalysisPackage

```typescript
export type LlmAnalysisPackage = {
  packageId: string
  date: string
  timezone: string
  device: {
    deviceId: string
    deviceType: 'desktop'
    role: 'primary_analysis'
  }
  userGoal?: string
  events: LlmEventItem[]
  privacyNote: string
}
export type LlmEventItem = {
  start: string
  end?: string
  category?: string
  activityType?: string
  appLabel?: string
  windowTitle?: string
  summary: string
}
```

------

## 8. Desktop 采集模块

### 8.1 ActiveWindowProvider

职责：

```text
获取当前活动窗口信息。
```

接口：

```rust
pub trait ActiveWindowProvider {
    fn get_active_window(&self) -> Result<ActiveWindowInfo, String>;
}
```

结构：

```rust
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ActiveWindowInfo {
    pub app_name: String,
    pub window_title: Option<String>,
    pub process_name: Option<String>,
}
```

Windows 第一版实现：

```text
获取前台窗口句柄。
获取窗口标题。
获取进程 ID。
通过进程 ID 获取 process_name。
尽可能推断 app_name。
```

无法获取时：

```text
app_name = "Unknown App"
window_title = null
process_name = null
```

------

### 8.2 IdleDetector

职责：

```text
检测用户是否处于空闲状态。
```

接口：

```rust
pub trait IdleDetector {
    fn get_idle_seconds(&self) -> Result<i64, String>;
    fn is_idle(&self, threshold_seconds: i64) -> Result<bool, String>;
}
```

默认规则：

```text
idle_seconds >= 180 秒，视为 idle。
```

MVP 设置：

```text
默认空闲阈值：3 分钟。
用户可在隐私/采集设置中修改为 1 / 3 / 5 / 10 分钟。
```

------

### 8.3 DesktopEventCollector

职责：

```text
周期性采样当前活动窗口，并生成 RawDesktopEvent。
```

采样间隔：

```text
默认每 5 秒采样一次。
```

采样逻辑：

```text
1. 获取当前 active window。
2. 获取 idle_seconds。
3. 判断 is_idle。
4. 与上一条采样比较。
5. 如果 app_name + window_title + process_name 未变化，则延长当前事件。
6. 如果发生变化，则关闭上一事件，开启新事件。
7. 如果进入 idle 状态，则生成 idle 事件或标记当前事件 idle。
8. 保存 raw_desktop_events。
```

------

### 8.4 事件去噪规则

```text
1. 持续时间小于 10 秒的窗口切换，默认不生成 SummaryEvent。
2. 系统弹窗、小工具窗口、任务栏窗口默认过滤或分类为 system。
3. 连续相同 App、相同窗口标题，间隔小于 30 秒，合并。
4. 用户 idle 超过 3 分钟后，后续时间不计入主动使用时长。
5. 异常长事件超过 6 小时，切割或降低 confidence。
```

------

## 9. Desktop 使用段合并模块

### 9.1 DesktopSessionMerger

职责：

```text
将 raw_desktop_events 合并成 DesktopUsageSession。
```

接口：

```rust
pub struct DesktopSessionMerger;

impl DesktopSessionMerger {
    pub fn merge(events: Vec<RawDesktopEvent>) -> Vec<DesktopUsageSession>;
}
```

合并规则：

```text
1. 按 start_time 升序排序。
2. 丢弃小于 10 秒的事件。
3. 同一 app_name + process_name + window_title，间隔小于 30 秒，合并。
4. 同一 app_name，但 window_title 不同，间隔小于 15 秒，可合并为同应用使用段。
5. idle 事件单独保留，用于 LLM 分析“离开电脑”。
6. 跨天事件按当天 00:00 到 23:59 切割。
```

------

## 10. Desktop App 分类模块

### 10.1 DesktopAppClassifier

职责：

```text
将 app_name / process_name / window_title 映射为 category 和 activity_type。
```

接口：

```rust
pub struct DesktopAppClassifier;

impl DesktopAppClassifier {
    pub fn classify(&self, session: &DesktopUsageSession) -> DesktopAppClassification;
}
```

分类结果：

```rust
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DesktopAppClassification {
    pub category: String,
    pub activity_type: String,
    pub is_sensitive: bool,
    pub confidence: f64,
}
```

------

### 10.2 支持 category

```text
coding
writing
browser
meeting
chat
email
design
terminal
file_management
reading
study
entertainment
music
game
shopping
finance
health
system
unknown
```

### 10.3 支持 activity_type

```text
productive
communication
entertainment
consumption
life_service
system
unknown
```

------

### 10.4 默认分类规则

示例：

```text
VS Code / Cursor / IntelliJ / PyCharm / WebStorm -> coding / productive
Terminal / PowerShell / CMD / Windows Terminal -> terminal / productive
Chrome / Edge / Firefox -> browser / consumption
Word / WPS / Typora / Obsidian / Notion -> writing 或 study / productive
Slack / Teams / Discord / WeChat / Telegram / LINE -> chat / communication
Outlook / Thunderbird / Gmail PWA -> email / communication
Figma / Photoshop / Illustrator -> design / productive
Spotify / Music -> music / entertainment
Steam / Epic Games -> game / entertainment
Windows Explorer -> file_management / system
Settings / Control Panel -> system / system
Bank / Pay / Finance title keywords -> finance / life_service
```

注意：

```text
规则不需要完美。
必须允许用户后续修改分类。
```

------

## 11. SummaryEvent 生成模块

### 11.1 DesktopSummaryGenerator

职责：

```text
将 DesktopUsageSession 转换为 SummaryEvent。
```

接口：

```rust
pub struct DesktopSummaryGenerator;

impl DesktopSummaryGenerator {
    pub fn generate(&self, sessions: Vec<DesktopUsageSession>) -> Vec<SummaryEvent>;
}
```

------

### 11.2 摘要模板

按 App：

```text
使用 {app_name} 约 {duration}。
```

按活动：

```text
在桌面端进行编码约 42 分钟。
在桌面端进行文档写作约 26 分钟。
在桌面端浏览网页约 18 分钟。
在桌面端聊天沟通约 13 分钟。
离开电脑或空闲约 35 分钟。
```

带窗口标题：

```text
在 {app_name} 中处理“{window_title}”约 {duration}。
```

隐私模式下：

```text
在桌面端使用编码工具约 42 分钟。
在桌面端使用浏览器约 18 分钟。
在桌面端使用聊天工具约 13 分钟。
```

------

### 11.3 confidence 计算

```text
完整窗口信息：0.9
只有 app_name：0.75
unknown app：0.5
idle 状态明确：0.85
异常长事件：0.4
敏感事件被泛化：0.7
```

------

### 11.4 llm_allowed 默认值

```text
coding：true
writing：true
browser：true
meeting：true
chat：true
email：true
design：true
terminal：true
file_management：true
reading：true
study：true
entertainment：true
music：true
game：true
shopping：true
finance：false
health：false
system：true，但 summary 泛化
unknown：true
```

------

## 12. 隐私设置模块

### 12.1 PrivacySettings

```typescript
export type PrivacySettings = {
  allowAppNameInLlm: boolean
  allowWindowTitleInLlm: boolean
  allowProcessNameInLlm: boolean
  allowFinanceEventsInLlm: boolean
  allowHealthEventsInLlm: boolean
  allowUnknownEventsInLlm: boolean
  confirmBeforeLlmAnalysis: boolean
  idleThresholdSeconds: number
  collectWindowTitle: boolean
  collectProcessName: boolean
}
```

默认值：

```text
allowAppNameInLlm = true
allowWindowTitleInLlm = false
allowProcessNameInLlm = false
allowFinanceEventsInLlm = false
allowHealthEventsInLlm = false
allowUnknownEventsInLlm = true
confirmBeforeLlmAnalysis = true
idleThresholdSeconds = 180
collectWindowTitle = true
collectProcessName = true
```

------

### 12.2 PrivacyPage

设置项：

```text
允许应用名称进入 LLM：默认开启
允许窗口标题进入 LLM：默认关闭
允许进程名进入 LLM：默认关闭
金融类事件进入 LLM：默认关闭
健康类事件进入 LLM：默认关闭
未知应用进入 LLM：默认开启
每次 LLM 分析前确认：默认开启
采集窗口标题：默认开启
采集进程名：默认开启
空闲判定阈值：1 / 3 / 5 / 10 分钟
```

说明文案：

```text
Life Debugger 不会记录键盘输入，不会录屏，不会读取文件内容。
桌面端只记录应用、窗口标题、使用时长和空闲状态，用于生成本地摘要。
当你启用云端 LLM 时，只有你确认后的摘要数据会被发送给你配置的模型服务商。
```

------

## 13. LLM 模块

第二阶段复用第一阶段的 LLM 抽象。

### 13.1 Provider

支持：

```text
DeepSeek
Qwen
Custom OpenAI-compatible
```

DeepSeek 默认配置：

```json
{
  "provider_id": "deepseek",
  "provider_name": "DeepSeek",
  "base_url": "https://api.deepseek.com",
  "model": "deepseek-chat",
  "temperature": 0.3,
  "max_tokens": 2048
}
```

Qwen 默认配置：

```json
{
  "provider_id": "qwen",
  "provider_name": "Qwen",
  "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
  "model": "qwen-plus",
  "temperature": 0.3,
  "max_tokens": 2048
}
```

------

### 13.2 API Key 存储

必须使用系统安全存储。

要求：

```text
不要将 API Key 明文写入 SQLite。
不要将 API Key 写入普通 JSON 配置。
不要将 API Key 打印到日志。
不要将 API Key 放入错误信息。
```

Rust 接口：

```rust
pub trait ApiKeyStore {
    fn save_api_key(&self, provider_id: &str, api_key: &str) -> Result<(), String>;
    fn get_api_key(&self, provider_id: &str) -> Result<Option<String>, String>;
    fn delete_api_key(&self, provider_id: &str) -> Result<(), String>;
}
```

------

### 13.3 OpenAICompatibleClient

接口：

```rust
pub struct OpenAICompatibleClient;

impl OpenAICompatibleClient {
    pub async fn test_connection(config: LlmProviderConfig) -> Result<(), String>;

    pub async fn generate_review(
        config: LlmProviderConfig,
        system_prompt: String,
        user_prompt: String,
    ) -> Result<String, String>;
}
```

请求结构：

```json
{
  "model": "deepseek-chat",
  "messages": [
    {
      "role": "system",
      "content": "..."
    },
    {
      "role": "user",
      "content": "..."
    }
  ],
  "temperature": 0.3,
  "max_tokens": 2048,
  "response_format": {
    "type": "json_object"
  }
}
```

兼容策略：

```text
如果 Provider 不支持 response_format，移除 response_format 后重试一次。
如果模型返回非 JSON，不要生成伪复盘，提示解析失败。
```

------

## 14. LLM 输入包构造

### 14.1 LlmPayloadPreviewBuilder

职责：

```text
从当天 summary_events 构造 LLMAnalysisPackage。
```

接口：

```rust
pub struct LlmPayloadPreviewBuilder;

impl LlmPayloadPreviewBuilder {
    pub fn build_for_date(date: String, privacy: PrivacySettings) -> Result<LlmAnalysisPackage, String>;
}
```

------

### 14.2 输入事件过滤规则

```text
只读取当天 SummaryEvent。
只包含 llm_allowed = true 的事件。
按 start_time 升序排列。
不包含 raw_desktop_events。
是否包含 app_name 由 allowAppNameInLlm 控制。
是否包含 window_title 由 allowWindowTitleInLlm 控制。
默认不包含 process_name。
finance / health 默认不进入 LLM。
```

------

### 14.3 LLMAnalysisPackage 示例

```json
{
  "package_id": "uuid",
  "date": "2026-06-01",
  "timezone": "Asia/Tokyo",
  "device": {
    "device_id": "desktop_001",
    "device_type": "desktop",
    "role": "primary_analysis"
  },
  "user_goal": "用户当天没有填写明确目标",
  "events": [
    {
      "start": "09:00",
      "end": "09:42",
      "category": "coding",
      "activity_type": "productive",
      "app_label": "VS Code",
      "summary": "在桌面端进行编码约 42 分钟。"
    },
    {
      "start": "10:10",
      "end": "10:28",
      "category": "browser",
      "activity_type": "consumption",
      "app_label": "Chrome",
      "summary": "在桌面端浏览网页约 18 分钟。"
    },
    {
      "start": "14:03",
      "end": "14:27",
      "category": "chat",
      "activity_type": "communication",
      "app_label": "WeChat",
      "summary": "在桌面端聊天沟通约 24 分钟。"
    }
  ],
  "privacy_note": "All events are local desktop summaries. Raw window events, keystrokes, file contents, screenshots, microphone and camera data are not included."
}
```

------

## 15. LLM Prompt

### 15.1 System Prompt

```text
你是 Life Debugger 的桌面行为复盘分析引擎。

你只能基于用户提供的桌面端摘要事件进行分析。
不要假装知道摘要之外的内容。
不要推断用户输入了什么。
不要推断文件具体内容。
不要给医学诊断。
不要使用羞辱、指责、道德审判式语言。
你的目标是帮助用户理解桌面工作流、注意力流动、任务切换、可能的分心点和可执行的改进建议。

请输出严格 JSON，不要输出 Markdown。
```

------

### 15.2 User Prompt

```text
下面是用户一天的 Desktop 桌面行为摘要。

请分析：
1. 今日行为概览；
2. 主要时间投入；
3. 可能的注意力切换点；
4. 可能的任务漂移；
5. 桌面工作流模式；
6. 积极行为模式；
7. 3 条具体改进建议；
8. 需要用户确认的不确定点。

要求：
- 只基于摘要事件分析；
- 不要推断键盘输入内容；
- 不要推断文件具体内容；
- 不要责备用户；
- 输出 JSON；
- JSON 字段必须符合指定 schema。

摘要数据如下：
{LLM_ANALYSIS_PACKAGE_JSON}
```

------

### 15.3 输出 JSON Schema

```json
{
  "daily_summary": "今天整体桌面行为概览",
  "time_distribution": [
    {
      "category": "coding",
      "duration_minutes": 120,
      "interpretation": "编码投入较多，说明今天有较明显的开发工作段"
    }
  ],
  "attention_shifts": [
    {
      "time_range": "14:03 - 14:27",
      "from": "coding",
      "to": "chat",
      "interpretation": "从编码工具切换到聊天工具，可能是沟通需求，也可能是任务中断",
      "confidence": 0.68
    }
  ],
  "possible_task_drift": [
    {
      "time_range": "15:10 - 15:45",
      "reason": "浏览器和多个非核心应用之间频繁切换，可能说明任务目标不够清晰",
      "confidence": 0.62
    }
  ],
  "positive_patterns": [
    "上午存在连续较长的编码工作段"
  ],
  "suggestions": [
    {
      "title": "给浏览器使用增加意图标记",
      "detail": "当你打开浏览器前，可以记录是查资料、处理任务还是随意浏览，后续复盘会更准确。"
    }
  ],
  "questions_for_user": [
    "14:03 - 14:27 的聊天工具使用是工作沟通，还是被动打断？"
  ],
  "risk_level": "low",
  "tone": "supportive"
}
```

------

## 16. LLM 输出解析

### 16.1 LlmOutputParser

职责：

```text
解析模型返回文本，提取 JSON，转换为 LlmReviewResult。
```

Rust 接口：

```rust
pub struct LlmOutputParser;

impl LlmOutputParser {
    pub fn parse(raw_text: String) -> Result<LlmReviewResult, String>;
}
```

处理情况：

```text
模型直接返回 JSON。
模型返回 ```json 包裹内容。
模型返回 Markdown + JSON。
模型返回无效 JSON。
模型漏字段。
```

策略：

```text
优先提取第一个完整 JSON object。
字段缺失时使用空数组或默认值。
完全无法解析时返回错误，不生成伪复盘。
```

------

## 17. Tauri Commands 设计

前端通过 Tauri commands 调用本地功能。

### 17.1 Collector Commands

```rust
#[tauri::command]
async fn start_collector() -> Result<(), String>;

#[tauri::command]
async fn stop_collector() -> Result<(), String>;

#[tauri::command]
async fn get_collector_status() -> Result<CollectorStatus, String>;

#[tauri::command]
async fn refresh_today_desktop_events() -> Result<RefreshResult, String>;
```

------

### 17.2 Timeline Commands

```rust
#[tauri::command]
async fn get_summary_events_for_date(date: String) -> Result<Vec<SummaryEvent>, String>;

#[tauri::command]
async fn update_summary_event_llm_allowed(event_id: String, allowed: bool) -> Result<(), String>;

#[tauri::command]
async fn delete_summary_event(event_id: String) -> Result<(), String>;

#[tauri::command]
async fn update_summary_event_category(
    event_id: String,
    category: String,
    activity_type: String
) -> Result<(), String>;
```

------

### 17.3 Dashboard Commands

```rust
#[tauri::command]
async fn get_dashboard_stats(date: String) -> Result<DashboardStats, String>;
```

------

### 17.4 LLM Commands

```rust
#[tauri::command]
async fn get_llm_config() -> Result<Option<LlmProviderConfig>, String>;

#[tauri::command]
async fn save_llm_config(config: LlmProviderConfig, api_key: Option<String>) -> Result<(), String>;

#[tauri::command]
async fn test_llm_connection(provider_id: String) -> Result<(), String>;

#[tauri::command]
async fn build_llm_payload_preview(date: String) -> Result<LlmAnalysisPackage, String>;

#[tauri::command]
async fn generate_llm_review(date: String) -> Result<LlmReviewResult, String>;

#[tauri::command]
async fn get_llm_review(date: String) -> Result<Option<LlmReviewResult>, String>;
```

------

### 17.5 Privacy Commands

```rust
#[tauri::command]
async fn get_privacy_settings() -> Result<PrivacySettings, String>;

#[tauri::command]
async fn save_privacy_settings(settings: PrivacySettings) -> Result<(), String>;
```

------

### 17.6 Data Commands

```rust
#[tauri::command]
async fn clear_raw_desktop_events() -> Result<(), String>;

#[tauri::command]
async fn clear_summary_events() -> Result<(), String>;

#[tauri::command]
async fn clear_llm_reviews() -> Result<(), String>;

#[tauri::command]
async fn export_summary_events_json(date: String) -> Result<String, String>;
```

------

## 18. UI 编程 Plan

### 18.1 OnboardingPage

内容：

```text
Life Debugger Desktop 是你的电脑行为复盘工具。

它会记录：
- 当前活动应用
- 窗口标题
- 应用使用时长
- 空闲状态

它不会记录：
- 键盘输入
- 文件内容
- 屏幕截图
- 录屏
- 麦克风
- 摄像头

如果启用云端 LLM，只有你确认后的摘要数据会发送给你配置的模型服务商。
```

按钮：

```text
开始使用
```

------

### 18.2 DashboardPage

展示卡片：

```text
今日桌面使用总时长
今日生产性时间
今日主要类别
今日应用切换次数
最长专注段
最近一次复盘时间
LLM 配置状态
采集器状态
```

按钮：

```text
刷新今日数据
生成今日复盘
预览 LLM 输入
记录当前意图
开始/暂停采集
```

------

### 18.3 TimelinePage

展示：

```text
按时间排序的 SummaryEvent 列表
```

每条事件展示：

```text
时间范围
应用名称
窗口标题，受隐私设置影响
分类
持续时间
摘要
是否允许进入 LLM
```

操作：

```text
切换 llm_allowed
修改分类
删除事件
隐藏窗口标题
```

------

### 18.4 CollectorStatusPage

展示：

```text
当前采集状态：运行中 / 已暂停
当前活动应用
当前窗口标题
当前空闲秒数
今日采集原始事件数量
今日生成摘要数量
最近采样时间
```

按钮：

```text
开始采集
暂停采集
立即采样
刷新今日数据
```

------

### 18.5 LlmSettingsPage

字段：

```text
Provider：DeepSeek / Qwen / Custom
Base URL
Model
API Key
Temperature
Max Tokens
```

按钮：

```text
保存
测试连接
清空配置
```

状态：

```text
未配置
已配置但未测试
连接成功
连接失败
```

------

### 18.6 LlmPayloadPreviewPage

展示：

```text
日期
Provider
模型名
将发送的事件数量
隐私提示
JSON 预览
```

用户可以：

```text
确认发送
返回修改
排除某条事件
隐藏窗口标题
隐藏应用名称
```

------

### 18.7 ReviewPage

状态：

```text
无复盘：提示用户生成
生成中：Loading
生成成功：展示复盘
生成失败：展示错误
```

展示模块：

```text
今日总结
时间分布
注意力切换
可能任务漂移
积极模式
建议
需要确认的问题
```

------

### 18.8 PrivacyPage

设置项：

```text
允许应用名称进入 LLM
允许窗口标题进入 LLM
允许进程名进入 LLM
允许金融类事件进入 LLM
允许健康类事件进入 LLM
允许未知应用进入 LLM
每次 LLM 分析前确认
采集窗口标题
采集进程名
空闲判定阈值
```

------

### 18.9 IntentPage

用户快速记录：

```text
我现在准备：
编码
写作
查资料
会议
沟通
阅读
设计
休息
娱乐
其他
```

保存后生成 manual_intent_events，并可进入 LLM 分析包。

------

### 18.10 DataManagementPage

功能：

```text
清空原始桌面事件
清空摘要事件
清空 LLM 复盘
清空 LLM 配置
导出今日摘要 JSON
导出全部摘要 JSON
```

------

## 19. ViewModel / State 设计

### 19.1 DashboardState

```typescript
export type DashboardState = {
  totalDesktopMinutes: number
  productiveMinutes: number
  summaryEventCount: number
  appSwitchCount: number
  longestFocusSegmentMinutes: number
  topCategories: CategoryStat[]
  latestReviewTime?: number
  llmConfigured: boolean
  collectorRunning: boolean
  isRefreshing: boolean
  errorMessage?: string
}
```

------

### 19.2 TimelineState

```typescript
export type TimelineState = {
  date: string
  events: SummaryEvent[]
  isLoading: boolean
  errorMessage?: string
}
```

------

### 19.3 ReviewState

```typescript
export type ReviewState = {
  date: string
  review?: LlmReviewResult
  isGenerating: boolean
  errorMessage?: string
}
```

------

### 19.4 LlmSettingsState

```typescript
export type LlmSettingsState = {
  providerId: 'deepseek' | 'qwen' | 'custom'
  providerName: string
  baseUrl: string
  model: string
  apiKeyFilled: boolean
  temperature: number
  maxTokens: number
  enabled: boolean
  testing: boolean
  testResult?: 'success' | 'failed'
  errorMessage?: string
}
```

------

## 20. 第二阶段开发顺序

### Step 1：创建 Tauri + React 项目骨架

任务：

```text
1. 创建 Tauri 项目。
2. 接入 React + TypeScript。
3. 创建基础页面。
4. 创建左侧导航。
5. 创建 Tauri command 调用封装。
```

验收：

```text
App 可以启动。
可以在 Dashboard / Timeline / Review / Settings 页面之间切换。
前端可以调用一个测试 Tauri command。
```

------

### Step 2：实现 SQLite 数据库

任务：

```text
1. 创建 SQLite 初始化模块。
2. 创建 migrations。
3. 创建 raw_desktop_events 表。
4. 创建 summary_events 表。
5. 创建 llm_configs 表。
6. 创建 llm_reviews 表。
7. 创建 manual_intent_events 表。
8. 创建 privacy_settings 表。
```

验收：

```text
App 启动后能自动创建数据库。
可以插入和读取测试 SummaryEvent。
Timeline 页面能展示测试数据。
```

------

### Step 3：实现 Active Window 采集

任务：

```text
1. 实现 ActiveWindowProvider。
2. Windows 下获取当前活动窗口标题。
3. 获取 process_name。
4. 获取 app_name。
5. 创建 get_current_active_window command。
6. CollectorStatusPage 展示当前活动窗口。
```

验收：

```text
打开不同应用时，CollectorStatusPage 能显示当前活动应用和窗口标题。
```

------

### Step 4：实现 IdleDetector

任务：

```text
1. 实现 idle time 获取。
2. 实现 is_idle。
3. 默认 idle 阈值 180 秒。
4. CollectorStatusPage 展示当前 idle 秒数。
```

验收：

```text
用户一段时间不操作后，idle 秒数增加。
恢复操作后，idle 秒数归零或明显降低。
```

------

### Step 5：实现 DesktopEventCollector

任务：

```text
1. 每 5 秒采样 active window。
2. 与上一条采样比较。
3. 未变化则延长当前事件。
4. 变化则关闭上一事件，开启新事件。
5. 保存 raw_desktop_events。
6. 支持 start_collector / stop_collector。
```

验收：

```text
启动采集器后，raw_desktop_events 中持续产生桌面事件。
切换应用后，事件能正确分段。
```

------

### Step 6：实现 DesktopSessionMerger

任务：

```text
1. 读取当天 raw_desktop_events。
2. 过滤小于 10 秒的噪音事件。
3. 合并相邻同应用事件。
4. 处理 idle 事件。
5. 输出 DesktopUsageSession。
```

验收：

```text
可以得到类似：
09:00 - 09:42 VS Code
10:10 - 10:28 Chrome
14:03 - 14:27 WeChat
这样的桌面使用段。
```

------

### Step 7：实现 DesktopAppClassifier

任务：

```text
1. 内置默认桌面 App 分类规则。
2. 支持 app_name 匹配。
3. 支持 process_name 匹配。
4. 支持 window_title 关键词匹配。
5. 未知应用标记为 unknown。
```

验收：

```text
VS Code 能识别为 coding。
Chrome 能识别为 browser。
Word / Typora 能识别为 writing。
Teams / WeChat 能识别为 chat。
Steam 能识别为 game。
```

------

### Step 8：生成 SummaryEvent

任务：

```text
1. 实现 DesktopSummaryGenerator。
2. 将 DesktopUsageSession 转为 SummaryEvent。
3. 保存到 summary_events。
4. refresh_today_desktop_events command 串联采集、合并、摘要生成。
```

验收：

```text
点击“刷新今日数据”后，TimelinePage 能展示真实桌面摘要。
```

------

### Step 9：实现 Dashboard 统计

任务：

```text
1. 今日桌面总使用时长。
2. 今日生产性时间。
3. 今日 SummaryEvent 数量。
4. Top categories。
5. 最长连续使用段。
6. 应用切换次数。
7. LLM 配置状态。
8. Collector 状态。
```

验收：

```text
Dashboard 能显示真实今日统计。
```

------

### Step 10：实现隐私设置

任务：

```text
1. 实现 PrivacySettings。
2. 实现 PrivacyRepository。
3. 实现 PrivacyPage。
4. 控制 app_name 是否进入 LLM。
5. 控制 window_title 是否进入 LLM。
6. 控制 process_name 是否进入 LLM。
7. 控制 finance / health 是否进入 LLM。
```

验收：

```text
修改隐私设置后，LLM Payload Preview 内容发生变化。
```

------

### Step 11：实现 LLM 配置

任务：

```text
1. 实现 LlmConfigRepository。
2. 实现 ApiKeyStore。
3. 实现 LlmSettingsPage。
4. 支持 DeepSeek。
5. 支持 Qwen。
6. 支持 Custom Provider。
7. API Key 使用系统安全存储。
```

验收：

```text
用户可以填写 Provider、Base URL、Model、API Key。
API Key 不明文写入 SQLite。
```

------

### Step 12：实现 OpenAI-compatible Client

任务：

```text
1. 实现 OpenAICompatibleClient。
2. 实现 test_connection。
3. 实现 generate_review。
4. 支持超时。
5. 支持 HTTP 错误提示。
6. 支持 response_format 失败后降级重试。
```

验收：

```text
用户点击“测试连接”后，可以得到成功或明确错误提示。
```

------

### Step 13：实现 LLM Payload Preview

任务：

```text
1. 实现 LlmPayloadPreviewBuilder。
2. 从当天 summary_events 构造 LLMAnalysisPackage。
3. 根据隐私设置过滤字段。
4. 实现 LlmPayloadPreviewPage。
```

验收：

```text
用户可以看到即将发送给 LLM 的 JSON。
用户可以确认发送或返回修改。
```

------

### Step 14：实现 LLM Review Engine

任务：

```text
1. 实现 LlmPromptBuilder。
2. 实现 LlmReviewEngine。
3. 调用 OpenAICompatibleClient。
4. 解析 LLM 输出。
5. 保存 llm_reviews。
6. 失败时返回明确错误。
```

验收：

```text
用户点击“生成今日复盘”，可以调用云端 LLM 并保存结果。
如果 LLM 失败，不生成伪复盘。
```

------

### Step 15：实现 ReviewPage

任务：

```text
1. 读取当天 llm_reviews。
2. 展示 daily_summary。
3. 展示 time_distribution。
4. 展示 attention_shifts。
5. 展示 possible_task_drift。
6. 展示 positive_patterns。
7. 展示 suggestions。
8. 展示 questions_for_user。
```

验收：

```text
用户可以看到结构化每日复盘。
```

------

### Step 16：实现手动意图记录

任务：

```text
1. 实现 IntentPage。
2. 用户选择当前意图。
3. 保存 manual_intent_events。
4. LLM Payload 中包含手动意图事件。
```

验收：

```text
用户记录“准备编码”后，LLM 预览包中能看到该意图事件。
```

------

### Step 17：实现数据管理

任务：

```text
1. 清空 raw_desktop_events。
2. 清空 summary_events。
3. 清空 llm_reviews。
4. 清空 LLM 配置。
5. 导出今日摘要 JSON。
```

验收：

```text
用户可以清空或导出本地数据。
```

------

### Step 18：UI 打磨和错误处理

任务：

```text
1. 空状态页面。
2. 加载状态。
3. 错误提示。
4. LLM 未配置提示。
5. 采集器未启动提示。
6. LLM 调用失败提示。
7. JSON 解析失败提示。
8. 隐私说明提示。
```

验收：

```text
普通用户可以顺利完成整个桌面端闭环，不需要查看日志。
```

------

## 21. 测试计划

### 21.1 单元测试

重点测试：

```text
DesktopSessionMerger
DesktopAppClassifier
DesktopSummaryGenerator
LlmPayloadPreviewBuilder
LlmOutputParser
PrivacySettings filtering
```

测试场景：

```text
窗口事件乱序
同应用短间隔切换
短事件过滤
idle 事件
跨天事件
未知应用
敏感窗口标题
LLM 返回 Markdown 包裹 JSON
LLM 返回非法 JSON
```

------

### 21.2 手动测试

测试环境：

```text
Windows 11
```

测试应用：

```text
VS Code
Chrome / Edge
Word / WPS
微信 / Teams
Windows Terminal
File Explorer
Settings
Steam，可选
```

测试流程：

```text
1. 启动 Life Debugger Desktop。
2. 查看 Onboarding。
3. 启动采集器。
4. 切换几个不同应用。
5. 等待采集 raw_desktop_events。
6. 点击刷新今日数据。
7. 查看 Timeline。
8. 配置 DeepSeek 或 Qwen。
9. 测试连接。
10. 预览 LLM 输入。
11. 生成今日复盘。
12. 查看 Review。
13. 修改隐私设置。
14. 再次预览 LLM 输入。
15. 清空数据。
```

------

### 21.3 隐私测试

确认：

```text
raw_desktop_events 不进入 LLM Payload。
process_name 默认不进入 LLM Payload。
window_title 默认不进入 LLM Payload。
API Key 不写入 SQLite 明文字段。
关闭 llm_allowed 后，该事件不进入 LLM Payload。
金融类事件默认不进入 LLM Payload。
健康类事件默认不进入 LLM Payload。
不采集键盘输入。
不截图。
不录屏。
不读取文件内容。
```

------

## 22. 第二阶段验收清单

### 22.1 功能验收

```text
[ ] Desktop App 可以正常启动
[ ] 可以展示 Onboarding
[ ] 可以启动和暂停采集器
[ ] 可以获取当前活动窗口
[ ] 可以获取窗口标题
[ ] 可以获取进程名
[ ] 可以检测 idle 状态
[ ] 可以保存 raw_desktop_events
[ ] 可以合并 DesktopUsageSession
[ ] 可以生成 SummaryEvent
[ ] Timeline 可以展示桌面摘要
[ ] Dashboard 可以展示今日统计
[ ] 可以配置 DeepSeek
[ ] 可以配置 Qwen
[ ] 可以配置 Custom Provider
[ ] API Key 安全保存
[ ] 可以测试 LLM 连接
[ ] 可以预览 LLM 输入
[ ] 可以调用 LLM 生成复盘
[ ] 可以保存 LLM Review
[ ] Review 页面可以展示复盘
[ ] 可以修改隐私设置
[ ] 可以记录手动意图
[ ] 可以清空本地数据
[ ] 可以导出摘要 JSON
```

------

### 22.2 非功能验收

```text
[ ] 不记录键盘输入
[ ] 不读取文件内容
[ ] 不截图
[ ] 不录屏
[ ] 不采集麦克风
[ ] 不采集摄像头
[ ] 不需要账号登录
[ ] 不连接开发者服务器
[ ] LLM 调用只使用用户填写的 Provider
[ ] LLM 分析前用户可预览摘要包
[ ] LLM 调用失败时不生成伪复盘
```

------

## 23. 第二阶段 Codex 执行提示词

可以直接把下面内容给 Codex：

```text
请根据《Life Debugger 第二阶段编程 Plan：Desktop 单端闭环 MVP》实现 Desktop App。

严格要求：

1. 使用 Tauri + React + TypeScript。
2. 使用 SQLite 保存本地数据。
3. 优先支持 Windows 11。
4. 实现当前活动窗口采集。
5. 实现窗口标题采集。
6. 实现进程名采集。
7. 实现 idle 状态检测。
8. 不记录键盘输入。
9. 不读取文件内容。
10. 不截图、不录屏。
11. 第二阶段不实现 Android 同步、不实现设备配对。
12. 必须实现 Desktop 单端闭环：
   活动窗口采集 -> 生成 SummaryEvent -> 配置 LLM -> 预览 LLM 输入 -> 调用 LLM -> 展示每日复盘。
13. LLM 使用 OpenAI-compatible 抽象。
14. 支持 DeepSeek、Qwen、自定义 Provider。
15. API Key 必须安全保存，不允许明文写入 SQLite。
16. LLM 复盘必须来自云端 LLM，不允许用规则伪造复盘。
17. 如果 LLM 调用失败，只展示错误，不生成伪复盘。
18. UI 必须包含：
    Onboarding、Dashboard、Timeline、Review、LLM Settings、LLM Payload Preview、Privacy、Intent、Data Management、Collector Status。
19. 先实现可运行 MVP，再做 UI 美化。
```

------

## 24. 第二阶段最终 Demo

目标 Demo 流程：

```text
1. 用户第一次打开 Life Debugger Desktop。
2. App 说明隐私原则。
3. 用户进入 Dashboard。
4. 用户点击“开始采集”。
5. 用户切换 VS Code、Chrome、Word、微信等应用。
6. 用户点击“刷新今日数据”。
7. Dashboard 显示今日桌面使用总时长、生产性时间、主要类别。
8. Timeline 显示今天的桌面使用摘要。
9. 用户进入 LLM 设置，填写 DeepSeek 或 Qwen API Key。
10. 用户点击“测试连接”，显示连接成功。
11. 用户点击“预览 LLM 输入”，看到将发送的摘要 JSON。
12. 用户确认发送。
13. App 调用云端 LLM。
14. Review 页面展示桌面每日复盘。
```

示例复盘效果：

```text
今天你的电脑使用呈现出几个明显模式：

1. 上午存在较长的编码工作段，主要集中在 VS Code。
2. 下午浏览器和聊天工具切换频率增加，可能说明任务中断变多。
3. 14:03 - 14:27 的聊天工具使用较长，可能是工作沟通，也可能是一次注意力漂移，需要你确认。
4. 晚间娱乐类应用使用增加，说明工作和休息边界比较明显。

建议：
- 在打开浏览器前记录一下意图，区分查资料和随意浏览。
- 将聊天工具集中到固定处理窗口。
- 对长时间编码段保留保护时间，避免被低优先级窗口切换打断。
```

------

## 25. 第二阶段完成后的下一步

第二阶段完成后，再进入第三阶段：

```text
手机和电脑第一次配对认证
```

第三阶段需要复用第二阶段保留的抽象：

```text
SummaryEvent 统一模型
device_id
device_type
source
sync_status
LlmProvider 抽象
LlmAnalysisPackage 抽象
PrivacySettings
LLM Payload Preview
```

因此第二阶段不要把数据结构写死为只能 Desktop 使用，应保持这些字段：

```text
device_id
device_type
source
sync_status
metadata_json
```

这样第三阶段做 Android 与 Desktop 双设备同步时，不需要重构核心数据模型。