# Life Debugger 第七阶段编程 Plan：自然语言查询 + 时间线搜索

## 0. 阶段目标

第七阶段实现 Life Debugger 的自然语言查询能力。

前六阶段已经完成：

```text
1. Android 单端闭环
2. Desktop 单端闭环
3. 手机电脑配对
4. 局域网 SummaryEvent 摘要同步
5. 主分析设备切换 + 跨设备 LLM 复盘
6. 反馈闭环 + 人类校正
```

第七阶段要解决的问题是：

```text
用户不只想看每日复盘。
用户还想主动追问自己的行为数据。
```

例如：

```text
我昨天为什么没写完文档？
我这周什么时候最容易刷手机？
我最近经常在哪些任务后切到短视频？
我昨天从电脑切到手机前在做什么？
我这周有几次把 LLM 判断纠正为计划休息？
我什么时候最容易从编码切到聊天？
昨天晚上我主要在手机上做了什么？
```

第七阶段完成后，用户可以在应用中直接输入自然语言问题，系统会基于本地 SummaryEvent、LLM Review、UserFeedback、FeedbackMemoryRule 和 EventCorrection 生成可追溯回答。

一句话目标：

> Life Debugger 从“每日复盘工具”升级为“可查询的人生调试数据库”。

------

## 1. 核心原则

第七阶段必须坚持：

```text
1. 查询只基于摘要数据。
2. 不读取 raw_usage_events。
3. 不读取 raw_desktop_events。
4. 不读取截图内容。
5. 不读取聊天内容。
6. 不读取键盘输入。
7. 不读取文件内容。
8. 不上传数据到开发者服务器。
9. 如果调用云端 LLM，只发送经过筛选和预览的摘要上下文。
10. 回答必须引用具体时间段或事件来源，不能空泛发挥。
```

重要原则：

```text
用户问的是“我的行为数据”，不是让 LLM 凭空心理分析。
```

因此回答必须基于：

```text
SummaryEvent
LlmReview
UserFeedback
EventCorrection
FeedbackMemoryRule
ManualIntent
```

------

## 2. 阶段边界

### 2.1 本阶段必须实现

```text
1. 自然语言查询页面。
2. 本地时间线搜索。
3. SQLite FTS 全文索引。
4. 查询意图解析。
5. 日期范围解析。
6. 设备范围解析。
7. 类别范围解析。
8. 查询相关事件检索。
9. 查询相关 Review 检索。
10. 查询相关 Feedback 检索。
11. 构造 QueryContext。
12. LLM 生成查询回答。
13. 回答引用具体证据。
14. 查询历史保存。
15. 查询结果可再次追问。
16. 查询结果支持跳转到时间线。
17. 查询输入预览。
18. 隐私过滤。
19. 无 LLM 时提供本地搜索结果。
20. Android 和 Desktop 都支持查询。
```

### 2.2 建议实现

```text
1. 快捷问题模板。
2. 最近常问问题。
3. 查询结果卡片。
4. 查询结果导出 Markdown。
5. 查询中的时间段可点击。
6. 查询结果支持用户反馈。
7. 查询意图调试面板。
8. 跨设备查询过滤器。
9. 模糊日期解析。
10. 查询结果缓存。
```

### 2.3 本阶段不实现

```text
1. 周报 / 月报自动生成。
2. 长期趋势仪表盘。
3. 复杂图表系统。
4. 本地向量数据库。
5. 个人模型训练。
6. 云端账号。
7. 开发者服务器。
8. 多用户协作。
```

说明：

```text
第七阶段可以先用 SQLite FTS + 结构化过滤 + LLM 汇总。
本地向量搜索可以作为增强，不作为 MVP 必需项。
```

------

## 3. 用户查询类型

### 3.1 时间回溯类

用户想知道某个时间段发生了什么。

示例：

```text
我昨天下午 2 点在做什么？
我昨晚 9 点到 11 点主要干了什么？
我昨天从电脑切到手机前在做什么？
```

系统应返回：

```text
具体时间线
设备来源
活动类别
摘要事件
相关 LLM Review 判断
相关用户反馈
```

------

### 3.2 原因解释类

用户想知道为什么某件事没完成或为什么分心。

示例：

```text
我昨天为什么没写完文档？
我今天为什么感觉效率低？
我下午为什么总是切到手机？
```

系统应：

```text
检索相关时间段的事件。
找出任务切换点。
找出长时间中断。
找出用户反馈过的解释。
用 LLM 生成谨慎解释。
```

注意：

```text
不能断言心理原因。
必须使用“可能”“看起来”“基于摘要”。
```

------

### 3.3 模式发现类

用户想找周期性行为模式。

示例：

```text
我这周什么时候最容易刷手机？
我最近经常在哪些任务后切到短视频？
我通常什么时候开始娱乐？
我工作日白天最常打开哪些 App？
```

系统应：

```text
按日期聚合 SummaryEvent。
按设备、类别、时间段统计。
找出高频模式。
让 LLM 解释模式。
```

------

### 3.4 反馈与纠正类

用户想查询自己如何纠正过系统。

示例：

```text
我这周把哪些判断标记为误判？
我经常把哪些行为标记为计划休息？
LLM 哪些判断经常不准？
我有哪些反馈规则？
```

系统应检索：

```text
user_feedback
event_corrections
feedback_memory_rules
```

------

### 3.5 复盘追问类

用户对之前的 Review 提问。

示例：

```text
昨天复盘里说我有任务漂移，具体是哪几次？
上次 LLM 建议我做什么？
我有没有采纳之前的建议？
```

系统应检索：

```text
llm_reviews
user_feedback
summary_events
```

------

## 4. 查询架构

整体流程：

```text
用户自然语言问题
        ↓
QueryIntentParser
        ↓
QueryPlan
        ↓
LocalRetriever
        ↓
QueryContextBuilder
        ↓
LLM QueryAnswerEngine
        ↓
Answer with Evidence
        ↓
Query History
```

详细流程：

```text
1. 用户输入问题。
2. 系统解析时间范围、设备范围、类别范围、查询类型。
3. 本地检索相关 SummaryEvent / Review / Feedback。
4. 构造 QueryContext。
5. 用户可预览将发送给 LLM 的查询上下文。
6. 调用用户配置的 LLM。
7. LLM 返回结构化回答。
8. UI 展示回答和证据卡片。
9. 用户可点击证据跳转到 Timeline。
10. 查询记录保存到本地。
```

------

## 5. 查询意图模型

### 5.1 QueryIntent

```json
{
  "query_id": "uuid",
  "raw_question": "我昨天为什么没写完文档？",
  "query_type": "reason_explanation",
  "date_range": {
    "start": 1780214400000,
    "end": 1780300799000,
    "label": "昨天"
  },
  "device_filter": ["android", "desktop"],
  "category_filter": ["writing", "browser", "chat", "short_video"],
  "keywords": ["文档", "写作", "中断", "手机"],
  "requires_llm": true,
  "created_at": 1780309000000
}
```

### 5.2 query_type 可选值

```text
timeline_lookup
reason_explanation
pattern_discovery
feedback_lookup
review_followup
time_distribution
device_switch
unknown
```

### 5.3 QueryIntentParser

MVP 可以采用两层策略：

```text
第一层：规则解析。
第二层：LLM 辅助解析。
```

规则解析负责：

```text
今天
昨天
本周
上周
最近 7 天
上午
下午
晚上
手机
电脑
短视频
聊天
浏览器
编码
写作
复盘
反馈
误判
计划休息
```

LLM 辅助解析负责：

```text
复杂自然语言问题。
模糊意图。
多个条件组合。
```

如果 LLM 未配置：

```text
只使用规则解析 + 本地搜索。
不生成解释型回答。
```

------

## 6. 日期范围解析

### 6.1 支持表达

```text
今天
昨天
前天
本周
上周
最近 7 天
最近 30 天
上午
下午
晚上
昨天下午
今天上午
这周晚上
6 月 1 日
2026-06-01
```

### 6.2 DateRangeParser

接口：

```kotlin
class DateRangeParser {
    fun parse(question: String, timezone: ZoneId): DateRange?
}
```

TypeScript / Rust 也需要对应实现。

DateRange：

```json
{
  "start": 1780214400000,
  "end": 1780300799000,
  "label": "昨天"
}
```

默认规则：

```text
如果用户未指定日期，默认查询今天。
如果用户问“最近”，默认最近 7 天。
如果用户问“这周”，按本地周起止时间。
```

------

## 7. 本地检索设计

### 7.1 检索数据源

第七阶段检索以下表：

```text
summary_events
llm_reviews
user_feedback
event_corrections
feedback_memory_rules
manual_intent_events
```

不检索：

```text
raw_usage_events
raw_desktop_events
```

### 7.2 SQLite FTS

新增 FTS 表。

#### summary_events_fts

```sql
CREATE VIRTUAL TABLE summary_events_fts USING fts5(
  event_id UNINDEXED,
  summary,
  category,
  activity_type,
  app_label,
  window_title,
  device_type,
  content='summary_events',
  content_rowid='rowid'
);
```

如果当前表没有 rowid 映射，MVP 可不使用 external content，改为普通 FTS 镜像表。

#### llm_reviews_fts

```sql
CREATE VIRTUAL TABLE llm_reviews_fts USING fts5(
  review_id UNINDEXED,
  date,
  review_type,
  output_text
);
```

output_text 是 output_json 的扁平化文本。

#### feedback_fts

```sql
CREATE VIRTUAL TABLE feedback_fts USING fts5(
  feedback_id UNINDEXED,
  correction_type,
  correction_text,
  judgment,
  target_type
);
```

#### feedback_memory_rules_fts

```sql
CREATE VIRTUAL TABLE feedback_memory_rules_fts USING fts5(
  rule_id UNINDEXED,
  rule_type,
  interpretation
);
```

### 7.3 FTS 更新策略

每次写入或更新以下数据时同步更新 FTS：

```text
summary_events
llm_reviews
user_feedback
feedback_memory_rules
```

提供重建索引功能：

```text
Settings -> Data Management -> Rebuild Search Index
```

------

## 8. LocalRetriever

### 8.1 LocalRetriever 职责

```text
根据 QueryIntent 检索相关本地证据。
```

输入：

```json
{
  "query_intent": {},
  "limit": 50
}
```

输出：

```json
{
  "events": [],
  "reviews": [],
  "feedback": [],
  "memory_rules": [],
  "manual_intents": []
}
```

### 8.2 检索策略

按优先级：

```text
1. 时间范围过滤。
2. 设备过滤。
3. category / activity_type 过滤。
4. 关键词 FTS 检索。
5. 事件类型相关过滤。
6. 返回最多 N 条。
```

### 8.3 查询类型对应策略

#### timeline_lookup

```text
优先返回时间范围内的 SummaryEvent。
按 start_time 排序。
```

#### reason_explanation

```text
返回目标时间段前后的事件。
重点返回设备切换、长时间娱乐、长时间聊天、空闲段、手动意图。
返回相关 Review 和 Feedback。
```

#### pattern_discovery

```text
查询较长时间范围。
按时间段、category、device_type 聚合。
返回高频模式候选。
```

#### feedback_lookup

```text
优先检索 user_feedback、event_corrections、feedback_memory_rules。
```

#### review_followup

```text
优先检索 llm_reviews。
再关联对应日期 SummaryEvent。
```

------

## 9. QueryContext

### 9.1 QueryContext 结构

```json
{
  "query_id": "uuid",
  "raw_question": "我昨天为什么没写完文档？",
  "query_type": "reason_explanation",
  "date_range": {
    "start": "2026-06-01T00:00:00+09:00",
    "end": "2026-06-01T23:59:59+09:00",
    "label": "昨天"
  },
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
  "evidence": {
    "events": [
      {
        "evidence_id": "event_001",
        "event_id": "uuid",
        "start": "14:03",
        "end": "14:27",
        "device_type": "android",
        "category": "short_video",
        "activity_type": "entertainment",
        "summary": "连续使用 YouTube 约 24 分钟"
      }
    ],
    "reviews": [],
    "feedback": [],
    "memory_rules": []
  },
  "privacy_note": "Only local summaries, reviews and user feedback are included. Raw usage events, screenshots, chat content, keystrokes and file contents are not included."
}
```

### 9.2 限制

为了控制 LLM 输入长度：

```text
events 最多 80 条。
reviews 最多 5 条。
feedback 最多 20 条。
memory_rules 最多 10 条。
总 JSON 字符数默认不超过 12000。
```

如果超出：

```text
优先保留时间最相关、关键词最相关、反馈相关的数据。
```

------

## 10. Query Answer LLM Prompt

### 10.1 System Prompt

```text
你是 Life Debugger 的自然语言查询分析引擎。

你只能基于用户提供的 QueryContext 回答。
不要假装知道 QueryContext 之外的内容。
不要推断 App 内具体内容。
不要推断用户输入内容。
不要推断文件内容。
不要做医学诊断。
不要使用羞辱、指责、道德审判式语言。

回答必须引用具体证据，例如时间段、设备、事件摘要或用户反馈。
如果证据不足，请明确说明“不足以判断”。
请输出严格 JSON，不要输出 Markdown。
```

### 10.2 User Prompt

```text
用户问题：
{RAW_QUESTION}

下面是从 Life Debugger 本地数据库检索出的 QueryContext。
请基于这些证据回答用户问题。

要求：
1. 直接回答问题；
2. 给出支持该回答的证据；
3. 如果存在不确定性，请说明；
4. 不要编造摘要中没有的内容；
5. 不要把计划休息直接说成任务逃避，除非有反馈或上下文支持；
6. 输出 JSON。

QueryContext:
{QUERY_CONTEXT_JSON}
```

------

## 11. Query Answer 输出 Schema

```json
{
  "answer_id": "uuid",
  "direct_answer": "基于昨天的摘要，你没写完文档的可能原因是下午出现了几次从电脑写作到手机娱乐或聊天的较长切换，但现有数据不足以确认真实原因。",
  "key_findings": [
    {
      "id": "finding_001",
      "title": "下午出现较长手机切换",
      "detail": "14:03 - 14:27 期间，设备从 Desktop 写作相关活动切换到 Android 短视频类使用。",
      "confidence": 0.72,
      "evidence_ids": ["event_001", "event_002"]
    }
  ],
  "evidence": [
    {
      "evidence_id": "event_001",
      "type": "summary_event",
      "time_range": "14:03 - 14:27",
      "device_type": "android",
      "summary": "连续使用 YouTube 约 24 分钟"
    }
  ],
  "uncertainties": [
    "摘要中没有直接记录你为什么切换到手机，因此不能确定是任务卡住、计划休息还是其他原因。"
  ],
  "suggested_followups": [
    "14:03 - 14:27 的手机使用是计划休息还是无意识切换？",
    "你想查看昨天所有从电脑到手机的切换吗？"
  ],
  "related_actions": [
    {
      "type": "open_timeline",
      "label": "查看昨天下午时间线",
      "date": "2026-06-01",
      "start_time": 1780251600000,
      "end_time": 1780262400000
    }
  ]
}
```

------

## 12. 查询历史数据库

### 12.1 query_sessions

```sql
CREATE TABLE query_sessions (
  query_id TEXT PRIMARY KEY,
  raw_question TEXT NOT NULL,
  query_type TEXT,
  date_range_json TEXT,
  query_intent_json TEXT,
  query_context_hash TEXT,
  answer_json TEXT,
  provider_id TEXT,
  model TEXT,
  created_on_device_id TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  sync_status TEXT DEFAULT 'local'
);
```

### 12.2 query_evidence_links

```sql
CREATE TABLE query_evidence_links (
  id TEXT PRIMARY KEY,
  query_id TEXT NOT NULL,
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
manual_intent
```

------

## 13. Android 模块设计

新增：

```text
query/
  QueryIntent.kt
  DateRangeParser.kt
  QueryIntentParser.kt
  LocalRetriever.kt
  QueryContextBuilder.kt
  QueryAnswerEngine.kt
  QueryHistoryRepository.kt
  SearchIndexRepository.kt
  QueryPromptBuilder.kt
  QueryOutputParser.kt

ui/
  query/
    QueryScreen.kt
    QueryInputBar.kt
    QueryResultView.kt
    EvidenceCard.kt
    QueryHistoryScreen.kt
    QuerySuggestionChips.kt
    QueryContextPreviewScreen.kt
```

### 13.1 QueryIntentParser

```kotlin
class QueryIntentParser {
    suspend fun parse(question: String): QueryIntent
}
```

### 13.2 LocalRetriever

```kotlin
class LocalRetriever {
    suspend fun retrieve(intent: QueryIntent): RetrievedContext
}
```

### 13.3 QueryAnswerEngine

```kotlin
class QueryAnswerEngine {
    suspend fun answer(question: String): QueryAnswer
}
```

------

## 14. Desktop 模块设计

新增：

```text
src-tauri/src/query/
  query_intent.rs
  date_range_parser.rs
  query_intent_parser.rs
  local_retriever.rs
  query_context_builder.rs
  query_answer_engine.rs
  query_history_repository.rs
  search_index_repository.rs
  query_prompt_builder.rs
  query_output_parser.rs

src-tauri/src/commands/
  query_commands.rs

src/pages/query/
  QueryPage.tsx
  QueryInputBar.tsx
  QueryResultView.tsx
  EvidenceCard.tsx
  QueryHistoryPage.tsx
  QuerySuggestionChips.tsx
  QueryContextPreviewPage.tsx
```

### 14.1 Tauri Commands

```rust
#[tauri::command]
async fn parse_query_intent(question: String) -> Result<QueryIntent, String>;

#[tauri::command]
async fn preview_query_context(question: String) -> Result<QueryContext, String>;

#[tauri::command]
async fn run_natural_language_query(question: String) -> Result<QueryAnswer, String>;

#[tauri::command]
async fn get_query_history(limit: i64) -> Result<Vec<QuerySession>, String>;

#[tauri::command]
async fn delete_query_session(query_id: String) -> Result<(), String>;

#[tauri::command]
async fn rebuild_search_index() -> Result<(), String>;
```

------

## 15. UI 设计

## 15.1 QueryPage

入口：

```text
Sidebar -> Query
BottomNav -> Ask
Review Page -> Ask about this day
Timeline Page -> Ask about selected range
```

页面布局：

```text
顶部：自然语言输入框
中部：快捷问题 chips
下方：查询结果
右侧或折叠区：证据列表
```

快捷问题：

```text
我昨天为什么效率低？
我这周什么时候最容易刷手机？
我最近有哪些任务漂移？
我今天从电脑切到手机几次？
我有哪些计划休息被误判？
```

------

## 15.2 QueryInputBar

功能：

```text
输入自然语言问题
提交查询
清空
语音输入，后续可选
```

状态：

```text
未输入
解析中
检索中
等待 LLM
回答完成
回答失败
```

------

## 15.3 QueryContextPreviewScreen

在发送 LLM 前展示：

```text
将发送给 LLM 的问题
解析出的时间范围
解析出的设备范围
检索到的事件数量
检索到的 Review 数量
检索到的 Feedback 数量
完整 QueryContext JSON
```

用户可以：

```text
确认发送
取消
移除某条证据
隐藏 App 名称
隐藏窗口标题
切换日期范围
```

------

## 15.4 QueryResultView

展示：

```text
直接回答
关键发现
证据卡片
不确定性说明
建议追问
相关操作
```

示例 UI：

```text
问题：我昨天为什么没写完文档？

回答：
基于昨天的摘要，最可能的原因是下午写作任务之后出现了几次较长的手机切换。不过摘要不足以判断这些切换是任务卡住、计划休息还是工作沟通。

关键证据：
- 14:03 - 14:27 Android：短视频类使用 24 分钟
- 15:10 - 15:28 Desktop：浏览器使用 18 分钟
- 16:02 - 16:18 Android：聊天类使用 16 分钟

不确定性：
没有手动意图记录，因此不能确认真实原因。
```

------

## 15.5 EvidenceCard

每个证据卡片显示：

```text
证据类型
时间范围
设备
类别
摘要
来源
```

操作：

```text
跳转到时间线
标记为相关
标记为不相关
添加反馈
```

------

## 15.6 QueryHistoryScreen

展示：

```text
历史问题
查询时间
查询类型
使用的模型
是否有答案
```

操作：

```text
重新打开
重新查询
删除
```

------

## 16. 查询结果反馈

第七阶段可以复用第六阶段反馈系统。

对 QueryAnswer 支持反馈：

```text
回答有用
回答没用
证据不相关
解释过度
遗漏了重要事件
```

保存为 user_feedback：

```text
target_type = query_answer
target_id = answer_id
```

这会帮助后续查询更准确。

------

## 17. 查询同步

查询历史可以本地保存。

MVP 默认：

```text
query_sessions 不自动同步。
```

建议：

```text
主分析设备保存查询历史。
非主设备可选择是否同步查询历史。
```

第七阶段可先不强制同步 query_sessions，避免隐私复杂度增加。

------

## 18. 隐私设计

### 18.1 查询前提示

首次使用 Query 功能时显示：

```text
自然语言查询会检索你的本地摘要数据。
如果你启用云端 LLM，系统会把本次查询相关的摘要上下文发送给你配置的模型服务商。
不会发送 raw events、截图、聊天内容、键盘输入或文件内容。
```

### 18.2 QueryContext Preview

默认开启：

```text
每次发送 LLM 前预览 QueryContext。
```

用户可以设置：

```text
每次查询前预览
只在第一次预览
直接发送摘要上下文
```

默认：

```text
每次查询前预览。
```

------

## 19. 开发顺序

### Step 1：数据库迁移

任务：

```text
1. 新增 query_sessions。
2. 新增 query_evidence_links。
3. 新增 summary_events_fts。
4. 新增 llm_reviews_fts。
5. 新增 feedback_fts。
6. 新增 feedback_memory_rules_fts。
```

验收：

```text
数据库迁移成功。
已有 SummaryEvent / Review / Feedback 不受影响。
```

------

### Step 2：构建搜索索引

任务：

```text
1. 实现 SearchIndexRepository。
2. 将 summary_events 写入 FTS。
3. 将 llm_reviews 扁平化写入 FTS。
4. 将 user_feedback 写入 FTS。
5. 将 feedback_memory_rules 写入 FTS。
6. 实现 rebuild_search_index。
```

验收：

```text
用户可以通过关键词搜索到相关 SummaryEvent 和 Review。
```

------

### Step 3：实现 DateRangeParser

任务：

```text
1. 支持今天、昨天、前天。
2. 支持本周、上周、最近 7 天。
3. 支持上午、下午、晚上。
4. 支持组合表达：昨天下午、今天晚上。
5. 默认今天。
```

验收：

```text
“我昨天下午做了什么”能解析出正确时间范围。
```

------

### Step 4：实现 QueryIntentParser

任务：

```text
1. 根据关键词判断 query_type。
2. 提取 device_filter。
3. 提取 category_filter。
4. 提取 keywords。
5. 判断是否 requires_llm。
6. 支持 LLM 辅助解析，作为可选增强。
```

验收：

```text
“我这周什么时候最容易刷手机”解析为 pattern_discovery。
“昨天复盘里说的任务漂移是什么”解析为 review_followup。
```

------

### Step 5：实现 LocalRetriever

任务：

```text
1. 根据 QueryIntent 查询 summary_events。
2. 查询 llm_reviews。
3. 查询 user_feedback。
4. 查询 feedback_memory_rules。
5. 查询 manual_intent_events。
6. 控制返回数量。
```

验收：

```text
输入一个问题后，系统能返回相关本地证据集合。
```

------

### Step 6：实现 QueryContextBuilder

任务：

```text
1. 将检索结果转换为 QueryContext。
2. 给每条证据分配 evidence_id。
3. 控制上下文大小。
4. 加入 privacy_note。
5. 支持 QueryContext JSON 预览。
```

验收：

```text
Preview 页面能展示即将发送给 LLM 的 QueryContext。
```

------

### Step 7：实现 QueryPromptBuilder

任务：

```text
1. 实现查询 system prompt。
2. 实现查询 user prompt。
3. 注入 raw_question。
4. 注入 QueryContext JSON。
5. 要求严格 JSON 输出。
```

验收：

```text
生成的 prompt 不包含 raw events，只包含摘要上下文。
```

------

### Step 8：实现 QueryAnswerEngine

任务：

```text
1. 检查 LLM 配置。
2. 构造 QueryContext。
3. 调用 LLM。
4. 解析 QueryAnswer JSON。
5. 保存 query_sessions。
6. 保存 query_evidence_links。
```

验收：

```text
用户输入问题后，系统能返回结构化回答。
```

------

### Step 9：实现 Query UI

任务：

```text
1. QueryPage。
2. QueryInputBar。
3. QuerySuggestionChips。
4. QueryContextPreviewScreen。
5. QueryResultView。
6. EvidenceCard。
7. QueryHistoryScreen。
```

验收：

```text
用户可以完整完成：
输入问题 -> 预览上下文 -> 调用 LLM -> 查看回答 -> 点击证据跳转时间线。
```

------

### Step 10：实现本地搜索模式

任务：

```text
1. LLM 未配置时，仍可搜索本地事件。
2. 返回本地匹配结果。
3. 不生成解释型回答。
4. 提示用户配置 LLM 可获得自然语言解释。
```

验收：

```text
没有 LLM API Key 时，用户仍能搜索“短视频”“写作”“聊天”。
```

------

### Step 11：实现查询结果反馈

任务：

```text
1. QueryAnswer 支持“有用 / 没用 / 证据不相关”。
2. 保存为 user_feedback。
3. target_type = query_answer。
4. 后续查询可检索这些反馈。
```

验收：

```text
用户可以反馈某次查询回答是否有用。
```

------

### Step 12：错误处理和 UI 打磨

任务：

```text
1. 无数据提示。
2. 日期范围无结果提示。
3. LLM 未配置提示。
4. LLM 调用失败提示。
5. JSON 解析失败提示。
6. 查询上下文过大提示。
7. 隐私预览提示。
```

验收：

```text
普通用户能理解查询失败原因。
```

------

## 20. 测试计划

### 20.1 单元测试

测试对象：

```text
DateRangeParser
QueryIntentParser
LocalRetriever
QueryContextBuilder
QueryPromptBuilder
QueryOutputParser
SearchIndexRepository
```

测试场景：

```text
今天 / 昨天 / 本周 / 最近 7 天
昨天下午
手机筛选
电脑筛选
短视频分类筛选
反馈查询
复盘追问
无数据
QueryContext 限长
LLM 返回 Markdown 包裹 JSON
LLM 返回非法 JSON
```

------

### 20.2 集成测试

场景一：时间线查询

```text
用户问：我昨天下午做了什么？
系统检索昨天下午 SummaryEvent。
LLM 返回按时间段组织的回答。
```

场景二：原因解释

```text
用户问：我昨天为什么没写完文档？
系统检索写作、浏览器、手机切换相关事件。
LLM 返回基于证据的谨慎解释。
```

场景三：模式发现

```text
用户问：我这周什么时候最容易刷手机？
系统检索最近 7 天 Android 娱乐/短视频事件。
LLM 总结高频时间段。
```

场景四：反馈查询

```text
用户问：我这周把哪些判断标记为误判？
系统检索 user_feedback。
返回误判列表和对应时间段。
```

------

## 21. 手动测试流程

```text
1. 完成前六阶段基础数据积累。
2. 打开 Query 页面。
3. 输入：我昨天做了什么？
4. 查看 QueryContext Preview。
5. 确认发送。
6. 查看回答和证据。
7. 点击证据跳转 Timeline。
8. 输入：我这周什么时候最容易刷手机？
9. 查看模式分析结果。
10. 输入：我有哪些计划休息反馈？
11. 查看反馈检索结果。
12. 在无 LLM 配置时测试本地搜索模式。
```

------

## 22. 第七阶段验收清单

### 22.1 功能验收

```text
[ ] 有 Query 页面
[ ] 支持自然语言输入
[ ] 支持快捷问题
[ ] 支持 DateRangeParser
[ ] 支持 QueryIntentParser
[ ] 支持 SQLite FTS 搜索
[ ] 支持 SummaryEvent 检索
[ ] 支持 LlmReview 检索
[ ] 支持 UserFeedback 检索
[ ] 支持 FeedbackMemoryRule 检索
[ ] 支持 QueryContext 构造
[ ] 支持 QueryContext Preview
[ ] 支持 LLM 查询回答
[ ] 支持证据卡片
[ ] 支持证据跳转时间线
[ ] 支持查询历史
[ ] 支持本地搜索模式
[ ] 支持查询结果反馈
```

### 22.2 非功能验收

```text
[ ] 不读取 raw_usage_events
[ ] 不读取 raw_desktop_events
[ ] 不读取截图
[ ] 不读取聊天内容
[ ] 不读取键盘输入
[ ] 不读取文件内容
[ ] QueryContext 可预览
[ ] LLM 输入只包含摘要证据
[ ] 证据不足时回答必须说明不确定性
[ ] 不上传数据到开发者服务器
[ ] LLM 调用失败时不编造回答
```

------

## 23. 第七阶段 Codex 执行提示词

可以直接把下面内容给 Codex：

```text
请根据《Life Debugger 第七阶段编程 Plan：自然语言查询 + 时间线搜索》实现第七阶段功能。

严格要求：

1. 第七阶段实现自然语言查询和时间线搜索。
2. 不实现周报/月报。
3. 不实现云端账号。
4. 不上传数据到开发者服务器。
5. 查询只能基于 SummaryEvent、LlmReview、UserFeedback、EventCorrection、FeedbackMemoryRule、ManualIntent。
6. 不允许查询 raw_usage_events。
7. 不允许查询 raw_desktop_events。
8. 不允许读取截图、聊天内容、键盘输入或文件内容。
9. 必须实现 Query 页面。
10. 必须实现 SQLite FTS 搜索。
11. 必须实现 DateRangeParser。
12. 必须实现 QueryIntentParser。
13. 必须实现 LocalRetriever。
14. 必须实现 QueryContextBuilder。
15. LLM 输入必须可预览。
16. LLM 回答必须引用证据。
17. 证据不足时必须说明不确定性。
18. LLM 未配置时，只展示本地搜索结果，不生成解释型回答。
19. 必须保存查询历史。
20. 查询结果中的证据必须能跳转到 Timeline。
```

------

## 24. 第七阶段最终 Demo

目标 Demo：

```text
1. 用户已经使用 Life Debugger 多天。
2. 系统已有 Android + Desktop SummaryEvent。
3. 系统已有跨设备 Review。
4. 系统已有用户反馈。
5. 用户打开 Query 页面。
6. 用户输入：我昨天为什么没写完文档？
7. 系统解析出：昨天、reason_explanation、写作相关。
8. 系统检索昨天写作、浏览器、手机切换、反馈数据。
9. 用户预览 QueryContext。
10. 用户确认发送。
11. LLM 返回基于证据的回答。
12. 用户点击证据，跳转到对应时间线。
```

示例回答：

```text
基于昨天的摘要，你没写完文档的可能原因不是单一事件，而是下午出现了几次较长的跨设备切换。

关键证据：
1. 14:03 - 14:27，Android 端出现短视频类使用 24 分钟。
2. 15:10 - 15:28，Desktop 端从写作切换到浏览器使用。
3. 16:02 - 16:18，Android 端出现聊天类使用 16 分钟。

不过，摘要中没有记录你当时的真实意图，因此不能确定这些切换是任务卡住、计划休息还是工作需要。建议你下次在从文档切到手机前记录一个快速意图。
```

演示文案：

```text
Life Debugger 不只是每天给你一份复盘。
你可以直接追问自己的行为数据：
“我昨天为什么没写完文档？”
“我这周什么时候最容易刷手机？”
“哪些判断我经常纠正？”
它会基于本地摘要和你的反馈，给出可追溯的回答。
```

------

## 25. 第七阶段完成后的下一步

第七阶段完成后，进入第八阶段：

```text
周报 / 月报 / 长期趋势
```

第八阶段将复用：

```text
SummaryEvent
CrossDeviceTimeline
LlmReview
UserFeedback
FeedbackMemoryRule
QuerySession
QueryEvidenceLinks
```

第八阶段可以实现：

```text
每周手机使用趋势
每周电脑专注趋势
任务漂移趋势
计划休息趋势
跨设备切换趋势
LLM 判断准确率趋势
查询问题趋势
```

第七阶段需要为第八阶段保留：

```text
query_type
date_range_json
query_context_hash
query_evidence_links
answer_json
user_feedback on query_answer
```

这些字段将帮助第八阶段知道用户最关心什么问题。