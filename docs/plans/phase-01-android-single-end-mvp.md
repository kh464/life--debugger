# Life Debugger 第一阶段编程 Plan：Android 单端闭环 MVP

## 0. 阶段目标

第一阶段只实现 Android 单端闭环。

目标是让用户只安装 Android App，也能完成：

```text
授权使用情况访问
        ↓
采集 App 使用事件
        ↓
生成 SummaryEvent 摘要事件
        ↓
保存到本地数据库
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
桌面端
双设备同步
局域网配对
二维码配对
mDNS 自动发现
iOS
Accessibility Service
后台截屏
后台录屏
通知内容读取
聊天内容读取
键盘输入记录
```

第一阶段成功标准：

> 用户只安装 Android App，授权使用情况访问，填写 LLM API Key，就能看到一份基于手机使用摘要生成的每日复盘。

------

## 1. 技术栈

Android：

```text
Kotlin
Jetpack Compose
Room
DataStore
Android Keystore / EncryptedSharedPreferences
UsageStatsManager
UsageEvents
WorkManager
OkHttp 或 Ktor Client
Kotlinx Serialization
Material 3
```

建议优先使用：

```text
Kotlin + Jetpack Compose + Room + DataStore + OkHttp + Kotlinx Serialization
```

理由：

- Compose 适合快速构建 MVP 前端；
- Room 适合结构化本地事件存储；
- DataStore 适合保存普通配置；
- Keystore / EncryptedSharedPreferences 适合保存 API Key；
- OkHttp 适合实现 OpenAI-compatible HTTP 调用；
- Kotlinx Serialization 适合处理 LLM 请求与输出 JSON。

------

## 2. 第一阶段功能范围

### 2.1 必须实现

```text
1. 首次启动引导页
2. Usage Access 权限检测与跳转
3. 读取 UsageEvents
4. 原始事件本地保存
5. App 使用段合并
6. App 分类
7. SummaryEvent 生成
8. 今日时间线展示
9. 今日统计展示
10. LLM Provider 配置页面
11. API Key 安全保存
12. LLM 连接测试
13. LLM 输入预览页面
14. LLM 分析调用
15. LLM JSON 输出校验
16. 每日复盘展示页面
17. 隐私设置页面
18. 数据管理页面
```

### 2.2 暂不实现

```text
1. 电脑端相关功能
2. 设备配对
3. 局域网同步
4. 云端账号
5. 用户注册登录
6. 远程数据库
7. 后台截图
8. Accessibility Service
9. 通知监听
10. 日历集成
11. 浏览器扩展
```

------

## 3. App 页面结构

第一阶段 Android App 需要包含以下页面：

```text
OnboardingScreen
PermissionScreen
DashboardScreen
TimelineScreen
ReviewScreen
LlmSettingsScreen
LlmPayloadPreviewScreen
PrivacyScreen
IntentScreen
DataManagementScreen
```

推荐底部导航：

```text
首页
时间线
复盘
设置
```

设置页中再进入：

```text
LLM 配置
隐私设置
数据管理
权限状态
```

------

## 4. 推荐项目结构

```text
android-app/
  app/
    src/main/java/com/lifedbg/mobile/
      MainActivity.kt
      LifeDbgApp.kt

      core/
        time/
          TimeUtils.kt
        result/
          AppResult.kt
        json/
          JsonProvider.kt
        device/
          DeviceIdProvider.kt

      db/
        LifeDbgDatabase.kt
        entity/
          RawUsageEventEntity.kt
          SummaryEventEntity.kt
          LlmConfigEntity.kt
          LlmReviewEntity.kt
          AppCategoryRuleEntity.kt
          ManualIntentEntity.kt
        dao/
          RawUsageEventDao.kt
          SummaryEventDao.kt
          LlmConfigDao.kt
          LlmReviewDao.kt
          AppCategoryRuleDao.kt
          ManualIntentDao.kt

      usage/
        UsageAccessChecker.kt
        UsageStatsReader.kt
        UsageEventNormalizer.kt
        UsageRepository.kt
        RawUsageEvent.kt

      summarize/
        UsageSession.kt
        SessionMerger.kt
        AppClassifier.kt
        SensitiveAppDetector.kt
        SummaryGenerator.kt
        SummaryRepository.kt

      llm/
        model/
          LlmProviderConfig.kt
          LlmAnalysisPackage.kt
          LlmReviewResult.kt
        provider/
          LlmProvider.kt
          OpenAICompatibleClient.kt
          DeepSeekProviderPreset.kt
          QwenProviderPreset.kt
          CustomProviderPreset.kt
        prompt/
          LlmPromptBuilder.kt
        review/
          LlmReviewEngine.kt
          LlmOutputParser.kt
          LlmReviewRepository.kt
        secure/
          ApiKeyStore.kt

      privacy/
        PrivacySettings.kt
        PrivacySettingsRepository.kt
        LlmPayloadPreviewBuilder.kt

      intent/
        ManualIntentRepository.kt
        ManualIntentViewModel.kt

      worker/
        UsageCollectWorker.kt
        DailySummaryWorker.kt

      ui/
        navigation/
          AppNavGraph.kt
          BottomNavBar.kt
        onboarding/
          OnboardingScreen.kt
        permission/
          PermissionScreen.kt
        dashboard/
          DashboardScreen.kt
          DashboardViewModel.kt
        timeline/
          TimelineScreen.kt
          TimelineViewModel.kt
        review/
          ReviewScreen.kt
          ReviewViewModel.kt
        llm/
          LlmSettingsScreen.kt
          LlmSettingsViewModel.kt
          LlmPayloadPreviewScreen.kt
        privacy/
          PrivacyScreen.kt
          PrivacyViewModel.kt
        intent/
          IntentScreen.kt
          IntentViewModel.kt
        data/
          DataManagementScreen.kt
          DataManagementViewModel.kt
```

------

## 5. 数据库设计

第一阶段只需要 Android 本地数据库。

### 5.1 raw_usage_events

用于保存从系统读取到的原始使用事件。

```sql
CREATE TABLE raw_usage_events (
  id TEXT PRIMARY KEY,
  package_name TEXT NOT NULL,
  event_type TEXT NOT NULL,
  timestamp INTEGER NOT NULL,
  processed INTEGER DEFAULT 0,
  created_at INTEGER NOT NULL
);
```

说明：

```text
package_name：真实包名，第一阶段默认本地保存。
event_type：内部标准化后的事件类型。
timestamp：事件发生时间，Unix 毫秒。
processed：是否已被摘要生成器处理。
created_at：写入数据库时间。
```

注意：

```text
raw_usage_events 只保存在本地。
不会上传给开发者服务器。
不会直接发送给 LLM。
```

### 5.2 summary_events

用于保存生成后的摘要事件。

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
  package_name TEXT,
  summary TEXT NOT NULL,
  confidence REAL DEFAULT 0,
  privacy_level TEXT DEFAULT 'summary_only',
  app_label_mode TEXT DEFAULT 'real',
  llm_allowed INTEGER DEFAULT 1,
  user_corrected INTEGER DEFAULT 0,
  sync_status TEXT DEFAULT 'local',
  metadata_json TEXT,
  created_at INTEGER NOT NULL
);
```

第一阶段策略：

```text
本地 summary_events 默认保存真实 app_label 和 package_name。
LLM 输入预览页面默认展示真实 App 名称。
是否将 package_name 发送给 LLM 由隐私设置控制。
默认不发送 package_name 给 LLM，只发送 app_label、category、activity_type、summary。
```

### 5.3 app_category_rules

用于保存 App 分类规则。

```sql
CREATE TABLE app_category_rules (
  package_name TEXT PRIMARY KEY,
  app_label TEXT,
  category TEXT NOT NULL,
  activity_type TEXT NOT NULL,
  is_sensitive INTEGER DEFAULT 0,
  user_override INTEGER DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
```

### 5.4 llm_configs

用于保存 LLM 配置信息。API Key 不明文入库，只保存引用。

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

### 5.5 llm_reviews

用于保存 LLM 每日复盘结果。

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

### 5.6 manual_intent_events

用于保存用户手动意图记录。

```sql
CREATE TABLE manual_intent_events (
  event_id TEXT PRIMARY KEY,
  device_id TEXT NOT NULL,
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

## 6. 数据模型

### 6.1 RawUsageEvent

```kotlin
data class RawUsageEvent(
    val id: String,
    val packageName: String,
    val eventType: String,
    val timestamp: Long,
    val createdAt: Long,
)
```

内部标准事件类型：

```text
ACTIVITY_RESUMED
ACTIVITY_PAUSED
ACTIVITY_STOPPED
MOVE_TO_FOREGROUND
MOVE_TO_BACKGROUND
UNKNOWN
```

### 6.2 UsageSession

```kotlin
data class UsageSession(
    val packageName: String,
    val appLabel: String?,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
)
```

### 6.3 SummaryEvent

```kotlin
data class SummaryEvent(
    val eventId: String,
    val deviceId: String,
    val deviceType: String = "android",
    val source: String = "android_usage_stats",
    val startTime: Long,
    val endTime: Long?,
    val durationSeconds: Long?,
    val category: String?,
    val activityType: String?,
    val appLabel: String?,
    val packageName: String?,
    val summary: String,
    val confidence: Double,
    val privacyLevel: String,
    val appLabelMode: String,
    val llmAllowed: Boolean,
    val userCorrected: Boolean,
    val syncStatus: String = "local",
    val metadataJson: String?,
    val createdAt: Long,
)
```

### 6.4 LlmAnalysisPackage

```kotlin
data class LlmAnalysisPackage(
    val packageId: String,
    val date: String,
    val timezone: String,
    val device: LlmDeviceInfo,
    val userGoal: String?,
    val events: List<LlmEventItem>,
    val privacyNote: String,
)
data class LlmDeviceInfo(
    val deviceId: String,
    val deviceType: String,
    val role: String = "primary_analysis",
)
data class LlmEventItem(
    val start: String,
    val end: String?,
    val category: String?,
    val activityType: String?,
    val appLabel: String?,
    val summary: String,
)
```

### 6.5 LlmReviewResult

```kotlin
data class LlmReviewResult(
    val dailySummary: String,
    val timeDistribution: List<TimeDistributionItem>,
    val attentionShifts: List<AttentionShiftItem>,
    val possibleTaskDrift: List<TaskDriftItem>,
    val positivePatterns: List<String>,
    val suggestions: List<SuggestionItem>,
    val questionsForUser: List<String>,
    val riskLevel: String,
    val tone: String,
)
```

------

## 7. Usage Access 权限模块

### 7.1 UsageAccessChecker

职责：

```text
检测用户是否授予 PACKAGE_USAGE_STATS 使用情况访问权限。
```

方法：

```kotlin
class UsageAccessChecker(
    private val context: Context
) {
    fun hasUsageAccess(): Boolean
    fun openUsageAccessSettings()
}
```

### 7.2 PermissionScreen

页面内容：

```text
标题：开启使用情况访问权限

说明：
Life Debugger 需要读取 App 使用时长，用于生成你的本地行为摘要。
它不会读取聊天内容，不会录屏，不会使用无障碍权限。

按钮：
去开启权限
我已开启，重新检测
```

验收标准：

```text
未授权时，首页引导用户进入 PermissionScreen。
授权后，用户可以进入 Dashboard。
```

------

## 8. 使用事件采集模块

### 8.1 UsageStatsReader

职责：

```text
读取指定时间范围内的 UsageEvents。
```

接口：

```kotlin
class UsageStatsReader(
    private val context: Context
) {
    fun readEvents(startTime: Long, endTime: Long): List<RawUsageEvent>
}
```

实现要求：

```text
1. 使用 UsageStatsManager.queryEvents(startTime, endTime)。
2. 遍历 UsageEvents。
3. 提取 packageName、eventType、timestamp。
4. 使用 UsageEventNormalizer 转换事件类型。
5. 忽略 packageName 为空的事件。
6. 返回 RawUsageEvent 列表。
```

### 8.2 UsageEventNormalizer

职责：

```text
兼容不同 Android 版本的事件类型。
```

接口：

```kotlin
object UsageEventNormalizer {
    fun normalize(eventType: Int): String
}
```

标准化结果：

```text
ACTIVITY_RESUMED
ACTIVITY_PAUSED
ACTIVITY_STOPPED
MOVE_TO_FOREGROUND
MOVE_TO_BACKGROUND
UNKNOWN
```

### 8.3 UsageRepository

职责：

```text
保存原始事件，避免重复写入。
```

方法：

```kotlin
class UsageRepository(
    private val dao: RawUsageEventDao
) {
    suspend fun insertEvents(events: List<RawUsageEvent>)
    suspend fun getUnprocessedEvents(startTime: Long, endTime: Long): List<RawUsageEvent>
    suspend fun markProcessed(ids: List<String>)
}
```

去重策略：

```text
id = sha256(packageName + eventType + timestamp)
```

------

## 9. 使用段合并模块

### 9.1 SessionMerger

职责：

```text
将原始 UsageEvents 合并为连续 App 使用段。
```

接口：

```kotlin
class SessionMerger {
    fun merge(events: List<RawUsageEvent>): List<UsageSession>
}
```

基础规则：

```text
1. 按 timestamp 升序排序。
2. 遇到 ACTIVITY_RESUMED 或 MOVE_TO_FOREGROUND，记录当前 App 开始时间。
3. 遇到 ACTIVITY_PAUSED、ACTIVITY_STOPPED 或 MOVE_TO_BACKGROUND，结束当前 App 使用段。
4. 如果缺少结束事件，使用下一个前台事件作为当前段结束时间。
5. 使用段小于 15 秒，默认丢弃。
6. 同一 App 相邻使用间隔小于 60 秒，合并。
7. 最长单段不超过 6 小时，超过则截断或标记异常。
```

### 9.2 边界情况

需要处理：

```text
只有开始事件，没有结束事件。
只有结束事件，没有开始事件。
多个 App 快速切换。
系统 Launcher。
锁屏前后的事件缺失。
跨天事件。
系统时间被用户调整。
```

本阶段简化处理：

```text
跨天事件按当天 00:00 到 23:59 切割。
异常长事件标记 confidence 较低。
系统 Launcher、Settings 可保留，但分类为 system。
```

------

## 10. App 分类模块

### 10.1 AppClassifier

职责：

```text
将 packageName / appLabel 分类为 category 和 activity_type。
```

接口：

```kotlin
class AppClassifier(
    private val context: Context,
    private val ruleRepository: AppCategoryRuleRepository
) {
    suspend fun classify(packageName: String): AppClassification
}
```

分类结果：

```kotlin
data class AppClassification(
    val packageName: String,
    val appLabel: String?,
    val category: String,
    val activityType: String,
    val isSensitive: Boolean,
    val confidence: Double,
)
```

### 10.2 默认分类

支持 category：

```text
work
study
browser
chat
short_video
social_media
music
game
shopping
finance
health
tool
system
navigation
reading
unknown
```

支持 activity_type：

```text
productive
communication
entertainment
consumption
life_service
system
unknown
```

### 10.3 默认分类规则

第一阶段可以内置一个简单规则表。

示例：

```text
Chrome / Firefox / Edge / Samsung Internet -> browser
YouTube / TikTok / 抖音 / 快手 / Bilibili -> short_video 或 entertainment
WeChat / Telegram / LINE / WhatsApp / Messenger -> chat
Gmail / Outlook -> work 或 communication
Notion / Obsidian / Kindle -> study 或 reading
支付宝 / 银行类 -> finance
设置 / 桌面 / 系统 UI -> system
```

注意：

```text
规则不需要完美。
必须允许用户后续在 UI 中修正分类。
```

### 10.4 App 名称读取

通过 PackageManager 获取 appLabel。

如果获取失败：

```text
appLabel = packageName
category = unknown
activityType = unknown
confidence = 0.3
```

------

## 11. 摘要生成模块

### 11.1 SummaryGenerator

职责：

```text
将 UsageSession 转换为 SummaryEvent。
```

接口：

```kotlin
class SummaryGenerator(
    private val deviceIdProvider: DeviceIdProvider,
    private val classifier: AppClassifier
) {
    suspend fun generate(sessions: List<UsageSession>): List<SummaryEvent>
}
```

### 11.2 单 App 摘要模板

模板：

```text
连续使用 {appLabel} 约 {duration}。
```

示例：

```text
连续使用 YouTube 约 24 分钟。
连续使用微信约 13 分钟。
连续使用 Chrome 约 18 分钟。
```

### 11.3 类别摘要模板

如果用户在隐私设置中关闭真实 App 名称：

```text
连续使用短视频类 App 约 24 分钟。
连续使用聊天类 App 约 13 分钟。
连续使用浏览器类 App 约 18 分钟。
```

### 11.4 confidence 计算

简单规则：

```text
有完整 start/end：0.9
缺失 end，用下一个事件推断：0.7
App 分类未知：0.5
时长异常：0.4
```

### 11.5 llm_allowed 默认值

第一阶段默认：

```text
普通 App：llm_allowed = true
金融类 App：llm_allowed = false
健康类 App：llm_allowed = false
密码管理器：llm_allowed = false
系统设置：llm_allowed = true，但 summary 泛化
unknown：llm_allowed = true
```

------

## 12. 后台任务

### 12.1 UsageCollectWorker

职责：

```text
周期性读取最近一段时间的 UsageEvents，并保存原始事件。
```

建议频率：

```text
每 30 分钟执行一次。
用户打开 App 时也立即执行一次。
```

WorkManager 约束：

```text
不需要联网。
不要求充电。
电量低时可以延后。
```

### 12.2 DailySummaryWorker

职责：

```text
将未处理 raw_usage_events 转换为 summary_events。
```

执行时机：

```text
UsageCollectWorker 后执行。
用户进入 Dashboard 时执行。
用户点击“刷新今日数据”时执行。
```

### 12.3 手动刷新

Dashboard 提供：

```text
刷新今日数据
```

点击后：

```text
读取今天 00:00 到当前时间的 UsageEvents
保存 raw_usage_events
生成 summary_events
刷新 UI
```

------

## 13. LLM Provider 模块

### 13.1 LlmProvider 抽象

```kotlin
interface LlmProvider {
    suspend fun testConnection(config: LlmProviderConfig): Result<Unit>
    suspend fun generateReview(
        config: LlmProviderConfig,
        systemPrompt: String,
        userPrompt: String
    ): Result<String>
}
```

### 13.2 OpenAICompatibleClient

职责：

```text
使用 OpenAI-compatible Chat Completions 接口调用 DeepSeek、Qwen、自定义模型。
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

注意：

```text
部分 Provider 对 response_format 支持不完全。
如果失败，移除 response_format 重试一次。
```

### 13.3 Provider 预设

DeepSeek：

```text
provider_id = deepseek
provider_name = DeepSeek
base_url = https://api.deepseek.com
model = deepseek-chat
```

Qwen：

```text
provider_id = qwen
provider_name = Qwen
base_url = https://dashscope.aliyuncs.com/compatible-mode/v1
model = qwen-plus
```

Custom：

```text
provider_id = custom
provider_name = Custom
base_url = 用户输入
model = 用户输入
```

### 13.4 API Key 安全保存

实现 ApiKeyStore：

```kotlin
interface ApiKeyStore {
    suspend fun saveApiKey(providerId: String, apiKey: String)
    suspend fun getApiKey(providerId: String): String?
    suspend fun deleteApiKey(providerId: String)
}
```

要求：

```text
不要把 API Key 明文存入 Room。
不要把 API Key 打印到日志。
不要把 API Key 放入崩溃日志。
```

------

## 14. LLM 输入包构造

### 14.1 LlmPayloadPreviewBuilder

职责：

```text
从 summary_events 构造 LLMAnalysisPackage。
```

接口：

```kotlin
class LlmPayloadPreviewBuilder(
    private val summaryRepository: SummaryRepository,
    private val privacySettingsRepository: PrivacySettingsRepository
) {
    suspend fun buildForDate(date: LocalDate): LlmAnalysisPackage
}
```

### 14.2 输入事件过滤

规则：

```text
只读取当天事件。
只包含 llm_allowed = true。
按 start_time 升序排列。
不包含 raw_usage_events。
不包含完整 packageName，除非用户在隐私设置中开启。
App 名称是否进入 LLM，由隐私设置控制。
```

### 14.3 LLMAnalysisPackage 示例

```json
{
  "package_id": "uuid",
  "date": "2026-06-01",
  "timezone": "Asia/Tokyo",
  "device": {
    "device_id": "android_phone_001",
    "device_type": "android",
    "role": "primary_analysis"
  },
  "user_goal": "用户当天没有填写明确目标",
  "events": [
    {
      "start": "09:00",
      "end": "09:42",
      "category": "browser",
      "activity_type": "consumption",
      "app_label": "Chrome",
      "summary": "连续使用 Chrome 约 42 分钟。"
    },
    {
      "start": "14:03",
      "end": "14:27",
      "category": "short_video",
      "activity_type": "entertainment",
      "app_label": "YouTube",
      "summary": "连续使用 YouTube 约 24 分钟。"
    }
  ],
  "privacy_note": "All events are local summaries. Raw app content, chat content, screenshots and keystrokes are not included."
}
```

------

## 15. LLM Prompt

### 15.1 System Prompt

```text
你是 Life Debugger 的行为复盘分析引擎。

你只能基于用户提供的摘要事件进行分析。
不要假装知道摘要之外的内容。
不要推断用户在 App 内看了什么具体内容。
不要给医学诊断。
不要使用羞辱、指责、道德审判式语言。
你的目标是帮助用户理解注意力流动、任务切换、可能的分心点和可执行的改进建议。

请输出严格 JSON，不要输出 Markdown。
```

### 15.2 User Prompt

```text
下面是用户一天的 Android 手机行为摘要。

请分析：
1. 今日行为概览；
2. 主要时间投入；
3. 可能的注意力切换点；
4. 可能的任务漂移；
5. 手机使用模式；
6. 积极行为模式；
7. 3 条具体改进建议；
8. 需要用户确认的不确定点。

要求：
- 只基于摘要事件分析；
- 不要推断具体 App 内内容；
- 不要责备用户；
- 输出 JSON；
- JSON 字段必须符合指定 schema。

摘要数据如下：
{LLM_ANALYSIS_PACKAGE_JSON}
```

### 15.3 输出 JSON Schema

```json
{
  "daily_summary": "今天整体行为概览",
  "time_distribution": [
    {
      "category": "short_video",
      "duration_minutes": 80,
      "interpretation": "短视频类使用较多，可能是主要娱乐时间来源"
    }
  ],
  "attention_shifts": [
    {
      "time_range": "14:03 - 14:27",
      "from": "unknown or previous activity",
      "to": "short_video",
      "interpretation": "这是一段较长的娱乐型手机使用，可能是一次注意力漂移，也可能是计划休息",
      "confidence": 0.68
    }
  ],
  "possible_task_drift": [
    {
      "time_range": "14:03 - 14:27",
      "reason": "娱乐类 App 连续使用时间较长，且出现在白天工作/学习时段",
      "confidence": 0.6
    }
  ],
  "positive_patterns": [
    "上午存在较长的阅读或工具类 App 使用段"
  ],
  "suggestions": [
    {
      "title": "为长时间娱乐使用设置开始意图",
      "detail": "当你打开娱乐类 App 前，可以先记录是计划休息还是无意识打开，后续复盘会更准确。"
    }
  ],
  "questions_for_user": [
    "14:03 - 14:27 的短视频使用是计划休息，还是无意识切换？"
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

接口：

```kotlin
class LlmOutputParser {
    fun parse(rawText: String): Result<LlmReviewResult>
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

## 17. UI 编程 Plan

### 17.1 OnboardingScreen

内容：

```text
Life Debugger 是你的手机使用复盘工具。
它会读取 App 使用时长，生成本地摘要。
它不会读取聊天内容，不会录屏，不会使用无障碍权限。
如果启用云端 LLM，摘要会发送给你配置的模型服务商。
```

按钮：

```text
开始使用
```

### 17.2 PermissionScreen

状态：

```text
已授权
未授权
```

按钮：

```text
去开启使用情况访问权限
重新检测
```

### 17.3 DashboardScreen

展示卡片：

```text
今日手机使用总时长
今日事件数量
今日主要类别
最近一次复盘
LLM 配置状态
```

按钮：

```text
刷新今日数据
生成今日复盘
记录当前意图
预览 LLM 输入
```

### 17.4 TimelineScreen

展示：

```text
按时间排序的 SummaryEvent 列表
```

每条事件展示：

```text
时间范围
App 名称或类别
持续时间
摘要
是否允许进入 LLM
```

操作：

```text
切换 llm_allowed
修改分类
删除事件
```

### 17.5 LlmSettingsScreen

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

测试连接成功显示：

```text
LLM 连接成功。
```

失败显示：

```text
LLM 连接失败，请检查 API Key、Base URL 或模型名。
```

### 17.6 LlmPayloadPreviewScreen

展示：

```text
即将发送给 LLM 的摘要包
事件数量
日期
Provider
模型名
JSON 预览
```

用户可以：

```text
确认发送
返回修改
排除某条事件
隐藏 App 名称
```

### 17.7 ReviewScreen

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

### 17.8 PrivacyScreen

设置项：

```text
允许 App 名称进入 LLM：默认开启
允许 packageName 进入 LLM：默认关闭
金融类 App 进入 LLM：默认关闭
健康类 App 进入 LLM：默认关闭
未知 App 进入 LLM：默认开启
每次 LLM 分析前确认：默认开启
```

### 17.9 IntentScreen

用户可快速记录：

```text
我现在准备：
工作
学习
写作
编码
查资料
放松
通勤
聊天
购物
休息
其他
```

保存后生成 manual_intent_events。

### 17.10 DataManagementScreen

功能：

```text
清空 raw_usage_events
清空 summary_events
清空 llm_reviews
清空 LLM 配置
导出本地摘要 JSON
```

------

## 18. ViewModel 分工

### 18.1 DashboardViewModel

职责：

```text
检测权限
刷新今日数据
统计今日摘要
触发复盘
跳转预览
```

状态：

```kotlin
data class DashboardUiState(
    val hasUsageAccess: Boolean,
    val totalUsageMinutes: Long,
    val summaryEventCount: Int,
    val topCategories: List<CategoryStat>,
    val llmConfigured: Boolean,
    val latestReviewTime: Long?,
    val isRefreshing: Boolean,
    val errorMessage: String?
)
```

### 18.2 TimelineViewModel

职责：

```text
读取当天 SummaryEvent
修改 llm_allowed
删除事件
修改分类
```

### 18.3 LlmSettingsViewModel

职责：

```text
读取 LLM 配置
保存 LLM 配置
保存 API Key
测试连接
```

### 18.4 ReviewViewModel

职责：

```text
读取当天 LLM Review
构造 LLM 输入包
调用 LlmReviewEngine
保存 Review
展示错误
```

### 18.5 PrivacyViewModel

职责：

```text
读取隐私设置
保存隐私设置
影响 LLM Payload 构造
```

------

## 19. 第一阶段开发顺序

### Step 1：创建 Android 项目骨架

任务：

```text
1. 创建 Kotlin + Compose Android 项目。
2. 配置 Material 3。
3. 配置 Navigation Compose。
4. 创建页面骨架。
5. 创建底部导航。
```

验收：

```text
App 可以启动。
可以在 Dashboard / Timeline / Review / Settings 之间切换。
```

------

### Step 2：实现数据库

任务：

```text
1. 添加 Room。
2. 创建 Entity。
3. 创建 Dao。
4. 创建 LifeDbgDatabase。
5. 写一个本地 Repository 测试插入和查询。
```

验收：

```text
可以插入 SummaryEvent。
可以在 TimelineScreen 中读取并展示测试数据。
```

------

### Step 3：实现 Usage Access 权限

任务：

```text
1. 实现 UsageAccessChecker。
2. 实现 PermissionScreen。
3. 未授权时显示权限引导。
4. 授权后进入 Dashboard。
```

验收：

```text
未授权时无法采集。
点击按钮可以跳转系统 Usage Access 设置页。
授权后 App 能检测到状态变化。
```

------

### Step 4：读取 UsageEvents

任务：

```text
1. 实现 UsageStatsReader。
2. 实现 UsageEventNormalizer。
3. 读取今天 00:00 到当前时间的事件。
4. 保存 raw_usage_events。
5. 在日志或调试页面显示读取数量。
```

验收：

```text
授权后点击“刷新今日数据”，raw_usage_events 表中出现数据。
```

------

### Step 5：合并 UsageSession

任务：

```text
1. 实现 SessionMerger。
2. 从 raw_usage_events 生成 UsageSession。
3. 处理小于 15 秒的噪音事件。
4. 合并同 App 相邻短间隔事件。
```

验收：

```text
可以得到类似：
09:00 - 09:42 Chrome
14:03 - 14:27 YouTube
这样的使用段。
```

------

### Step 6：实现 App 分类

任务：

```text
1. 实现 AppClassifier。
2. 通过 PackageManager 获取 App 名称。
3. 内置默认分类规则。
4. 保存 app_category_rules。
5. 未知 App 标记为 unknown。
```

验收：

```text
常见 App 能被分类为 browser/chat/short_video/social_media/system 等。
```

------

### Step 7：生成 SummaryEvent

任务：

```text
1. 实现 SummaryGenerator。
2. 将 UsageSession 转为 SummaryEvent。
3. 保存到 summary_events。
4. TimelineScreen 展示真实摘要。
```

验收：

```text
TimelineScreen 可以展示今天的手机使用摘要。
```

------

### Step 8：实现 Dashboard 统计

任务：

```text
1. 今日总使用时长。
2. 今日 SummaryEvent 数量。
3. Top categories。
4. 今日最长使用段。
5. LLM 配置状态。
```

验收：

```text
Dashboard 能显示真实今日统计。
```

------

### Step 9：实现隐私设置

任务：

```text
1. 使用 DataStore 保存 PrivacySettings。
2. 实现 PrivacyScreen。
3. 控制 App 名称是否进入 LLM。
4. 控制 packageName 是否进入 LLM。
5. 控制敏感分类是否进入 LLM。
```

验收：

```text
修改隐私设置后，LLM Payload Preview 内容发生变化。
```

------

### Step 10：实现 LLM 配置

任务：

```text
1. 实现 LlmConfigEntity。
2. 实现 LlmConfigRepository。
3. 实现 ApiKeyStore。
4. 实现 LlmSettingsScreen。
5. 支持 DeepSeek / Qwen / Custom。
```

验收：

```text
用户可以填写并保存 Provider、Base URL、Model、API Key。
API Key 不明文写入 Room。
```

------

### Step 11：实现 OpenAI-compatible Client

任务：

```text
1. 实现 OpenAICompatibleClient。
2. 实现 testConnection。
3. 实现 generateReview。
4. 支持超时。
5. 支持错误展示。
```

验收：

```text
用户点击“测试连接”后，可以得到成功或明确错误提示。
```

------

### Step 12：实现 LLM Payload Preview

任务：

```text
1. 实现 LlmPayloadPreviewBuilder。
2. 从当天 summary_events 构造 LLMAnalysisPackage。
3. 根据隐私设置过滤字段。
4. 实现 LlmPayloadPreviewScreen。
```

验收：

```text
用户可以看到即将发送给 LLM 的 JSON。
用户可以确认或返回修改。
```

------

### Step 13：实现 LLM Review Engine

任务：

```text
1. 实现 LlmPromptBuilder。
2. 实现 LlmReviewEngine。
3. 调用 OpenAICompatibleClient。
4. 解析 LLM 输出。
5. 保存 llm_reviews。
```

验收：

```text
用户点击“生成今日复盘”，可以调用云端 LLM 并保存结果。
```

------

### Step 14：实现 ReviewScreen

任务：

```text
1. 读取当天 llm_reviews。
2. 展示 daily_summary。
3. 展示 time_distribution。
4. 展示 attention_shifts。
5. 展示 possible_task_drift。
6. 展示 suggestions。
7. 展示 questions_for_user。
```

验收：

```text
用户可以看到结构化每日复盘。
```

------

### Step 15：实现 WorkManager 自动采集

任务：

```text
1. 实现 UsageCollectWorker。
2. 实现 DailySummaryWorker。
3. App 启动时注册周期任务。
4. 允许用户手动刷新。
```

验收：

```text
用户不手动刷新时，App 也能周期性更新摘要。
```

------

### Step 16：打磨 UI 和错误处理

任务：

```text
1. 空状态页面。
2. 加载状态。
3. 错误提示。
4. 权限未开提示。
5. LLM 未配置提示。
6. LLM 调用失败提示。
7. JSON 解析失败提示。
```

验收：

```text
普通用户可以顺利完成整个流程，不需要看日志。
```

------

## 20. 测试计划

### 20.1 单元测试

测试：

```text
UsageEventNormalizer
SessionMerger
AppClassifier
SummaryGenerator
LlmPayloadPreviewBuilder
LlmOutputParser
```

重点测试：

```text
事件乱序
缺少结束事件
短事件过滤
同 App 合并
隐私设置过滤
LLM 返回 Markdown 包裹 JSON
LLM 返回非法 JSON
```

### 20.2 手动测试

测试设备：

```text
Android 10+
Android 12+
Android 14+
Android 15+
```

测试流程：

```text
1. 新安装 App。
2. 查看 Onboarding。
3. 授权 Usage Access。
4. 刷新今日数据。
5. 查看 Timeline。
6. 配置 DeepSeek。
7. 测试连接。
8. 预览 LLM 输入。
9. 生成今日复盘。
10. 查看 Review。
11. 修改隐私设置。
12. 再次预览 LLM 输入。
```

### 20.3 隐私测试

确认：

```text
raw_usage_events 不进入 LLM Payload。
packageName 默认不进入 LLM Payload。
API Key 不写入 Room 明文字段。
关闭 llm_allowed 后，该事件不进入 LLM Payload。
金融类事件默认不进入 LLM Payload。
```

------

## 21. 第一阶段验收清单

### 21.1 功能验收

```text
[ ] App 可以正常启动
[ ] 可以检测 Usage Access 权限
[ ] 可以跳转权限设置页
[ ] 可以读取 UsageEvents
[ ] 可以保存 raw_usage_events
[ ] 可以生成 SummaryEvent
[ ] 可以展示今日 Timeline
[ ] 可以展示 Dashboard 统计
[ ] 可以配置 DeepSeek
[ ] 可以配置 Qwen
[ ] 可以配置 Custom Provider
[ ] API Key 安全保存
[ ] 可以测试 LLM 连接
[ ] 可以预览 LLM 输入
[ ] 可以调用 LLM 生成复盘
[ ] 可以保存 LLM Review
[ ] 可以展示 Review
[ ] 可以修改隐私设置
[ ] 可以清空本地数据
```

### 21.2 非功能验收

```text
[ ] 不使用 Accessibility Service
[ ] 不录屏
[ ] 不截图
[ ] 不读取聊天内容
[ ] 不记录键盘输入
[ ] 不需要账号登录
[ ] 不连接开发者服务器
[ ] LLM 调用只使用用户填写的 Provider
[ ] LLM 分析前用户可预览摘要包
```

------

## 22. Codex 执行提示词

可以直接把下面内容给 Codex：

```text
请根据《Life Debugger 第一阶段编程 Plan：Android 单端闭环 MVP》实现 Android App。

严格要求：

1. 使用 Kotlin + Jetpack Compose。
2. 使用 Room 保存本地数据。
3. 使用 UsageStatsManager / UsageEvents 读取 App 使用事件。
4. 不使用 Accessibility Service。
5. 不截图、不录屏、不读取聊天内容。
6. 第一阶段不实现桌面端、不实现同步。
7. 必须实现 Android 单端闭环：
   Usage Access 授权 -> 采集 UsageEvents -> 生成 SummaryEvent -> 配置 LLM -> 预览 LLM 输入 -> 调用 LLM -> 展示每日复盘。
8. LLM 使用 OpenAI-compatible 抽象。
9. 支持 DeepSeek、Qwen、自定义 Provider。
10. API Key 必须安全保存，不允许明文写入 Room。
11. LLM 复盘必须来自云端 LLM，不允许用规则伪造复盘。
12. 如果 LLM 调用失败，只展示错误，不生成伪复盘。
13. UI 必须包含：
    Onboarding、Permission、Dashboard、Timeline、Review、LLM Settings、LLM Payload Preview、Privacy、Intent、Data Management。
14. 先实现可运行 MVP，再做 UI 美化。
```

------

## 23. 第一阶段最终 Demo

目标 Demo 流程：

```text
1. 用户第一次打开 Life Debugger。
2. App 说明隐私原则。
3. 用户开启 Usage Access 权限。
4. 用户回到 App，点击“刷新今日数据”。
5. Dashboard 显示今日手机使用总时长、主要类别、事件数量。
6. Timeline 显示今天的 App 使用摘要。
7. 用户进入 LLM 设置，填写 DeepSeek 或 Qwen API Key。
8. 用户点击“测试连接”，显示连接成功。
9. 用户点击“预览 LLM 输入”，看到将发送的摘要 JSON。
10. 用户确认发送。
11. App 调用云端 LLM。
12. Review 页面展示每日复盘。
```

示例复盘效果：

```text
今天你的手机使用呈现出几个明显模式：

1. 娱乐类 App 使用集中在下午和晚上。
2. 聊天类 App 出现多次短时间打开，可能是轻量打断。
3. 浏览器使用时间较长，可能包含查资料或信息消费，需要你进一步确认。
4. 14:03 - 14:27 的短视频使用较长，可能是计划休息，也可能是一次注意力漂移。

建议：
- 在打开娱乐类 App 前记录一下意图。
- 将短视频使用集中到固定休息窗口。
- 对浏览器使用增加手动分类，以区分查资料和随意浏览。
```

------

## 24. 第一阶段完成后的下一步

第一阶段完成后，再进入第二阶段：

```text
Desktop 单端闭环
```

第二阶段不要影响第一阶段代码，应提前保留这些抽象：

```text
SummaryEvent 统一模型
LlmProvider 抽象
LlmAnalysisPackage 抽象
device_id
device_type
source
sync_status
```

这样第三阶段做双设备同步时，不需要重构核心数据模型。