# Life Debugger 第十阶段编码 Plan：发布、演示和 GitHub 包装

## 0. 阶段目标

第十阶段目标是将 Life Debugger 从一个功能完整的工程项目，包装成一个可以正式发布、可被用户理解、可被开发者贡献、可在 GitHub 上传播的开源产品。

前九阶段已经完成：

```text
1. Android 单端闭环
2. Desktop 单端闭环
3. 手机电脑配对
4. 局域网 SummaryEvent 摘要同步
5. 主分析设备切换 + 跨设备 LLM 复盘
6. 反馈闭环 + 人类校正
7. 自然语言查询 + 时间线搜索
8. 周报 / 月报 / 长期趋势
9. 隐私、安全与可靠性强化
```

第十阶段要解决的问题是：

```text
这个项目如何被普通用户快速理解？
如何被开发者快速运行？
如何让用户相信它不是监控软件？
如何让 GitHub 访客 30 秒内看懂它的价值？
如何用 Demo 展示跨设备人生调试的震撼点？
如何发布 Android APK 和 Desktop 安装包？
如何组织文档、贡献指南、Roadmap 和 Release？
```

一句话目标：

> 把 Life Debugger 做成一个可以公开发布、可演示、可安装、可信任、可传播的开源项目。

------

## 1. 第十阶段核心产物

第十阶段最终需要产出：

```text
1. GitHub README
2. 项目官网 / Landing Page
3. Demo 数据模式
4. Demo GIF / 视频脚本
5. 安装包构建流程
6. Android APK Release
7. Desktop 安装包 Release
8. 文档站
9. 隐私白皮书
10. 安全架构说明
11. 数据流图
12. 开发者快速启动指南
13. 贡献指南
14. Issue / PR 模板
15. Roadmap
16. Release Notes
17. 自动构建 CI
18. 自动测试 CI
19. 版本号策略
20. 发布检查清单
```

------

## 2. 第十阶段范围

### 2.1 必须实现

```text
1. 统一项目 monorepo 结构。
2. 编写高质量 README。
3. 编写 docs 文档目录。
4. 编写隐私与安全文档。
5. 编写架构文档。
6. 编写本地运行指南。
7. 编写 Android 构建指南。
8. 编写 Desktop 构建指南。
9. 实现 Demo Mode。
10. 实现示例数据导入。
11. 实现一键生成 Demo 数据。
12. 实现 Desktop 安装包构建。
13. 实现 Android APK 构建。
14. 配置 GitHub Actions。
15. 配置自动测试。
16. 配置 Release 构建。
17. 添加 Issue 模板。
18. 添加 PR 模板。
19. 添加 LICENSE。
20. 添加 CONTRIBUTING。
21. 添加 SECURITY。
22. 添加 CHANGELOG。
23. 添加 ROADMAP。
24. 添加 CODE_OF_CONDUCT，可选但建议。
25. 添加 screenshots / demo assets。
```

### 2.2 建议实现

```text
1. 项目官网。
2. 文档站，例如 VitePress / Docusaurus / Nextra。
3. Demo 视频录制脚本。
4. GitHub README 顶部动图。
5. 架构图 SVG。
6. 数据流图 SVG。
7. 隐私承诺图。
8. App Store / Play Store 预备文案。
9. Product Hunt / Hacker News 发布文案。
10. GitHub Topics 优化。
11. Star History Badge。
12. Open Graph 分享图。
13. 多语言 README，中文 + 英文。
```

### 2.3 本阶段不实现

```text
1. 新的行为采集能力。
2. 新的 LLM 分析能力。
3. 新的同步协议。
4. 新的安全协议。
5. 云端账号。
6. 开发者服务器。
7. 付费系统。
8. 商业化后台。
```

第十阶段重点是：

```text
可发布
可理解
可信任
可演示
可贡献
可维护
```

不是继续堆新功能。

------

## 3. 推荐仓库结构

最终仓库建议采用 monorepo。

```text
life-debugger/
  README.md
  README.zh-CN.md
  LICENSE
  CONTRIBUTING.md
  SECURITY.md
  CODE_OF_CONDUCT.md
  CHANGELOG.md
  ROADMAP.md
  .gitignore
  .editorconfig

  .github/
    workflows/
      android-ci.yml
      desktop-ci.yml
      release.yml
      docs.yml
    ISSUE_TEMPLATE/
      bug_report.yml
      feature_request.yml
      privacy_concern.yml
      security_report.yml
    PULL_REQUEST_TEMPLATE.md

  apps/
    android/
      ...
    desktop/
      ...

  packages/
    shared-schema/
      summary-event.schema.json
      sync-bundle.schema.json
      llm-review.schema.json
      trend-report.schema.json
      query-context.schema.json

    shared-prompts/
      daily-review.md
      cross-device-review.md
      query-answer.md
      trend-report.md

    demo-data/
      demo-day.json
      demo-week.json
      demo-cross-device.json
      demo-feedback.json
      demo-trend.json

  docs/
    index.md
    getting-started.md
    installation.md
    android-setup.md
    desktop-setup.md
    llm-provider-setup.md
    privacy.md
    security.md
    architecture.md
    data-model.md
    sync-protocol.md
    llm-payloads.md
    backup-restore.md
    troubleshooting.md
    development.md
    roadmap.md

  website/
    landing-page/
      ...

  assets/
    screenshots/
      dashboard.png
      timeline.png
      review.png
      query.png
      trends.png
      privacy-center.png
    diagrams/
      architecture.svg
      data-flow.svg
      sync-flow.svg
      privacy-model.svg
    demo/
      demo-script.md
      demo-video-outline.md
      demo.gif
```

------

## 4. README 设计

README 是 GitHub 传播的核心。

### 4.1 README 顶部结构

README 顶部必须 30 秒内讲清楚：

```text
Life Debugger 是什么？
它解决什么痛点？
它为什么不是监控软件？
它的 Demo 有多震撼？
如何快速安装体验？
```

推荐结构：

```markdown
# Life Debugger

A local-first personal behavior debugger for understanding how your attention moves across phone and desktop.

[Demo GIF]

## What is this?

Life Debugger turns your Android and desktop activity summaries into a private, cross-device timeline, then uses your own LLM provider to generate daily reviews, feedback-aware insights, natural language queries, and long-term trends.

## Why?

Because most productivity tools tell you what to do.
Life Debugger helps you understand what actually happened.

## Privacy First

- No account
- No developer server
- No raw screen recording
- No keystroke logging
- No chat content
- Local summaries only
- User-owned LLM API key
- LAN sync between paired devices
- LLM payload preview before sending
```

### 4.2 README 必须包含的模块

```text
1. 项目一句话介绍。
2. Demo GIF。
3. 核心功能列表。
4. 隐私承诺。
5. 架构图。
6. 安装方式。
7. 快速开始。
8. LLM Provider 配置。
9. Demo Mode。
10. 开发者运行指南。
11. Roadmap。
12. 贡献方式。
13. License。
```

### 4.3 中文 README

项目应提供：

```text
README.md：英文
README.zh-CN.md：中文
```

中文 README 要突出：

```text
人生调试器
本地优先
不上传原始数据
只同步摘要
用户自配 LLM
跨设备时间线
每日复盘
反馈校正
自然语言查询
周报月报
```

------

## 5. Demo Mode 设计

第十阶段必须实现 Demo Mode。

原因：

```text
真实采集需要权限和时间。
GitHub 访客不一定愿意立刻授权。
Demo Mode 可以让用户 1 分钟内看到产品价值。
```

### 5.1 Demo Mode 功能

用户点击：

```text
Try Demo Mode
```

系统自动导入示例数据：

```text
1. 一天的 Android 手机摘要。
2. 一天的 Desktop 电脑摘要。
3. 跨设备切换事件。
4. 一份每日复盘。
5. 一些用户反馈。
6. 一份自然语言查询结果。
7. 一份周报趋势。
```

### 5.2 Demo 数据要求

Demo 数据必须展示最有冲击力的故事：

```text
上午用户在电脑端编码。
中午有计划休息。
下午用户在写文档时切到手机短视频。
晚上用户复盘，LLM 判断可能是注意力漂移。
用户反馈：这次不是逃避，是计划休息。
系统后续复盘变得更准确。
周报显示下午 14:00 - 16:00 是高切换时段。
```

### 5.3 Demo 数据文件

```text
packages/demo-data/
  demo-day.json
  demo-cross-device.json
  demo-review.json
  demo-feedback.json
  demo-query.json
  demo-trend.json
```

### 5.4 DemoDataImporter

Android 和 Desktop 都要实现 DemoDataImporter。

接口：

```text
importDemoData()
clearDemoData()
isDemoModeEnabled()
```

Demo 数据必须打标：

```text
metadata_json.demo = true
```

用户可以一键清除 Demo 数据。

------

## 6. Demo 脚本

### 6.1 Demo 视频主线

Demo 视频建议 90 秒。

脚本：

```text
0 - 10 秒：
展示问题：你每天在手机和电脑之间切换，但很难知道注意力到底去了哪里。

10 - 25 秒：
展示跨设备时间线：
09:00 Desktop 编码
10:30 Desktop 浏览器查资料
14:03 Android 短视频
14:35 Desktop 回到文档

25 - 45 秒：
展示 LLM 复盘：
系统指出 14:03 可能是一次从文档写作到手机娱乐的注意力漂移。

45 - 60 秒：
展示用户反馈：
用户点击“这是计划休息”，系统记住。

60 - 75 秒：
展示自然语言查询：
用户问：我这周什么时候最容易刷手机？

75 - 90 秒：
展示周报趋势和隐私中心：
No account. No developer server. Local summaries only.
```

### 6.2 Demo GIF

README 顶部 GIF 内容：

```text
Dashboard -> Cross-device Timeline -> Review -> Feedback -> Query -> Trends -> Privacy Center
```

时长建议：

```text
10 - 20 秒
```

------

## 7. 文档站设计

### 7.1 文档目录

```text
docs/
  index.md
  getting-started.md
  installation.md
  android-setup.md
  desktop-setup.md
  demo-mode.md
  llm-provider-setup.md
  privacy.md
  security.md
  architecture.md
  data-model.md
  sync-protocol.md
  llm-payloads.md
  feedback-system.md
  query-system.md
  trend-reports.md
  backup-restore.md
  troubleshooting.md
  development.md
  contributing.md
```

### 7.2 Getting Started

必须包含：

```text
1. 安装 Desktop。
2. 安装 Android。
3. 开启 Usage Access。
4. 配置 DeepSeek / Qwen。
5. 生成第一份复盘。
6. 配对手机和电脑。
7. 开启局域网同步。
8. 查看跨设备时间线。
```

### 7.3 Privacy 文档

必须清楚说明：

```text
Life Debugger 采集什么？
不采集什么？
同步什么？
不上传什么？
什么时候会调用云端 LLM？
LLM 会收到什么？
用户如何预览和阻止？
如何删除数据？
如何导出数据？
```

### 7.4 Security 文档

必须说明：

```text
设备配对
局域网同步
同步包签名
同步包加密
设备撤销
API Key 存储
私钥存储
备份加密
日志脱敏
Safe Mode
威胁模型
```

------

## 8. 架构图与数据流图

第十阶段需要生成图。

### 8.1 architecture.svg

内容：

```text
Android App
Desktop App
Local SQLite
SummaryEvent
LAN Sync
Primary Analysis Device
LLM Provider
Review / Query / Trends
Security & Privacy Center
```

### 8.2 data-flow.svg

展示：

```text
Raw local events
        ↓
Local summarization
        ↓
SummaryEvent
        ↓
Local DB
        ↓
LAN sync，optional
        ↓
LLM payload preview
        ↓
User-configured LLM provider
        ↓
Review saved locally
```

必须强调：

```text
raw events 不进入 LLM
raw events 不同步
developer server 不存在
```

### 8.3 sync-flow.svg

展示：

```text
Pairing QR
Device Identity
Pairwise Key
Signed Envelope
Encrypted Payload
SyncBundle
Deduplication
```

### 8.4 privacy-model.svg

展示：

```text
No account
No developer server
No raw upload
Local summaries
User-owned LLM key
Payload preview
Encrypted LAN sync
Encrypted backup
```

------

## 9. 安装包构建

### 9.1 Android 构建

需要支持：

```text
Debug APK
Release APK
Signed APK
```

Gradle 任务：

```text
./gradlew assembleDebug
./gradlew assembleRelease
```

第十阶段需要文档化：

```text
如何本地构建 APK
如何配置签名
如何安装到手机
如何开启 Usage Access
```

### 9.2 Desktop 构建

Tauri 构建：

```text
npm run tauri build
```

目标平台：

```text
Windows 11，第一优先级
macOS，后续
Linux，后续
```

Windows 输出：

```text
.exe installer
.msi，可选
```

### 9.3 Release Artifacts

GitHub Release 应包含：

```text
life-debugger-desktop-windows-x64.exe
life-debugger-android-universal.apk
checksums.txt
release-notes.md
```

------

## 10. GitHub Actions

### 10.1 android-ci.yml

执行：

```text
checkout
setup-java
setup-gradle
lint
unit test
assembleDebug
upload artifact
```

### 10.2 desktop-ci.yml

执行：

```text
checkout
setup-node
setup-rust
install dependencies
typecheck
lint
test
tauri build，至少 Windows runner
upload artifact
```

### 10.3 release.yml

触发：

```text
push tag v*
manual workflow_dispatch
```

执行：

```text
build Android release
build Desktop release
generate checksum
create GitHub Release
upload artifacts
```

### 10.4 docs.yml

执行：

```text
build docs
deploy GitHub Pages
```

------

## 11. 版本号策略

采用 SemVer：

```text
MAJOR.MINOR.PATCH
```

示例：

```text
0.1.0-alpha
0.2.0-alpha
0.5.0-beta
1.0.0
```

### 11.1 Alpha 版本定义

```text
核心功能可用。
可能有 bug。
数据结构可能迁移。
适合开发者和早期用户。
```

### 11.2 Beta 版本定义

```text
核心功能稳定。
隐私安全机制完善。
安装包可用。
文档较完整。
```

### 11.3 1.0 版本定义

```text
Android + Desktop 稳定。
跨设备同步稳定。
LLM 复盘稳定。
备份恢复稳定。
隐私文档完整。
```

------

## 12. Release Notes 模板

```markdown
# Life Debugger v0.1.0-alpha

## Highlights

- Android local summary collection
- Desktop activity summary
- Cross-device LAN sync
- LLM daily review
- Feedback correction
- Natural language query
- Weekly trends
- Privacy and security center

## Privacy

- No account
- No developer server
- Local summaries only
- LLM payload preview
- Encrypted LAN sync

## Downloads

- Windows Desktop Installer
- Android APK

## Known Issues

- macOS and Linux are experimental
- Android background collection depends on system restrictions
- LLM behavior depends on provider

## Upgrade Notes

...
```

------

## 13. Issue 模板

### 13.1 Bug Report

字段：

```text
App version
Platform
OS version
What happened
Expected behavior
Steps to reproduce
Screenshots
Sanitized logs
```

提醒：

```text
Please do not include API keys, private data, raw logs, or screenshots containing sensitive content.
```

### 13.2 Feature Request

字段：

```text
Problem
Proposed solution
Alternative solutions
Privacy considerations
```

### 13.3 Privacy Concern

字段：

```text
Concern area
What data you think may be exposed
Steps to reproduce
Expected privacy behavior
```

### 13.4 Security Report

SECURITY.md 中说明：

```text
不要在公开 issue 中提交安全漏洞。
请通过指定邮箱或私密渠道报告。
```

如果没有邮箱，可以写：

```text
For now, please open a minimal public issue asking for a private contact channel, without disclosing exploit details.
```

------

## 14. CONTRIBUTING.md

必须包含：

```text
项目目标
隐私原则
开发环境
Android 开发
Desktop 开发
测试方式
代码风格
提交规范
PR 流程
安全相关贡献规范
不要引入会破坏隐私承诺的功能
```

重点规则：

```text
任何新采集能力必须说明：
- 采集什么
- 不采集什么
- 是否进入 LLM
- 是否同步
- 如何关闭
- 如何删除
```

------

## 15. SECURITY.md

必须包含：

```text
支持版本
报告安全问题方式
不要公开提交漏洞细节
数据安全模型
本地密钥存储
同步加密
LLM Provider 风险说明
已知限制
```

已知限制要诚实：

```text
如果用户设备本身被恶意软件控制，Life Debugger 无法完全保护本地数据。
如果用户启用云端 LLM，摘要数据会发送给用户配置的模型服务商。
Life Debugger 不控制第三方 LLM Provider 的数据处理政策。
```

------

## 16. ROADMAP.md

Roadmap 建议分层：

```text
Done
In Progress
Planned
Research
Not Planned
```

Not Planned 必须写清楚：

```text
No keystroke logging
No microphone recording
No hidden screen recording
No developer-hosted user data server
No selling user behavior data
No Accessibility Service dependency in MVP
```

这对信任非常重要。

------

## 17. License

建议开源协议：

```text
AGPL-3.0
```

或：

```text
Apache-2.0
MIT
```

选择建议：

```text
如果希望防止别人拿去闭源做商业 SaaS，可以考虑 AGPL-3.0。
如果希望最大化传播和采用，可以考虑 Apache-2.0 或 MIT。
```

MVP 推荐：

```text
Apache-2.0
```

原因：

```text
更容易被开发者和公司接受。
对 GitHub 传播更友好。
```

但如果项目未来非常强调开源保护，可以选择 AGPL。

------

## 18. 网站 Landing Page

### 18.1 页面结构

```text
Hero
Problem
Demo
How it works
Privacy first
Features
Architecture
Download
Developer docs
Roadmap
GitHub CTA
```

### 18.2 Hero 文案

```text
Debug your life across phone and desktop.

Life Debugger turns your local activity summaries into a private cross-device timeline, then uses your own LLM provider to help you understand where your attention went.
```

中文版本：

```text
调试你的人生，而不是监控你的人生。

Life Debugger 将手机和电脑上的本地行为摘要合并为跨设备时间线，并通过你自己配置的大模型生成复盘、查询和长期趋势。
```

### 18.3 Privacy Section

必须突出：

```text
No account
No developer server
No raw screen upload
No keystroke logging
No chat content
Local summaries only
Encrypted LAN sync
User-owned LLM API key
Preview before sending to LLM
```

------

## 19. Demo 数据生成器

### 19.1 DemoScenarioGenerator

生成一组虚拟数据。

接口：

```text
generateDemoDay()
generateDemoWeek()
generateDemoMonth()
clearDemoData()
```

### 19.2 Demo 事件结构

必须包含：

```text
Desktop coding
Desktop writing
Desktop browser research
Android chat
Android short video
Android planned break
Cross-device shift
Feedback correction
Query answer
Weekly trend report
```

### 19.3 Demo 数据要求

```text
不能使用真实用户数据。
不能包含真实姓名。
不能包含真实聊天内容。
不能包含真实网站历史。
不能包含真实 API Key。
```

------

## 20. UI 打磨任务

第十阶段需要统一 UI 体验。

### 20.1 空状态

所有页面必须有空状态：

```text
暂无数据
暂无复盘
暂无趋势报告
暂无配对设备
暂无查询历史
暂无反馈
```

### 20.2 Loading 状态

所有耗时操作必须显示：

```text
采集中
同步中
生成中
审计中
备份中
恢复中
导出中
```

### 20.3 Error 状态

错误必须可理解：

```text
LLM 未配置
设备不可达
同步失败
权限未开启
数据库异常
备份恢复失败
```

### 20.4 First-run Checklist

首次启动后显示：

```text
1. 开启权限
2. 配置 LLM
3. 生成第一份复盘
4. 配对设备，可选
5. 查看隐私中心
```

------

## 21. 第十阶段开发顺序

### Step 1：整理仓库结构

任务：

```text
1. 调整为 monorepo。
2. apps/android。
3. apps/desktop。
4. packages/shared-schema。
5. packages/demo-data。
6. docs。
7. website。
8. assets。
```

验收：

```text
仓库结构清晰。
Android 和 Desktop 可以独立构建。
```

------

### Step 2：编写 README

任务：

```text
1. 编写英文 README.md。
2. 编写中文 README.zh-CN.md。
3. 添加 Demo GIF 占位。
4. 添加架构图。
5. 添加安装说明。
6. 添加隐私承诺。
7. 添加 Roadmap。
```

验收：

```text
新用户 30 秒内能理解项目价值。
开发者 5 分钟内知道如何运行。
```

------

### Step 3：实现 Demo Mode

任务：

```text
1. 创建 demo-data JSON。
2. Android 实现 DemoDataImporter。
3. Desktop 实现 DemoDataImporter。
4. UI 增加 Try Demo Mode。
5. UI 增加 Clear Demo Data。
```

验收：

```text
用户不授权、不配置、不等待，也能看到完整 Demo。
```

------

### Step 4：生成 Demo 素材

任务：

```text
1. 准备 Demo 脚本。
2. 录制 Desktop Demo。
3. 录制 Android Demo。
4. 制作 README GIF。
5. 截图 Dashboard、Timeline、Review、Query、Trends、Privacy Center。
```

验收：

```text
README 和官网可以展示清晰产品效果。
```

------

### Step 5：编写 docs 文档

任务：

```text
1. Getting Started。
2. Installation。
3. Android Setup。
4. Desktop Setup。
5. LLM Provider Setup。
6. Privacy。
7. Security。
8. Architecture。
9. Sync Protocol。
10. Backup Restore。
11. Troubleshooting。
```

验收：

```text
用户和开发者不需要读源码就能理解项目。
```

------

### Step 6：生成架构图和数据流图

任务：

```text
1. architecture.svg。
2. data-flow.svg。
3. sync-flow.svg。
4. privacy-model.svg。
5. 在 README 和 docs 中引用。
```

验收：

```text
架构和隐私边界一眼能看懂。
```

------

### Step 7：配置 GitHub Actions

任务：

```text
1. android-ci.yml。
2. desktop-ci.yml。
3. release.yml。
4. docs.yml。
5. 缓存 Gradle / npm / cargo。
6. 上传构建产物。
```

验收：

```text
PR 自动测试。
Tag 自动构建 Release。
```

------

### Step 8：配置 Release 构建

任务：

```text
1. Android debug APK。
2. Android release APK。
3. Desktop Windows installer。
4. checksum 文件。
5. Release Notes 自动模板。
```

验收：

```text
GitHub Release 可以下载可安装文件。
```

------

### Step 9：添加开源治理文件

任务：

```text
1. LICENSE。
2. CONTRIBUTING.md。
3. SECURITY.md。
4. CODE_OF_CONDUCT.md。
5. CHANGELOG.md。
6. ROADMAP.md。
7. Issue templates。
8. PR template。
```

验收：

```text
项目具备标准开源协作结构。
```

------

### Step 10：实现网站 Landing Page

任务：

```text
1. 创建 website。
2. Hero。
3. Demo。
4. Features。
5. Privacy。
6. Architecture。
7. Download。
8. Docs。
9. GitHub CTA。
```

验收：

```text
项目有可分享的官网页面。
```

------

### Step 11：统一 UI 打磨

任务：

```text
1. 统一颜色。
2. 统一卡片样式。
3. 统一空状态。
4. 统一错误状态。
5. 统一加载状态。
6. 完善 First-run Checklist。
7. 添加 Demo Mode 入口。
```

验收：

```text
产品看起来像一个完整应用，而不是工程原型。
```

------

### Step 12：发布前检查

任务：

```text
1. 清理测试 API Key。
2. 清理真实个人数据。
3. 确认 Demo 数据全是虚构。
4. 确认日志脱敏。
5. 确认隐私文档准确。
6. 确认安装包可运行。
7. 确认 README 图片正常。
8. 确认 Release Artifacts 可下载。
```

验收：

```text
项目可以公开发布。
```

------

## 22. 测试计划

### 22.1 文档测试

检查：

```text
README 链接是否有效
docs 链接是否有效
安装步骤是否可执行
构建命令是否正确
隐私承诺是否和实际代码一致
```

### 22.2 构建测试

检查：

```text
Android Debug 构建
Android Release 构建
Desktop Dev 启动
Desktop Release 构建
Windows 安装包安装
GitHub Actions 构建
```

### 22.3 Demo 测试

检查：

```text
Demo Mode 可启用
Demo 数据可导入
Demo 数据可清除
Dashboard 显示 Demo 数据
Timeline 显示 Demo 数据
Review 显示 Demo 数据
Query 显示 Demo 数据
Trends 显示 Demo 数据
Privacy Center 可用
```

### 22.4 发布测试

检查：

```text
下载 APK 可安装
下载 Windows 安装包可安装
首次启动流程正常
LLM 未配置时提示正常
无权限时提示正常
Demo Mode 可用
```

------

## 23. 第十阶段验收清单

### 23.1 工程验收

```text
[ ] 仓库结构清晰
[ ] Android 可独立构建
[ ] Desktop 可独立构建
[ ] GitHub Actions 可运行
[ ] Release workflow 可运行
[ ] Demo Mode 可用
[ ] Demo 数据可清除
[ ] 安装包可生成
[ ] checksums 可生成
```

### 23.2 文档验收

```text
[ ] README.md 完成
[ ] README.zh-CN.md 完成
[ ] Getting Started 完成
[ ] Installation 完成
[ ] Privacy 文档完成
[ ] Security 文档完成
[ ] Architecture 文档完成
[ ] Sync Protocol 文档完成
[ ] Backup Restore 文档完成
[ ] Troubleshooting 完成
[ ] CONTRIBUTING 完成
[ ] SECURITY 完成
[ ] ROADMAP 完成
```

### 23.3 传播验收

```text
[ ] README 顶部有 Demo GIF
[ ] 有核心截图
[ ] 有架构图
[ ] 有数据流图
[ ] 有隐私模型图
[ ] 有官网或 Landing Page
[ ] 有 Demo 视频脚本
[ ] 有 Release Notes
```

### 23.4 隐私验收

```text
[ ] README 明确 No account
[ ] README 明确 No developer server
[ ] README 明确 No raw upload
[ ] README 明确 No keystroke logging
[ ] README 明确 No chat content
[ ] README 明确 User-owned LLM key
[ ] Privacy 文档与代码行为一致
[ ] Demo 数据不包含真实隐私
```

------

## 24. 第十阶段 Codex 执行提示词

可以直接把下面内容给 Codex：

```text
请根据《Life Debugger 第十阶段编码 Plan：发布、演示和 GitHub 包装》实现第十阶段工作。

严格要求：

1. 第十阶段不新增核心采集或分析功能。
2. 重点是发布、演示、文档、安装包、CI、开源协作和 GitHub 包装。
3. 整理 monorepo 结构。
4. 编写 README.md 和 README.zh-CN.md。
5. 编写 docs 文档。
6. 编写 Privacy 和 Security 文档。
7. 实现 Demo Mode。
8. Demo 数据必须是虚构数据。
9. Demo 数据必须可一键导入和清除。
10. 配置 Android 构建。
11. 配置 Desktop 构建。
12. 配置 GitHub Actions。
13. 配置 Release workflow。
14. 添加 LICENSE、CONTRIBUTING、SECURITY、CHANGELOG、ROADMAP。
15. 添加 Issue 和 PR 模板。
16. 添加架构图、数据流图、同步图、隐私模型图。
17. 添加 Demo GIF 和截图占位。
18. 添加网站 Landing Page。
19. 确保 README 明确写出 No account、No developer server、No raw upload、No keystroke logging、No chat content、User-owned LLM key。
20. 发布前检查不得包含真实 API Key、真实用户数据或未脱敏日志。
```

------

## 25. 第十阶段最终 Demo

最终公开 Demo 应展示：

```text
1. 用户打开 Life Debugger。
2. 点击 Try Demo Mode。
3. Dashboard 显示一天的跨设备行为。
4. Timeline 显示电脑写作到手机短视频的切换。
5. Review 显示 LLM 判断可能是注意力漂移。
6. 用户反馈“这是计划休息”。
7. Query 页面输入：我这周什么时候最容易刷手机？
8. Trends 页面展示周报。
9. Privacy Center 展示 No developer server、LLM Payload Preview、Encrypted LAN sync。
```

演示文案：

```text
Life Debugger helps you understand your attention across phone and desktop.

It does not upload raw data.
It does not record keystrokes.
It does not read chat content.
It does not require an account.

It turns local summaries into a cross-device timeline, lets your own LLM provider generate reviews, and learns from your corrections.
```

中文演示文案：

```text
Life Debugger 不是监控你的人生，而是帮助你调试自己的一天。

它不需要账号。
不上传原始数据。
不记录键盘输入。
不读取聊天内容。
不连接开发者服务器。

它只把本地生成的摘要合并成跨设备时间线，并通过你自己配置的大模型生成复盘、查询和长期趋势。
```

------

## 26. 第十阶段完成后的项目状态

第十阶段完成后，Life Debugger 达到：

```text
可安装
可演示
可理解
可信任
可贡献
可发布
```

此时可以发布：

```text
v0.1.0-alpha
```

发布目标：

```text
GitHub
Hacker News
Product Hunt
Reddit
V2EX
少数派
即刻
Twitter / X
开发者社区
```

最适合传播的定位：

```text
A local-first, privacy-first personal behavior debugger across phone and desktop.
```

中文定位：

```text
一个本地优先、隐私优先的跨设备人生调试器。
```

------

## 27. 第十阶段完成后的后续方向

第十阶段之后，不建议继续堆大阶段，而应进入版本迭代。

### v0.2

```text
增强安装体验
完善 macOS / Linux
优化 Android 后台稳定性
优化 UI
修复同步问题
```

### v0.3

```text
浏览器扩展
更准确的网页摘要
项目 / 文件上下文增强
```

### v0.4

```text
Ollama 本地 LLM
本地 embedding
本地语义搜索
```

### v0.5

```text
插件系统
自定义分析模板
社区 prompt
```

### v1.0

```text
稳定同步
稳定备份恢复
完整文档
跨平台安装包
成熟隐私审计
```

最终目标：

> Life Debugger 成为每个人都能在本地运行的个人行为操作系统。