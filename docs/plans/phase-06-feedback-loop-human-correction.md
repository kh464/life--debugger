# Life Debugger 第六阶段编程 Plan：反馈闭环 + 人类校正

## 0. 阶段目标

第六阶段实现 Life Debugger 的反馈闭环能力。

前五阶段已经实现：

```text
1. Android 单端闭环
2. Desktop 单端闭环
3. 手机电脑配对
4. 局域网 SummaryEvent 摘要同步
5. 主分析设备切换 + 跨设备 LLM 复盘
```

第六阶段要解决的问题是：

```text
LLM 生成的复盘不一定完全准确。
用户必须能够纠正 LLM 的判断。
系统必须记住用户的纠正。
后续复盘必须参考这些纠正，减少重复误判。
```

第六阶段完成后，用户可以对每条 LLM 分析做反馈：

```text
准确
不准确
这是计划休息
这是工作沟通
这是任务逃避
这是查资料
这是误判
以后类似情况忽略
```

系统会把这些反馈保存到本地，并在后续 LLM Prompt 中加入相关历史反馈，使 Life Debugger 越用越懂用户。

------

## 1. 阶段边界

### 1.1 本阶段必须实现

```text
1. Review 页面支持对每条 LLM 判断反馈。
2. 支持对 attention_shifts 反馈。
3. 支持对 possible_task_drift 反馈。
4. 支持对 possible_planned_breaks 反馈。
5. 支持对 suggestions 反馈。
6. 支持对 questions_for_user 回答。
7. 支持修改 SummaryEvent 分类。
8. 支持标记某个时间段为计划休息。
9. 支持标记某个时间段为工作需要。
10. 支持标记某个时间段为误判。
11. 保存用户反馈到本地数据库。
12. 后续 LLM 分析时注入相关历史反馈。
13. 主分析设备生成复盘时使用反馈。
14. 非主设备可以展示反馈状态。
15. 反馈可以在设备之间同步。
16. 反馈不会发送给开发者服务器。
17. 反馈不会用于训练云端模型。
```

### 1.2 本阶段建议实现

```text
1. 反馈标签自动聚合。
2. 类似事件匹配。
3. 用户反馈统计页面。
4. 反馈撤销。
5. 反馈编辑。
6. 反馈同步历史。
7. 事件分类修正历史。
8. LLM 判断准确率统计。
```

### 1.3 本阶段不实现

```text
1. 自然语言查询。
2. 周报 / 月报。
3. 长期趋势分析。
4. 本地向量搜索。
5. 自动训练用户个人模型。
6. 云端账号。
7. 开发者服务器同步。
8. 多用户协作。
```

------

## 2. 核心概念

### 2.1 Feedback

Feedback 是用户对 LLM 判断或 SummaryEvent 的显式反馈。

反馈对象可以是：

```text
SummaryEvent
attention_shift
possible_task_drift
possible_planned_break
suggestion
question_for_user
daily_summary
```

### 2.2 Correction

Correction 是用户对系统事件理解的修正。

例如：

```text
系统认为：14:03 - 14:27 是任务漂移。
用户修正：这是计划休息。
```

这条 correction 后续应影响类似情况。

### 2.3 Feedback Memory

Feedback Memory 是本地保存的用户反馈记忆。

它不是模型训练数据。

它只是后续构造 LLM Prompt 时注入的上下文。

例如：

```text
用户多次反馈：午饭后 12:30 - 13:00 的手机娱乐是计划休息。
后续 LLM 分析午饭后娱乐时，应更谨慎，不要直接判断为任务漂移。
```

------

## 3. 用户反馈类型

### 3.1 判断反馈 Judgment Feedback

用于评价 LLM 某条判断是否准确。

```text
accurate
inaccurate
partially_accurate
not_sure
```

中文 UI：

```text
准确
不准确
部分准确
不确定
```

### 3.2 语义修正 Semantic Correction

用于解释该时间段真实含义。

```text
planned_break
work_communication
study_related
research_related
task_escape
entertainment
life_service
mistake
ignore_similar
other
```

中文 UI：

```text
这是计划休息
这是工作沟通
这是学习相关
这是查资料
这是任务逃避
这是娱乐放松
这是生活事务
这是误判
以后类似情况忽略
其他
```

### 3.3 建议反馈 Suggestion Feedback

用于评价 LLM 给出的建议。

```text
useful
not_useful
already_doing
too_strict
too_vague
not_relevant
```

中文 UI：

```text
有用
没用
我已经在做
太严格
太空泛
不相关
```

### 3.4 问题回答 Question Answer

用于回答 LLM 提出的问题。

例如 LLM 问：

```text
14:03 - 14:27 的手机短视频使用是计划休息，还是写作卡住后的切换？
```

用户回答：

```text
这是计划休息。
```

该回答要变成 Feedback Memory。

------

## 4. 数据库设计

Android 与 Desktop 都需要增加相同结构。

### 4.1 user_feedback

保存用户对某条分析或事件的反馈。

```sql
CREATE TABLE user_feedback (
  feedback_id TEXT PRIMARY KEY,
  review_id TEXT,
  target_type TEXT NOT NULL,
  target_id TEXT NOT NULL,
  target_time_start INTEGER,
  target_time_end INTEGER,
  origin_device_id TEXT,
  origin_device_type TEXT,
  judgment TEXT,
  correction_type TEXT,
  correction_text TEXT,
  confidence REAL DEFAULT 1.0,
  applies_to_similar INTEGER DEFAULT 0,
  created_on_device_id TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  sync_status TEXT DEFAULT 'local',
  metadata_json TEXT
);
```

字段说明：

```text
feedback_id：反馈唯一 ID。
review_id：关联的复盘 ID，可为空。
target_type：反馈对象类型。
target_id：反馈对象 ID。
target_time_start：反馈涉及的时间段开始。
target_time_end：反馈涉及的时间段结束。
origin_device_id：被反馈事件来源设备。
origin_device_type：android / desktop。
judgment：准确、不准确、部分准确等。
correction_type：计划休息、工作沟通、任务逃避等。
correction_text：用户补充说明。
confidence：用户反馈可信度，默认 1.0。
applies_to_similar：是否适用于类似情况。
created_on_device_id：用户在哪台设备上创建反馈。
sync_status：反馈同步状态。
metadata_json：保留扩展字段。
```

target_type 可选值：

```text
summary_event
attention_shift
possible_task_drift
possible_planned_break
suggestion
question_for_user
daily_summary
```

judgment 可选值：

```text
accurate
inaccurate
partially_accurate
not_sure
```

correction_type 可选值：

```text
planned_break
work_communication
study_related
research_related
task_escape
entertainment
life_service
mistake
ignore_similar
other
```

sync_status 可选值：

```text
local
pending
syncing
synced
failed
ignored
```

------

### 4.2 event_corrections

保存用户对 SummaryEvent 分类或语义的直接修正。

```sql
CREATE TABLE event_corrections (
  correction_id TEXT PRIMARY KEY,
  event_id TEXT NOT NULL,
  original_category TEXT,
  original_activity_type TEXT,
  corrected_category TEXT,
  corrected_activity_type TEXT,
  original_summary TEXT,
  corrected_summary TEXT,
  correction_reason TEXT,
  applies_to_similar INTEGER DEFAULT 0,
  created_on_device_id TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  sync_status TEXT DEFAULT 'local'
);
```

示例：

```text
原分类：short_video / entertainment
用户修正：planned_break / rest
修正原因：午休时间计划休息
```

------

### 4.3 feedback_memory_rules

将用户反馈沉淀为可复用规则。

```sql
CREATE TABLE feedback_memory_rules (
  rule_id TEXT PRIMARY KEY,
  rule_type TEXT NOT NULL,
  condition_json TEXT NOT NULL,
  interpretation TEXT NOT NULL,
  source_feedback_ids TEXT NOT NULL,
  confidence REAL DEFAULT 0.7,
  enabled INTEGER DEFAULT 1,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
```

rule_type 可选值：

```text
time_pattern
category_pattern
device_switch_pattern
app_pattern
manual_rule
ignore_pattern
```

示例 condition_json：

```json
{
  "time_range": "12:00-13:30",
  "category": "short_video",
  "device_type": "android"
}
```

示例 interpretation：

```text
用户多次反馈午饭后的手机娱乐是计划休息，后续不要直接判断为任务漂移。
```

------

### 4.4 llm_reviews 增强

llm_reviews 增加反馈统计字段。

```sql
ALTER TABLE llm_reviews ADD COLUMN feedback_count INTEGER DEFAULT 0;
ALTER TABLE llm_reviews ADD COLUMN has_user_feedback INTEGER DEFAULT 0;
ALTER TABLE llm_reviews ADD COLUMN feedback_summary_json TEXT;
```

如果字段已存在则跳过。

------

## 5. 数据模型

### 5.1 UserFeedback

Kotlin / TypeScript / Rust 都需要对应模型。

```json
{
  "feedback_id": "uuid",
  "review_id": "review_uuid",
  "target_type": "possible_task_drift",
  "target_id": "drift_001",
  "target_time_start": 1780303380000,
  "target_time_end": 1780304820000,
  "origin_device_id": "android_phone_001",
  "origin_device_type": "android",
  "judgment": "inaccurate",
  "correction_type": "planned_break",
  "correction_text": "这是我午休时计划刷视频，不是任务逃避。",
  "confidence": 1.0,
  "applies_to_similar": true,
  "created_on_device_id": "desktop_001",
  "created_at": 1780307000000,
  "updated_at": 1780307000000,
  "sync_status": "pending"
}
```

### 5.2 EventCorrection

```json
{
  "correction_id": "uuid",
  "event_id": "event_uuid",
  "original_category": "short_video",
  "original_activity_type": "entertainment",
  "corrected_category": "planned_break",
  "corrected_activity_type": "rest",
  "original_summary": "连续使用 YouTube 约 24 分钟。",
  "corrected_summary": "午休期间计划观看视频约 24 分钟。",
  "correction_reason": "用户反馈这是计划休息。",
  "applies_to_similar": true,
  "created_on_device_id": "desktop_001",
  "created_at": 1780307000000,
  "updated_at": 1780307000000,
  "sync_status": "pending"
}
```

### 5.3 FeedbackMemoryRule

```json
{
  "rule_id": "uuid",
  "rule_type": "time_pattern",
  "condition": {
    "time_range": "12:00-13:30",
    "category": "short_video",
    "device_type": "android"
  },
  "interpretation": "用户多次反馈午饭后的短视频使用是计划休息，后续分析时应优先视为计划休息，而不是任务漂移。",
  "source_feedback_ids": ["feedback_001", "feedback_002"],
  "confidence": 0.8,
  "enabled": true
}
```

------

## 6. 反馈对象 ID 设计

LLM 输出中的每条分析都必须有稳定 ID。

第五阶段输出可能是：

```json
{
  "cross_device_attention_shifts": [
    {
      "time_range": "14:03 - 14:27",
      "from_device": "desktop",
      "to_device": "android",
      "interpretation": "...",
      "confidence": 0.72
    }
  ]
}
```

第六阶段需要补充 ID：

```json
{
  "cross_device_attention_shifts": [
    {
      "id": "attention_shift_001",
      "time_range": "14:03 - 14:27",
      "start_time": 1780303380000,
      "end_time": 1780304820000,
      "from_device": "desktop",
      "to_device": "android",
      "interpretation": "...",
      "confidence": 0.72
    }
  ]
}
```

要求：

```text
1. LLM 输出 schema 中每个可反馈条目都要有 id。
2. 如果 LLM 没有返回 id，解析器自动生成。
3. id 在同一个 review 内稳定。
4. target_id = 条目 id。
```

需要支持 ID 的字段：

```text
attention_shifts
cross_device_attention_shifts
possible_task_drift
possible_planned_breaks
suggestions
questions_for_user
time_distribution
device_distribution
```

------

## 7. LLM 输出 Schema 增强

第六阶段更新 Review JSON Schema。

### 7.1 attention_shifts

```json
{
  "id": "attention_shift_001",
  "time_range": "14:03 - 14:27",
  "start_time": 1780303380000,
  "end_time": 1780304820000,
  "from_device": "desktop",
  "from_activity": "writing",
  "to_device": "android",
  "to_activity": "short_video",
  "interpretation": "可能发生了从文档写作到手机娱乐的注意力漂移，也可能是计划休息",
  "confidence": 0.72
}
```

### 7.2 suggestions

```json
{
  "id": "suggestion_001",
  "title": "给跨设备切换添加意图标记",
  "detail": "当你从电脑切到手机前，可以记录是休息、沟通还是逃避任务。",
  "related_time_range": "14:03 - 14:27"
}
```

### 7.3 questions_for_user

```json
{
  "id": "question_001",
  "question": "14:03 - 14:27 的手机短视频使用是计划休息，还是写作卡住后的切换？",
  "related_time_range": "14:03 - 14:27",
  "start_time": 1780303380000,
  "end_time": 1780304820000
}
```

------

## 8. UI 设计

## 8.1 Review 页面反馈交互

每条 LLM 判断下方显示反馈按钮。

### attention_shift 卡片

展示：

```text
14:03 - 14:27
从 Desktop 写作 切换到 Android 短视频
LLM 判断：可能是注意力漂移，也可能是计划休息
置信度：72%
```

反馈按钮：

```text
准确
不准确
这是计划休息
这是工作需要
这是误判
以后类似情况忽略
```

点击后弹出可选补充：

```text
是否适用于类似情况？
[ ] 只修正这一次
[ ] 以后类似情况也这样理解

补充说明：
[文本输入框]
```

### possible_task_drift 卡片

反馈按钮：

```text
确实是任务逃避
不是任务逃避
这是计划休息
这是工作沟通
这是查资料
以后类似情况忽略
```

### suggestion 卡片

反馈按钮：

```text
有用
没用
太严格
太空泛
不相关
我已经在做
```

### question_for_user 卡片

展示：

```text
LLM 想确认：
14:03 - 14:27 的手机短视频使用是计划休息，还是写作卡住后的切换？
```

快捷回答：

```text
计划休息
任务卡住
工作需要
查资料
无意识打开
其他
```

------

## 8.2 Timeline 页面事件修正

Timeline 每条 SummaryEvent 增加操作：

```text
修改分类
标记为计划休息
标记为工作需要
标记为误判
不进入 LLM
以后类似事件也这样处理
```

用户修改分类时：

```text
原分类：short_video / entertainment
新分类：planned_break / rest
原因：午休期间计划休息
是否应用到类似事件：是 / 否
```

------

## 8.3 Feedback Center 页面

新增页面：

```text
Feedback Center / 反馈中心
```

入口：

```text
Settings -> Feedback Center
Review Page -> 查看全部反馈
```

展示：

```text
反馈总数
已应用到后续分析的反馈数
被标记为计划休息的时间段
被标记为误判的判断
用户自定义规则
LLM 判断准确率
```

列表字段：

```text
反馈时间
反馈对象
原判断
用户修正
是否应用到类似情况
是否已同步
```

操作：

```text
编辑反馈
撤销反馈
禁用反馈规则
删除反馈
```

------

## 8.4 LLM Payload Preview 增强

预览页新增：

```text
将注入的历史反馈
将应用的用户修正规则
被排除的事件
被重新解释的事件
```

示例：

```text
本次分析将参考 3 条历史反馈：

1. 用户多次反馈 12:00 - 13:30 的短视频使用通常是计划休息。
2. 用户反馈微信在工作日 10:00 - 18:00 主要用于工作沟通。
3. 用户反馈晚间 21:00 后的视频使用通常是主动娱乐，不应标记为任务漂移。
```

------

## 9. 反馈记忆生成逻辑

### 9.1 单条反馈保存

用户点击反馈后，立即保存到 user_feedback。

如果用户勾选“以后类似情况也这样理解”：

```text
applies_to_similar = true
```

系统尝试生成 feedback_memory_rules。

### 9.2 规则生成

规则生成可以先用确定性逻辑，不需要 LLM。

规则类型：

#### time_pattern

适用于：

```text
用户多次反馈某个时间段的某类行为。
```

例如：

```text
12:00 - 13:30
Android
short_video
planned_break
```

#### category_pattern

适用于：

```text
用户多次反馈某类 App 用途。
```

例如：

```text
chat 类 App 在工作日白天多为工作沟通。
```

#### device_switch_pattern

适用于：

```text
用户多次反馈从电脑到手机的某类切换。
```

例如：

```text
Desktop writing -> Android chat 通常是工作沟通。
```

#### ignore_pattern

适用于：

```text
用户选择以后类似情况忽略。
```

例如：

```text
晚间 22:00 后的娱乐不再标记为任务漂移。
```

------

## 10. 类似事件匹配

### 10.1 SimilarityMatcher

实现一个轻量匹配器。

输入：

```text
当前 SummaryEvent 或 LLM 分析条目
历史 feedback_memory_rules
```

输出：

```text
相关反馈记忆列表
```

匹配维度：

```text
时间段
设备类型
category
activity_type
app_label
from_device
to_device
from_activity
to_activity
weekday / weekend
```

### 10.2 匹配规则

示例：

```text
如果当前事件 category = short_video
且时间在 12:00 - 13:30
且历史规则中存在 planned_break
则注入该反馈规则。
```

不要过度匹配。

MVP 限制：

```text
最多注入 10 条历史反馈。
只注入 confidence >= 0.6 的规则。
只注入 enabled = true 的规则。
优先注入时间最近、匹配度最高的规则。
```

------

## 11. LLM Prompt 增强

第六阶段需要让 LLM 使用用户反馈。

### 11.1 Feedback Context

构造 LLMAnalysisPackage 时增加：

```json
{
  "feedback_context": [
    {
      "rule_type": "time_pattern",
      "condition": "12:00-13:30, android, short_video",
      "interpretation": "用户反馈午饭后的短视频使用通常是计划休息，不应直接判断为任务漂移。",
      "confidence": 0.8
    },
    {
      "rule_type": "category_pattern",
      "condition": "weekday daytime, chat",
      "interpretation": "用户反馈工作日白天的聊天类 App 多数是工作沟通。",
      "confidence": 0.75
    }
  ]
}
```

### 11.2 System Prompt 增强

在原 system prompt 中加入：

```text
你会收到用户过去对你分析结果的反馈记忆。
这些反馈只代表用户自己的解释偏好，不是普遍规律。
当当前事件与反馈记忆匹配时，应优先参考用户反馈。
如果反馈记忆与当前摘要矛盾，请说明不确定性，不要强行套用。
```

### 11.3 User Prompt 增强

加入：

```text
请结合 feedback_context 分析。
如果某个判断受到用户历史反馈影响，请在对应条目的 interpretation 中体现不确定性。
不要因为历史反馈而忽略当前摘要事实。
```

### 11.4 输出 Schema 增强

每个判断增加：

```json
{
  "used_feedback_rule_ids": ["rule_001"],
  "feedback_influence": "用户过去反馈午饭后的短视频通常是计划休息，因此本次判断更倾向于计划休息而非任务漂移。"
}
```

------

## 12. 反馈同步设计

第六阶段需要让反馈在主分析设备和非主设备之间同步。

### 12.1 FeedbackSyncBundle

```json
{
  "protocol": "lifedbg-feedback-sync-v1",
  "bundle_id": "uuid",
  "from_device_id": "desktop_001",
  "to_device_id": "android_phone_001",
  "generated_at": 1780308000000,
  "feedback_count": 3,
  "feedback_items": [],
  "event_correction_count": 1,
  "event_corrections": [],
  "memory_rule_count": 2,
  "memory_rules": []
}
```

### 12.2 POST /api/feedback/sync

接收端接口：

```http
POST /api/feedback/sync
```

校验：

```text
1. protocol = lifedbg-feedback-sync-v1。
2. from_device_id 是已配对设备。
3. to_device_id 是本机设备。
4. feedback_id 幂等去重。
5. correction_id 幂等去重。
6. rule_id 幂等去重。
```

响应：

```json
{
  "status": "ok",
  "protocol": "lifedbg-feedback-sync-v1",
  "bundle_id": "uuid",
  "received_feedback_count": 3,
  "stored_feedback_count": 2,
  "deduplicated_feedback_count": 1,
  "server_time": 1780308003000
}
```

### 12.3 同步方向

默认：

```text
反馈在哪台设备创建，就从该设备同步到已配对设备。
```

如果存在主分析设备：

```text
主分析设备必须接收所有反馈。
因为主分析设备负责后续 LLM 分析。
```

------

## 13. Android 模块设计

新增或完善：

```text
feedback/
  UserFeedback.kt
  EventCorrection.kt
  FeedbackMemoryRule.kt
  FeedbackRepository.kt
  EventCorrectionRepository.kt
  FeedbackMemoryRepository.kt
  FeedbackRuleGenerator.kt
  SimilarityMatcher.kt
  FeedbackSyncClient.kt
  FeedbackSyncServer.kt
  FeedbackPayloadBuilder.kt

ui/
  feedback/
    FeedbackButtonRow.kt
    FeedbackDialog.kt
    FeedbackCenterScreen.kt
    FeedbackRuleListScreen.kt
```

### 13.1 FeedbackRepository

```kotlin
class FeedbackRepository {
    suspend fun saveFeedback(feedback: UserFeedback)
    suspend fun getFeedbackForReview(reviewId: String): List<UserFeedback>
    suspend fun getFeedbackForTarget(targetType: String, targetId: String): List<UserFeedback>
    suspend fun deleteFeedback(feedbackId: String)
    suspend fun getPendingSyncFeedback(): List<UserFeedback>
}
```

### 13.2 FeedbackRuleGenerator

```kotlin
class FeedbackRuleGenerator {
    suspend fun generateRuleFromFeedback(feedback: UserFeedback): FeedbackMemoryRule?
}
```

### 13.3 SimilarityMatcher

```kotlin
class SimilarityMatcher {
    suspend fun findRelevantRules(events: List<SummaryEvent>): List<FeedbackMemoryRule>
}
```

------

## 14. Desktop 模块设计

新增或完善：

```text
src-tauri/src/feedback/
  user_feedback.rs
  event_correction.rs
  feedback_memory_rule.rs
  feedback_repository.rs
  event_correction_repository.rs
  feedback_memory_repository.rs
  feedback_rule_generator.rs
  similarity_matcher.rs
  feedback_sync_client.rs
  feedback_sync_server.rs
  feedback_payload_builder.rs

src-tauri/src/commands/
  feedback_commands.rs

src/pages/feedback/
  FeedbackCenterPage.tsx
  FeedbackDialog.tsx
  FeedbackButtonRow.tsx
  FeedbackRuleListPage.tsx
```

### 14.1 Tauri Commands

```rust
#[tauri::command]
async fn save_user_feedback(feedback: UserFeedback) -> Result<(), String>;

#[tauri::command]
async fn get_feedback_for_review(review_id: String) -> Result<Vec<UserFeedback>, String>;

#[tauri::command]
async fn delete_user_feedback(feedback_id: String) -> Result<(), String>;

#[tauri::command]
async fn get_feedback_memory_rules() -> Result<Vec<FeedbackMemoryRule>, String>;

#[tauri::command]
async fn update_feedback_memory_rule_enabled(
    rule_id: String,
    enabled: bool
) -> Result<(), String>;

#[tauri::command]
async fn build_feedback_context_for_date(date: String) -> Result<Vec<FeedbackMemoryRule>, String>;

#[tauri::command]
async fn sync_feedback_to_paired_devices() -> Result<(), String>;
```

------

## 15. LLM Payload Builder 修改

Android 和 Desktop 的 Payload Builder 都要修改：

```text
SingleDeviceLlmPayloadBuilder
CrossDeviceLlmPayloadBuilder
```

新增步骤：

```text
1. 读取当天 SummaryEvent。
2. 读取用户隐私设置。
3. 读取相关 feedback_memory_rules。
4. 读取当天直接相关 user_feedback。
5. 构造 feedback_context。
6. 注入 LLMAnalysisPackage。
```

MVP 规则：

```text
每次最多注入 10 条 feedback_memory_rules。
每条规则最多 200 字。
总 feedback_context 不超过 1500 字。
```

避免：

```text
不要把全部历史反馈无脑塞给 LLM。
不要把用户很长的私密备注全部塞给 LLM。
不要注入 disabled 的规则。
```

------

## 16. 开发顺序

### Step 1：数据库迁移

任务：

```text
1. Android 新增 user_feedback 表。
2. Android 新增 event_corrections 表。
3. Android 新增 feedback_memory_rules 表。
4. Desktop 新增 user_feedback 表。
5. Desktop 新增 event_corrections 表。
6. Desktop 新增 feedback_memory_rules 表。
7. llm_reviews 增加反馈统计字段。
```

验收：

```text
数据库迁移成功。
旧 Review 和 SummaryEvent 不受影响。
```

------

### Step 2：LLM 输出条目 ID 支持

任务：

```text
1. 修改 LLM 输出 Schema。
2. 更新 LlmOutputParser。
3. 如果 LLM 没返回 id，自动生成 id。
4. Review 页面使用这些 id 作为反馈 target_id。
```

验收：

```text
Review 中每条可反馈分析都有稳定 target_id。
```

------

### Step 3：Review 页面添加反馈按钮

任务：

```text
1. attention_shift 卡片添加反馈按钮。
2. possible_task_drift 卡片添加反馈按钮。
3. possible_planned_break 卡片添加反馈按钮。
4. suggestion 卡片添加反馈按钮。
5. question_for_user 卡片添加快捷回答。
```

验收：

```text
用户可以对每条 LLM 判断提交反馈。
反馈保存到 user_feedback。
```

------

### Step 4：实现 FeedbackDialog

任务：

```text
1. 支持选择 judgment。
2. 支持选择 correction_type。
3. 支持输入 correction_text。
4. 支持 applies_to_similar。
5. 支持保存。
6. 支持取消。
```

验收：

```text
用户可以完整提交一条反馈。
```

------

### Step 5：Timeline 事件修正

任务：

```text
1. SummaryEvent 支持修改 category。
2. SummaryEvent 支持修改 activity_type。
3. SummaryEvent 支持修改 summary。
4. 保存 event_corrections。
5. 更新 summary_events 当前记录。
```

验收：

```text
用户可以把某条事件标记为计划休息、工作沟通或误判。
```

------

### Step 6：FeedbackRuleGenerator

任务：

```text
1. 用户反馈 applies_to_similar = true 时生成规则。
2. 支持 time_pattern。
3. 支持 category_pattern。
4. 支持 device_switch_pattern。
5. 支持 ignore_pattern。
6. 保存 feedback_memory_rules。
```

验收：

```text
用户选择“以后类似情况也这样理解”后，系统生成反馈记忆规则。
```

------

### Step 7：Feedback Center 页面

任务：

```text
1. 展示反馈总数。
2. 展示反馈列表。
3. 展示反馈规则列表。
4. 支持禁用规则。
5. 支持删除反馈。
6. 支持编辑反馈说明。
```

验收：

```text
用户可以管理自己的反馈记忆。
```

------

### Step 8：SimilarityMatcher

任务：

```text
1. 根据当前事件匹配 feedback_memory_rules。
2. 支持时间段匹配。
3. 支持 category 匹配。
4. 支持 device_type 匹配。
5. 支持 device switch pattern 匹配。
6. 输出相关规则。
```

验收：

```text
对于午饭后短视频事件，可以匹配用户之前保存的计划休息规则。
```

------

### Step 9：LLM Payload 注入 feedback_context

任务：

```text
1. 修改 SingleDeviceLlmPayloadBuilder。
2. 修改 CrossDeviceLlmPayloadBuilder。
3. 注入 feedback_context。
4. LLM Preview 页面展示即将注入的反馈规则。
```

验收：

```text
用户可以在 LLM 输入预览中看到历史反馈将如何影响本次分析。
```

------

### Step 10：更新 LLM Prompt

任务：

```text
1. System Prompt 增加反馈记忆说明。
2. User Prompt 增加 feedback_context 使用要求。
3. 输出 Schema 增加 used_feedback_rule_ids。
4. 输出 Schema 增加 feedback_influence。
```

验收：

```text
LLM 输出能体现它是否参考了用户反馈。
```

------

### Step 11：反馈同步

任务：

```text
1. 实现 FeedbackSyncBundle。
2. 实现 /api/feedback/sync。
3. 实现 FeedbackSyncClient。
4. 实现 FeedbackSyncServer。
5. 双端按 feedback_id 去重。
6. 同步 event_corrections。
7. 同步 feedback_memory_rules。
```

验收：

```text
用户在 Desktop 上反馈后，Android 能看到反馈状态。
用户在 Android 上反馈后，Desktop 能收到反馈规则。
主分析设备能使用另一端创建的反馈。
```

------

### Step 12：Review 重新生成

任务：

```text
1. 用户提交反馈后，Review 页面提示“可基于反馈重新生成复盘”。
2. 用户点击重新生成。
3. 新的 LLM Payload 包含 feedback_context。
4. 新 Review 保存为新版本。
5. 旧 Review 不删除。
```

验收：

```text
用户反馈“这是计划休息”后，重新生成复盘时 LLM 不再直接判断为任务漂移。
```

------

### Step 13：UI 打磨和错误处理

任务：

```text
1. 反馈保存成功提示。
2. 反馈保存失败提示。
3. 反馈同步失败提示。
4. 规则生成失败提示。
5. 重新生成 Review 失败提示。
6. 已反馈状态标记。
7. 反馈撤销确认。
```

验收：

```text
用户能清楚知道自己的反馈是否已经保存、是否已经用于后续分析。
```

------

## 17. 测试计划

### 17.1 单元测试

测试对象：

```text
FeedbackRepository
EventCorrectionRepository
FeedbackMemoryRepository
FeedbackRuleGenerator
SimilarityMatcher
LlmPayload feedback_context injection
LlmOutputParser id generation
FeedbackSyncBundle parser
```

重点测试：

```text
LLM 输出无 id 时自动补 id
保存 feedback
删除 feedback
生成 time_pattern rule
生成 category_pattern rule
相似事件匹配
禁用 rule 后不注入 Prompt
feedback_context 数量限制
feedback 同步幂等去重
```

------

### 17.2 集成测试

场景一：计划休息反馈

```text
1. LLM 判断 12:30 - 12:50 短视频可能是任务漂移。
2. 用户反馈：这是计划休息。
3. 用户勾选：以后类似情况也这样理解。
4. 系统生成 feedback_memory_rule。
5. 用户重新生成 Review。
6. 新 LLM Payload 包含该规则。
7. 新 Review 对类似事件更谨慎。
```

场景二：工作沟通反馈

```text
1. LLM 判断 10:00 微信使用可能是打断。
2. 用户反馈：这是工作沟通。
3. 后续工作日白天聊天类 App 分析时注入该反馈。
```

场景三：建议反馈

```text
1. LLM 建议：减少聊天工具使用。
2. 用户反馈：不相关，这是我的工作工具。
3. 后续复盘建议应减少类似泛化建议。
```

场景四：反馈同步

```text
1. Desktop 生成跨设备 Review。
2. 用户在 Desktop 对某条判断反馈。
3. Desktop 同步 feedback 到 Android。
4. Android Feedback Center 能看到该反馈。
5. Android 被设为主分析设备后，可以使用这条反馈。
```

------

## 18. 手动测试流程

```text
1. 完成第五阶段跨设备复盘。
2. 打开 Review 页面。
3. 找到一条 possible_task_drift。
4. 点击“这是计划休息”。
5. 勾选“以后类似情况也这样理解”。
6. 输入说明：午饭后固定休息。
7. 保存反馈。
8. 打开 Feedback Center。
9. 确认反馈和规则已保存。
10. 打开 LLM Payload Preview。
11. 确认 feedback_context 中包含该规则。
12. 重新生成 Review。
13. 确认新 Review 对该时间段判断更谨慎。
14. 在另一台设备查看反馈是否同步。
```

------

## 19. 第六阶段验收清单

### 19.1 功能验收

```text
[ ] Review 每条核心判断都有反馈入口
[ ] 用户可以反馈准确 / 不准确 / 部分准确
[ ] 用户可以标记计划休息
[ ] 用户可以标记工作沟通
[ ] 用户可以标记任务逃避
[ ] 用户可以标记误判
[ ] 用户可以反馈建议是否有用
[ ] 用户可以回答 LLM 提出的问题
[ ] 用户可以修改 SummaryEvent 分类
[ ] 用户反馈保存到 user_feedback
[ ] 事件修正保存到 event_corrections
[ ] applies_to_similar 可生成 feedback_memory_rules
[ ] Feedback Center 可以展示反馈
[ ] 用户可以禁用反馈规则
[ ] LLM Payload Preview 展示 feedback_context
[ ] 后续 LLM 分析会注入 feedback_context
[ ] LLM 输出能包含 used_feedback_rule_ids
[ ] 用户可以基于反馈重新生成 Review
[ ] 反馈可以同步到已配对设备
```

### 19.2 非功能验收

```text
[ ] 反馈数据不上传开发者服务器
[ ] 反馈数据不用于训练云端模型
[ ] 反馈只作为用户本地 Prompt 上下文
[ ] 禁用的反馈规则不会进入 LLM Prompt
[ ] raw_usage_events 不进入 feedback_context
[ ] raw_desktop_events 不进入 feedback_context
[ ] 聊天内容不进入 feedback_context
[ ] 键盘输入不进入 feedback_context
[ ] 反馈同步幂等去重
[ ] 反馈删除后不会继续影响后续分析
```

------

## 20. 第六阶段 Codex 执行提示词

可以直接把下面内容给 Codex：

```text
请根据《Life Debugger 第六阶段编程 Plan：反馈闭环 + 人类校正》实现第六阶段功能。

严格要求：

1. 第六阶段只实现反馈闭环和人类校正。
2. 不实现自然语言查询。
3. 不实现周报/月报。
4. 不实现云端账号。
5. 不上传任何数据到开发者服务器。
6. 用户反馈必须保存在本地数据库。
7. 用户反馈不得用于训练云端模型。
8. Review 页面每条核心 LLM 判断都要有反馈入口。
9. 用户可以反馈准确、不准确、部分准确。
10. 用户可以标记计划休息、工作沟通、任务逃避、误判。
11. 用户可以对 LLM 建议反馈有用/没用/不相关。
12. 用户可以回答 LLM 的 questions_for_user。
13. 用户可以修改 SummaryEvent 分类。
14. 用户选择“以后类似情况也这样理解”时，应生成 feedback_memory_rules。
15. 后续 LLM Payload 必须注入相关 feedback_context。
16. LLM Payload Preview 必须展示将注入的反馈记忆。
17. LLM 输出条目必须有稳定 id；如果模型没返回 id，解析器自动生成。
18. 用户可以基于反馈重新生成 Review。
19. 反馈应支持同步到已配对设备。
20. 禁用或删除的反馈规则不得继续影响后续分析。
```

------

## 21. 第六阶段最终 Demo

目标 Demo：

```text
1. 用户完成跨设备 LLM 复盘。
2. LLM 判断：
   14:03 - 14:27 从电脑写作切到手机短视频，可能是任务漂移。
3. 用户点击：
   这是计划休息。
4. 用户勾选：
   以后类似情况也这样理解。
5. 系统保存反馈，并生成反馈记忆规则。
6. 用户重新生成复盘。
7. 新复盘中 LLM 表述变为：
   该时间段可能是计划休息，因为用户过去反馈类似午后短视频使用通常是主动休息。
8. Android 和 Desktop 都可以看到该反馈。
```

演示文案：

```text
Life Debugger 不只是分析你的一天。
它会接受你的纠正。
当你告诉它“这不是逃避，这是计划休息”后，它会在后续复盘中记住这个偏好。
```

------

## 22. 第六阶段完成后的下一步

第六阶段完成后，进入第七阶段：

```text
自然语言查询 + 时间线搜索
```

第七阶段将基于以下数据能力：

```text
SummaryEvent
CrossDeviceTimeline
LlmReview
UserFeedback
FeedbackMemoryRule
EventCorrection
```

第七阶段可以实现：

```text
我昨天为什么没写完文档？
我这周什么时候最容易刷手机？
哪些任务之后我最容易切到短视频？
哪些 LLM 判断我经常纠正？
我经常把哪些行为标记为计划休息？
```

因此第六阶段必须保留：

```text
feedback_id
target_type
target_id
target_time_start
target_time_end
correction_type
correction_text
applies_to_similar
feedback_memory_rules
used_feedback_rule_ids
feedback_influence
```

这些字段会成为第七阶段自然语言查询的重要数据基础。