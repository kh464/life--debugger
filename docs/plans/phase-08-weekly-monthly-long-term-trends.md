# Life Debugger 第八阶段编码 Plan：周报 / 月报 / 长期趋势

## 0. 阶段目标

第八阶段实现 Life Debugger 的长期趋势分析能力。

前七阶段已经完成：

```text
1. Android 单端闭环
2. Desktop 单端闭环
3. 手机电脑配对
4. 局域网 SummaryEvent 摘要同步
5. 主分析设备切换 + 跨设备 LLM 复盘
6. 反馈闭环 + 人类校正
7. 自然语言查询 + 时间线搜索
```

第八阶段要解决的问题是：

```text
用户不只想知道“今天发生了什么”。
用户还想知道“这一周 / 这个月，我的行为模式发生了什么变化”。
```

例如：

```text
这周我什么时候最容易刷手机？
这周电脑专注时间比上周多了吗？
我最近任务漂移变多还是变少？
我通常在哪些任务后切到手机？
我这月的娱乐时间主要集中在哪些时段？
我对 LLM 判断的纠正主要集中在哪类事件？
```

第八阶段完成后，用户可以看到：

```text
周报
月报
长期趋势
跨设备使用趋势
注意力切换趋势
任务漂移趋势
计划休息趋势
反馈修正趋势
LLM 判断准确率趋势
```

一句话目标：

> Life Debugger 从“每日调试器”升级为“长期行为模式分析器”。

------

## 1. 核心原则

第八阶段必须坚持：

```text
1. 只基于 SummaryEvent、LLM Review、UserFeedback、EventCorrection、FeedbackMemoryRule、QuerySession 等摘要数据。
2. 不读取 raw_usage_events。
3. 不读取 raw_desktop_events。
4. 不读取截图内容。
5. 不读取聊天内容。
6. 不读取键盘输入。
7. 不读取文件内容。
8. 不上传数据到开发者服务器。
9. 趋势统计可以由本地确定性算法生成。
10. 趋势解释、建议、模式总结必须由 LLM 生成。
11. LLM 输入必须可预览。
12. 报告中的结论必须能追溯到统计指标或具体时间段。
```

重要区分：

```text
本地统计负责“算数”：
- 总时长
- 分类占比
- 设备占比
- 切换次数
- 趋势增减
- 高频时段

LLM 负责“解释”：
- 为什么可能出现这种模式
- 哪些变化值得注意
- 哪些建议更适合用户
- 哪些地方需要用户确认
```

不要用规则伪造趋势分析结论。

------

## 2. 阶段边界

### 2.1 本阶段必须实现

```text
1. 周报生成。
2. 月报生成。
3. 自定义时间范围报告。
4. 趋势指标本地聚合。
5. 趋势图数据生成。
6. 跨设备时间分布统计。
7. 活动类别趋势统计。
8. 手机 / 电脑使用趋势统计。
9. 注意力切换趋势统计。
10. 任务漂移趋势统计。
11. 计划休息趋势统计。
12. 用户反馈趋势统计。
13. LLM 趋势报告生成。
14. 报告输入预览。
15. 报告历史保存。
16. 报告详情页面。
17. 趋势 Dashboard 页面。
18. 报告导出 Markdown。
19. Android 和 Desktop 都支持查看报告。
20. 主分析设备负责生成跨设备报告。
```

### 2.2 建议实现

```text
1. 趋势卡片收藏。
2. 趋势异常提醒。
3. 周报自动生成提醒。
4. 月报自动生成提醒。
5. 报告对比：本周 vs 上周，本月 vs 上月。
6. 用户目标对比。
7. 查询历史趋势。
8. 反馈准确率趋势。
9. 报告重新生成。
10. 报告中的指标跳转到对应时间线。
```

### 2.3 本阶段不实现

```text
1. 云端账号。
2. 开发者服务器。
3. 多用户协作。
4. 个人模型训练。
5. 本地向量数据库。
6. 自动行为干预。
7. 系统级强制拦截。
8. 浏览器插件增强。
9. 更深层 App 内容解析。
```

------

## 3. 报告类型

### 3.1 Weekly Report

周报。

默认范围：

```text
本地时区下，周一 00:00 到周日 23:59:59。
```

支持：

```text
本周
上周
最近 7 天
自定义 7 天
```

### 3.2 Monthly Report

月报。

默认范围：

```text
本地时区下，自然月第一天 00:00 到最后一天 23:59:59。
```

支持：

```text
本月
上月
最近 30 天
自定义月份
```

### 3.3 Custom Range Report

自定义范围报告。

支持：

```text
任意开始日期
任意结束日期
最多 90 天
```

MVP 限制：

```text
为了避免 LLM 输入过大，自定义范围默认不超过 90 天。
超过 90 天时，只生成本地统计，不调用 LLM。
```

### 3.4 Trend Snapshot

趋势快照。

用于 Dashboard 展示，不一定调用 LLM。

例如：

```text
本周手机娱乐时间比上周减少 18%。
本周电脑生产性时间比上周增加 12%。
本周跨设备切换次数最多的时间段是 14:00 - 16:00。
```

这些可以由本地统计生成，但解释性建议仍交给 LLM。

------

## 4. 核心指标体系

### 4.1 时间投入指标

```text
total_tracked_minutes
desktop_minutes
android_minutes
productive_minutes
communication_minutes
entertainment_minutes
consumption_minutes
study_minutes
work_minutes
idle_minutes
unknown_minutes
```

### 4.2 设备指标

```text
desktop_usage_minutes
android_usage_minutes
desktop_ratio
android_ratio
cross_device_switch_count
desktop_to_android_switch_count
android_to_desktop_switch_count
dual_device_overlap_minutes，可选
```

### 4.3 类别指标

```text
category_total_minutes
category_daily_average
category_peak_day
category_peak_hour
category_change_vs_previous_period
top_categories
rising_categories
falling_categories
```

类别包括：

```text
coding
writing
browser
chat
email
short_video
social_media
game
shopping
finance
health
music
reading
study
system
unknown
```

### 4.4 注意力切换指标

```text
attention_shift_count
possible_task_drift_count
possible_planned_break_count
long_mobile_session_count
quick_check_count
device_switch_after_productive_task_count
entertainment_after_work_count
chat_after_focus_count
```

注意：

```text
这些指标可以从 SummaryEvent、LLM Review 和 UserFeedback 中综合生成。
不要仅凭规则断言真实心理原因。
```

### 4.5 反馈指标

```text
feedback_count
accurate_feedback_count
inaccurate_feedback_count
planned_break_corrections
work_communication_corrections
task_escape_confirmations
mistake_corrections
ignore_similar_count
feedback_rule_count
llm_judgment_accuracy_estimate
```

### 4.6 查询指标

来自第七阶段 query_sessions。

```text
query_count
top_query_types
most_asked_topics
query_answer_feedback_positive_count
query_answer_feedback_negative_count
```

这可以用于报告中体现：

```text
用户本周最关心的问题是什么。
```

------

## 5. 数据库设计

Android 和 Desktop 都需要支持以下表。

### 5.1 trend_reports

保存周报、月报、自定义报告。

```sql
CREATE TABLE trend_reports (
  report_id TEXT PRIMARY KEY,
  report_type TEXT NOT NULL,
  date_range_start INTEGER NOT NULL,
  date_range_end INTEGER NOT NULL,
  date_range_label TEXT NOT NULL,
  primary_device_id TEXT NOT NULL,
  primary_device_type TEXT NOT NULL,
  report_scope TEXT NOT NULL,
  metrics_json TEXT NOT NULL,
  llm_input_hash TEXT,
  llm_output_json TEXT,
  provider_id TEXT,
  model TEXT,
  generated_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  sync_status TEXT DEFAULT 'local',
  metadata_json TEXT
);
```

report_type 可选：

```text
weekly
monthly
custom_range
trend_snapshot
```

report_scope 可选：

```text
single_device
cross_device
selected_devices
```

### 5.2 trend_metric_snapshots

保存可复用指标快照。

```sql
CREATE TABLE trend_metric_snapshots (
  snapshot_id TEXT PRIMARY KEY,
  report_id TEXT,
  metric_key TEXT NOT NULL,
  metric_value REAL NOT NULL,
  metric_unit TEXT,
  dimension_type TEXT,
  dimension_value TEXT,
  date_range_start INTEGER NOT NULL,
  date_range_end INTEGER NOT NULL,
  comparison_value REAL,
  comparison_delta REAL,
  comparison_delta_percent REAL,
  created_at INTEGER NOT NULL
);
```

示例：

```text
metric_key = android_usage_minutes
metric_value = 420
metric_unit = minutes
comparison_delta_percent = -18
```

### 5.3 trend_chart_data

保存图表数据。

```sql
CREATE TABLE trend_chart_data (
  chart_id TEXT PRIMARY KEY,
  report_id TEXT NOT NULL,
  chart_type TEXT NOT NULL,
  title TEXT NOT NULL,
  data_json TEXT NOT NULL,
  created_at INTEGER NOT NULL
);
```

chart_type 可选：

```text
line
bar
stacked_bar
pie
heatmap
calendar
table
```

### 5.4 report_generation_jobs

报告生成任务记录。

```sql
CREATE TABLE report_generation_jobs (
  job_id TEXT PRIMARY KEY,
  report_type TEXT NOT NULL,
  date_range_start INTEGER NOT NULL,
  date_range_end INTEGER NOT NULL,
  status TEXT NOT NULL,
  started_at INTEGER,
  finished_at INTEGER,
  error_code TEXT,
  error_message TEXT,
  created_at INTEGER NOT NULL
);
```

status 可选：

```text
created
running
succeeded
failed
cancelled
```

### 5.5 report_evidence_links

报告与证据链接。

```sql
CREATE TABLE report_evidence_links (
  id TEXT PRIMARY KEY,
  report_id TEXT NOT NULL,
  evidence_type TEXT NOT NULL,
  evidence_id TEXT NOT NULL,
  created_at INTEGER NOT NULL
);
```

evidence_type：

```text
summary_event
llm_review
user_feedback
event_correction
feedback_memory_rule
query_session
manual_intent
```

------

## 6. 核心数据模型

### 6.1 TrendReport

```json
{
  "report_id": "uuid",
  "report_type": "weekly",
  "date_range_start": 1780041600000,
  "date_range_end": 1780646399000,
  "date_range_label": "2026-05-25 至 2026-05-31",
  "primary_device_id": "desktop_001",
  "primary_device_type": "desktop",
  "report_scope": "cross_device",
  "metrics": {},
  "llm_output": {},
  "provider_id": "deepseek",
  "model": "deepseek-chat",
  "generated_at": 1780650000000
}
```

### 6.2 TrendMetrics

```json
{
  "total_tracked_minutes": 2840,
  "desktop_minutes": 1620,
  "android_minutes": 1220,
  "productive_minutes": 980,
  "communication_minutes": 420,
  "entertainment_minutes": 680,
  "cross_device_switch_count": 46,
  "possible_task_drift_count": 12,
  "planned_break_corrections": 5,
  "top_categories": [
    {
      "category": "coding",
      "duration_minutes": 620,
      "change_vs_previous_percent": 14
    },
    {
      "category": "short_video",
      "duration_minutes": 310,
      "change_vs_previous_percent": -18
    }
  ],
  "peak_hours": [
    {
      "hour": 14,
      "label": "14:00 - 15:00",
      "android_minutes": 120,
      "desktop_minutes": 80,
      "dominant_category": "short_video"
    }
  ]
}
```

### 6.3 TrendChartData

```json
{
  "chart_id": "uuid",
  "chart_type": "stacked_bar",
  "title": "每日设备使用时间",
  "data": [
    {
      "date": "2026-05-25",
      "desktop_minutes": 240,
      "android_minutes": 180
    },
    {
      "date": "2026-05-26",
      "desktop_minutes": 300,
      "android_minutes": 160
    }
  ]
}
```

------

## 7. 趋势聚合逻辑

### 7.1 TrendAggregator

职责：

```text
从 SummaryEvent、Review、Feedback、QuerySession 中聚合趋势指标。
```

输入：

```json
{
  "start_time": 1780041600000,
  "end_time": 1780646399000,
  "scope": "cross_device",
  "device_filter": ["desktop_001", "android_phone_001"]
}
```

输出：

```json
{
  "metrics": {},
  "chart_data": [],
  "evidence_links": []
}
```

### 7.2 聚合数据源

必须使用：

```text
summary_events
llm_reviews
user_feedback
event_corrections
feedback_memory_rules
manual_intent_events
query_sessions
```

禁止使用：

```text
raw_usage_events
raw_desktop_events
```

### 7.3 基础聚合

按天聚合：

```text
date
device_type
category
activity_type
duration_minutes
event_count
```

按小时聚合：

```text
hour_of_day
device_type
category
duration_minutes
event_count
```

按设备聚合：

```text
device_id
device_type
duration_minutes
event_count
```

按反馈聚合：

```text
correction_type
judgment
target_type
count
```

------

## 8. 对比周期设计

### 8.1 周报对比

本周报告默认对比上周。

```text
当前周期：2026-05-25 至 2026-05-31
对比周期：2026-05-18 至 2026-05-24
```

### 8.2 月报对比

本月报告默认对比上月。

```text
当前周期：2026-05-01 至 2026-05-31
对比周期：2026-04-01 至 2026-04-30
```

### 8.3 自定义范围对比

默认对比前一个等长周期。

```text
当前周期：最近 10 天
对比周期：再往前 10 天
```

### 8.4 Delta 计算

```text
delta = current_value - previous_value
delta_percent = delta / previous_value * 100
```

特殊情况：

```text
previous_value = 0 且 current_value > 0：
delta_percent = null
comparison_label = "新增"

previous_value = 0 且 current_value = 0：
delta_percent = 0
```

------

## 9. 图表数据设计

### 9.1 必须生成的图表

周报 / 月报至少生成：

```text
1. 每日设备使用时间 stacked_bar
2. 活动类别占比 pie 或 bar
3. 每日生产性 / 娱乐 / 沟通趋势 line
4. 跨设备切换次数 line
5. 高频使用时段 heatmap
6. 反馈修正类型统计 bar
```

### 9.2 chart_data 示例：每日设备使用

```json
{
  "chart_type": "stacked_bar",
  "title": "每日设备使用时间",
  "data": [
    {
      "date": "2026-05-25",
      "desktop_minutes": 240,
      "android_minutes": 180
    }
  ]
}
```

### 9.3 chart_data 示例：高频时段

```json
{
  "chart_type": "heatmap",
  "title": "一周高频使用时段",
  "data": [
    {
      "weekday": "Monday",
      "hour": 14,
      "total_minutes": 48,
      "dominant_category": "short_video"
    }
  ]
}
```

------

## 10. LLM 趋势报告设计

### 10.1 TrendReportInputPackage

```json
{
  "package_id": "uuid",
  "report_type": "weekly",
  "date_range": {
    "start": "2026-05-25",
    "end": "2026-05-31",
    "label": "2026-05-25 至 2026-05-31"
  },
  "timezone": "Asia/Tokyo",
  "scope": "cross_device",
  "devices": [
    {
      "device_id": "desktop_001",
      "device_type": "desktop",
      "device_name": "Ko-PC"
    },
    {
      "device_id": "android_phone_001",
      "device_type": "android",
      "device_name": "Pixel Phone"
    }
  ],
  "metrics": {},
  "comparison": {
    "enabled": true,
    "previous_range_label": "2026-05-18 至 2026-05-24",
    "delta_metrics": {}
  },
  "feedback_summary": {},
  "query_summary": {},
  "notable_events": [],
  "privacy_note": "Only local summaries, reviews, feedback and aggregate metrics are included. Raw usage events, screenshots, chat content, keystrokes and file contents are not included."
}
```

### 10.2 notable_events

为了让 LLM 解释趋势时有证据，需要提供少量代表性事件。

规则：

```text
最多 20 条。
优先选择：
- 长时间事件
- 跨设备切换前后事件
- 被用户反馈修正的事件
- LLM Review 中高置信度的 attention shift
- 用户查询过的相关事件
```

示例：

```json
{
  "event_id": "uuid",
  "time_range": "2026-05-28 14:03 - 14:27",
  "device_type": "android",
  "category": "short_video",
  "summary": "连续使用 YouTube 约 24 分钟",
  "why_notable": "本周多次出现的下午娱乐切换之一"
}
```

------

## 11. LLM Prompt

### 11.1 System Prompt

```text
你是 Life Debugger 的长期趋势分析引擎。

你只能基于用户提供的趋势指标、摘要事件、历史反馈和查询记录进行分析。
不要假装知道输入之外的内容。
不要推断 App 内具体内容。
不要推断键盘输入、聊天内容或文件内容。
不要给医学诊断。
不要使用羞辱、指责、道德审判式语言。

你的目标是帮助用户理解一周或一个月中的行为模式变化、跨设备使用趋势、注意力切换趋势，以及可执行的改进建议。

请输出严格 JSON，不要输出 Markdown。
```

### 11.2 User Prompt

```text
下面是用户的 Life Debugger 趋势报告输入包。

请生成趋势报告，分析：
1. 本周期总体概览；
2. 与上一周期相比的主要变化；
3. 手机与电脑使用趋势；
4. 主要活动类别趋势；
5. 注意力切换和任务漂移趋势；
6. 用户反馈揭示的误判或偏好；
7. 值得肯定的积极模式；
8. 需要用户确认的不确定点；
9. 3 到 5 条具体建议。

要求：
- 只基于输入包分析；
- 不要编造未提供的内容；
- 不要责备用户；
- 如果证据不足，请说明不确定性；
- 输出 JSON；
- JSON 字段必须符合指定 schema。

趋势报告输入包如下：
{TREND_REPORT_INPUT_PACKAGE_JSON}
```

------

## 12. LLM 输出 Schema

```json
{
  "report_summary": "本周整体行为概览",
  "major_changes": [
    {
      "id": "change_001",
      "title": "短视频时间下降",
      "detail": "本周短视频类使用时间比上周减少 18%。",
      "direction": "decrease",
      "confidence": 0.82,
      "related_metric_keys": ["short_video_minutes"]
    }
  ],
  "device_trends": [
    {
      "device_type": "desktop",
      "title": "电脑端生产性时间增加",
      "detail": "电脑端 coding 和 writing 合计时间较上周增加。",
      "confidence": 0.76
    }
  ],
  "category_trends": [
    {
      "category": "chat",
      "title": "聊天类使用集中在工作日下午",
      "detail": "聊天类事件在工作日 10:00 - 18:00 较多，结合用户反馈，可能主要承担工作沟通功能。",
      "confidence": 0.7
    }
  ],
  "attention_trends": [
    {
      "id": "attention_trend_001",
      "title": "下午跨设备切换较多",
      "detail": "14:00 - 16:00 是本周从电脑切换到手机最频繁的时间段。",
      "confidence": 0.74
    }
  ],
  "feedback_insights": [
    {
      "title": "用户多次修正午间娱乐为计划休息",
      "detail": "这说明午间短视频不应直接判断为任务漂移。",
      "related_feedback_count": 3
    }
  ],
  "positive_patterns": [
    "上午电脑端存在较稳定的编码或写作时间段。"
  ],
  "suggestions": [
    {
      "title": "保护下午 14:00 前后的任务切换点",
      "detail": "如果这是高分心时段，可以在开始任务前记录意图，减少无意识切换。"
    }
  ],
  "questions_for_user": [
    "下午 14:00 - 16:00 的手机切换主要是计划休息、沟通，还是任务卡住后的转移？"
  ],
  "uncertainties": [
    "摘要数据不能判断用户真实心理原因，只能显示行为切换模式。"
  ],
  "risk_level": "low",
  "tone": "supportive"
}
```

------

## 13. Android 模块设计

新增：

```text
trend/
  TrendReport.kt
  TrendMetrics.kt
  TrendChartData.kt
  TrendReportInputPackage.kt
  TrendAggregator.kt
  TrendComparisonCalculator.kt
  TrendChartDataBuilder.kt
  TrendReportRepository.kt
  TrendReportEngine.kt
  TrendPromptBuilder.kt
  TrendOutputParser.kt
  NotableEventSelector.kt
  ReportExportService.kt

ui/
  trend/
    TrendDashboardScreen.kt
    ReportListScreen.kt
    ReportDetailScreen.kt
    ReportGenerateScreen.kt
    ReportInputPreviewScreen.kt
    TrendChartCard.kt
    MetricCard.kt
    ReportExportScreen.kt
```

### 13.1 TrendAggregator

```kotlin
class TrendAggregator {
    suspend fun aggregate(
        startTime: Long,
        endTime: Long,
        scope: ReportScope
    ): TrendMetrics
}
```

### 13.2 TrendReportEngine

```kotlin
class TrendReportEngine {
    suspend fun generateReport(
        reportType: ReportType,
        startTime: Long,
        endTime: Long,
        scope: ReportScope
    ): TrendReport
}
```

### 13.3 TrendChartDataBuilder

```kotlin
class TrendChartDataBuilder {
    fun buildCharts(metrics: TrendMetrics): List<TrendChartData>
}
```

------

## 14. Desktop 模块设计

新增：

```text
src-tauri/src/trend/
  trend_report.rs
  trend_metrics.rs
  trend_chart_data.rs
  trend_report_input_package.rs
  trend_aggregator.rs
  trend_comparison_calculator.rs
  trend_chart_data_builder.rs
  trend_report_repository.rs
  trend_report_engine.rs
  trend_prompt_builder.rs
  trend_output_parser.rs
  notable_event_selector.rs
  report_export_service.rs

src-tauri/src/commands/
  trend_commands.rs

src/pages/trend/
  TrendDashboardPage.tsx
  ReportListPage.tsx
  ReportDetailPage.tsx
  ReportGeneratePage.tsx
  ReportInputPreviewPage.tsx
  TrendChartCard.tsx
  MetricCard.tsx
  ReportExportPage.tsx
```

### 14.1 Tauri Commands

```rust
#[tauri::command]
async fn get_trend_dashboard() -> Result<TrendDashboardData, String>;

#[tauri::command]
async fn preview_trend_report_input(
    report_type: String,
    start_time: i64,
    end_time: i64,
    scope: String
) -> Result<TrendReportInputPackage, String>;

#[tauri::command]
async fn generate_trend_report(
    report_type: String,
    start_time: i64,
    end_time: i64,
    scope: String
) -> Result<TrendReport, String>;

#[tauri::command]
async fn get_trend_reports(
    report_type: Option<String>,
    limit: i64
) -> Result<Vec<TrendReport>, String>;

#[tauri::command]
async fn get_trend_report_detail(
    report_id: String
) -> Result<TrendReport, String>;

#[tauri::command]
async fn delete_trend_report(
    report_id: String
) -> Result<(), String>;

#[tauri::command]
async fn export_trend_report_markdown(
    report_id: String
) -> Result<String, String>;
```

------

## 15. UI 设计

## 15.1 TrendDashboard

入口：

```text
Sidebar -> Trends
BottomNav -> Trends
Review Page -> View Weekly Trends
Query Page -> Generate Trend Report
```

页面卡片：

```text
本周总追踪时间
本周电脑使用时间
本周手机使用时间
生产性时间
娱乐时间
跨设备切换次数
任务漂移次数
计划休息修正次数
LLM 判断反馈准确率
```

趋势摘要卡：

```text
相比上周：
- 手机娱乐时间下降 18%
- 电脑生产性时间上升 12%
- 下午跨设备切换仍然较多
```

按钮：

```text
生成本周周报
生成上周周报
生成本月月报
自定义范围报告
查看历史报告
```

------

## 15.2 ReportGeneratePage

字段：

```text
报告类型：周报 / 月报 / 自定义范围
日期范围
报告范围：本设备 / 跨设备 / 选择设备
是否包含用户反馈
是否包含查询历史摘要
是否包含代表性事件
Provider / Model
```

按钮：

```text
预览输入包
生成报告
取消
```

------

## 15.3 ReportInputPreviewPage

展示：

```text
将发送给 LLM 的趋势输入包
日期范围
设备列表
指标摘要
代表性事件
反馈摘要
查询摘要
完整 JSON
```

用户可以：

```text
排除代表性事件
排除反馈摘要
排除查询摘要
隐藏 App 名称
确认生成
取消
```

------

## 15.4 ReportDetailPage

展示模块：

```text
报告标题
日期范围
生成时间
总体总结
主要变化
设备趋势
类别趋势
注意力趋势
反馈洞察
积极模式
建议
需要确认的问题
不确定性
图表区域
证据链接
```

支持操作：

```text
重新生成
导出 Markdown
删除报告
跳转相关时间线
基于报告提问
```

------

## 15.5 TrendChartCard

MVP 图表可以先用简单图表组件。

需要支持：

```text
bar
line
stacked_bar
pie
heatmap
table
```

如果图表库暂时未接入，可以先用表格和简化条形图。

------

## 16. 报告导出 Markdown

### 16.1 导出内容

```text
标题
日期范围
总体总结
主要变化
设备趋势
类别趋势
注意力趋势
反馈洞察
建议
不确定性
核心指标表
```

### 16.2 Markdown 示例

```markdown
# Life Debugger 周报：2026-05-25 至 2026-05-31

## 总览

本周你的电脑端生产性时间有所增加，手机娱乐时间有所下降。

## 主要变化

- 短视频时间比上周减少 18%
- 电脑编码时间比上周增加 14%
- 下午 14:00 - 16:00 仍然是跨设备切换高峰

## 建议

1. 继续保护上午的电脑专注时间。
2. 对下午跨设备切换添加意图记录。
3. 对聊天类使用区分工作沟通和分心。
```

------

## 17. 报告同步策略

第五阶段已经有 Review 回传机制。

第八阶段新增 TrendReport 同步。

### 17.1 TrendReportSyncBundle

```json
{
  "protocol": "lifedbg-trend-report-sync-v1",
  "bundle_id": "uuid",
  "from_device_id": "desktop_001",
  "to_device_id": "android_phone_001",
  "report_id": "uuid",
  "report_type": "weekly",
  "date_range_start": 1780041600000,
  "date_range_end": 1780646399000,
  "report_summary": {},
  "generated_at": 1780650000000
}
```

### 17.2 同步策略

```text
主分析设备生成报告。
非主设备接收报告摘要。
非主设备不重复调用 LLM。
```

MVP 可以只同步：

```text
report_id
report_type
date_range
llm_output_json
metrics_json
```

------

## 18. 开发顺序

### Step 1：数据库迁移

任务：

```text
1. 新增 trend_reports。
2. 新增 trend_metric_snapshots。
3. 新增 trend_chart_data。
4. 新增 report_generation_jobs。
5. 新增 report_evidence_links。
```

验收：

```text
数据库迁移成功。
旧数据不受影响。
```

------

### Step 2：实现 TrendAggregator

任务：

```text
1. 聚合 SummaryEvent。
2. 聚合 LlmReview。
3. 聚合 UserFeedback。
4. 聚合 QuerySession。
5. 生成 TrendMetrics。
6. 支持 weekly / monthly / custom_range。
```

验收：

```text
给定日期范围，可以生成总时长、设备时长、类别时长、反馈数量等基础指标。
```

------

### Step 3：实现 TrendComparisonCalculator

任务：

```text
1. 自动计算对比周期。
2. 聚合对比周期指标。
3. 计算 delta。
4. 计算 delta_percent。
5. 处理 previous_value = 0。
```

验收：

```text
周报能显示本周 vs 上周的指标变化。
```

------

### Step 4：实现 TrendChartDataBuilder

任务：

```text
1. 生成每日设备使用 stacked_bar 数据。
2. 生成类别占比数据。
3. 生成生产性 / 娱乐趋势 line 数据。
4. 生成跨设备切换趋势数据。
5. 生成高频时段 heatmap 数据。
6. 生成反馈类型统计数据。
```

验收：

```text
TrendDashboard 可以展示基础图表或表格。
```

------

### Step 5：实现 NotableEventSelector

任务：

```text
1. 选择长时间事件。
2. 选择跨设备切换相关事件。
3. 选择用户反馈修正过的事件。
4. 选择 Review 中高置信度事件。
5. 限制最多 20 条。
```

验收：

```text
TrendReportInputPackage 中包含少量代表性事件。
```

------

### Step 6：实现 TrendReportInputPackageBuilder

任务：

```text
1. 组合 metrics。
2. 组合 comparison。
3. 组合 feedback_summary。
4. 组合 query_summary。
5. 组合 notable_events。
6. 添加 privacy_note。
7. 控制 JSON 大小。
```

验收：

```text
ReportInputPreviewPage 能展示即将发送给 LLM 的输入包。
```

------

### Step 7：实现 TrendPromptBuilder

任务：

```text
1. 实现 system prompt。
2. 实现 user prompt。
3. 注入 TrendReportInputPackage。
4. 要求严格 JSON 输出。
```

验收：

```text
可以构造趋势报告 LLM 请求。
```

------

### Step 8：实现 TrendReportEngine

任务：

```text
1. 检查当前设备是否为主分析设备。
2. 构造 TrendReportInputPackage。
3. 调用 LLM。
4. 解析输出 JSON。
5. 保存 trend_reports。
6. 保存 trend_chart_data。
7. 保存 report_evidence_links。
```

验收：

```text
主分析设备可以生成周报或月报。
```

------

### Step 9：实现 TrendDashboard UI

任务：

```text
1. 展示本周核心指标。
2. 展示与上周对比。
3. 展示图表卡片。
4. 提供生成报告入口。
5. 提供历史报告入口。
```

验收：

```text
用户可以在 Trends 页面看到本周趋势概览。
```

------

### Step 10：实现 ReportGeneratePage

任务：

```text
1. 选择报告类型。
2. 选择日期范围。
3. 选择报告范围。
4. 选择是否包含反馈和查询摘要。
5. 进入输入包预览。
```

验收：

```text
用户可以配置并预览报告生成参数。
```

------

### Step 11：实现 ReportDetailPage

任务：

```text
1. 展示 LLM 输出内容。
2. 展示核心指标。
3. 展示图表。
4. 支持跳转相关时间线。
5. 支持重新生成。
6. 支持删除报告。
```

验收：

```text
用户可以完整阅读周报 / 月报。
```

------

### Step 12：实现 Markdown 导出

任务：

```text
1. 将 TrendReport 转为 Markdown。
2. 支持复制到剪贴板。
3. 支持保存为 .md 文件，Desktop 端。
4. Android 端支持分享文本。
```

验收：

```text
用户可以导出报告。
```

------

### Step 13：实现 TrendReport 同步

任务：

```text
1. 实现 TrendReportSyncBundle。
2. 主分析设备生成报告后回传给非主设备。
3. 非主设备保存为 imported trend report。
4. 非主设备不重复调用 LLM。
```

验收：

```text
Desktop 生成周报后，Android 可以查看报告摘要。
Android 生成周报后，Desktop 可以查看报告摘要。
```

------

### Step 14：错误处理与 UI 打磨

任务：

```text
1. 数据不足提示。
2. LLM 未配置提示。
3. 非主设备无法生成跨设备报告提示。
4. LLM 调用失败提示。
5. JSON 解析失败提示。
6. 报告输入过大提示。
7. 导出失败提示。
```

验收：

```text
用户能理解报告生成失败原因。
```

------

## 19. 测试计划

### 19.1 单元测试

测试对象：

```text
TrendAggregator
TrendComparisonCalculator
TrendChartDataBuilder
NotableEventSelector
TrendReportInputPackageBuilder
TrendOutputParser
ReportExportService
```

测试场景：

```text
无数据
只有 Android 数据
只有 Desktop 数据
跨设备数据
本周 vs 上周
本月 vs 上月
previous_value = 0
类别聚合
反馈聚合
查询历史聚合
notable_events 限制数量
LLM 返回 Markdown 包裹 JSON
LLM 返回非法 JSON
```

### 19.2 集成测试

场景一：周报

```text
1. 系统中存在最近 14 天 SummaryEvent。
2. 用户生成本周周报。
3. 系统聚合本周和上周数据。
4. 用户预览 LLM 输入包。
5. LLM 生成周报。
6. 报告保存并展示。
```

场景二：月报

```text
1. 系统中存在最近 60 天 SummaryEvent。
2. 用户生成本月月报。
3. 系统聚合本月和上月数据。
4. LLM 生成月报。
5. 用户导出 Markdown。
```

场景三：反馈趋势

```text
1. 用户有多条 user_feedback。
2. 周报中显示计划休息修正次数。
3. LLM 输出反馈洞察。
```

场景四：报告同步

```text
1. Desktop 是主分析设备。
2. Desktop 生成周报。
3. Desktop 回传报告摘要到 Android。
4. Android 可以查看报告摘要。
```

------

## 20. 手动测试流程

```text
1. 准备至少 7 天 SummaryEvent 数据。
2. 打开 Trends 页面。
3. 查看本周指标卡。
4. 点击生成本周周报。
5. 预览 TrendReportInputPackage。
6. 确认发送 LLM。
7. 查看周报详情。
8. 查看图表。
9. 点击某条趋势，跳转相关时间线。
10. 导出 Markdown。
11. 切换到 Android，查看同步来的周报摘要。
12. 生成本月月报。
13. 测试数据不足时的提示。
```

------

## 21. 第八阶段验收清单

### 21.1 功能验收

```text
[ ] 有 Trends 页面
[ ] 可以显示本周核心指标
[ ] 可以显示本周 vs 上周变化
[ ] 可以生成周报
[ ] 可以生成月报
[ ] 可以生成自定义范围报告
[ ] 可以聚合 SummaryEvent 指标
[ ] 可以聚合 UserFeedback 指标
[ ] 可以聚合 QuerySession 指标
[ ] 可以生成图表数据
[ ] 可以选择代表性事件
[ ] 可以预览 TrendReportInputPackage
[ ] 可以调用 LLM 生成趋势报告
[ ] 可以保存 trend_reports
[ ] 可以展示报告详情
[ ] 可以导出 Markdown
[ ] 可以跳转相关时间线
[ ] 主设备可以生成跨设备报告
[ ] 非主设备不重复调用 LLM
[ ] 报告可以同步到非主设备
```

### 21.2 非功能验收

```text
[ ] 不读取 raw_usage_events
[ ] 不读取 raw_desktop_events
[ ] 不读取截图
[ ] 不读取聊天内容
[ ] 不读取键盘输入
[ ] 不读取文件内容
[ ] 不上传数据到开发者服务器
[ ] LLM 输入包可预览
[ ] LLM 趋势解释必须基于指标和摘要证据
[ ] LLM 失败时不生成伪报告
[ ] 非主设备不重复调用 LLM
[ ] 数据不足时明确提示
```

------

## 22. 第八阶段 Codex 执行提示词

可以直接把下面内容给 Codex：

```text
请根据《Life Debugger 第八阶段编码 Plan：周报 / 月报 / 长期趋势》实现第八阶段功能。

严格要求：

1. 第八阶段实现周报、月报和长期趋势。
2. 不实现云端账号。
3. 不上传数据到开发者服务器。
4. 不读取 raw_usage_events。
5. 不读取 raw_desktop_events。
6. 不读取截图、聊天内容、键盘输入或文件内容。
7. 趋势统计只能基于 SummaryEvent、LlmReview、UserFeedback、EventCorrection、FeedbackMemoryRule、QuerySession、ManualIntent。
8. 本地算法可以生成指标和图表数据。
9. 趋势解释和建议必须由 LLM 生成。
10. LLM 输入必须可预览。
11. LLM 调用失败时，不要生成伪报告。
12. 必须支持周报。
13. 必须支持月报。
14. 必须支持自定义范围报告。
15. 必须支持本周期 vs 上周期对比。
16. 必须生成趋势图表数据。
17. 必须保存 trend_reports。
18. 必须支持报告详情页。
19. 必须支持 Markdown 导出。
20. 主分析设备生成跨设备报告，非主设备只接收报告摘要。
```

------

## 23. 第八阶段最终 Demo

目标 Demo：

```text
1. 用户已经使用 Life Debugger 至少一周。
2. 系统中已有 Android + Desktop SummaryEvent。
3. 系统中已有跨设备 Review。
4. 系统中已有用户反馈。
5. 用户打开 Trends 页面。
6. 页面显示本周核心指标：
   - 电脑生产性时间
   - 手机娱乐时间
   - 跨设备切换次数
   - 计划休息修正次数
7. 用户点击“生成本周周报”。
8. 系统展示 TrendReportInputPackage 预览。
9. 用户确认发送。
10. 主分析设备调用 DeepSeek / Qwen。
11. 系统展示周报。
12. 用户导出 Markdown。
13. 非主设备收到报告摘要。
```

示例报告输出：

```text
本周你的电脑端生产性时间比上周增加 12%，主要集中在上午的编码和写作任务。
手机娱乐时间比上周下降 18%，说明你可能更集中地安排了娱乐使用。

值得注意的是，14:00 - 16:00 仍然是跨设备切换高峰。
这并不一定代表任务漂移，因为你多次反馈午间和下午部分娱乐是计划休息。
但如果你希望提升下午连续工作时长，可以在进入该时间段前记录任务意图。
```

演示文案：

```text
Life Debugger 不只是告诉你今天发生了什么。
它会逐渐看到你的长期模式：
哪段时间最容易切到手机，
哪些娱乐其实是计划休息，
你的电脑专注时间是否在增加，
以及哪些建议真正适合你。
```

------

## 24. 第八阶段完成后的下一步

第八阶段完成后，进入第九阶段：

```text
隐私、安全与可靠性强化
```

第九阶段将重点处理：

```text
同步包签名
局域网传输加密
私钥安全存储强化
敏感数据审计
数据库备份与恢复
导出 / 导入
崩溃日志脱敏
安全模式
隐私检查器
```

第八阶段需要为第九阶段保留：

```text
report_id
report_type
llm_input_hash
report_evidence_links
sync_status
metadata_json
```

这些字段会帮助第九阶段做数据审计、隐私检查和安全同步。