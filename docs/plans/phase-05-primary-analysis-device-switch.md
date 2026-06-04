# Life Debugger 第五阶段编程 Plan：主分析设备切换 + 跨设备 LLM 复盘

## 0. 阶段目标

第五阶段实现 Life Debugger 的核心跨设备价值：

```text
用户可以选择手机或电脑作为主分析设备。
主分析设备汇总所有已同步 SummaryEvent。
主分析设备构造跨设备 LLMAnalysisPackage。
主分析设备调用用户配置的云端 LLM。
主分析设备保存跨设备复盘结果。
非主设备可以查看从主设备同步或回传的复盘摘要。
```

第五阶段完成后，Life Debugger 不再只是：

```text
Android 单端复盘
Desktop 单端复盘
Android -> Desktop 摘要同步
```

而是进入真正的跨设备行为调试：

```text
Android 手机行为摘要
        +
Desktop 电脑行为摘要
        ↓
统一跨设备时间线
        ↓
主分析设备调用 LLM
        ↓
生成统一人生调试复盘
```

本阶段成功标准：

> 用户完成手机和电脑配对，并完成 SummaryEvent 同步后，可以在应用中选择主分析设备。默认电脑端为主分析设备。主分析设备可以基于 Android + Desktop 的混合时间线调用云端 LLM，生成跨设备每日复盘。

------

## 1. 与前四阶段的关系

第一阶段已完成：

```text
Android 单端闭环：
UsageEvents -> SummaryEvent -> LLM Preview -> LLM Review
```

第二阶段已完成：

```text
Desktop 单端闭环：
Window Activity -> SummaryEvent -> LLM Preview -> LLM Review
```

第三阶段已完成：

```text
手机与电脑第一次配对：
DeviceIdentity -> QR Pairing -> paired_devices
```

第四阶段已完成：

```text
Android -> Desktop 局域网同步：
SummaryEvent -> SyncBundle -> Desktop Timeline
```

第五阶段在前四阶段基础上新增：

```text
主分析设备角色
主分析设备选择 UI
跨设备 LLMAnalysisPackage
跨设备 Review 生成
跨设备 Review 存储
跨设备 Review 展示
复盘结果回传
手机作为主分析设备时的 Desktop -> Android 同步
混合时间线分析
LLM 输入预览增强
```

------

## 2. 第五阶段范围

### 2.1 必须实现

```text
1. 主分析设备角色模型
2. 主分析设备选择 UI
3. 双设备默认电脑端为主分析设备
4. 单设备默认本设备为主分析设备
5. 用户可以手动切换主分析设备
6. Android 和 Desktop 都能识别当前主分析设备
7. 主分析设备汇总本机 + 已同步 SummaryEvent
8. 构造跨设备 LLMAnalysisPackage
9. 跨设备 LLM 输入预览
10. 主分析设备调用云端 LLM
11. 保存跨设备 LLM Review
12. Review 页面展示跨设备复盘
13. Desktop Timeline 展示 Android + Desktop 混合事件
14. Android Timeline 展示 Android + Desktop 混合事件，前提是手机为主分析设备或收到回传摘要
15. 复盘结果回传给非主设备
16. 非主设备只展示主设备生成的复盘摘要，不重复调用 LLM
17. 支持 Desktop 为主分析设备
18. 支持 Android 为主分析设备
```

### 2.2 建议实现

```text
1. 主分析设备状态同步
2. 主分析设备冲突检测
3. Review 版本号
4. Review 输入包 hash
5. 跨设备时间线筛选
6. 用户选择是否包含某个设备事件进入 LLM
7. LLM 输入包按设备分组展示
8. 主分析设备变更历史
9. 复盘结果回传同步历史
```

### 2.3 暂不实现

```text
1. 多台手机 + 多台电脑复杂主设备仲裁
2. 云端账号
3. 云端中继同步
4. 多用户协作
5. 实时同步
6. 端到端加密云备份
7. 本地向量搜索
8. 长期趋势报告
9. 周报/月报
10. 自动训练个人模型
```

------

## 3. 核心概念

### 3.1 Primary Analysis Device

主分析设备负责：

```text
汇总所有设备的 SummaryEvent
构造 LLM 输入包
调用云端 LLM
保存 Review
把 Review 摘要回传给非主设备
```

主分析设备可以是：

```text
Android
Desktop
```

默认规则：

```text
只有 Android：Android 是主分析设备。
只有 Desktop：Desktop 是主分析设备。
Android + Desktop：Desktop 默认是主分析设备。
用户可以手动改为 Android。
```

### 3.2 Secondary Device

非主设备负责：

```text
继续采集本设备 SummaryEvent
同步 SummaryEvent 给主分析设备
接收主分析设备回传的 Review 摘要
展示只读复盘
不主动调用 LLM 生成跨设备复盘
```

### 3.3 CrossDeviceReview

跨设备复盘是主分析设备基于多个设备的 SummaryEvent 生成的 Review。

它和单端 Review 的区别：

```text
单端 Review：只分析当前设备事件。
跨设备 Review：分析 Android + Desktop 混合时间线。
```

------

## 4. 第五阶段同步方向

第五阶段需要从第四阶段的单向同步扩展为基于主分析设备的方向同步。

### 4.1 Desktop 为主分析设备

默认模式。

```text
Android -> Desktop 同步 SummaryEvent
Desktop 汇总 Android + Desktop SummaryEvent
Desktop 调用 LLM
Desktop 保存 CrossDeviceReview
Desktop -> Android 回传 Review 摘要
```

### 4.2 Android 为主分析设备

用户手动选择。

```text
Desktop -> Android 同步 SummaryEvent
Android 汇总 Android + Desktop SummaryEvent
Android 调用 LLM
Android 保存 CrossDeviceReview
Android -> Desktop 回传 Review 摘要
```

### 4.3 本阶段必须支持的最小闭环

必须跑通：

```text
Desktop 为主分析设备：
Android SummaryEvent -> Desktop
Desktop 跨设备 LLM Review
Desktop Review 页面展示跨设备复盘
Desktop 回传 Review 摘要到 Android
Android Review 页面展示来自 Desktop 的复盘摘要
```

建议同时实现：

```text
Android 为主分析设备：
Desktop SummaryEvent -> Android
Android 跨设备 LLM Review
Android 回传 Review 摘要到 Desktop
```

如果 Codex 一次实现困难，可以分两轮：

```text
第一轮：Desktop 为主分析设备。
第二轮：Android 为主分析设备。
```

------

## 5. 数据库变更

### 5.1 devices 表增强

Android 和 Desktop 都需要有 devices 表或扩展现有 device_identity / paired_devices。

```sql
CREATE TABLE devices (
  device_id TEXT PRIMARY KEY,
  device_name TEXT NOT NULL,
  device_type TEXT NOT NULL,
  role TEXT NOT NULL,
  is_local INTEGER DEFAULT 0,
  is_primary_analysis_device INTEGER DEFAULT 0,
  paired_at INTEGER,
  last_seen_at INTEGER,
  last_sync_at INTEGER,
  public_key TEXT,
  metadata_json TEXT
);
```

role 可选值：

```text
collector
primary_analysis
secondary
```

注意：

```text
本机设备也要写入 devices 表。
paired_devices 可以继续保留，但 devices 用于统一展示和主设备判断。
```

### 5.2 app_settings 表

保存当前主分析设备配置。

```sql
CREATE TABLE app_settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
```

关键配置：

```text
primary_analysis_device_id
primary_analysis_device_type
review_scope_default
cross_device_review_enabled
review_result_back_sync_enabled
```

默认值：

```text
单 Android：primary_analysis_device_id = local Android device_id
单 Desktop：primary_analysis_device_id = local Desktop device_id
双设备：primary_analysis_device_id = Desktop device_id
review_scope_default = cross_device
cross_device_review_enabled = true
review_result_back_sync_enabled = true
```

### 5.3 summary_events 字段确认

summary_events 必须包含：

```sql
origin_device_id TEXT
origin_device_type TEXT
received_at INTEGER
remote_event_id TEXT
sync_status TEXT
```

如果没有，执行迁移：

```sql
ALTER TABLE summary_events ADD COLUMN origin_device_id TEXT;
ALTER TABLE summary_events ADD COLUMN origin_device_type TEXT;
ALTER TABLE summary_events ADD COLUMN received_at INTEGER;
ALTER TABLE summary_events ADD COLUMN remote_event_id TEXT;
```

写入规则：

```text
本机事件：
origin_device_id = local device_id
origin_device_type = local device_type

远端事件：
origin_device_id = source device_id
origin_device_type = source device_type
remote_event_id = source event_id
```

### 5.4 llm_reviews 增强

用于保存跨设备复盘。

```sql
CREATE TABLE llm_reviews (
  review_id TEXT PRIMARY KEY,
  date TEXT NOT NULL,
  review_type TEXT NOT NULL,
  primary_device_id TEXT NOT NULL,
  primary_device_type TEXT NOT NULL,
  provider_id TEXT NOT NULL,
  model TEXT NOT NULL,
  input_package_hash TEXT NOT NULL,
  output_json TEXT NOT NULL,
  user_feedback_json TEXT,
  created_at INTEGER NOT NULL,
  synced_to_secondary INTEGER DEFAULT 0,
  metadata_json TEXT
);
```

review_type 可选：

```text
single_device
cross_device
imported_from_primary
```

### 5.5 review_sync_bundles

用于复盘结果回传。

```sql
CREATE TABLE review_sync_bundles (
  bundle_id TEXT PRIMARY KEY,
  from_device_id TEXT NOT NULL,
  to_device_id TEXT NOT NULL,
  review_id TEXT NOT NULL,
  date TEXT NOT NULL,
  status TEXT NOT NULL,
  generated_at INTEGER NOT NULL,
  sent_at INTEGER,
  received_at INTEGER,
  error_code TEXT,
  error_message TEXT
);
```

status：

```text
created
sending
acknowledged
failed
cancelled
```

------

## 6. 主分析设备配置模型

### 6.1 PrimaryAnalysisConfig

```json
{
  "primary_device_id": "desktop_001",
  "primary_device_type": "desktop",
  "cross_device_review_enabled": true,
  "review_scope_default": "cross_device",
  "review_result_back_sync_enabled": true,
  "updated_at": 1780306000000
}
```

### 6.2 ReviewScope

```text
single_device
cross_device
selected_devices
```

含义：

```text
single_device：只分析当前设备事件。
cross_device：分析所有可用设备事件。
selected_devices：用户手动选择参与分析的设备。
```

第五阶段默认：

```text
review_scope_default = cross_device
```

------

## 7. 设备角色同步

主分析设备切换后，另一端需要知道当前谁是主设备。

### 7.1 RoleUpdateMessage

```json
{
  "protocol": "lifedbg-role-v1",
  "message_id": "uuid",
  "from_device_id": "desktop_001",
  "to_device_id": "android_phone_001",
  "primary_analysis_device_id": "desktop_001",
  "primary_analysis_device_type": "desktop",
  "updated_at": 1780306000000
}
```

### 7.2 POST /api/role/update

接收端接口：

```http
POST /api/role/update
```

请求：

```json
{
  "protocol": "lifedbg-role-v1",
  "message_id": "uuid",
  "from_device_id": "desktop_001",
  "to_device_id": "android_phone_001",
  "primary_analysis_device_id": "desktop_001",
  "primary_analysis_device_type": "desktop",
  "updated_at": 1780306000000
}
```

响应：

```json
{
  "status": "ok",
  "protocol": "lifedbg-role-v1",
  "message_id": "uuid",
  "received_at": 1780306002000
}
```

### 7.3 冲突处理

如果两端都认为自己是主分析设备：

```text
以 updated_at 最新的配置为准。
如果 updated_at 相同，Desktop 优先。
UI 提示用户确认。
```

MVP 简化：

```text
主分析设备切换只能在当前主设备上操作。
非主设备切换时，提示：请先在主分析设备上修改。
```

建议第五阶段先采用 MVP 简化策略。

------

## 8. 跨设备 LLMAnalysisPackage

### 8.1 CrossDeviceLlmAnalysisPackage

```json
{
  "package_id": "uuid",
  "date": "2026-06-01",
  "timezone": "Asia/Tokyo",
  "review_scope": "cross_device",
  "primary_device": {
    "device_id": "desktop_001",
    "device_type": "desktop",
    "device_name": "Ko-PC",
    "role": "primary_analysis"
  },
  "devices": [
    {
      "device_id": "desktop_001",
      "device_type": "desktop",
      "device_name": "Ko-PC",
      "role": "primary_analysis"
    },
    {
      "device_id": "android_phone_001",
      "device_type": "android",
      "device_name": "Pixel Phone",
      "role": "collector"
    }
  ],
  "user_goal": "用户当天没有填写明确目标",
  "events": [
    {
      "start": "09:00",
      "end": "09:42",
      "device_id": "desktop_001",
      "device_type": "desktop",
      "device_name": "Ko-PC",
      "category": "coding",
      "activity_type": "productive",
      "app_label": "VS Code",
      "summary": "在桌面端进行编码约 42 分钟"
    },
    {
      "start": "09:43",
      "end": "09:55",
      "device_id": "android_phone_001",
      "device_type": "android",
      "device_name": "Pixel Phone",
      "category": "chat",
      "activity_type": "communication",
      "app_label": "微信",
      "summary": "连续使用微信约 12 分钟"
    }
  ],
  "privacy_note": "All events are local summaries. Raw app content, chat content, screenshots, file contents and keystrokes are not included."
}
```

### 8.2 构造规则

主分析设备构造 LLM 输入包时：

```text
1. 读取当天 summary_events。
2. 包含本机事件。
3. 包含已同步远端事件。
4. 只包含 llm_allowed = true 的事件。
5. 按 start_time 升序排序。
6. 根据隐私设置隐藏 App 名称、窗口标题、packageName、processName。
7. 标明每条事件来自哪个设备。
8. 标明 review_scope = cross_device。
9. 不包含 raw_usage_events。
10. 不包含 raw_desktop_events。
```

### 8.3 隐私过滤规则

继续沿用前几阶段规则：

```text
raw events 永不进入 LLM。
聊天内容永不进入 LLM。
键盘输入永不进入 LLM。
截图内容永不进入 LLM。
文件内容永不进入 LLM。
敏感 App 默认不进入 LLM。
金融 / 健康类默认不进入 LLM。
```

额外新增：

```text
用户可以按设备排除事件。
用户可以选择“只分析电脑”或“只分析手机”。
用户可以在 LLM 输入预览页面删除某条事件。
```

------

## 9. 跨设备 LLM Prompt

### 9.1 System Prompt

```text
你是 Life Debugger 的跨设备行为复盘分析引擎。

你只能基于用户提供的跨设备摘要事件进行分析。
不要假装知道摘要之外的内容。
不要推断用户在 App 内看了什么具体内容。
不要推断用户输入了什么。
不要推断文件具体内容。
不要给医学诊断。
不要使用羞辱、指责、道德审判式语言。

你的目标是帮助用户理解手机与电脑之间的注意力流动、任务切换、可能的分心点、积极行为模式和可执行的改进建议。

请输出严格 JSON，不要输出 Markdown。
```

### 9.2 User Prompt

```text
下面是用户一天的跨设备行为摘要，包含 Android 手机与 Desktop 电脑事件。

请分析：
1. 今日跨设备行为概览；
2. 手机与电脑分别投入了哪些时间；
3. 手机与电脑之间的主要切换点；
4. 哪些切换可能是计划休息；
5. 哪些切换可能是任务漂移；
6. 哪些时间段体现了较好的专注模式；
7. 3 条具体改进建议；
8. 需要用户确认的不确定点。

要求：
- 只基于摘要事件分析；
- 不要推断 App 内具体内容；
- 不要推断键盘输入内容；
- 不要责备用户；
- 输出 JSON；
- JSON 字段必须符合指定 schema。

跨设备摘要数据如下：
{CROSS_DEVICE_LLM_ANALYSIS_PACKAGE_JSON}
```

### 9.3 输出 JSON Schema

```json
{
  "daily_summary": "今天整体跨设备行为概览",
  "device_distribution": [
    {
      "device_type": "desktop",
      "device_name": "Ko-PC",
      "duration_minutes": 260,
      "interpretation": "电脑端主要承担工作和生产性任务"
    },
    {
      "device_type": "android",
      "device_name": "Pixel Phone",
      "duration_minutes": 180,
      "interpretation": "手机端主要承担沟通和娱乐使用"
    }
  ],
  "time_distribution": [
    {
      "category": "coding",
      "duration_minutes": 120,
      "interpretation": "编码投入较多"
    },
    {
      "category": "short_video",
      "duration_minutes": 48,
      "interpretation": "短视频使用是主要娱乐时间来源之一"
    }
  ],
  "cross_device_attention_shifts": [
    {
      "time_range": "14:03 - 14:27",
      "from_device": "desktop",
      "from_activity": "writing",
      "to_device": "android",
      "to_activity": "short_video",
      "interpretation": "可能发生了从文档写作到手机娱乐的注意力漂移，也可能是计划休息",
      "confidence": 0.72
    }
  ],
  "possible_task_drift": [
    {
      "time_range": "14:03 - 14:27",
      "reason": "生产性电脑任务后紧接较长手机娱乐使用",
      "confidence": 0.68
    }
  ],
  "possible_planned_breaks": [
    {
      "time_range": "12:30 - 12:50",
      "reason": "接近午间休息时间，手机娱乐使用可能是计划休息",
      "confidence": 0.61
    }
  ],
  "positive_patterns": [
    "上午存在连续较长的电脑端编码工作段",
    "晚间手机娱乐集中在较固定时间段"
  ],
  "suggestions": [
    {
      "title": "给跨设备切换添加意图标记",
      "detail": "当你从电脑切到手机前，可以记录是休息、沟通还是逃避任务，这会显著提升复盘准确性。"
    }
  ],
  "questions_for_user": [
    "14:03 - 14:27 的手机短视频使用是计划休息，还是写作卡住后的切换？"
  ],
  "risk_level": "low",
  "tone": "supportive"
}
```

------

## 10. Review 回传设计

### 10.1 ReviewSummaryBundle

主分析设备生成复盘后，向非主设备回传 Review 摘要。

```json
{
  "protocol": "lifedbg-review-sync-v1",
  "bundle_id": "uuid",
  "review_id": "uuid",
  "from_device_id": "desktop_001",
  "to_device_id": "android_phone_001",
  "date": "2026-06-01",
  "review_type": "cross_device",
  "generated_at": 1780307000000,
  "review_summary": {
    "daily_summary": "今天整体跨设备行为概览...",
    "top_attention_shifts": [
      {
        "time_range": "14:03 - 14:27",
        "summary": "从电脑写作切换到手机短视频"
      }
    ],
    "suggestions": [
      {
        "title": "给跨设备切换添加意图标记",
        "detail": "当你从电脑切到手机前，可以记录切换原因。"
      }
    ],
    "questions_for_user": [
      "14:03 - 14:27 的手机使用是计划休息吗？"
    ]
  },
  "input_package_hash": "sha256_hash"
}
```

注意：

```text
回传给非主设备的是 Review 摘要，不是完整 LLM 输入包。
非主设备不需要重新调用 LLM。
```

### 10.2 POST /api/review/sync

接收端接口：

```http
POST /api/review/sync
```

接收后：

```text
1. 校验 protocol。
2. 校验 from_device_id 是已配对设备。
3. 校验 to_device_id 是本机。
4. 保存为 llm_reviews。
5. review_type = imported_from_primary。
6. 返回确认。
```

响应：

```json
{
  "status": "ok",
  "protocol": "lifedbg-review-sync-v1",
  "bundle_id": "uuid",
  "review_id": "uuid",
  "received_at": 1780307003000
}
```

------

## 11. API 设计

### 11.1 主分析设备配置 API

Android 与 Desktop 都需要实现。

```http
GET /api/role/current
```

响应：

```json
{
  "protocol": "lifedbg-role-v1",
  "local_device_id": "desktop_001",
  "primary_analysis_device_id": "desktop_001",
  "primary_analysis_device_type": "desktop",
  "updated_at": 1780306000000
}
POST /api/role/update
```

请求：

```json
{
  "protocol": "lifedbg-role-v1",
  "message_id": "uuid",
  "from_device_id": "desktop_001",
  "to_device_id": "android_phone_001",
  "primary_analysis_device_id": "desktop_001",
  "primary_analysis_device_type": "desktop",
  "updated_at": 1780306000000
}
```

### 11.2 Review 回传 API

```http
POST /api/review/sync
```

请求：

```json
{
  "protocol": "lifedbg-review-sync-v1",
  "bundle_id": "uuid",
  "review_id": "uuid",
  "from_device_id": "desktop_001",
  "to_device_id": "android_phone_001",
  "date": "2026-06-01",
  "review_type": "cross_device",
  "generated_at": 1780307000000,
  "review_summary": {},
  "input_package_hash": "sha256_hash"
}
```

------

## 12. Android 模块设计

在 Android 项目中新增或完善：

```text
role/
  PrimaryAnalysisConfig.kt
  PrimaryAnalysisRepository.kt
  RoleSyncClient.kt
  RoleSyncServer.kt
  RoleResolver.kt

crossdevice/
  CrossDeviceTimelineRepository.kt
  CrossDeviceLlmPayloadBuilder.kt
  CrossDeviceReviewEngine.kt
  ReviewBackSyncClient.kt
  ReviewBackSyncServer.kt

sync/
  DesktopToAndroidSyncServer.kt
  DesktopToAndroidSyncClient.kt

ui/
  devices/
    PrimaryDeviceSelector.kt
    DeviceRoleCard.kt

  review/
    CrossDeviceReviewScreen.kt
    ReviewSourceBadge.kt

  timeline/
    DeviceFilterBar.kt
    CrossDeviceTimelineScreen.kt
```

### 12.1 PrimaryAnalysisRepository

职责：

```text
保存和读取当前主分析设备配置。
```

接口：

```kotlin
class PrimaryAnalysisRepository {
    suspend fun getConfig(): PrimaryAnalysisConfig
    suspend fun setPrimaryDevice(deviceId: String, deviceType: String)
    suspend fun isLocalDevicePrimary(): Boolean
}
```

### 12.2 CrossDeviceLlmPayloadBuilder

职责：

```text
从 Android 本地 summary_events 中读取本机事件和已同步 Desktop 事件，构造跨设备 LLM 输入包。
```

接口：

```kotlin
class CrossDeviceLlmPayloadBuilder(
    private val summaryRepository: SummaryRepository,
    private val deviceRepository: DeviceRepository,
    private val privacySettingsRepository: PrivacySettingsRepository
) {
    suspend fun buildForDate(
        date: LocalDate,
        scope: ReviewScope
    ): CrossDeviceLlmAnalysisPackage
}
```

### 12.3 ReviewBackSyncServer

职责：

```text
当 Android 是非主设备时，接收 Desktop 回传的 Review 摘要。
```

接口：

```kotlin
class ReviewBackSyncServer {
    suspend fun start()
    suspend fun stop()
}
```

------

## 13. Desktop 模块设计

在 Desktop 项目中新增或完善：

```text
src-tauri/src/role/
  primary_analysis_config.rs
  primary_analysis_repository.rs
  role_sync_client.rs
  role_sync_server.rs
  role_resolver.rs

src-tauri/src/crossdevice/
  cross_device_timeline_repository.rs
  cross_device_llm_payload_builder.rs
  cross_device_review_engine.rs
  review_back_sync_client.rs
  review_back_sync_server.rs

src-tauri/src/commands/
  role_commands.rs
  cross_device_review_commands.rs
  review_sync_commands.rs

src/pages/devices/
  PrimaryDeviceSelector.tsx
  DeviceRoleCard.tsx

src/pages/review/
  CrossDeviceReviewPage.tsx
  ReviewScopeSelector.tsx

src/pages/timeline/
  DeviceFilterBar.tsx
  CrossDeviceTimelinePage.tsx
```

### 13.1 PrimaryAnalysisRepository

职责：

```text
保存当前主分析设备配置。
```

Rust 接口：

```rust
pub struct PrimaryAnalysisRepository;

impl PrimaryAnalysisRepository {
    pub fn get_config(&self) -> Result<PrimaryAnalysisConfig, String>;
    pub fn set_primary_device(&self, device_id: String, device_type: String) -> Result<(), String>;
    pub fn is_local_device_primary(&self) -> Result<bool, String>;
}
```

### 13.2 CrossDeviceLlmPayloadBuilder

职责：

```text
从 Desktop summary_events 中读取 Desktop 本机事件和 Android 同步事件，构造跨设备 LLM 输入包。
```

Rust 接口：

```rust
pub struct CrossDeviceLlmPayloadBuilder;

impl CrossDeviceLlmPayloadBuilder {
    pub fn build_for_date(
        &self,
        date: String,
        scope: ReviewScope
    ) -> Result<CrossDeviceLlmAnalysisPackage, String>;
}
```

### 13.3 CrossDeviceReviewEngine

职责：

```text
调用 LLM 生成跨设备 Review。
```

Rust 接口：

```rust
pub struct CrossDeviceReviewEngine;

impl CrossDeviceReviewEngine {
    pub async fn generate_cross_device_review(
        &self,
        date: String,
        scope: ReviewScope
    ) -> Result<LlmReviewResult, String>;
}
```

### 13.4 ReviewBackSyncClient

职责：

```text
主分析设备生成 Review 后，将 Review 摘要发送给非主设备。
```

Rust 接口：

```rust
pub struct ReviewBackSyncClient;

impl ReviewBackSyncClient {
    pub async fn send_review_summary(
        &self,
        target_device_id: String,
        review_id: String
    ) -> Result<(), String>;
}
```

------

## 14. Tauri Commands 设计

Desktop 新增 commands。

### 14.1 Role Commands

```rust
#[tauri::command]
async fn get_primary_analysis_config() -> Result<PrimaryAnalysisConfig, String>;

#[tauri::command]
async fn set_primary_analysis_device(
    device_id: String,
    device_type: String
) -> Result<(), String>;

#[tauri::command]
async fn get_available_analysis_devices() -> Result<Vec<DeviceInfo>, String>;

#[tauri::command]
async fn sync_primary_analysis_role_to_paired_devices() -> Result<(), String>;
```

### 14.2 Cross-device Review Commands

```rust
#[tauri::command]
async fn build_cross_device_llm_payload_preview(
    date: String,
    scope: String
) -> Result<CrossDeviceLlmAnalysisPackage, String>;

#[tauri::command]
async fn generate_cross_device_review(
    date: String,
    scope: String
) -> Result<LlmReviewResult, String>;

#[tauri::command]
async fn get_cross_device_review(
    date: String
) -> Result<Option<LlmReviewResult>, String>;

#[tauri::command]
async fn send_review_summary_to_secondary_devices(
    review_id: String
) -> Result<(), String>;
```

### 14.3 Cross-device Timeline Commands

```rust
#[tauri::command]
async fn get_cross_device_timeline(
    date: String,
    device_filter: Option<Vec<String>>
) -> Result<Vec<SummaryEvent>, String>;
```

------

## 15. Android API / Repository 方法

Android 新增方法。

### 15.1 Primary Config

```kotlin
suspend fun getPrimaryAnalysisConfig(): PrimaryAnalysisConfig

suspend fun setPrimaryAnalysisDevice(
    deviceId: String,
    deviceType: String
)

suspend fun isLocalDevicePrimary(): Boolean
```

### 15.2 Cross-device Payload

```kotlin
suspend fun buildCrossDeviceLlmPayloadPreview(
    date: LocalDate,
    scope: ReviewScope
): CrossDeviceLlmAnalysisPackage
```

### 15.3 Cross-device Review

```kotlin
suspend fun generateCrossDeviceReview(
    date: LocalDate,
    scope: ReviewScope
): LlmReviewResult

suspend fun getCrossDeviceReview(
    date: LocalDate
): LlmReviewResult?
```

### 15.4 Review Back Sync

```kotlin
suspend fun receiveReviewSummary(bundle: ReviewSummaryBundle): Result<Unit>

suspend fun sendReviewSummaryToSecondaryDevice(
    targetDeviceId: String,
    reviewId: String
): Result<Unit>
```

------

## 16. UI 设计

## 16.1 Devices & Sync 页面增强

Android 和 Desktop 都需要显示：

```text
当前本机角色
当前主分析设备
已配对设备
是否允许本设备成为主分析设备
主分析设备切换按钮
角色同步状态
```

设备卡片：

```text
设备名
设备类型
是否本机
是否主分析设备
最近同步时间
最近连接时间
```

操作：

```text
设为主分析设备
同步角色配置
解除配对
```

提示文案：

```text
主分析设备负责汇总所有摘要事件，并调用你配置的云端 LLM 生成跨设备复盘。
```

默认状态：

```text
双设备时，电脑端默认是主分析设备。
```

------

## 16.2 主分析设备选择 UI

### Desktop

```text
当前主分析设备：Ko-PC

可选设备：
[✓] Ko-PC，Desktop，本机，推荐
[ ] Pixel Phone，Android，已配对
```

切换到 Android 时提示：

```text
切换后，手机将负责汇总跨设备摘要并调用云端 LLM。
电脑端会把本机 SummaryEvent 同步给手机。
请确认手机端已配置 LLM Provider。
```

### Android

```text
当前主分析设备：Ko-PC

可选设备：
[ ] Pixel Phone，Android，本机
[✓] Ko-PC，Desktop，已配对，默认推荐
```

切换到手机时提示：

```text
切换后，手机将负责调用云端 LLM。
请确认手机端已配置 DeepSeek、Qwen 或自定义 Provider。
```

------

## 16.3 CrossDeviceTimeline 页面

功能：

```text
显示手机 + 电脑混合时间线。
支持按设备过滤。
支持按类别过滤。
支持查看事件来源。
```

事件显示：

```text
09:00 - 09:42  Desktop / Ko-PC      编码：在 VS Code 中编码约 42 分钟
09:43 - 09:55  Android / Pixel      聊天：连续使用微信约 12 分钟
14:03 - 14:27  Android / Pixel      短视频：连续使用 YouTube 约 24 分钟
14:35 - 15:10  Desktop / Ko-PC      写作：在 Word 中写作约 35 分钟
```

------

## 16.4 LLM Payload Preview 页面增强

新增：

```text
Review 类型：单设备 / 跨设备
参与设备列表
每个设备事件数量
每个设备总时长
即将发送给 LLM 的完整 JSON
```

用户可以：

```text
排除某个设备
排除某条事件
隐藏 App 名称
隐藏窗口标题
隐藏 packageName
隐藏 processName
确认发送
取消分析
```

------

## 16.5 Review 页面增强

新增 ReviewScopeSelector：

```text
单设备复盘
跨设备复盘
```

跨设备 Review 展示模块：

```text
今日跨设备总结
设备时间分布
活动类别分布
跨设备注意力切换
可能任务漂移
可能计划休息
积极行为模式
建议
需要确认的问题
```

如果当前设备不是主分析设备：

```text
当前设备不是主分析设备。
跨设备复盘由 Ko-PC 生成。
你正在查看主设备同步来的复盘摘要。
```

如果非主设备还没收到复盘：

```text
等待主分析设备生成或同步复盘。
```

------

## 17. LLM 调用规则

第五阶段必须坚持：

```text
跨设备复盘必须由 LLM 生成。
不能用规则分析伪造 Review。
LLM 调用失败时，只展示错误。
```

允许使用确定性逻辑做：

```text
事件排序
事件按设备分组
时间分布统计
LLM 输入包构造
隐私过滤
JSON 校验
Review 保存
Review 回传
```

不允许规则替代：

```text
注意力诊断
任务漂移解释
行为模式解释
改进建议
跨设备切换判断
```

这些必须来自 LLM。

------

## 18. 第五阶段开发顺序

### Step 1：数据库迁移

任务：

```text
1. Android 增加 app_settings 表。
2. Desktop 增加 app_settings 表。
3. Android 增强 llm_reviews。
4. Desktop 增强 llm_reviews。
5. 两端增加 review_sync_bundles。
6. 确认 summary_events 有 origin_device_id / origin_device_type。
```

验收：

```text
数据库迁移不破坏前四阶段数据。
旧 Review 仍可查看。
新 Review 可标记 review_type。
```

------

### Step 2：统一 devices 视图

任务：

```text
1. 两端将本机 device_identity 映射为 devices 记录。
2. 两端将 paired_devices 映射为 devices 记录。
3. DeviceSync 页面显示本机 + 已配对设备。
4. 标记哪个设备是本机。
5. 标记哪个设备是当前主分析设备。
```

验收：

```text
设备页可以看到 Android 和 Desktop 两个设备。
可以区分本机、已配对设备、主分析设备。
```

------

### Step 3：实现 PrimaryAnalysisRepository

任务：

```text
1. Android 实现 PrimaryAnalysisRepository。
2. Desktop 实现 PrimaryAnalysisRepository。
3. 单设备时默认本机为主分析设备。
4. 双设备时默认 Desktop 为主分析设备。
5. 保存 primary_analysis_device_id。
```

验收：

```text
两端都能读取当前主分析设备。
双设备时默认电脑端为主分析设备。
```

------

### Step 4：实现主分析设备选择 UI

任务：

```text
1. DeviceSync 页面加入 PrimaryDeviceSelector。
2. 展示所有可选设备。
3. 用户可以选择 Desktop 或 Android。
4. 切换时保存配置。
5. 切换后 UI 立即更新。
```

验收：

```text
用户可以在设备页切换主分析设备。
当前主设备有明显标记。
```

------

### Step 5：实现角色配置同步

任务：

```text
1. 实现 RoleUpdateMessage。
2. 实现 /api/role/update。
3. 当前主设备切换后，将配置发送给非主设备。
4. 非主设备保存 primary_analysis_device_id。
5. 处理网络失败。
```

验收：

```text
在 Desktop 上切换主分析设备后，Android 能看到变化。
在 Android 上切换后，Desktop 能看到变化。
```

MVP 简化：

```text
如果角色同步失败，只在 UI 显示“等待同步”。
用户可以点击“重新同步角色配置”。
```

------

### Step 6：补齐 Desktop -> Android SummaryEvent 同步

任务：

```text
1. Android 实现 SyncServer /api/sync，接收 Desktop SummaryEvent。
2. Desktop 实现 SyncClient，将 Desktop SummaryEvent 同步到 Android。
3. Android 校验 Desktop 是已配对设备。
4. Android 根据 event_id 去重。
5. Android Timeline 可以显示 Desktop 事件。
```

验收：

```text
当 Android 被设为主分析设备时，Desktop 可以把本机 SummaryEvent 同步到 Android。
Android Timeline 可以看到 Desktop 事件。
```

说明：

```text
第四阶段已实现 Android -> Desktop。
第五阶段必须补齐 Desktop -> Android，否则 Android 无法真正作为主分析设备。
```

------

### Step 7：实现 CrossDeviceTimelineRepository

任务：

```text
1. Android 读取本机 + 远端 SummaryEvent。
2. Desktop 读取本机 + 远端 SummaryEvent。
3. 按 start_time 排序。
4. 支持设备过滤。
5. 支持类别过滤。
```

验收：

```text
两端都可以展示跨设备时间线。
事件能显示来源设备。
```

------

### Step 8：实现 CrossDeviceLlmPayloadBuilder

任务：

```text
1. 读取当天跨设备 SummaryEvent。
2. 过滤 llm_allowed = false。
3. 根据隐私设置过滤字段。
4. 标记每条事件来源设备。
5. 生成 CrossDeviceLlmAnalysisPackage。
6. 支持 selected_devices scope。
```

验收：

```text
LLM Payload Preview 可以展示 Android + Desktop 混合摘要包。
用户可以看到哪些事件即将发送给 LLM。
```

------

### Step 9：实现跨设备 LLM PromptBuilder

任务：

```text
1. 新增 cross-device system prompt。
2. 新增 cross-device user prompt。
3. 注入 CrossDeviceLlmAnalysisPackage JSON。
4. 要求严格 JSON 输出。
```

验收：

```text
主分析设备可以生成正确的跨设备 LLM 请求。
```

------

### Step 10：实现 CrossDeviceReviewEngine

任务：

```text
1. 判断当前设备是否为主分析设备。
2. 如果不是主分析设备，禁止本地生成跨设备 Review。
3. 如果是主分析设备，构造跨设备 LLM 输入包。
4. 调用已配置的 LLM Provider。
5. 解析 LLM 输出。
6. 保存 llm_reviews，review_type = cross_device。
```

验收：

```text
主分析设备可以生成跨设备 Review。
非主设备不会重复调用 LLM。
```

------

### Step 11：实现 Review 页面跨设备展示

任务：

```text
1. Review 页面加入 scope 选择。
2. 支持 single_device review。
3. 支持 cross_device review。
4. 展示 device_distribution。
5. 展示 cross_device_attention_shifts。
6. 展示 possible_planned_breaks。
7. 展示 possible_task_drift。
8. 展示 suggestions。
```

验收：

```text
用户可以看到结构化跨设备复盘。
```

------

### Step 12：实现 Review 回传

任务：

```text
1. 主分析设备生成 Review 后构造 ReviewSummaryBundle。
2. 实现 /api/review/sync。
3. 非主设备接收 ReviewSummaryBundle。
4. 非主设备保存 llm_reviews，review_type = imported_from_primary。
5. 非主设备 Review 页面展示同步来的复盘摘要。
```

验收：

```text
Desktop 生成跨设备 Review 后，Android 可以看到该 Review 摘要。
Android 生成跨设备 Review 后，Desktop 可以看到该 Review 摘要。
```

------

### Step 13：处理主设备切换后的同步方向

任务：

```text
1. 如果 Desktop 是主设备，Android -> Desktop 同步。
2. 如果 Android 是主设备，Desktop -> Android 同步。
3. DeviceSync 页面显示当前同步方向。
4. 非主设备提示“本设备会把摘要同步给主设备”。
```

验收：

```text
切换主分析设备后，同步方向正确变化。
```

------

### Step 14：错误处理和 UI 打磨

任务：

```text
1. 主设备没有配置 LLM。
2. 主设备不可达。
3. 非主设备未收到 Review。
4. 角色配置不同步。
5. 跨设备事件为空。
6. LLM 调用失败。
7. Review 回传失败。
8. 设备同步方向错误。
```

验收：

```text
用户能理解问题，并知道该在手机端还是电脑端操作。
```

------

## 19. 测试计划

### 19.1 单元测试

测试对象：

```text
PrimaryAnalysisRepository
RoleResolver
CrossDeviceTimelineRepository
CrossDeviceLlmPayloadBuilder
CrossDeviceReviewEngine
ReviewBackSyncClient
ReviewBackSyncServer
```

重点场景：

```text
单 Android 默认本机主设备
单 Desktop 默认本机主设备
双设备默认 Desktop 主设备
用户切换 Android 为主设备
角色更新时间冲突
跨设备事件排序
设备过滤
llm_allowed 过滤
隐私字段过滤
Review 回传解析
```

### 19.2 集成测试

Desktop 为主设备：

```text
1. Android 与 Desktop 已配对。
2. Android SummaryEvent 已同步到 Desktop。
3. Desktop 生成跨设备 LLM Payload。
4. Desktop 调用 LLM 生成 Review。
5. Desktop 保存 cross_device Review。
6. Desktop 回传 ReviewSummaryBundle 给 Android。
7. Android 显示 imported_from_primary Review。
```

Android 为主设备：

```text
1. 用户将 Android 设为主分析设备。
2. Desktop SummaryEvent 同步到 Android。
3. Android 生成跨设备 LLM Payload。
4. Android 调用 LLM 生成 Review。
5. Android 保存 cross_device Review。
6. Android 回传 ReviewSummaryBundle 给 Desktop。
7. Desktop 显示 imported_from_primary Review。
```

错误场景：

```text
主设备未配置 LLM。
非主设备尝试生成跨设备 Review。
远端设备不可达。
RoleUpdateMessage 发送失败。
Review 回传失败。
跨设备事件为空。
LLM 返回非法 JSON。
```

------

## 20. 手动测试流程

### 20.1 Desktop 为主分析设备

```text
1. 完成 Android 与 Desktop 配对。
2. Android 产生手机 SummaryEvent。
3. Desktop 产生电脑 SummaryEvent。
4. Android 同步 SummaryEvent 到 Desktop。
5. Desktop Devices 页面显示：主分析设备 = Desktop。
6. Desktop 打开 CrossDeviceTimeline。
7. 确认能看到 Android + Desktop 事件。
8. Desktop 打开 LLM Payload Preview。
9. 确认 payload 中包含两个设备事件。
10. Desktop 点击生成跨设备复盘。
11. Desktop Review 页面展示跨设备复盘。
12. Desktop 回传 Review 摘要到 Android。
13. Android Review 页面显示来自 Desktop 的复盘摘要。
```

### 20.2 Android 为主分析设备

```text
1. 在设备页将主分析设备切换为 Android。
2. Desktop 显示当前主分析设备 = Android。
3. Desktop 将本机 SummaryEvent 同步给 Android。
4. Android CrossDeviceTimeline 显示 Android + Desktop 事件。
5. Android 预览跨设备 LLM 输入包。
6. Android 调用 LLM 生成跨设备复盘。
7. Android Review 页面展示跨设备复盘。
8. Android 回传 Review 摘要到 Desktop。
9. Desktop Review 页面显示来自 Android 的复盘摘要。
```

------

## 21. 第五阶段验收清单

### 21.1 功能验收

```text
[ ] 单 Android 默认本机是主分析设备
[ ] 单 Desktop 默认本机是主分析设备
[ ] Android + Desktop 双设备默认 Desktop 是主分析设备
[ ] 用户可以切换主分析设备
[ ] 两端设备页能显示当前主分析设备
[ ] 主分析设备配置可以同步到另一端
[ ] Desktop 为主时，Android -> Desktop 同步方向正常
[ ] Android 为主时，Desktop -> Android 同步方向正常
[ ] 两端都能展示跨设备时间线
[ ] 跨设备时间线显示设备来源
[ ] 主分析设备可以构造跨设备 LLM Payload
[ ] LLM Payload Preview 显示参与设备
[ ] LLM Payload Preview 显示每个设备事件数量
[ ] 主分析设备可以生成跨设备 Review
[ ] 非主设备不能重复生成跨设备 Review
[ ] 跨设备 Review 可以保存到 llm_reviews
[ ] Review 页面可以展示 device_distribution
[ ] Review 页面可以展示 cross_device_attention_shifts
[ ] Review 页面可以展示 possible_task_drift
[ ] Review 页面可以展示 suggestions
[ ] 主设备可以回传 Review 摘要给非主设备
[ ] 非主设备可以展示 imported_from_primary Review
```

### 21.2 非功能验收

```text
[ ] 不上传数据到开发者服务器
[ ] 不需要账号登录
[ ] 不同步 raw_usage_events
[ ] 不同步 raw_desktop_events
[ ] 不同步截图
[ ] 不同步聊天内容
[ ] 不同步键盘输入
[ ] LLM 输入包只包含 SummaryEvent
[ ] LLM 分析前用户可以预览输入包
[ ] 跨设备 Review 必须来自 LLM
[ ] LLM 调用失败时不生成伪复盘
[ ] 非主设备不重复调用 LLM
[ ] 敏感事件默认不进入 LLM
```

------

## 22. 第五阶段 Codex 执行提示词

可以直接把下面内容给 Codex：

```text
请根据《Life Debugger 第五阶段编程 Plan：主分析设备切换 + 跨设备 LLM 复盘》实现第五阶段功能。

严格要求：

1. 第五阶段实现主分析设备切换和跨设备 LLM 复盘。
2. 不实现云端账号。
3. 不实现开发者服务器同步。
4. 不上传 raw_usage_events。
5. 不上传 raw_desktop_events。
6. 不上传截图、聊天内容、键盘输入或文件内容。
7. 单设备时默认本设备为主分析设备。
8. Android + Desktop 双设备时默认 Desktop 为主分析设备。
9. 用户可以手动切换主分析设备。
10. 当前主分析设备负责汇总跨设备 SummaryEvent。
11. 当前主分析设备负责调用云端 LLM。
12. 非主设备不能重复调用 LLM 生成跨设备 Review。
13. 跨设备 LLM 输入包只能包含 SummaryEvent。
14. LLM 输入包必须标明每条事件来源设备。
15. LLM 输入包必须可预览。
16. 跨设备 Review 必须来自 LLM，不允许规则伪造。
17. LLM 调用失败时只展示错误。
18. Desktop 为主时，Android SummaryEvent 同步到 Desktop。
19. Android 为主时，Desktop SummaryEvent 同步到 Android。
20. 主设备生成 Review 后，应把 Review 摘要回传给非主设备。
21. 两端 Review 页面都要能展示跨设备复盘。
22. 两端 Timeline 页面都要能展示跨设备事件来源。
23. 先实现 Desktop 为主设备的完整闭环，再实现 Android 为主设备。
```

------

## 23. 第五阶段最终 Demo

### Demo 1：Desktop 为主分析设备

```text
1. 用户完成手机与电脑配对。
2. Android 端产生手机使用摘要。
3. Desktop 端产生电脑使用摘要。
4. Android 自动同步 SummaryEvent 到 Desktop。
5. Desktop 显示跨设备时间线。
6. Desktop 预览跨设备 LLM 输入包。
7. 用户确认发送。
8. Desktop 调用 DeepSeek / Qwen。
9. Desktop 展示跨设备复盘。
10. Android 收到 Desktop 回传的复盘摘要。
```

示例输出：

```text
今天你的跨设备行为呈现出三个明显模式：

1. 上午电脑端存在较长编码工作段，说明生产性投入较集中。
2. 下午 14:03 左右，从电脑写作切换到手机短视频，可能是一次注意力漂移，也可能是计划休息。
3. 聊天类 App 在手机和电脑端均有出现，可能承担沟通功能，但也可能造成多次轻量打断。

建议：
- 当你从电脑切到手机前，快速记录“休息 / 沟通 / 逃避 / 查资料”。
- 将手机娱乐集中到固定休息窗口。
- 对浏览器和聊天工具设置意图标记，以区分工作需要和无意识切换。
```

### Demo 2：Android 为主分析设备

```text
1. 用户在设备页将主分析设备切换为 Android。
2. Desktop 将电脑 SummaryEvent 同步给 Android。
3. Android 显示跨设备时间线。
4. Android 预览跨设备 LLM 输入包。
5. Android 调用 DeepSeek / Qwen。
6. Android 展示跨设备复盘。
7. Desktop 收到 Android 回传的复盘摘要。
```

------

## 24. 第五阶段完成后的下一步

第五阶段完成后，进入第六阶段：

```text
跨设备复盘体验增强 + 反馈闭环
```

第六阶段可以实现：

```text
用户反馈每条 LLM 判断是否准确
计划休息 / 任务漂移 标注
意图记录与复盘联动
跨设备切换原因追问
周报 / 月报
长期行为趋势
本地向量搜索
自然语言查询
```

第五阶段需要为第六阶段保留：

```text
review_id
input_package_hash
user_feedback_json
questions_for_user
cross_device_attention_shifts
possible_task_drift
possible_planned_breaks
origin_device_id
origin_device_type
```

不要删除这些字段，即使第五阶段 UI 暂时没有完全使用。