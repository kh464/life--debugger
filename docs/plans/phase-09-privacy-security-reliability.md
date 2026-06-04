# Life Debugger 第九阶段编码 Plan：隐私、安全与可靠性强化

## 0. 阶段目标

第九阶段的目标不是增加新的分析功能，而是把 Life Debugger 从“可用的 MVP”强化为“值得信任的本地优先隐私工具”。

前八阶段已经实现：

```text
1. Android 单端闭环
2. Desktop 单端闭环
3. 手机电脑配对
4. 局域网 SummaryEvent 摘要同步
5. 主分析设备切换 + 跨设备 LLM 复盘
6. 反馈闭环 + 人类校正
7. 自然语言查询 + 时间线搜索
8. 周报 / 月报 / 长期趋势
```

第九阶段要重点解决：

```text
同步是否可信？
局域网传输是否安全？
API Key 和私钥是否安全保存？
数据库是否容易损坏？
备份和恢复是否可靠？
日志是否泄露隐私？
崩溃信息是否包含敏感数据？
用户是否能清楚知道哪些数据会进入 LLM？
用户是否能一键导出、删除、审计自己的数据？
```

一句话目标：

> 第九阶段把 Life Debugger 打造成真正“隐私优先、本地优先、用户可审计、可恢复、可安全同步”的个人行为调试系统。

------

## 1. 核心原则

第九阶段必须坚持：

```text
1. 不引入开发者服务器。
2. 不引入云端账号。
3. 不上传 raw_usage_events。
4. 不上传 raw_desktop_events。
5. 不上传截图、聊天内容、键盘输入或文件内容。
6. 所有敏感密钥只保存在本地安全存储。
7. 局域网同步必须基于已配对设备。
8. 同步包必须可校验来源。
9. LLM 输入必须可预览、可过滤、可审计。
10. 日志、错误、崩溃报告不得包含 API Key、原始事件、LLM 输入全文或敏感摘要。
11. 用户必须能导出、备份、恢复、删除自己的数据。
12. 用户必须能看到隐私风险检查结果。
```

------

## 2. 阶段边界

### 2.1 本阶段必须实现

```text
1. 数据分级系统。
2. 隐私审计器 Privacy Auditor。
3. LLM 输入审计。
4. 同步包签名。
5. 同步包防重放。
6. 局域网传输加密。
7. 私钥安全存储强化。
8. API Key 安全存储强化。
9. 配对设备信任管理。
10. 设备撤销与重新配对。
11. 数据库完整性检查。
12. 数据库自动备份。
13. 加密备份导出。
14. 加密备份恢复。
15. 数据导出 JSON / Markdown。
16. 数据删除与清空。
17. 日志脱敏。
18. 崩溃日志脱敏。
19. 安全模式。
20. 可靠性测试与恢复流程。
```

### 2.2 建议实现

```text
1. 密钥轮换。
2. 同步协议版本迁移。
3. 安全事件日志。
4. 隐私风险评分。
5. 敏感 App 自动检测增强。
6. LLM Provider 风险提示。
7. 网络攻击模拟测试。
8. 数据库损坏自动修复。
9. 备份文件校验。
10. 设备信任指纹展示。
```

### 2.3 本阶段不实现

```text
1. 云端同步。
2. 云端账号。
3. 多用户协作。
4. 企业管理后台。
5. 远程设备控制。
6. 自动封锁应用。
7. 监控 App 内内容。
8. Accessibility Service。
9. 后台录屏。
10. 键盘输入记录。
```

------

## 3. 威胁模型

第九阶段先明确要防什么。

### 3.1 需要防护的风险

```text
1. 未配对设备伪造同步请求。
2. 已配对设备被删除后继续同步。
3. 同步包被重复提交导致数据重复。
4. 同步包被篡改。
5. 局域网中其他设备监听同步内容。
6. 用户 API Key 被写入日志。
7. 用户私钥被明文保存。
8. LLM 输入包包含用户不想发送的敏感摘要。
9. 崩溃日志泄露用户数据。
10. 数据库损坏导致数据丢失。
11. 备份文件被别人打开。
12. 用户无法确认到底保存了哪些数据。
```

### 3.2 暂不处理的风险

```text
1. 操作系统本身被攻破。
2. 用户设备被恶意软件完全控制。
3. LLM Provider 侧的数据政策风险。
4. 用户主动复制粘贴隐私内容到外部。
5. 物理攻击者获得已解锁设备。
```

这些不是本项目 MVP 能彻底解决的问题，但 UI 中要如实提示用户。

------

## 4. 数据分级系统

第九阶段需要为所有数据定义 privacy_class。

### 4.1 隐私等级

```text
public_metadata
local_summary
sensitive_summary
secret
raw_private
derived_analysis
exportable
syncable
llm_allowed
```

含义：

```text
public_metadata：
设备名、设备类型、协议版本等低敏元数据。

local_summary：
SummaryEvent 摘要事件，例如“连续使用 YouTube 约 24 分钟”。

sensitive_summary：
金融、健康、密码管理器、敏感 App 相关摘要。

secret：
API Key、private key、pairing token、backup password 派生密钥。

raw_private：
raw_usage_events、raw_desktop_events 等原始事件。

derived_analysis：
LLM Review、TrendReport、QueryAnswer、FeedbackMemoryRule。

exportable：
用户允许导出的数据。

syncable：
允许在已配对设备间同步的数据。

llm_allowed：
允许进入 LLM 输入包的数据。
```

### 4.2 数据分级表

新增表：

```sql
CREATE TABLE data_privacy_labels (
  object_id TEXT PRIMARY KEY,
  object_type TEXT NOT NULL,
  privacy_class TEXT NOT NULL,
  syncable INTEGER DEFAULT 0,
  llm_allowed INTEGER DEFAULT 0,
  exportable INTEGER DEFAULT 1,
  sensitive INTEGER DEFAULT 0,
  reason TEXT,
  updated_at INTEGER NOT NULL
);
```

object_type 可选：

```text
summary_event
raw_usage_event
raw_desktop_event
llm_review
trend_report
query_session
user_feedback
event_correction
feedback_memory_rule
manual_intent
api_key
device_key
sync_bundle
backup_file
```

------

## 5. Privacy Auditor 隐私审计器

### 5.1 目标

Privacy Auditor 用于检查：

```text
哪些数据会同步？
哪些数据会进入 LLM？
哪些数据可导出？
哪些数据是敏感摘要？
哪些数据不应该出现在日志？
哪些配置可能增加隐私风险？
```

### 5.2 PrivacyAuditResult

```json
{
  "audit_id": "uuid",
  "created_at": 1780309000000,
  "risk_level": "low",
  "summary": "当前配置整体风险较低。敏感 App 默认不会进入 LLM。",
  "items": [
    {
      "id": "audit_item_001",
      "severity": "warning",
      "category": "llm",
      "title": "真实 App 名称允许进入 LLM",
      "detail": "当前配置允许将 App 名称发送给云端 LLM。你可以在隐私设置中关闭。",
      "action": "open_privacy_settings"
    }
  ]
}
```

severity：

```text
info
warning
high
critical
```

category：

```text
llm
sync
backup
logging
key_storage
sensitive_apps
database
export
```

### 5.3 审计规则

必须检查：

```text
1. 是否允许真实 App 名称进入 LLM。
2. 是否允许窗口标题进入 LLM。
3. 是否允许 packageName 进入 LLM。
4. 是否允许 processName 进入 LLM。
5. 敏感 App 是否被排除。
6. 金融 / 健康类事件是否进入 LLM。
7. 是否存在明文 API Key 记录。
8. 是否存在 private_key 明文记录。
9. 是否存在同步失败后 stuck syncing 事件。
10. 是否存在未签名同步包。
11. 是否存在未加密备份。
12. 是否开启调试日志。
13. 是否存在过大的 LLM 输入包。
14. 是否存在未配对设备访问记录。
```

------

## 6. LLM 输入审计

### 6.1 LlmPayloadAuditor

职责：

```text
在任何 LLM 调用前检查输入包。
```

检查对象：

```text
Daily Review Payload
CrossDevice Review Payload
QueryContext
TrendReportInputPackage
FeedbackContext
```

### 6.2 检查规则

```text
1. 不允许包含 raw_usage_events。
2. 不允许包含 raw_desktop_events。
3. 不允许包含 API Key。
4. 不允许包含 private_key。
5. 不允许包含 pairing_token。
6. 不允许包含 backup password。
7. 不允许包含键盘输入。
8. 不允许包含聊天内容。
9. 不允许包含文件内容。
10. 敏感事件如果 llm_allowed = false，不得进入 payload。
11. payload 必须包含 privacy_note。
12. payload 必须可被用户预览。
```

### 6.3 LlmPayloadAuditResult

```json
{
  "allowed": true,
  "risk_level": "low",
  "blocked_reason": null,
  "warnings": [
    "本次输入包含真实 App 名称。",
    "本次输入包含窗口标题摘要。"
  ],
  "redacted_fields": [
    "package_name",
    "process_name"
  ]
}
```

如果审计失败：

```text
阻止 LLM 调用。
显示明确原因。
允许用户修改隐私设置后重试。
```

------

## 7. 同步包签名

### 7.1 目标

防止未授权设备伪造同步包或篡改同步包。

第四阶段和第五阶段已有：

```text
SyncBundle
ReviewSummaryBundle
FeedbackSyncBundle
TrendReportSyncBundle
```

第九阶段统一增加签名。

### 7.2 SignedEnvelope

所有跨设备消息统一封装：

```json
{
  "protocol": "lifedbg-envelope-v1",
  "message_id": "uuid",
  "message_type": "sync_bundle",
  "from_device_id": "android_phone_001",
  "to_device_id": "desktop_001",
  "created_at": 1780309000000,
  "nonce": "random_base64",
  "payload_hash": "sha256_base64",
  "signature": "base64_signature",
  "payload": {}
}
```

message_type 可选：

```text
sync_bundle
review_sync_bundle
feedback_sync_bundle
trend_report_sync_bundle
role_update
health_check
```

### 7.3 签名规则

```text
1. 发送端对 canonical payload 计算 SHA-256。
2. 发送端使用本机 private_key 签名 payload_hash + nonce + created_at + to_device_id。
3. 接收端从 paired_devices 读取 from_device_id 的 public_key。
4. 接收端验证 signature。
5. 验证失败则拒绝。
```

### 7.4 防重放

新增表：

```sql
CREATE TABLE received_message_nonces (
  nonce TEXT PRIMARY KEY,
  from_device_id TEXT NOT NULL,
  message_id TEXT NOT NULL,
  received_at INTEGER NOT NULL
);
```

接收规则：

```text
1. nonce 不得重复。
2. message_id 不得重复。
3. created_at 与当前时间差不得超过 10 分钟，允许用户配置。
4. 已使用 nonce 直接拒绝。
```

错误码：

```text
SIGNATURE_MISSING
SIGNATURE_INVALID
NONCE_REUSED
MESSAGE_EXPIRED
DEVICE_NOT_PAIRED
PAYLOAD_HASH_MISMATCH
```

------

## 8. 局域网传输加密

### 8.1 目标

第四阶段使用 HTTP 局域网传输。第九阶段要加强为加密传输。

MVP 推荐路线：

```text
第一步：SignedEnvelope 签名，防篡改。
第二步：对 payload 做对称加密，防局域网监听。
第三步：保留 HTTP 作为传输层，但 body 是加密 envelope。
```

这样比直接上 HTTPS 证书简单，也更适合局域网设备。

### 8.2 EncryptedEnvelope

```json
{
  "protocol": "lifedbg-encrypted-envelope-v1",
  "message_id": "uuid",
  "from_device_id": "android_phone_001",
  "to_device_id": "desktop_001",
  "created_at": 1780309000000,
  "nonce": "random_base64",
  "key_id": "pair_key_001",
  "cipher": "AES-GCM",
  "ciphertext": "base64_ciphertext",
  "payload_hash": "sha256_base64",
  "signature": "base64_signature"
}
```

### 8.3 Pairwise Sync Key

配对完成后，双方生成或协商 pairwise_sync_key。

新增表：

```sql
CREATE TABLE pairwise_keys (
  key_id TEXT PRIMARY KEY,
  paired_device_id TEXT NOT NULL,
  key_ref TEXT NOT NULL,
  algorithm TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  rotated_at INTEGER,
  status TEXT NOT NULL
);
```

status：

```text
active
rotated
revoked
```

### 8.4 加密规则

```text
1. payload 先序列化为 canonical JSON。
2. 使用 pairwise_sync_key 加密 payload。
3. 使用 AES-GCM 或平台可用的认证加密算法。
4. nonce / iv 每次随机生成。
5. 接收端解密失败则拒绝。
6. 解密后继续校验 payload_hash 和 signature。
```

### 8.5 兼容策略

第九阶段升级协议时要支持：

```text
lifedbg-sync-v1，旧协议，只签名或未加密。
lifedbg-encrypted-envelope-v1，新协议，加密 + 签名。
```

默认新设备使用新协议。

旧设备提示：

```text
对方设备使用旧同步协议，建议升级后再同步。
```

------

## 9. 密钥安全存储强化

### 9.1 Android

必须使用：

```text
Android Keystore 保存设备私钥。
EncryptedSharedPreferences / DataStore + Keystore 保存 API Key 引用。
pairwise_sync_key 必须加密保存。
```

禁止：

```text
将 private_key 明文写入 Room。
将 API Key 明文写入 Room。
将 pairwise_sync_key 明文写入 Room。
将密钥打印到日志。
```

### 9.2 Desktop

必须使用系统安全存储：

```text
Windows Credential Manager
macOS Keychain
Linux Secret Service
```

保存：

```text
LLM API Key
device private key
pairwise_sync_key
backup encryption key，可选
```

数据库中只保存：

```text
api_key_ref
private_key_ref
pairwise_key_ref
```

### 9.3 KeyHealthChecker

检查：

```text
1. private_key 是否存在。
2. public_key 是否与 private_key 匹配。
3. API Key ref 是否可读取。
4. pairwise_sync_key 是否可读取。
5. 是否发现明文密钥遗留字段。
```

如果发现明文密钥：

```text
提示用户迁移。
迁移成功后删除明文字段。
```

------

## 10. 设备信任管理

### 10.1 PairedDevice 增强

paired_devices 增加：

```sql
ALTER TABLE paired_devices ADD COLUMN trust_status TEXT DEFAULT 'trusted';
ALTER TABLE paired_devices ADD COLUMN fingerprint TEXT;
ALTER TABLE paired_devices ADD COLUMN revoked_at INTEGER;
ALTER TABLE paired_devices ADD COLUMN last_security_check_at INTEGER;
```

trust_status：

```text
trusted
untrusted
revoked
needs_repair
```

### 10.2 设备指纹

设备指纹来自 public_key hash。

展示格式：

```text
A4F9 21C8 9B10 7E3D
```

UI 显示：

```text
设备名：Pixel Phone
类型：Android
信任状态：已信任
设备指纹：A4F9 21C8 9B10 7E3D
上次同步：2026-06-01 21:30
```

### 10.3 撤销设备

用户点击“撤销信任设备”后：

```text
1. paired_devices.trust_status = revoked。
2. 删除 pairwise_sync_key。
3. 拒绝该设备后续同步。
4. 保留历史 SummaryEvent。
5. UI 标记该设备已撤销。
```

### 10.4 重新配对

被撤销设备想再次同步，必须重新走第三阶段配对流程。

------

## 11. 数据库可靠性强化

### 11.1 IntegrityChecker

检查：

```text
1. SQLite integrity_check。
2. 必要表是否存在。
3. migration 版本是否正确。
4. summary_events 是否存在空 event_id。
5. sync_bundles 是否存在 stuck 状态。
6. llm_reviews output_json 是否可解析。
7. trend_reports metrics_json 是否可解析。
8. FTS 索引是否需要重建。
```

### 11.2 自动修复

可自动修复：

```text
1. 重建 FTS。
2. stuck syncing -> pending。
3. 缺失 privacy label 自动补齐。
4. 缺失 origin_device_id 自动用本机 device_id 填充。
5. 无效 JSON 标记为 corrupted。
```

不可自动修复：

```text
1. SQLite 文件损坏。
2. 大量主表缺失。
3. 密钥丢失。
```

这些需要进入安全恢复流程。

### 11.3 数据库备份

每次重大操作前自动备份：

```text
数据库迁移前
导入备份前
清空数据前
删除设备前
批量隐私清理前
```

备份位置：

```text
本地应用数据目录 / backups
```

备份保留策略：

```text
最多保留 10 个自动备份。
保留最近 30 天。
用户可手动清理。
```

------

## 12. 加密备份导出与恢复

### 12.1 备份类型

支持：

```text
完整加密备份
摘要数据备份
报告与复盘备份
设置备份
```

### 12.2 BackupManifest

```json
{
  "backup_id": "uuid",
  "backup_version": "lifedbg-backup-v1",
  "created_at": 1780309000000,
  "created_on_device_id": "desktop_001",
  "included_tables": [
    "summary_events",
    "llm_reviews",
    "user_feedback",
    "trend_reports",
    "query_sessions",
    "privacy_settings"
  ],
  "excluded_tables": [
    "raw_usage_events",
    "raw_desktop_events"
  ],
  "encrypted": true,
  "algorithm": "AES-GCM",
  "checksum": "sha256_base64"
}
```

### 12.3 默认备份策略

默认导出不包含：

```text
raw_usage_events
raw_desktop_events
API Key
private_key
pairing_token
pairwise_sync_key
```

用户可选：

```text
是否包含 LLM 配置，不含 API Key。
是否包含 paired_devices，不含私钥。
是否包含原始事件，默认禁止，不建议开放。
```

### 12.4 BackupExporter

职责：

```text
导出用户选择的数据。
生成 manifest。
压缩。
加密。
生成 checksum。
```

### 12.5 BackupImporter

职责：

```text
校验 manifest。
校验 checksum。
解密。
预览将导入的数据。
导入前自动备份当前数据库。
执行导入。
处理冲突。
```

冲突策略：

```text
相同 event_id：跳过。
相同 review_id：跳过。
相同 feedback_id：跳过。
设置冲突：询问用户使用当前设置还是备份设置。
```

------

## 13. 日志脱敏

### 13.1 禁止写入日志

```text
API Key
private_key
pairwise_sync_key
pairing_token
raw_usage_events 明细
raw_desktop_events 明细
完整 LLM 输入包
完整 LLM 输出
聊天内容
窗口标题全文，除非用户开启 debug 且确认
packageName，可选脱敏
```

### 13.2 SafeLogger

所有日志必须经过 SafeLogger。

接口：

```text
safeInfo(message, metadata)
safeWarn(message, metadata)
safeError(message, error, metadata)
```

SafeLogger 自动脱敏字段：

```text
api_key
authorization
private_key
pairing_token
signature
ciphertext
password
secret
llm_payload
raw_event
```

替换为：

```text
[REDACTED]
```

### 13.3 Debug Mode

Debug Mode 默认关闭。

开启时提示：

```text
调试日志可能包含更多本地运行信息，但仍不会记录 API Key、私钥、原始事件、完整 LLM 输入。
```

------

## 14. 崩溃日志脱敏

### 14.1 CrashReportSanitizer

崩溃报告只允许包含：

```text
App 版本
操作系统版本
设备类型
错误类型
脱敏堆栈
模块名
最近操作类型
```

不允许包含：

```text
数据库内容
SummaryEvent 全文
LLM 输入全文
LLM 输出全文
API Key
private_key
pairing_token
用户问题全文
窗口标题全文
App 使用明细
```

### 14.2 本地崩溃日志

崩溃日志默认只保存在本地。

用户可以：

```text
查看
复制脱敏日志
删除
导出
```

不得自动上传开发者服务器。

------

## 15. 安全模式

### 15.1 目标

当数据库损坏、密钥丢失或同步异常时，App 可以进入安全模式，而不是崩溃。

### 15.2 Safe Mode 触发条件

```text
数据库无法打开
migration 失败
密钥读取失败
隐私审计发现 critical 风险
同步协议异常大量出现
LLM 配置损坏
```

### 15.3 Safe Mode 功能

安全模式中允许：

```text
查看错误原因
运行数据库完整性检查
从备份恢复
导出脱敏日志
清空损坏配置
重建搜索索引
重置同步状态
退出安全模式
```

安全模式中禁止：

```text
采集新数据
自动同步
自动调用 LLM
自动生成报告
```

------

## 16. UI 设计

## 16.1 Security & Privacy Center

新增页面：

```text
Settings -> Security & Privacy Center
```

展示模块：

```text
隐私审计结果
密钥状态
同步安全状态
已配对设备信任状态
数据库健康状态
备份状态
日志与崩溃报告
LLM 输入审计
```

### 16.2 Privacy Audit Card

展示：

```text
当前风险等级：低 / 中 / 高
最近审计时间
发现的问题数量
建议操作
```

按钮：

```text
立即审计
查看详情
修复建议
```

### 16.3 Device Trust Page

展示：

```text
已信任设备
设备指纹
信任状态
最近同步
协议版本
是否启用加密同步
```

操作：

```text
撤销信任
重新配对
复制设备指纹
查看安全日志
```

### 16.4 Backup & Restore Page

功能：

```text
创建加密备份
导入备份
查看自动备份
删除旧备份
恢复到某个备份
导出摘要 JSON
导出报告 Markdown
```

### 16.5 Logs Page

功能：

```text
查看本地脱敏日志
导出脱敏日志
清空日志
开启 / 关闭 Debug Mode
```

### 16.6 Safe Mode Page

展示：

```text
Life Debugger 当前处于安全模式。
原因：数据库迁移失败 / 密钥不可用 / 隐私审计 critical。
```

操作：

```text
运行完整性检查
从备份恢复
重建索引
重置同步状态
导出脱敏日志
退出安全模式
```

------

## 17. Android 模块设计

新增或强化：

```text
security/
  DataPrivacyLabel.kt
  PrivacyAuditor.kt
  LlmPayloadAuditor.kt
  SignedEnvelope.kt
  EncryptedEnvelope.kt
  EnvelopeSigner.kt
  EnvelopeVerifier.kt
  EnvelopeEncryptor.kt
  EnvelopeDecryptor.kt
  NonceRepository.kt
  KeyHealthChecker.kt
  PairwiseKeyManager.kt
  DeviceTrustManager.kt
  SafeLogger.kt
  CrashReportSanitizer.kt
  SafeModeManager.kt

backup/
  BackupManifest.kt
  BackupExporter.kt
  BackupImporter.kt
  BackupIntegrityChecker.kt
  BackupRepository.kt

reliability/
  DatabaseIntegrityChecker.kt
  AutoBackupManager.kt
  MigrationGuard.kt
  SyncRepairTool.kt

ui/security/
  SecurityPrivacyCenterScreen.kt
  PrivacyAuditScreen.kt
  DeviceTrustScreen.kt
  BackupRestoreScreen.kt
  LogsScreen.kt
  SafeModeScreen.kt
```

------

## 18. Desktop 模块设计

新增或强化：

```text
src-tauri/src/security/
  data_privacy_label.rs
  privacy_auditor.rs
  llm_payload_auditor.rs
  signed_envelope.rs
  encrypted_envelope.rs
  envelope_signer.rs
  envelope_verifier.rs
  envelope_encryptor.rs
  envelope_decryptor.rs
  nonce_repository.rs
  key_health_checker.rs
  pairwise_key_manager.rs
  device_trust_manager.rs
  safe_logger.rs
  crash_report_sanitizer.rs
  safe_mode_manager.rs

src-tauri/src/backup/
  backup_manifest.rs
  backup_exporter.rs
  backup_importer.rs
  backup_integrity_checker.rs
  backup_repository.rs

src-tauri/src/reliability/
  database_integrity_checker.rs
  auto_backup_manager.rs
  migration_guard.rs
  sync_repair_tool.rs

src-tauri/src/commands/
  security_commands.rs
  backup_commands.rs
  reliability_commands.rs

src/pages/security/
  SecurityPrivacyCenterPage.tsx
  PrivacyAuditPage.tsx
  DeviceTrustPage.tsx
  BackupRestorePage.tsx
  LogsPage.tsx
  SafeModePage.tsx
```

------

## 19. API / Commands 设计

### 19.1 Security Commands

```rust
#[tauri::command]
async fn run_privacy_audit() -> Result<PrivacyAuditResult, String>;

#[tauri::command]
async fn audit_llm_payload(payload_json: String) -> Result<LlmPayloadAuditResult, String>;

#[tauri::command]
async fn get_device_trust_status() -> Result<Vec<DeviceTrustInfo>, String>;

#[tauri::command]
async fn revoke_paired_device(device_id: String) -> Result<(), String>;

#[tauri::command]
async fn rotate_pairwise_key(device_id: String) -> Result<(), String>;

#[tauri::command]
async fn run_key_health_check() -> Result<KeyHealthResult, String>;
```

### 19.2 Backup Commands

```rust
#[tauri::command]
async fn create_encrypted_backup(options: BackupOptions) -> Result<BackupResult, String>;

#[tauri::command]
async fn preview_backup_import(file_path: String) -> Result<BackupManifest, String>;

#[tauri::command]
async fn import_backup(file_path: String, options: ImportOptions) -> Result<ImportResult, String>;

#[tauri::command]
async fn list_local_backups() -> Result<Vec<BackupInfo>, String>;

#[tauri::command]
async fn delete_backup(backup_id: String) -> Result<(), String>;
```

### 19.3 Reliability Commands

```rust
#[tauri::command]
async fn run_database_integrity_check() -> Result<DatabaseHealthResult, String>;

#[tauri::command]
async fn repair_sync_status() -> Result<RepairResult, String>;

#[tauri::command]
async fn rebuild_all_indexes() -> Result<(), String>;

#[tauri::command]
async fn export_sanitized_logs() -> Result<String, String>;

#[tauri::command]
async fn clear_logs() -> Result<(), String>;

#[tauri::command]
async fn enter_safe_mode(reason: String) -> Result<(), String>;

#[tauri::command]
async fn exit_safe_mode() -> Result<(), String>;
```

------

## 20. 开发顺序

### Step 1：数据库迁移

任务：

```text
1. 新增 data_privacy_labels。
2. 新增 received_message_nonces。
3. 新增 pairwise_keys。
4. 增强 paired_devices。
5. 新增 backup 相关元数据表。
6. 新增 security_events，可选。
```

验收：

```text
数据库迁移成功。
旧数据不丢失。
旧同步功能仍可运行。
```

------

### Step 2：实现 DataPrivacyLabel 系统

任务：

```text
1. 为 SummaryEvent 自动打 privacy label。
2. 为 raw events 自动打 raw_private。
3. 为 API Key / private key 引用打 secret。
4. 为 LLM Review 打 derived_analysis。
5. 为 TrendReport 打 derived_analysis。
6. 为 UserFeedback 打 derived_analysis。
```

验收：

```text
Privacy Auditor 可以读取所有主要对象的数据分级。
```

------

### Step 3：实现 PrivacyAuditor

任务：

```text
1. 实现审计规则。
2. 检查 LLM 配置。
3. 检查同步配置。
4. 检查敏感 App 设置。
5. 检查日志设置。
6. 输出 PrivacyAuditResult。
7. UI 展示审计结果。
```

验收：

```text
Security & Privacy Center 可以显示当前隐私风险。
```

------

### Step 4：实现 LlmPayloadAuditor

任务：

```text
1. 在所有 LLM 调用前接入审计器。
2. 审计 Daily Review Payload。
3. 审计 CrossDevice Review Payload。
4. 审计 QueryContext。
5. 审计 TrendReportInputPackage。
6. 审计失败时阻止调用。
```

验收：

```text
LLM Payload 中如果包含禁止字段，系统会阻止发送并提示用户。
```

------

### Step 5：实现 SignedEnvelope

任务：

```text
1. 定义 SignedEnvelope。
2. 实现 canonical JSON。
3. 实现 payload_hash。
4. 实现签名。
5. 实现验签。
6. 接入 SyncBundle。
7. 接入 ReviewSyncBundle。
8. 接入 FeedbackSyncBundle。
9. 接入 TrendReportSyncBundle。
```

验收：

```text
被篡改的同步包会被接收端拒绝。
未签名的新版同步包会被拒绝。
```

------

### Step 6：实现 Nonce 防重放

任务：

```text
1. 新增 received_message_nonces。
2. 每个 envelope 生成随机 nonce。
3. 接收端检查 nonce 是否用过。
4. 检查 created_at 是否过期。
5. 重复 nonce 拒绝。
```

验收：

```text
重复提交同一加密 envelope 不会被再次处理。
```

------

### Step 7：实现 EncryptedEnvelope

任务：

```text
1. 实现 pairwise key 管理。
2. 实现 payload 加密。
3. 实现 payload 解密。
4. 接入同步发送端。
5. 接入同步接收端。
6. 保留旧协议兼容提示。
```

验收：

```text
局域网同步请求体不再包含明文 SummaryEvent。
接收端可以解密并处理。
```

------

### Step 8：强化密钥存储

任务：

```text
1. Android 使用 Keystore 保存 private key。
2. Desktop 使用系统安全存储保存 private key。
3. API Key 只保存安全引用。
4. pairwise_sync_key 只保存安全引用。
5. 实现 KeyHealthChecker。
6. 检测明文遗留字段。
```

验收：

```text
数据库中不存在明文 API Key、private key、pairwise key。
```

------

### Step 9：实现 DeviceTrustManager

任务：

```text
1. 展示设备指纹。
2. 支持撤销设备。
3. 撤销后拒绝同步。
4. 支持重新配对。
5. 展示 trust_status。
```

验收：

```text
用户撤销某设备后，该设备不能继续同步。
```

------

### Step 10：实现 DatabaseIntegrityChecker

任务：

```text
1. 运行 SQLite integrity_check。
2. 检查必要表。
3. 检查 JSON 字段。
4. 检查 stuck sync。
5. 检查 FTS 索引。
6. 输出 DatabaseHealthResult。
```

验收：

```text
用户可以在 UI 中运行数据库健康检查。
```

------

### Step 11：实现 AutoBackupManager

任务：

```text
1. 迁移前自动备份。
2. 导入前自动备份。
3. 删除数据前自动备份。
4. 控制备份数量。
5. UI 展示自动备份列表。
```

验收：

```text
危险操作前会创建本地备份。
```

------

### Step 12：实现加密备份导出

任务：

```text
1. 实现 BackupManifest。
2. 选择导出范围。
3. 排除 secret 和 raw_private。
4. 生成 JSON 包。
5. 加密备份文件。
6. 生成 checksum。
7. UI 支持保存备份。
```

验收：

```text
用户可以导出一个加密备份文件。
备份文件不能直接明文查看。
```

------

### Step 13：实现备份恢复

任务：

```text
1. 选择备份文件。
2. 解密。
3. 校验 checksum。
4. 预览 manifest。
5. 导入前自动备份当前数据库。
6. 按 event_id / review_id / feedback_id 去重导入。
7. UI 显示导入结果。
```

验收：

```text
用户可以从加密备份恢复数据。
重复数据不会重复导入。
```

------

### Step 14：实现 SafeLogger

任务：

```text
1. 替换项目中直接日志调用。
2. 自动脱敏敏感字段。
3. 日志分级。
4. Debug Mode 默认关闭。
5. UI 可查看脱敏日志。
```

验收：

```text
日志中不会出现 API Key、private key、pairing_token、完整 LLM payload。
```

------

### Step 15：实现 CrashReportSanitizer

任务：

```text
1. 捕获本地崩溃信息。
2. 脱敏堆栈和上下文。
3. 保存本地崩溃日志。
4. UI 可复制脱敏报告。
5. 不自动上传。
```

验收：

```text
崩溃报告不包含用户数据和密钥。
```

------

### Step 16：实现 Safe Mode

任务：

```text
1. 实现 SafeModeManager。
2. 检测启动异常。
3. 进入 SafeMode UI。
4. 提供完整性检查。
5. 提供恢复备份。
6. 提供重建索引。
7. 提供重置同步状态。
```

验收：

```text
数据库或密钥异常时，App 不直接崩溃，而是进入安全模式。
```

------

### Step 17：UI 集成

任务：

```text
1. Security & Privacy Center。
2. PrivacyAuditPage。
3. DeviceTrustPage。
4. BackupRestorePage。
5. LogsPage。
6. SafeModePage。
7. 在 LLM Preview 页面显示审计结果。
8. 在 Sync 页面显示加密 / 签名状态。
```

验收：

```text
用户能从设置页完成审计、备份、恢复、撤销设备和查看安全状态。
```

------

### Step 18：兼容性与迁移测试

任务：

```text
1. 从第八阶段旧数据库升级到第九阶段。
2. 从旧同步协议升级到加密 envelope。
3. 已配对设备重新协商 pairwise key。
4. 老数据补齐 privacy labels。
5. 旧 Review / Report 仍可查看。
```

验收：

```text
升级后旧数据不丢失，旧功能不破坏。
```

------

## 21. 测试计划

### 21.1 单元测试

测试对象：

```text
PrivacyAuditor
LlmPayloadAuditor
EnvelopeSigner
EnvelopeVerifier
EnvelopeEncryptor
EnvelopeDecryptor
NonceRepository
KeyHealthChecker
BackupExporter
BackupImporter
SafeLogger
CrashReportSanitizer
DatabaseIntegrityChecker
```

重点测试：

```text
签名成功
签名失败
payload 被篡改
nonce 重复
message 过期
未配对设备
加密解密成功
错误 key 解密失败
API Key 脱敏
LLM payload 脱敏
备份 checksum 成功
备份 checksum 失败
数据库 stuck sync 修复
```

------

### 21.2 集成测试

场景一：加密同步

```text
1. Android 和 Desktop 已配对。
2. 双方存在 pairwise key。
3. Android 构造 EncryptedEnvelope。
4. Desktop 解密并验签。
5. Desktop 保存 SummaryEvent。
6. 重放同一 envelope，Desktop 拒绝。
```

场景二：设备撤销

```text
1. Desktop 撤销 Android 设备。
2. Android 再次发送同步包。
3. Desktop 返回 DEVICE_REVOKED。
4. Desktop 不写入任何事件。
```

场景三：LLM Payload 审计

```text
1. 构造包含 packageName 的 QueryContext。
2. 用户隐私设置禁止 packageName。
3. Auditor 阻止调用。
4. UI 提示用户修改设置或移除字段。
```

场景四：加密备份恢复

```text
1. 用户导出加密备份。
2. 清空部分数据。
3. 导入备份。
4. 数据恢复。
5. 重复导入不产生重复事件。
```

场景五：安全模式

```text
1. 模拟数据库 integrity_check 失败。
2. App 进入 Safe Mode。
3. 用户从备份恢复。
4. App 正常启动。
```

------

## 22. 手动测试流程

```text
1. 打开 Security & Privacy Center。
2. 点击立即隐私审计。
3. 查看 LLM、同步、备份、日志风险项。
4. 打开 Device Trust 页面。
5. 查看已配对设备指纹。
6. 撤销一个设备。
7. 确认该设备无法同步。
8. 重新配对设备。
9. 触发一次 SummaryEvent 同步。
10. 确认同步请求使用加密 envelope。
11. 打开 LLM Preview。
12. 确认 Payload Audit 通过。
13. 创建加密备份。
14. 删除部分数据。
15. 从备份恢复。
16. 导出脱敏日志。
17. 开启 Debug Mode，确认仍不会输出密钥。
18. 模拟异常，确认 Safe Mode 可用。
```

------

## 23. 第九阶段验收清单

### 23.1 功能验收

```text
[ ] 有 Security & Privacy Center
[ ] 可以运行 Privacy Audit
[ ] 可以运行 LLM Payload Audit
[ ] 所有 LLM 调用前都会审计 payload
[ ] 有 DataPrivacyLabel 系统
[ ] 同步包支持签名
[ ] 同步包支持防重放 nonce
[ ] 同步包支持加密 envelope
[ ] 未配对设备不能同步
[ ] 被撤销设备不能同步
[ ] 可以查看设备指纹
[ ] 可以撤销设备信任
[ ] API Key 安全保存
[ ] private key 安全保存
[ ] pairwise key 安全保存
[ ] 可以运行 KeyHealthCheck
[ ] 可以运行 DatabaseIntegrityCheck
[ ] 危险操作前自动备份
[ ] 可以导出加密备份
[ ] 可以导入加密备份
[ ] 可以导出摘要 JSON
[ ] 可以导出报告 Markdown
[ ] 日志自动脱敏
[ ] 崩溃日志自动脱敏
[ ] 可以查看本地脱敏日志
[ ] 可以进入 Safe Mode
[ ] Safe Mode 可以恢复备份
```

### 23.2 非功能验收

```text
[ ] 不上传数据到开发者服务器
[ ] 不引入云端账号
[ ] 不同步 raw_usage_events
[ ] 不同步 raw_desktop_events
[ ] 不上传截图
[ ] 不上传聊天内容
[ ] 不记录键盘输入
[ ] 不读取文件内容
[ ] 日志不包含 API Key
[ ] 日志不包含 private key
[ ] 日志不包含 pairing token
[ ] 日志不包含完整 LLM payload
[ ] 崩溃报告不包含用户行为明细
[ ] 备份默认不包含 secret
[ ] 加密备份无法明文打开
[ ] 数据库迁移不破坏旧数据
[ ] 加密同步失败时有明确提示
```

------

## 24. 第九阶段 Codex 执行提示词

可以直接把下面内容给 Codex：

```text
请根据《Life Debugger 第九阶段编码 Plan：隐私、安全与可靠性强化》实现第九阶段功能。

严格要求：

1. 第九阶段只做隐私、安全与可靠性强化。
2. 不实现云端账号。
3. 不引入开发者服务器。
4. 不上传 raw_usage_events。
5. 不上传 raw_desktop_events。
6. 不上传截图、聊天内容、键盘输入或文件内容。
7. 必须实现 Security & Privacy Center。
8. 必须实现 PrivacyAuditor。
9. 必须实现 LlmPayloadAuditor。
10. 所有 LLM 调用前必须审计 payload。
11. 必须实现同步包签名。
12. 必须实现 nonce 防重放。
13. 必须实现局域网同步 payload 加密。
14. 必须强化 API Key、private key、pairwise key 的本地安全存储。
15. 必须实现设备信任管理和撤销设备。
16. 必须实现数据库完整性检查。
17. 必须实现自动备份。
18. 必须实现加密备份导出。
19. 必须实现加密备份恢复。
20. 必须实现日志脱敏。
21. 必须实现崩溃日志脱敏。
22. 必须实现 Safe Mode。
23. 日志中不得出现 API Key、private key、pairing token、完整 LLM payload。
24. 旧数据和旧数据库必须能平滑迁移。
25. 加密同步可以先实现 Android -> Desktop，再扩展 Desktop -> Android，但数据结构必须支持双向。
```

------

## 25. 第九阶段最终 Demo

目标 Demo：

```text
1. 用户打开 Security & Privacy Center。
2. 点击“立即隐私审计”。
3. 系统显示当前风险等级和建议。
4. 用户查看已配对设备指纹。
5. 用户创建一个加密备份。
6. 用户完成一次 Android -> Desktop 同步。
7. 开发者展示同步请求体已经是 encrypted envelope。
8. 用户撤销 Android 设备。
9. Android 再次同步失败，Desktop 拒绝。
10. 用户从加密备份恢复数据。
11. 用户导出脱敏日志，日志中没有 API Key 或隐私数据。
```

示例演示文案：

```text
Life Debugger 处理的是用户最敏感的行为数据。
第九阶段让它真正具备开源隐私工具应有的可信基础：

- 不上传开发者服务器；
- 同步包签名和加密；
- 设备可撤销；
- LLM 输入可审计；
- 日志自动脱敏；
- 数据可加密备份和恢复；
- 出错时进入安全模式，而不是损坏用户数据。
```

------

## 26. 第九阶段完成后的下一步

第九阶段完成后，进入第十阶段：

```text
发布、演示和 GitHub 包装
```

第十阶段将重点处理：

```text
README
官网
Demo GIF
架构图
隐私白皮书
安装包
示例数据
贡献指南
Roadmap
Issue 模板
Release 流程
自动构建
代码签名
```

第九阶段需要为第十阶段提供：

```text
隐私承诺材料
安全架构说明
数据流图
威胁模型
本地优先说明
同步安全说明
备份恢复说明
LLM 输入审计说明
日志脱敏说明
```

这些内容会成为第十阶段 GitHub README 和官网的核心信任资产。