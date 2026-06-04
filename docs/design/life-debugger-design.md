# Life Debugger 人生调试器：单端/双端 + 摘要同步 + 云端 LLM 分析设计方案

## 1. 项目定位

Life Debugger 是一个本地优先的个人行为调试系统，用来帮助用户理解自己在手机和电脑上的时间流向、注意力切换、任务中断、拖延触发点和每日行为模式。

本方案面向 MVP 阶段，支持：

- 只有手机端使用；
- 只有电脑端使用；
- 手机端 + 电脑端同时使用；
- 用户自主选择主分析设备；
- 默认不上传原始数据到开发者服务器；
- 不使用 Accessibility Service；
- 暂不考虑 iOS；
- 手机端只生成摘要，不同步原始使用事件；
- 同步不是实时同步，而是在设备空闲时自动同步；
- 第一次同步需要配对认证；
- 配对完成后，同一局域网内可自动发现并同步；
- MVP 分析阶段必须使用云端 LLM；
- LLM 可选择 DeepSeek、通义千问/Qwen 或其他 OpenAI-compatible Provider；
- LLM API Key、Base URL、模型名等配置由用户在应用界面填写；
- 电脑端和手机端都要有完整、清晰、美观的前端界面。

一句话目标：

> Life Debugger 不是监控软件，而是一个由用户掌控的个人行为调试器。它通过本地摘要、局域网同步和用户自选云端 LLM，帮助用户复盘自己的一天。

------

## 2. 核心原则

### 2.1 不采集原始敏感内容

Android 端不采集：

- 屏幕内容；
- 聊天内容；
- 输入内容；
- 视频内容；
- 图片内容；
- 麦克风内容；
- 通话内容；
- 精确定位轨迹；
- App 内页面结构；
- Accessibility Service 数据。

电脑端 MVP 也不采集：

- 键盘输入内容；
- 密码；
- 麦克风；
- 摄像头；
- 隐私窗口内容；
- 用户明确排除的 App 或网站。

### 2.2 只同步摘要

手机端向主分析设备同步的不是原始事件，而是摘要事件。

例如，手机端内部可能知道：

```text
14:03 打开 com.xxx.shortvideo
14:09 切换到 com.xxx.shortvideo2
14:27 退出手机
```

但同步时只能发送：

```text
14:03 - 14:27，短视频娱乐类使用，持续 24 分钟。
```

默认同步真实包名，同步真实 App 名称。

### 2.3 云端 LLM 只接收摘要包

MVP 阶段必须使用 LLM，因此需要明确：

- 开发者自己的服务器不保存用户数据；
- 但如果用户启用 DeepSeek、Qwen 等云端模型，摘要数据会被发送给用户配置的模型服务商；
- 应用必须在 UI 中清楚提示这一点；
- 用户必须可以预览即将发送给 LLM 的摘要包；
- 用户可以关闭某些类别参与 LLM 分析；
- 用户可以关闭真实 App 名称；
- 用户可以将敏感 App 永久排除。

推荐产品表述：

> Life Debugger 不上传原始数据。
> 当你启用云端 LLM 分析时，系统只会把本地生成的行为摘要发送给你配置的模型服务商。

------

## 3. 支持的运行模式

系统支持三种运行模式。

### 3.1 仅手机模式

用户只安装 Android App。

此时：

- Android 手机就是唯一设备；
- Android 手机默认成为主分析设备；
- Android 本地采集 App 使用摘要；
- Android 本地配置 LLM；
- Android 本地调用云端 LLM；
- Android 本地展示每日复盘；
- 不需要桌面端；
- 不需要同步。

适合场景：

```text
用户主要使用手机；
用户暂时不想安装桌面端；
用户只关心手机使用时间和注意力切换。
```

### 3.2 仅电脑模式

用户只安装桌面端。

此时：

- 桌面端就是唯一设备；
- 桌面端默认成为主分析设备；
- 桌面端本地采集电脑行为摘要；
- 桌面端本地配置 LLM；
- 桌面端调用云端 LLM；
- 桌面端展示每日复盘；
- 不需要 Android 端；
- 不需要同步。

适合场景：

```text
用户主要在电脑上工作；
用户想先用桌面端体验 Life Debugger；
开发者想先验证桌面端价值。
```

### 3.3 手机 + 电脑双设备模式

用户同时安装 Android 端和桌面端。

此时：

- 第一次需要配对认证；
- 配对完成后，同一局域网下自动发现；
- 系统在设备空闲时自动同步；
- 同步不是实时同步；
- 用户需要选择主分析设备；
- 默认主分析设备为电脑端；
- 手机端只同步摘要事件；
- 主分析设备负责合并时间线、调用 LLM、生成复盘。

默认策略：

```text
单设备时：本设备就是主分析设备。
双设备时：默认电脑端为主分析设备。
用户可以手动改为手机端为主分析设备。
```

------

## 4. 设备角色设计

### 4.1 数据采集设备 Collector Device

负责采集本设备行为并生成摘要。

Android Collector：

```text
读取 UsageStatsManager / UsageEvents
生成手机使用摘要
本地保存摘要
等待同步
```

Desktop Collector：

```text
采集窗口标题
采集应用使用段
采集浏览器扩展摘要
采集空闲状态
生成电脑行为摘要
本地保存摘要
等待分析或同步
```

### 4.2 主分析设备 Primary Analysis Device

负责：

- 接收其他设备摘要；
- 合并跨设备时间线；
- 构造 LLM 分析包；
- 调用云端 LLM；
- 保存 LLM 分析结果；
- 展示每日复盘；
- 提供问答入口。

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
```

用户可以在设置中修改。

### 4.3 非主分析设备 Secondary Device

负责：

- 本地采集；
- 本地摘要；
- 在空闲时同步摘要给主分析设备；
- 不调用 LLM；
- 可以查看本机基础统计；
- 可以查看从主分析设备回传的分析结果摘要。

------

## 5. 总体架构

```text
┌──────────────────────────────┐
│        Android App            │
│                              │
│  UsageStats Reader            │
│  Summary Generator            │
│  Local SQLite                 │
│  LLM Config UI                │
│  Sync Client / Receiver       │
│  Review UI                    │
└───────────────┬──────────────┘
                │
        LAN Pairing + Idle Sync
                │
┌───────────────▼──────────────┐
│        Desktop App            │
│                              │
│  Window Tracker               │
│  Browser Extension Receiver   │
│  Local SQLite                 │
│  LLM Config UI                │
│  Sync Server / Client         │
│  Timeline Merge               │
│  LLM Review Engine            │
│  Review UI                    │
└──────────────────────────────┘
```

注意：

- Android 和 Desktop 都要具备 Collector 能力；
- Android 和 Desktop 都要具备主分析能力；
- 只是双设备默认让 Desktop 当主分析设备；
- 同步模块需要双向设计，因为主分析设备可以切换。

------

## 6. Android 端采集方案

### 6.1 使用 UsageStatsManager

Android 端使用 UsageStatsManager 查询 App 使用历史和 UsageEvents。

采集目标：

```text
packageName
eventType
timestamp
```

关注事件：

```text
ACTIVITY_RESUMED
ACTIVITY_PAUSED
ACTIVITY_STOPPED
MOVE_TO_FOREGROUND
MOVE_TO_BACKGROUND
```

不同 Android 版本需要兼容处理。

### 6.2 不使用 Accessibility Service

MVP 阶段禁止使用：

```text
AccessibilityService
读取 View Tree
读取按钮文本
读取 App 内页面文字
读取聊天内容
后台截屏
后台录屏
```

### 6.3 手机端摘要能力

Android 端将原始事件转为摘要事件。

示例原始行为：

```text
用户连续使用短视频类 App 24 分钟。
```

生成摘要：

```json
{
  "event_id": "uuid",
  "device_id": "android_phone_001",
  "device_type": "android",
  "source": "android_usage_stats",
  "start_time": 1780303380000,
  "end_time": 1780304820000,
  "duration_seconds": 1440,
  "category": "short_video",
  "activity_type": "entertainment",
  "summary": "连续使用短视频类 App 约 24 分钟",
  "confidence": 0.86,
  "privacy_level": "summary_only",
  "app_label_mode": "generic",
  "llm_allowed": true,
  "created_at": 1780304880000
}
```

默认不同步：

```text
真实 packageName
真实 App 名称
App 内内容
通知内容
截图内容
```

------

## 7. 桌面端采集方案

桌面端 MVP 采集内容：

```text
当前活动应用
窗口标题
浏览器页面标题
浏览器域名
用户空闲状态
应用切换时间
```

建议分级采集：

### 7.1 低敏模式，默认

```text
应用名称
窗口标题泛化
浏览器域名
使用时长
活动类别
```

例如：

```text
Chrome 浏览技术文档 18 分钟
VS Code 编码 42 分钟
Word 编辑文档 26 分钟
```

### 7.2 标准模式，用户开启

```text
完整窗口标题
浏览器网页标题
文件名
项目名
```

### 7.3 隐私模式

以下内容不采集或泛化：

```text
无痕窗口
密码管理器
银行网站
支付网站
医疗健康网站
用户排除的 App
用户排除的网站
```

桌面端摘要事件示例：

```json
{
  "event_id": "uuid",
  "device_id": "desktop_001",
  "device_type": "desktop",
  "source": "desktop_window_tracker",
  "start_time": 1780302000000,
  "end_time": 1780303380000,
  "duration_seconds": 1380,
  "category": "writing",
  "activity_type": "productive",
  "summary": "在桌面端进行文档写作约 23 分钟",
  "confidence": 0.9,
  "privacy_level": "metadata",
  "llm_allowed": true,
  "created_at": 1780303390000
}
```

------

## 8. 摘要事件统一模型

所有设备都使用同一种 SummaryEvent。

```json
{
  "event_id": "uuid",
  "device_id": "device_uuid",
  "device_type": "android | desktop",
  "source": "android_usage_stats | desktop_window_tracker | browser_extension | manual_intent",
  "start_time": 1780302000000,
  "end_time": 1780303380000,
  "duration_seconds": 1380,
  "category": "writing | short_video | chat | browser | coding | study | game | shopping | finance | health | system | unknown",
  "activity_type": "productive | communication | entertainment | consumption | life_service | system | unknown",
  "summary": "用户可读摘要",
  "confidence": 0.0,
  "privacy_level": "summary_only | metadata | user_input | sensitive_masked",
  "app_label_mode": "generic | real | hidden",
  "llm_allowed": true,
  "user_corrected": false,
  "metadata_json": {},
  "created_at": 1780303390000
}
```

重要要求：

```text
只有 SummaryEvent 可以被同步。
只有 llm_allowed = true 的事件可以进入 LLM 分析包。
敏感事件默认 llm_allowed = false。
用户可手动修改事件是否允许参与 LLM 分析。
```

------

## 9. 手动意图记录

手机端和电脑端都需要支持手动意图记录。

用户可以快速选择：

```text
我现在准备：
- 工作
- 学习
- 写作
- 编码
- 查资料
- 放松
- 通勤
- 聊天
- 购物
- 休息
- 其他
```

手动意图事件：

```json
{
  "event_id": "uuid",
  "device_id": "device_uuid",
  "device_type": "android | desktop",
  "source": "manual_intent",
  "start_time": 1780302000000,
  "end_time": null,
  "category": "study",
  "activity_type": "productive",
  "summary": "用户手动记录：准备学习论文",
  "privacy_level": "user_input",
  "llm_allowed": true,
  "created_at": 1780302000000
}
```

作用：

- 帮助 LLM 判断用户原计划；
- 分析计划行为与实际行为的偏移；
- 增强复盘准确性；
- 减少纯自动采集造成的误判。

------

## 10. 同步设计

### 10.1 第一次必须配对认证

双设备第一次连接时必须配对。

配对方式：

```text
桌面端生成二维码，手机端扫码。
或手机端生成二维码，桌面端扫码。
```

因为主分析设备可以是手机，也可以是电脑，所以配对能力要双向支持。

二维码内容：

```json
{
  "protocol": "lifedbg-pairing-v1",
  "device_id": "desktop_001",
  "device_name": "Ko-PC",
  "device_type": "desktop",
  "host": "192.168.1.8",
  "port": 58231,
  "public_key": "base64_public_key",
  "pairing_token": "random_token",
  "expires_at": 1780303680000
}
```

配对成功后，双方保存：

```text
对方 device_id
对方 device_name
对方 device_type
对方 public_key
最近连接地址
配对时间
用户确认的信任关系
```

### 10.2 配对后自动发现

配对完成后，只要两个设备在同一个局域网内，就可以自动发现并准备同步。

自动发现方式：

```text
mDNS / Bonjour / Zeroconf
局域网广播
最近 IP + 端口重试
手动输入 IP 作为备用方案
```

自动发现不代表实时同步。

### 10.3 同步不是实时同步

MVP 不做实时流式同步。

同步策略：

```text
设备空闲时同步
充电时优先同步
Wi-Fi 下同步
电量充足时同步
距离上次同步超过一定时间再同步
本地待同步事件超过阈值再同步
用户点击“立即同步”时同步
```

Android 端建议使用 WorkManager 做周期任务和约束任务。

桌面端建议根据空闲检测器判断：

```text
键盘鼠标无操作超过 3 分钟
CPU 负载不高
当前不是全屏游戏或视频会议
```

### 10.4 智能空闲同步规则

同步触发条件：

```text
已配对
位于同一局域网
存在 pending 摘要事件
当前设备空闲
对方设备在线
距离上次成功同步超过 10 分钟
```

同步延迟：

```text
默认 10 - 30 分钟检查一次
不要每秒轮询
不要实时上传
不要影响用户当前操作
```

### 10.5 双向同步方向

同步方向由主分析设备决定。

#### 电脑为主分析设备

```text
Android -> Desktop
Desktop 本地分析
Desktop 调用 LLM
Desktop 保存复盘
可选：Desktop -> Android 回传复盘摘要
```

#### 手机为主分析设备

```text
Desktop -> Android
Android 本地分析
Android 调用 LLM
Android 保存复盘
可选：Android -> Desktop 回传复盘摘要
```

### 10.6 同步数据包

```json
{
  "protocol": "lifedbg-sync-v1",
  "bundle_id": "uuid",
  "from_device_id": "android_phone_001",
  "to_device_id": "desktop_001",
  "primary_analysis_device_id": "desktop_001",
  "generated_at": 1780305000000,
  "from_time": 1780300000000,
  "to_time": 1780305000000,
  "events": []
}
```

同步响应：

```json
{
  "status": "ok",
  "bundle_id": "uuid",
  "received_event_count": 30,
  "deduplicated_event_count": 2,
  "stored_event_count": 28,
  "server_time": 1780305010000
}
```

同步要求：

```text
event_id 必须全局唯一。
bundle_id 必须全局唯一。
接收端必须幂等去重。
未收到 ok 响应，不得标记为 synced。
同步失败后进入 retry。
```

------

## 11. LLM 分析设计

### 11.1 MVP 必须使用 LLM

MVP 阶段不使用规则分析生成最终复盘。

允许使用确定性逻辑做：

```text
事件排序
事件合并
摘要生成
敏感信息过滤
LLM 输入包构造
同步调度
数据去重
```

不允许用规则替代 LLM 输出：

```text
每日复盘
注意力诊断
任务逃避判断
建议生成
行为模式解释
```

这些必须由 LLM 生成。

### 11.2 LLM Provider 设计

需要抽象 LLM Provider。

支持：

```text
DeepSeek
Qwen / 通义千问
OpenAI-compatible 自定义服务
```

Provider 配置字段：

```json
{
  "provider_id": "deepseek",
  "provider_name": "DeepSeek",
  "base_url": "https://api.deepseek.com",
  "api_key": "user_input_api_key",
  "model": "deepseek-chat",
  "temperature": 0.3,
  "max_tokens": 2048,
  "enabled": true
}
```

Qwen 配置示例：

```json
{
  "provider_id": "qwen",
  "provider_name": "Qwen",
  "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
  "api_key": "user_input_api_key",
  "model": "qwen-plus",
  "temperature": 0.3,
  "max_tokens": 2048,
  "enabled": true
}
```

自定义 Provider：

```json
{
  "provider_id": "custom_openai_compatible",
  "provider_name": "Custom",
  "base_url": "user_input_base_url",
  "api_key": "user_input_api_key",
  "model": "user_input_model",
  "temperature": 0.3,
  "max_tokens": 2048,
  "enabled": true
}
```

### 11.3 API Key 存储

API Key 必须只保存在本地。

Android：

```text
EncryptedSharedPreferences 或 Android Keystore
```

Desktop：

```text
系统 Keychain / Credential Manager / Secret Service
不要明文写入普通配置文件
```

### 11.4 LLM 输入包

主分析设备构造 LLMAnalysisPackage。

```json
{
  "package_id": "uuid",
  "date": "2026-06-01",
  "timezone": "Asia/Tokyo",
  "devices": [
    {
      "device_id": "desktop_001",
      "device_type": "desktop",
      "role": "primary_analysis"
    },
    {
      "device_id": "android_phone_001",
      "device_type": "android",
      "role": "collector"
    }
  ],
  "user_goal": "用户当天没有填写明确目标",
  "events": [
    {
      "start": "09:00",
      "end": "09:42",
      "device": "desktop",
      "category": "writing",
      "activity_type": "productive",
      "summary": "进行文档写作约 42 分钟"
    },
    {
      "start": "09:42",
      "end": "09:55",
      "device": "android",
      "category": "chat",
      "activity_type": "communication",
      "summary": "连续使用聊天类 App 约 13 分钟"
    }
  ],
  "privacy_note": "All events are local summaries. Raw app content, chat content, screenshots and keystrokes are not included."
}
```

### 11.5 LLM Prompt

系统提示词：

```text
你是 Life Debugger 的行为复盘分析引擎。
你只能基于用户提供的摘要事件进行分析。
不要假装知道摘要之外的内容。
不要给医学诊断。
不要使用羞辱、指责、道德审判式语言。
你的目标是帮助用户理解注意力流动、任务切换、可能的分心点和可执行的改进建议。
请输出结构化 JSON。
```

用户提示词：

```text
下面是用户一天的跨设备行为摘要。请分析：
1. 今日行为概览；
2. 主要时间投入；
3. 可能的注意力切换点；
4. 可能的任务漂移；
5. 手机和电脑之间的切换模式；
6. 3 条具体改进建议；
7. 需要用户确认的不确定点。

只基于摘要，不要推断具体 App 内内容。
```

### 11.6 LLM 输出 JSON Schema

```json
{
  "daily_summary": "今天整体行为概览",
  "time_distribution": [
    {
      "category": "writing",
      "duration_minutes": 120,
      "interpretation": "写作投入较多"
    }
  ],
  "attention_shifts": [
    {
      "time_range": "14:03 - 14:27",
      "from": "desktop writing",
      "to": "android short_video",
      "interpretation": "可能发生了从工作到娱乐的注意力漂移",
      "confidence": 0.72
    }
  ],
  "possible_task_drift": [
    {
      "time_range": "10:42 - 10:55",
      "reason": "生产性任务后紧接较长手机娱乐或社交通信",
      "confidence": 0.68
    }
  ],
  "positive_patterns": [
    "上午存在连续 42 分钟写作段"
  ],
  "suggestions": [
    {
      "title": "给任务卡住点添加手动标记",
      "detail": "当你从电脑切到手机前，可以快速记录一下原因，后续复盘会更准确。"
    }
  ],
  "questions_for_user": [
    "14:03 的手机使用是计划休息，还是因为文档写作卡住？"
  ],
  "risk_level": "low | medium | high",
  "tone": "supportive"
}
```

### 11.7 LLM 分析结果存储

```sql
CREATE TABLE llm_reviews (
  review_id TEXT PRIMARY KEY,
  date TEXT NOT NULL,
  primary_device_id TEXT NOT NULL,
  provider_id TEXT NOT NULL,
  model TEXT NOT NULL,
  input_package_hash TEXT NOT NULL,
  output_json TEXT NOT NULL,
  created_at INTEGER NOT NULL
);
```

------

## 12. UI 设计要求

电脑端和手机端都必须有良好的前端界面。不要只做后台服务。

### 12.1 共同 UI 页面

Android 和 Desktop 都要有：

```text
首页 Dashboard
今日时间线 Timeline
每日复盘 Review
设备与同步 Devices & Sync
LLM 配置 LLM Settings
隐私设置 Privacy
手动意图 Intent
数据管理 Data
```

### 12.2 首页 Dashboard

展示：

```text
今日总使用时长
今日主要活动类别
今日手机/电脑切换次数
最近一次 LLM 分析时间
当前主分析设备
同步状态
LLM 配置状态
```

按钮：

```text
立即分析
立即同步
记录当前意图
查看今日复盘
进入隐私设置
```

### 12.3 设备与同步页面

功能：

```text
显示当前设备角色
选择主分析设备
查看已配对设备
添加新设备
解除配对
查看最近同步时间
查看待同步事件数量
手动立即同步
显示自动同步策略
```

主分析设备选择：

```text
当前设备
已配对电脑
已配对手机
```

提示文案：

```text
主分析设备负责汇总所有摘要事件，并调用你配置的 LLM 生成复盘。
```

### 12.4 LLM 配置页面

字段：

```text
Provider：DeepSeek / Qwen / Custom
Base URL
API Key
Model
Temperature
Max Tokens
是否启用
测试连接
保存配置
```

隐私提示：

```text
启用云端 LLM 后，Life Debugger 会把本地生成的行为摘要发送给你配置的模型服务商。
不会发送原始 App 使用事件、聊天内容、截图、键盘输入或屏幕录制。
```

功能：

```text
测试 API Key
测试模型调用
预览即将发送给 LLM 的摘要包
清空 LLM 配置
```

### 12.5 每日复盘页面

展示：

```text
LLM 生成的今日总结
时间投入分布
注意力切换点
可能任务漂移点
积极行为模式
改进建议
LLM 不确定问题
```

每条分析要允许用户反馈：

```text
准确
不准确
这是计划休息
这是工作需要
以后忽略类似情况
```

用户反馈会用于后续摘要修正，不直接训练云端模型。

### 12.6 隐私设置页面

设置项：

```text
是否允许真实 App 名称进入摘要
是否允许真实窗口标题进入摘要
是否允许某类事件进入 LLM
敏感 App 列表
排除 App
排除网站
暂停采集
清空本地数据
清空同步数据
清空 LLM 分析历史
```

### 12.7 LLM 输入预览页面

用户点击“预览 LLM 输入”时，展示：

```text
今天将发送给 LLM 的所有摘要事件。
```

用户可以：

```text
删除某条摘要
隐藏 App 名称
隐藏窗口标题
排除某一类事件
确认发送
取消分析
```

------

## 13. 数据库设计

### 13.1 通用表：devices

```sql
CREATE TABLE devices (
  device_id TEXT PRIMARY KEY,
  device_type TEXT NOT NULL,
  device_name TEXT NOT NULL,
  role TEXT NOT NULL,
  is_primary_analysis_device INTEGER DEFAULT 0,
  paired_at INTEGER,
  last_seen_at INTEGER,
  public_key TEXT,
  metadata_json TEXT
);
```

### 13.2 通用表：summary_events

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
  summary TEXT NOT NULL,
  confidence REAL DEFAULT 0,
  privacy_level TEXT DEFAULT 'summary_only',
  app_label_mode TEXT DEFAULT 'generic',
  llm_allowed INTEGER DEFAULT 1,
  user_corrected INTEGER DEFAULT 0,
  sync_status TEXT DEFAULT 'local',
  metadata_json TEXT,
  created_at INTEGER NOT NULL
);
```

### 13.3 Android 本地表：raw_usage_events

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

### 13.4 配对表：paired_devices

```sql
CREATE TABLE paired_devices (
  paired_device_id TEXT PRIMARY KEY,
  paired_device_name TEXT NOT NULL,
  paired_device_type TEXT NOT NULL,
  public_key TEXT NOT NULL,
  last_known_host TEXT,
  last_known_port INTEGER,
  pairing_status TEXT NOT NULL,
  paired_at INTEGER NOT NULL,
  last_sync_at INTEGER
);
```

### 13.5 LLM 配置表

不要保存明文 API Key。API Key 存系统安全存储，此表只保存引用。

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

### 13.6 LLM 分析历史表

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

### 13.7 同步包表

```sql
CREATE TABLE sync_bundles (
  bundle_id TEXT PRIMARY KEY,
  from_device_id TEXT NOT NULL,
  to_device_id TEXT NOT NULL,
  direction TEXT NOT NULL,
  generated_at INTEGER NOT NULL,
  sent_at INTEGER,
  received_at INTEGER,
  status TEXT NOT NULL,
  event_count INTEGER DEFAULT 0
);
```

------

## 14. 模块划分

## 14.1 Android 模块

```text
android-app/
  app/
    usage/
      UsageStatsReader.kt
      UsageEventNormalizer.kt
      UsageRepository.kt

    summarize/
      SessionMerger.kt
      SummaryGenerator.kt
      AppClassifier.kt
      SensitiveAppMasker.kt

    sync/
      PairingManager.kt
      LanDiscoveryManager.kt
      IdleSyncScheduler.kt
      SyncClient.kt
      SyncServer.kt
      SyncBundleBuilder.kt

    llm/
      LlmProvider.kt
      OpenAICompatibleClient.kt
      LlmConfigRepository.kt
      LlmReviewEngine.kt
      LlmPromptBuilder.kt

    privacy/
      PrivacySettingsRepository.kt
      ExclusionRulesRepository.kt
      LlmPayloadPreviewBuilder.kt

    ui/
      DashboardScreen.kt
      TimelineScreen.kt
      ReviewScreen.kt
      DeviceSyncScreen.kt
      LlmSettingsScreen.kt
      PrivacyScreen.kt
      IntentScreen.kt
      DataManagementScreen.kt

    db/
      LifeDbgDatabase.kt
```

### Android 必须支持的能力

```text
作为单独手机端使用。
作为双设备中的采集设备使用。
作为双设备中的主分析设备使用。
配置云端 LLM。
调用云端 LLM。
展示 LLM 复盘。
```

------

## 14.2 Desktop 模块

```text
desktop-app/
  src/
    collector/
      WindowTracker.ts
      BrowserEventReceiver.ts
      IdleDetector.ts
      DesktopSummaryGenerator.ts

    sync/
      PairingManager.ts
      LanDiscoveryManager.ts
      IdleSyncScheduler.ts
      SyncClient.ts
      SyncServer.ts
      SyncBundleBuilder.ts

    llm/
      LlmProvider.ts
      OpenAICompatibleClient.ts
      LlmConfigRepository.ts
      LlmReviewEngine.ts
      LlmPromptBuilder.ts

    timeline/
      TimelineRepository.ts
      TimelineMerger.ts
      LlmAnalysisPackageBuilder.ts

    privacy/
      PrivacySettingsRepository.ts
      ExclusionRulesRepository.ts
      LlmPayloadPreviewBuilder.ts

    ui/
      pages/
        Dashboard.tsx
        Timeline.tsx
        Review.tsx
        DeviceSync.tsx
        LlmSettings.tsx
        Privacy.tsx
        Intent.tsx
        DataManagement.tsx

    db/
      Database.ts
```

### Desktop 必须支持的能力

```text
作为单独电脑端使用。
作为双设备中的采集设备使用。
作为双设备中的主分析设备使用。
配置云端 LLM。
调用云端 LLM。
展示 LLM 复盘。
```

------

## 15. LLM 调用流程

主分析设备执行以下流程：

```text
1. 读取当天 SummaryEvent。
2. 过滤 llm_allowed = false 的事件。
3. 根据隐私设置隐藏 App 名称、窗口标题、网站域名。
4. 构造 LLMAnalysisPackage。
5. 生成用户可预览的 LLM 输入。
6. 用户点击“立即分析”。
7. 调用用户配置的云端 LLM。
8. 要求 LLM 输出 JSON。
9. 校验 JSON。
10. 保存 llm_reviews。
11. 在每日复盘页面展示结果。
```

注意：

```text
MVP 中每日复盘必须来自 LLM。
如果 LLM 调用失败，不要用规则生成伪复盘。
只能提示用户：LLM 调用失败，请检查配置。
```

------

## 16. LLM 错误处理

需要处理：

```text
API Key 缺失
Base URL 错误
模型名错误
余额不足
网络错误
返回非 JSON
超时
限流
Provider 不兼容
```

UI 提示示例：

```text
LLM 调用失败：模型没有返回有效 JSON。
你可以重试，或切换到其他模型。
```

不要提示：

```text
系统已根据规则生成复盘。
```

因为 MVP 要求必须使用 LLM。

------

## 17. 安全与隐私提示

应用首次启动时必须展示：

```text
Life Debugger 默认只在本地保存你的行为摘要。
它不会读取聊天内容，不会录屏，不会记录键盘输入。
如果你启用云端 LLM，系统会把你确认后的摘要数据发送给你配置的模型服务商，用于生成复盘。
```

LLM 分析前必须展示：

```text
下面这些摘要将发送给云端 LLM。
请确认是否继续。
```

用户可以设置：

```text
每次分析前都确认
只在第一次确认
永远自动分析
关闭 LLM 分析
```

默认：

```text
每次分析前都确认。
```

------

## 18. MVP 开发路线

### 阶段一：单手机 MVP

目标：

```text
Android 可以独立完成采集、摘要、LLM 分析和复盘展示。
```

任务：

```text
1. Android UsageStats 权限引导。
2. 读取 App 使用事件。
3. 生成 SummaryEvent。
4. 本地数据库保存。
5. LLM 配置页面。
6. LLM 输入预览页面。
7. 调用 DeepSeek 或 Qwen。
8. 展示每日复盘。
```

验收标准：

```text
用户只装 Android App，也能看到基于 LLM 的手机使用复盘。
```

### 阶段二：单电脑 MVP

目标：

```text
Desktop 可以独立完成电脑端摘要、LLM 分析和复盘展示。
```

任务：

```text
1. 桌面端采集活动窗口。
2. 生成 SummaryEvent。
3. 本地数据库保存。
4. LLM 配置页面。
5. LLM 输入预览页面。
6. 调用 DeepSeek 或 Qwen。
7. 展示每日复盘。
```

验收标准：

```text
用户只装桌面端，也能看到基于 LLM 的电脑使用复盘。
```

### 阶段三：双设备配对

目标：

```text
手机和电脑可以第一次配对认证。
```

任务：

```text
1. 设备身份生成。
2. 二维码配对。
3. public key 交换。
4. paired_devices 保存。
5. 设备页展示已配对设备。
```

验收标准：

```text
手机和电脑完成一次安全配对，双方能识别对方。
```

### 阶段四：局域网空闲自动同步

目标：

```text
配对后，同一局域网中设备空闲时自动同步摘要。
```

任务：

```text
1. 局域网自动发现。
2. IdleSyncScheduler。
3. SyncBundleBuilder。
4. SyncClient / SyncServer。
5. 幂等去重。
6. 同步状态展示。
```

验收标准：

```text
配对后的手机和电脑在同一局域网中，不需要用户手动操作，也能在空闲时同步摘要。
```

### 阶段五：主分析设备切换

目标：

```text
用户可以选择手机或电脑作为主分析设备。
```

任务：

```text
1. 设备角色管理。
2. 主分析设备选择 UI。
3. 同步方向切换。
4. 主分析设备调用 LLM。
5. 非主设备展示同步来的复盘摘要。
```

验收标准：

```text
电脑为主时，手机摘要同步到电脑分析。
手机为主时，电脑摘要同步到手机分析。
```

------

## 19. Codex 实现要求

请 Codex 严格遵守：

```text
1. 不使用 Accessibility Service。
2. 暂不实现 iOS。
3. 支持单手机、单电脑、手机+电脑三种模式。
4. 单设备时，本设备默认是主分析设备。
5. 双设备时，默认电脑端是主分析设备。
6. 用户可以手动修改主分析设备。
7. 第一次跨设备同步必须配对认证。
8. 配对后，同一局域网内自动发现。
9. 同步不是实时同步，必须基于空闲策略。
10. 手机端只同步 SummaryEvent。
11. 不同步原始 UsageEvents。
12. 不同步聊天内容、截图内容、输入内容。
13. MVP 每日复盘必须使用云端 LLM。
14. 不要用规则分析替代 LLM 复盘。
15. LLM Provider 使用 OpenAI-compatible 抽象。
16. 支持 DeepSeek、Qwen、自定义 Provider。
17. API Key 必须本地安全保存。
18. LLM 输入必须可预览。
19. 用户必须能排除敏感事件进入 LLM。
20. Android 和 Desktop 都必须有完整 UI。
```

------

## 20. 第一版 Demo 效果

### Demo 1：只有手机

```text
用户安装 Android App。
授权使用情况访问。
填写 DeepSeek API Key。
晚上点击“生成复盘”。

系统输出：
今天手机使用总时长 4 小时 12 分钟。
其中短视频类 1 小时 20 分钟，聊天类 58 分钟，浏览器类 35 分钟。
LLM 认为 14:03 - 14:27 是一次较明显的娱乐型注意力漂移。
```

### Demo 2：只有电脑

```text
用户安装桌面端。
填写 Qwen API Key。
桌面端记录应用使用摘要。
晚上点击“生成复盘”。

系统输出：
今天电脑端主要投入在编码和文档写作。
下午 16:20 后出现较多浏览器切换，可能说明任务目标变得不清晰。
```

### Demo 3：手机 + 电脑

```text
用户安装 Android App 和 Desktop App。
第一次扫码配对。
之后二者在同一 Wi-Fi 下自动同步。
用户默认选择电脑作为主分析设备。
晚上电脑端调用 LLM 生成跨设备复盘。

系统输出：
14:03 前你在电脑端写文档。
14:03 - 14:27 手机端出现短视频类使用。
LLM 判断这可能是一次从文档写作到娱乐内容的注意力漂移。
建议你在写作卡住时使用“手动意图记录”，区分计划休息和逃避式切换。
```

------

## 21. 推荐技术栈

### Android

```text
Kotlin
Jetpack Compose
Room
WorkManager
UsageStatsManager
DataStore
Android Keystore
OkHttp / Ktor Client
QR Scanner
mDNS / NSD
```

### Desktop

```text
Tauri
React
TypeScript
SQLite
Rust / Node local server
System Keychain
QRCode
mDNS / Zeroconf
Timeline UI
```

### LLM

```text
OpenAI-compatible client abstraction
DeepSeek Provider
Qwen Provider
Custom Provider
JSON schema validation
Retry and timeout handling
```

------

## 22. 最小可行版本定义

最小可行版本不是直接做完整双端系统，而是按优先级完成：

```text
优先级 1：
Android 单端闭环：
采集摘要 -> 配置 LLM -> 预览摘要包 -> 调用 LLM -> 展示复盘。

优先级 2：
Desktop 单端闭环：
采集摘要 -> 配置 LLM -> 预览摘要包 -> 调用 LLM -> 展示复盘。

优先级 3：
双设备闭环：
扫码配对 -> 局域网发现 -> 空闲同步 -> 主分析设备合并摘要 -> LLM 生成跨设备复盘。
```

最终 MVP 证明点：

> 即使不上传原始数据，Life Debugger 也能通过本地摘要 + 用户自配 LLM，生成可理解、可追问、可改进的个人行为复盘。

这版更适合给 Codex：先让它从 **Android 单端闭环** 开始实现，因为它最能验证“手机用户更多”的核心假

。