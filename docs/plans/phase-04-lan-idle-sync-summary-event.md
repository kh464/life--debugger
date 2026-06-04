# Life Debugger 第四阶段编程 Plan：局域网空闲自动同步 SummaryEvent

## 0. 阶段目标

第四阶段实现：已经完成配对的 Android 与 Desktop，在同一局域网中可以自动同步 SummaryEvent 摘要事件。

本阶段目标：

```text
已配对设备
        ↓
同一局域网中自动发现或通过最近地址重连
        ↓
设备空闲时触发同步
        ↓
发送 SummaryEvent，不发送 raw events
        ↓
接收端幂等去重
        ↓
更新 sync_status
        ↓
设备页展示同步状态
```

本阶段成功标准：

> 手机和电脑完成第三阶段配对后，只要处于同一局域网，系统就能在设备空闲时自动把 Android 端 SummaryEvent 同步到 Desktop 端，Desktop 时间线可以看到来自 Android 的摘要事件。

------

## 1. 与前三阶段的关系

第一阶段已经完成：

```text
Android 单端闭环：
UsageEvents -> SummaryEvent -> LLM Preview -> LLM Review
```

第二阶段已经完成：

```text
Desktop 单端闭环：
Window Activity -> SummaryEvent -> LLM Preview -> LLM Review
```

第三阶段已经完成：

```text
Android 与 Desktop 第一次配对：
DeviceIdentity -> QR Pairing -> paired_devices
```

第四阶段在第三阶段基础上新增：

```text
局域网发现
同步服务
同步客户端
SyncBundle 构造
SummaryEvent 传输
接收端去重
sync_status 更新
空闲同步调度
同步状态 UI
```

不要在第四阶段重构第一、第二阶段的采集逻辑，也不要重写第三阶段的配对逻辑。

------

## 2. 第四阶段范围

### 2.1 必须实现

```text
1. Android 端 SyncClient
2. Desktop 端 SyncServer
3. Android 端 SyncBundleBuilder
4. Desktop 端 SyncReceiver
5. Desktop 端接收 /api/sync
6. SummaryEvent 幂等去重
7. Android 端 sync_status 更新
8. Desktop 端 timeline_events / summary_events 写入
9. Android 端空闲同步调度
10. Desktop 端同步状态展示
11. Android 端同步状态展示
12. 同步失败重试
13. 手动“立即同步”
14. 最近连接地址重试
15. 同步错误提示
```

### 2.2 建议实现

```text
1. Desktop 端也支持 SyncClient
2. Android 端也支持 SyncServer
3. mDNS / NSD 局域网发现
4. 同步包签名
5. 接收端设备身份校验
6. 同步历史记录
7. 只在 Wi-Fi 下同步
8. 只在电量充足时同步
9. 只在用户空闲时同步
```

### 2.3 暂不实现

```text
1. 主分析设备切换
2. 手机作为主分析设备时的反向同步
3. 跨设备 LLM 分析
4. 复盘结果回传
5. 云端同步
6. 远程服务器中继
7. 端到端加密数据库同步
8. 实时流式同步
9. 冲突编辑解决器
10. 多设备复杂拓扑
```

------

## 3. 本阶段同步方向

第四阶段采用默认方向：

```text
Android -> Desktop
```

原因：

```text
1. 第三阶段主流程是 Desktop 生成二维码，Android 扫码配对。
2. 双设备默认电脑端为主分析设备。
3. 第五阶段才实现主分析设备切换。
4. 第四阶段先跑通最重要的跨设备数据闭环。
```

第四阶段可以保留双向同步接口抽象，但 MVP 只需要真正跑通：

```text
Android SummaryEvent 同步到 Desktop。
```

------

## 4. 核心原则

第四阶段必须遵守：

```text
1. 只同步 SummaryEvent。
2. 不同步 raw_usage_events。
3. 不同步 raw_desktop_events。
4. 不同步截图。
5. 不同步聊天内容。
6. 不同步键盘输入。
7. 不上传到开发者服务器。
8. 不需要账号登录。
9. 同步不是实时同步。
10. 同步必须基于已配对设备关系。
11. 未配对设备不能同步。
12. 接收端必须幂等去重。
```

------

## 5. 同步状态模型

### 5.1 SummaryEvent sync_status

第四阶段需要正式使用 summary_events 中的 sync_status 字段。

可选值：

```text
local
pending
syncing
synced
failed
ignored
```

含义：

```text
local：本地事件，尚未进入同步队列。
pending：待同步。
syncing：正在同步。
synced：已成功同步。
failed：同步失败，后续可重试。
ignored：用户选择不参与同步。
```

Android 端状态流转：

```text
local / pending
        ↓
syncing
        ↓
synced

失败时：
syncing -> failed -> pending -> syncing
```

Desktop 接收端状态：

```text
received remote SummaryEvent 后，写入本地 summary_events。
sync_status = synced
source_device_id = Android device_id
```

------

## 6. SyncBundle 设计

### 6.1 SyncBundle

Android 向 Desktop 发送 SyncBundle。

```json
{
  "protocol": "lifedbg-sync-v1",
  "bundle_id": "uuid",
  "from_device_id": "android_phone_001",
  "from_device_name": "Pixel Phone",
  "from_device_type": "android",
  "to_device_id": "desktop_001",
  "generated_at": 1780305000000,
  "from_time": 1780300000000,
  "to_time": 1780305000000,
  "event_count": 12,
  "events": []
}
```

字段说明：

```text
protocol：固定为 lifedbg-sync-v1。
bundle_id：同步包唯一 ID。
from_device_id：发送端设备 ID。
from_device_name：发送端设备名称。
from_device_type：android。
to_device_id：接收端设备 ID。
generated_at：同步包生成时间。
from_time：本包事件的最早时间。
to_time：本包事件的最晚时间。
event_count：事件数量。
events：SummaryEvent 数组。
```

### 6.2 SyncEventItem

同步包内事件结构：

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
  "app_label": "YouTube",
  "package_name": "com.google.android.youtube",
  "summary": "连续使用 YouTube 约 24 分钟",
  "confidence": 0.86,
  "privacy_level": "summary_only",
  "app_label_mode": "real",
  "llm_allowed": true,
  "user_corrected": false,
  "metadata_json": "{}",
  "created_at": 1780304880000
}
```

注意：

```text
第四阶段按照当前产品设定，默认允许同步真实 App 名称和真实 package_name。
但必须尊重隐私设置：
- 用户关闭真实 App 名称同步时，app_label 应泛化。
- 用户关闭 package_name 同步时，package_name 应置空。
- 敏感 App 如果被设置为不同步，则该事件不进入 SyncBundle。
```

------

## 7. SyncResponse 设计

Desktop 接收成功后返回：

```json
{
  "status": "ok",
  "protocol": "lifedbg-sync-v1",
  "bundle_id": "uuid",
  "received_event_count": 12,
  "deduplicated_event_count": 2,
  "stored_event_count": 10,
  "ignored_event_count": 0,
  "server_time": 1780305010000
}
```

失败响应：

```json
{
  "status": "error",
  "protocol": "lifedbg-sync-v1",
  "bundle_id": "uuid",
  "error_code": "DEVICE_NOT_PAIRED",
  "message": "Device is not paired."
}
```

错误码：

```text
INVALID_PROTOCOL
DEVICE_NOT_PAIRED
DEVICE_ID_MISMATCH
BUNDLE_EMPTY
BUNDLE_TOO_LARGE
INVALID_EVENT_SCHEMA
SIGNATURE_INVALID
SERVER_BUSY
DATABASE_ERROR
UNKNOWN_ERROR
```

------

## 8. 桌面端同步接口

### 8.1 POST /api/sync

Desktop 本地服务新增接口：

```http
POST /api/sync
```

请求体：

```json
{
  "protocol": "lifedbg-sync-v1",
  "bundle_id": "uuid",
  "from_device_id": "android_phone_001",
  "from_device_name": "Pixel Phone",
  "from_device_type": "android",
  "to_device_id": "desktop_001",
  "generated_at": 1780305000000,
  "from_time": 1780300000000,
  "to_time": 1780305000000,
  "event_count": 12,
  "events": []
}
```

Desktop 校验：

```text
1. protocol 是否为 lifedbg-sync-v1。
2. from_device_id 是否存在于 paired_devices。
3. from_device_type 是否为 android。
4. to_device_id 是否为本机 device_id。
5. bundle_id 是否已经处理过。
6. event_count 是否与 events.length 一致。
7. events 是否全部为 SummaryEvent。
8. 每个 event.device_id 是否等于 from_device_id。
9. 每个 event.event_id 是否存在。
10. 每个 event.start_time 是否存在。
```

通过后：

```text
1. 对 events 逐条幂等插入。
2. 如果 event_id 已存在，则跳过。
3. 如果 event_id 不存在，则写入 summary_events。
4. 写入 sync_bundles 记录。
5. 更新 paired_devices.last_seen_at。
6. 返回 SyncResponse。
```

------

## 9. 数据库变更

### 9.1 Android：summary_events 增强

确保 summary_events 包含：

```sql
ALTER TABLE summary_events ADD COLUMN last_sync_attempt_at INTEGER;
ALTER TABLE summary_events ADD COLUMN synced_at INTEGER;
ALTER TABLE summary_events ADD COLUMN sync_error TEXT;
```

如果已有字段则跳过。

### 9.2 Android：sync_bundles

```sql
CREATE TABLE sync_bundles (
  bundle_id TEXT PRIMARY KEY,
  from_device_id TEXT NOT NULL,
  to_device_id TEXT NOT NULL,
  direction TEXT NOT NULL,
  generated_at INTEGER NOT NULL,
  sent_at INTEGER,
  acknowledged_at INTEGER,
  status TEXT NOT NULL,
  event_count INTEGER DEFAULT 0,
  stored_event_count INTEGER DEFAULT 0,
  deduplicated_event_count INTEGER DEFAULT 0,
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

### 9.3 Desktop：summary_events 增强

Desktop 端 summary_events 需要能保存远端事件。

如果没有以下字段，需要补充：

```sql
ALTER TABLE summary_events ADD COLUMN origin_device_id TEXT;
ALTER TABLE summary_events ADD COLUMN origin_device_type TEXT;
ALTER TABLE summary_events ADD COLUMN received_at INTEGER;
ALTER TABLE summary_events ADD COLUMN remote_event_id TEXT;
```

写入规则：

```text
本机 Desktop 事件：
origin_device_id = desktop device_id
origin_device_type = desktop
remote_event_id = null

Android 同步事件：
origin_device_id = android device_id
origin_device_type = android
remote_event_id = android event_id
```

### 9.4 Desktop：sync_bundles

```sql
CREATE TABLE sync_bundles (
  bundle_id TEXT PRIMARY KEY,
  from_device_id TEXT NOT NULL,
  to_device_id TEXT NOT NULL,
  direction TEXT NOT NULL,
  generated_at INTEGER NOT NULL,
  received_at INTEGER,
  status TEXT NOT NULL,
  event_count INTEGER DEFAULT 0,
  stored_event_count INTEGER DEFAULT 0,
  deduplicated_event_count INTEGER DEFAULT 0,
  ignored_event_count INTEGER DEFAULT 0,
  error_code TEXT,
  error_message TEXT
);
```

------

## 10. Android 模块设计

在 Android 项目中新增或完善：

```text
sync/
  SyncBundle.kt
  SyncResponse.kt
  SyncBundleBuilder.kt
  SyncClient.kt
  SyncQueueRepository.kt
  IdleSyncScheduler.kt
  NetworkStateChecker.kt
  DeviceReachabilityChecker.kt
  SyncPolicy.kt
  SyncStatusRepository.kt
```

### 10.1 SyncBundleBuilder.kt

职责：

```text
从本地 summary_events 选出待同步事件，构造 SyncBundle。
```

接口：

```kotlin
class SyncBundleBuilder(
    private val summaryRepository: SummaryRepository,
    private val privacySettingsRepository: PrivacySettingsRepository,
    private val identityManager: DeviceIdentityManager
) {
    suspend fun buildForDevice(target: PairedDevice): SyncBundle?
}
```

选取规则：

```text
1. 只选 sync_status in ('local', 'pending', 'failed')。
2. 只选用户允许同步的事件。
3. 不选 ignored。
4. 不选敏感且被用户排除的事件。
5. 每个 bundle 最多 200 条事件。
6. 每个 bundle 最大 1MB。
7. 按 start_time 升序。
```

构造后：

```text
将被选中事件标记为 syncing。
生成 sync_bundles 记录，status = created。
```

### 10.2 SyncClient.kt

职责：

```text
向 Desktop /api/sync 发送 SyncBundle。
```

接口：

```kotlin
class SyncClient(
    private val httpClient: OkHttpClient
) {
    suspend fun sendBundle(
        target: PairedDevice,
        bundle: SyncBundle
    ): Result<SyncResponse>
}
```

请求地址：

```text
http://{target.last_known_host}:{target.last_known_port}/api/sync
```

注意：

```text
第四阶段先使用 HTTP 局域网明文传输。
因为只在局域网内、只同步摘要。
后续阶段可以加入签名、加密或 HTTPS。
```

如果要更安全，可以在本阶段加入签名，但不要阻塞 MVP。

### 10.3 IdleSyncScheduler.kt

职责：

```text
在合适时机触发同步。
```

触发条件：

```text
1. 已存在 paired desktop device。
2. 当前存在 pending SummaryEvent。
3. 当前连接 Wi-Fi。
4. 距离上次同步超过 10 分钟。
5. 电量不低。
6. 用户未关闭自动同步。
7. App 后台也可通过 WorkManager 执行。
```

Android MVP 空闲判断：

```text
Android 端不强求精准判断用户空闲。
第四阶段可以采用：
- 设备正在充电，或
- 屏幕关闭，或
- WorkManager 后台周期任务触发，或
- 用户点击“立即同步”。
```

### 10.4 NetworkStateChecker.kt

职责：

```text
判断当前是否适合同步。
```

接口：

```kotlin
class NetworkStateChecker(
    private val context: Context
) {
    fun isWifiConnected(): Boolean
    fun isBatteryLow(): Boolean
    fun isCharging(): Boolean
}
```

### 10.5 SyncQueueRepository.kt

职责：

```text
管理 SummaryEvent 同步状态。
```

接口：

```kotlin
class SyncQueueRepository(
    private val summaryEventDao: SummaryEventDao,
    private val syncBundleDao: SyncBundleDao
) {
    suspend fun getPendingEvents(limit: Int): List<SummaryEvent>
    suspend fun markSyncing(eventIds: List<String>)
    suspend fun markSynced(eventIds: List<String>, syncedAt: Long)
    suspend fun markFailed(eventIds: List<String>, error: String)
    suspend fun resetStuckSyncingEvents()
}
```

resetStuckSyncingEvents 规则：

```text
如果某事件 syncing 超过 30 分钟，恢复为 pending。
```

------

## 11. Desktop 模块设计

在 Desktop 项目中新增或完善：

```text
src-tauri/src/sync/
  sync_server.rs
  sync_receiver.rs
  sync_bundle.rs
  sync_response.rs
  sync_validator.rs
  sync_deduplicator.rs
  sync_bundle_repository.rs
```

### 11.1 SyncServer

职责：

```text
在 Desktop 端启动本地同步服务，提供 /api/sync。
```

接口：

```rust
pub struct SyncServer;

impl SyncServer {
    pub async fn start(&self, port: Option<u16>) -> Result<SyncServerInfo, String>;
    pub async fn stop(&self) -> Result<(), String>;
    pub fn is_running(&self) -> bool;
}
```

第四阶段策略：

```text
Desktop App 启动后，如果存在已配对 Android 设备，则启动 SyncServer。
端口可以复用第三阶段配对服务端口，也可以使用固定同步端口。
推荐使用固定本地同步端口 + UI 可配置。
```

推荐：

```text
sync_port = 58232
pairing_port = 随机临时端口
```

paired_devices 中保存：

```text
last_known_host
last_known_port
```

配对完成后，Desktop 需要把 sync_port 告诉 Android。

如果第三阶段 PairResponse 没有 sync_port，本阶段需要扩展 PairResponse。

### 11.2 PairResponse 扩展

第三阶段 PairResponse 增加：

```json
{
  "sync_host": "192.168.1.8",
  "sync_port": 58232,
  "sync_endpoint": "http://192.168.1.8:58232/api/sync"
}
```

Android 保存到 paired_devices：

```text
last_known_host = sync_host
last_known_port = sync_port
```

### 11.3 SyncValidator

职责：

```text
校验 SyncBundle 是否来自可信设备。
```

接口：

```rust
pub struct SyncValidator;

impl SyncValidator {
    pub fn validate(&self, bundle: &SyncBundle) -> Result<(), SyncError>;
}
```

校验规则：

```text
1. protocol = lifedbg-sync-v1。
2. from_device_id 在 paired_devices 中。
3. paired_devices.pairing_status = paired。
4. to_device_id 等于本机 device_id。
5. event_count 等于 events.len。
6. bundle_id 非空。
7. events 非空。
8. 所有 event.device_id 等于 from_device_id。
```

### 11.4 SyncDeduplicator

职责：

```text
根据 event_id 去重。
```

接口：

```rust
pub struct SyncDeduplicator;

impl SyncDeduplicator {
    pub fn split_new_and_existing(
        &self,
        events: Vec<SummaryEvent>
    ) -> Result<DedupResult, String>;
}
```

DedupResult：

```rust
pub struct DedupResult {
    pub new_events: Vec<SummaryEvent>,
    pub existing_event_ids: Vec<String>,
}
```

去重规则：

```text
如果 summary_events.event_id 已存在，视为重复。
重复事件不报错。
重复事件计入 deduplicated_event_count。
```

### 11.5 SyncReceiver

职责：

```text
接收 SyncBundle，校验、去重、保存、返回 SyncResponse。
```

接口：

```rust
pub struct SyncReceiver;

impl SyncReceiver {
    pub async fn receive_bundle(&self, bundle: SyncBundle) -> SyncResponse;
}
```

处理流程：

```text
1. validate bundle。
2. 检查 bundle_id 是否已处理。
3. deduplicate events。
4. 将新事件写入 summary_events。
5. 写入 sync_bundles。
6. 更新 paired_devices.last_seen_at。
7. 返回 SyncResponse。
```

------

## 12. 局域网发现设计

第四阶段可分两层实现。

### 12.1 MVP：最近地址重试

先使用第三阶段保存的地址：

```text
paired_devices.last_known_host
paired_devices.last_known_port
```

Android 同步时直接请求：

```text
http://last_known_host:last_known_port/api/sync
```

如果失败：

```text
提示用户：
电脑不可达，请确认手机和电脑在同一 Wi-Fi，且桌面端 Life Debugger 正在运行。
```

### 12.2 增强：局域网发现

后续可加入：

```text
Android NSD / Network Service Discovery
Desktop mDNS / Zeroconf
服务名：_lifedbg-sync._tcp
```

服务信息：

```text
device_id
device_name
device_type
sync_port
protocol_version
```

发现后：

```text
1. Android 找到局域网中的 Life Debugger Desktop。
2. 根据 device_id 判断是否为已配对设备。
3. 如果是已配对设备，更新 last_known_host / last_known_port。
4. 然后同步。
```

第四阶段建议执行顺序：

```text
先实现最近地址重试。
再实现 mDNS / NSD 自动发现。
```

------

## 13. 同步策略

### 13.1 手动同步

Android DeviceSyncScreen 提供按钮：

```text
立即同步
```

点击后：

```text
1. 检查是否有已配对 Desktop。
2. 检查 Desktop last_known_host / last_known_port。
3. 构造 SyncBundle。
4. 发送 /api/sync。
5. 根据 SyncResponse 更新状态。
```

### 13.2 自动同步

Android 使用 WorkManager。

触发条件：

```text
1. 每 15 - 30 分钟检查一次。
2. 只在 Wi-Fi 下。
3. 电量不低。
4. 存在待同步 SummaryEvent。
5. 已配对 Desktop。
```

可选约束：

```text
设备充电时优先。
屏幕关闭时优先。
```

### 13.3 同步频率

默认：

```text
最小间隔：10 分钟。
周期检查：30 分钟。
每次最多同步：200 条事件。
失败后退避：5 分钟、15 分钟、30 分钟。
```

避免：

```text
不要每秒轮询。
不要实时同步。
不要在用户每产生一个事件时立刻同步。
```

------

## 14. Android UI 设计

### 14.1 DeviceSyncScreen 增强

展示：

```text
已配对设备
最近同步时间
待同步事件数量
上次同步结果
自动同步开关
仅 Wi-Fi 同步开关
立即同步按钮
```

设备卡片字段：

```text
设备名
设备类型
最近地址
最近同步时间
待同步事件数
同步状态
```

状态示例：

```text
已连接
待同步 12 条
正在同步
上次同步成功
上次同步失败：电脑不可达
```

### 14.2 SyncStatusCard

显示：

```text
自动同步：开启 / 关闭
网络：Wi-Fi / 非 Wi-Fi
电脑状态：可达 / 不可达 / 未检测
待同步事件：N 条
最近同步：时间
```

按钮：

```text
立即同步
重试
查看同步历史
```

### 14.3 TimelineScreen 增强

每条 SummaryEvent 显示 sync_status：

```text
本地
待同步
同步中
已同步
同步失败
不同步
```

用户可以：

```text
将事件设为不同步
重新加入同步队列
```

------

## 15. Desktop UI 设计

### 15.1 DeviceSyncPage 增强

展示：

```text
同步服务状态
同步服务端口
已配对设备
最近接收时间
今日接收事件数
最近同步包
```

按钮：

```text
启动同步服务
停止同步服务
刷新状态
复制同步地址
```

### 15.2 PairedDeviceList 增强

每个设备展示：

```text
设备名
设备类型
配对时间
最近连接时间
最近同步时间
已接收事件数
```

### 15.3 TimelinePage 增强

Desktop 时间线需要展示事件来源：

```text
Desktop 本机
Android 手机
```

每条事件显示：

```text
设备来源
时间范围
类别
摘要
同步来源
```

示例：

```text
14:03 - 14:27 Android 手机：连续使用 YouTube 约 24 分钟。
14:35 - 15:10 Desktop：在 VS Code 中编码约 35 分钟。
```

------

## 16. Desktop Dashboard 增强

Dashboard 增加跨设备统计入口，但第四阶段不做 LLM 跨设备分析。

展示：

```text
今日 Desktop 本机摘要数
今日 Android 同步摘要数
最近同步时间
已配对设备数
同步服务状态
```

注意：

```text
第四阶段 Desktop 可以看到 Android 事件。
但每日复盘仍然可以暂时只分析本机，或让用户手动选择是否包含 Android 同步事件。
真正默认跨设备 LLM 复盘放到第五阶段。
```

为了不混淆，第四阶段建议：

```text
Review 页面增加选项：
[ ] 包含已同步 Android 事件
默认关闭。
```

------

## 17. 安全设计

### 17.1 已配对设备校验

Desktop 接收 /api/sync 时必须校验：

```text
from_device_id 必须存在于 paired_devices。
paired_device.public_key 必须存在。
paired_device.pairing_status 必须为 paired。
```

未配对设备请求：

```text
返回 DEVICE_NOT_PAIRED。
不写入任何事件。
```

### 17.2 Bundle 防重复

Desktop 必须记录 bundle_id。

如果同一个 bundle_id 再次提交：

```text
可以返回 ok，但 stored_event_count = 0。
或者返回 DUPLICATE_BUNDLE。
推荐返回 ok，保证发送端幂等。
```

### 17.3 事件防重复

使用 event_id 去重。

```text
同一个 event_id 只保存一次。
```

### 17.4 签名可选

第四阶段 MVP 可以先不做签名。

但建议保留字段：

```json
{
  "signature": "base64_signature",
  "signed_payload_hash": "sha256_hash"
}
```

如果实现签名：

```text
Android 用 private_key 对 bundle hash 签名。
Desktop 用 paired_devices.public_key 校验。
```

如果不实现：

```text
字段可以为空。
SyncValidator 暂不校验签名。
第五阶段或安全增强阶段再启用。
```

------

## 18. 网络错误处理

Android 同步失败时需要识别：

```text
Desktop 不在线
IP 变化
端口不可达
HTTP 超时
协议错误
设备未配对
服务器忙
数据库写入失败
```

用户提示：

```text
无法连接电脑，请确认手机和电脑在同一个 Wi-Fi，并且电脑端 Life Debugger 正在运行。
```

如果返回 DEVICE_NOT_PAIRED：

```text
这台电脑未信任当前手机，请重新配对。
```

如果 IP 可能变化：

```text
电脑地址可能已变化，请重新打开电脑端设备页，或在后续版本使用自动发现。
```

------

## 19. 第四阶段开发顺序

### Step 1：扩展数据库字段

任务：

```text
1. Android summary_events 增加同步字段。
2. Android 新增 sync_bundles 表。
3. Desktop summary_events 增加 origin_device 字段。
4. Desktop 新增 sync_bundles 表。
5. 添加 migrations。
```

验收：

```text
两端 App 升级后数据库正常迁移。
旧数据不丢失。
summary_events 能保存 sync_status 和 origin_device_id。
```

------

### Step 2：Desktop 实现 SyncServer 骨架

任务：

```text
1. 新增 SyncServer。
2. 启动本地 HTTP 服务。
3. 提供 GET /health。
4. 提供 POST /api/sync 占位接口。
5. DeviceSyncPage 展示同步服务状态。
```

验收：

```text
Desktop 可以启动同步服务。
Android 或浏览器访问 /health 能得到 ok。
```

------

### Step 3：扩展第三阶段 PairResponse

任务：

```text
1. Desktop PairResponse 增加 sync_host。
2. Desktop PairResponse 增加 sync_port。
3. Desktop PairResponse 增加 sync_endpoint。
4. Android 保存到 paired_devices.last_known_host / last_known_port。
```

验收：

```text
重新配对后，Android paired_devices 中有 Desktop 的同步地址。
```

------

### Step 4：Android 实现 SyncBundleBuilder

任务：

```text
1. 查询待同步 SummaryEvent。
2. 根据隐私设置过滤字段。
3. 构造 SyncBundle。
4. 创建 sync_bundles 记录。
5. 将事件标记为 syncing。
```

验收：

```text
点击调试按钮可以生成 SyncBundle。
SyncBundle 中只包含 SummaryEvent。
不包含 raw_usage_events。
```

------

### Step 5：Android 实现 SyncClient

任务：

```text
1. 读取 paired Desktop 地址。
2. POST SyncBundle 到 /api/sync。
3. 解析 SyncResponse。
4. 超时处理。
5. 错误码处理。
```

验收：

```text
Android 可以向 Desktop /api/sync 发送请求。
Desktop 能收到请求。
```

------

### Step 6：Desktop 实现 SyncValidator

任务：

```text
1. 校验 protocol。
2. 校验 from_device_id 已配对。
3. 校验 to_device_id 是本机。
4. 校验 event_count。
5. 校验事件结构。
```

验收：

```text
未配对设备请求被拒绝。
协议错误请求被拒绝。
合法请求进入接收流程。
```

------

### Step 7：Desktop 实现 SyncReceiver

任务：

```text
1. 接收 SyncBundle。
2. 根据 event_id 去重。
3. 插入新 SummaryEvent。
4. 写入 sync_bundles。
5. 更新 paired_devices.last_seen_at / last_sync_at。
6. 返回 SyncResponse。
```

验收：

```text
Desktop 能保存 Android 同步来的 SummaryEvent。
重复同步不会重复插入。
```

------

### Step 8：Android 更新同步状态

任务：

```text
1. 收到 SyncResponse status=ok。
2. 根据本次 bundle 的 event_id 标记 synced。
3. 保存 synced_at。
4. 更新 sync_bundles 状态。
5. 失败时标记 failed 并保存 error。
```

验收：

```text
Android 同步成功后，Timeline 中事件显示“已同步”。
同步失败后显示“同步失败”。
```

------

### Step 9：Android 手动同步 UI

任务：

```text
1. DeviceSyncScreen 增加“立即同步”按钮。
2. 显示待同步事件数量。
3. 显示同步中状态。
4. 显示同步成功结果。
5. 显示同步失败原因。
```

验收：

```text
用户点击“立即同步”后，可以看到同步结果。
```

------

### Step 10：Desktop Timeline 展示 Android 事件

任务：

```text
1. TimelinePage 查询 summary_events 时包含 origin_device_type。
2. UI 标记事件来源。
3. Android 事件显示手机图标或来源标签。
4. Desktop 本机事件显示电脑标签。
```

验收：

```text
Desktop Timeline 中可以同时看到 Desktop 事件和 Android 同步事件。
```

------

### Step 11：Android WorkManager 自动同步

任务：

```text
1. 实现 IdleSyncScheduler。
2. 注册周期 WorkManager。
3. 只在 Wi-Fi 下同步。
4. 电量低时延后。
5. 遵守最小同步间隔。
6. 失败后指数退避。
```

验收：

```text
无需手动点击，在同一 Wi-Fi 下过一段时间可以自动同步。
```

------

### Step 12：同步历史页面

任务：

```text
1. Android 展示 sync_bundles 历史。
2. Desktop 展示 sync_bundles 历史。
3. 显示 bundle_id、事件数量、时间、状态、错误码。
```

验收：

```text
用户可以查看最近同步成功或失败记录。
```

------

### Step 13：错误处理与 UI 打磨

任务：

```text
1. Desktop 不在线提示。
2. 设备未配对提示。
3. Token / trust 错误提示。
4. 数据库失败提示。
5. 网络超时提示。
6. 待同步为空提示。
7. 自动同步关闭提示。
```

验收：

```text
普通用户能理解同步失败原因，并知道下一步该做什么。
```

------

## 20. 测试计划

### 20.1 单元测试

Android：

```text
SyncBundleBuilder
SyncClient response parsing
SyncQueueRepository status transition
Privacy filtering before sync
NetworkStateChecker
```

Desktop：

```text
SyncValidator
SyncDeduplicator
SyncReceiver
sync_bundles repository
origin_device fields mapping
```

重点测试：

```text
空 bundle
重复 event_id
重复 bundle_id
未配对 device_id
to_device_id 不匹配
event_count 不一致
敏感事件过滤
package_name 隐私过滤
app_label 隐私过滤
```

------

### 20.2 集成测试

场景：

```text
1. Android 与 Desktop 已配对。
2. Android 有 10 条 pending SummaryEvent。
3. Desktop SyncServer 已启动。
4. Android 点击立即同步。
5. Desktop 收到 10 条事件。
6. Android 标记 10 条为 synced。
7. 再次点击同步，不重复插入。
8. Desktop Timeline 显示 Android 事件。
```

错误场景：

```text
1. Desktop 未启动，Android 同步失败。
2. Desktop IP 错误，Android 同步超时。
3. Android 未配对，Desktop 拒绝。
4. 修改 bundle to_device_id，Desktop 拒绝。
5. 重复提交同一 bundle，Desktop 不重复插入。
```

------

### 20.3 手动测试流程

```text
1. 完成第三阶段配对。
2. Android 端产生一些 SummaryEvent。
3. Desktop 打开 DeviceSyncPage，确认 SyncServer 运行。
4. Android 打开 DeviceSyncScreen。
5. 查看待同步事件数量。
6. 点击“立即同步”。
7. Android 显示同步成功。
8. Desktop DeviceSyncPage 显示最近接收时间。
9. Desktop Timeline 显示 Android 事件。
10. Android Timeline 显示事件已同步。
11. 再次点击立即同步，Desktop 不重复插入事件。
12. 关闭 Desktop，再次点击同步，Android 显示电脑不可达。
```

------

## 21. 第四阶段验收清单

### 21.1 功能验收

```text
[ ] Desktop 可以启动 SyncServer
[ ] Desktop 提供 /health
[ ] Desktop 提供 /api/sync
[ ] Android paired_devices 保存 Desktop sync_host / sync_port
[ ] Android 可以统计 pending SummaryEvent
[ ] Android 可以构造 SyncBundle
[ ] Android 只同步 SummaryEvent
[ ] Android 不同步 raw_usage_events
[ ] Android 可以向 Desktop 发送 SyncBundle
[ ] Desktop 可以校验已配对设备
[ ] Desktop 可以拒绝未配对设备
[ ] Desktop 可以保存 Android SummaryEvent
[ ] Desktop 可以根据 event_id 去重
[ ] Desktop 可以记录 sync_bundles
[ ] Android 可以根据 SyncResponse 标记 synced
[ ] Android 同步失败时标记 failed
[ ] Android DeviceSyncScreen 显示同步状态
[ ] Desktop DeviceSyncPage 显示接收状态
[ ] Desktop Timeline 显示 Android 事件
[ ] 手动立即同步可用
[ ] WorkManager 自动同步可用
[ ] 重复同步不会重复插入
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
[ ] 未配对设备不能同步
[ ] 同步不是实时同步
[ ] 自动同步不会高频轮询
[ ] 网络失败有明确提示
[ ] 数据库迁移不破坏旧数据
```

------

## 22. 第四阶段 Codex 执行提示词

可以直接把下面内容给 Codex：

```text
请根据《Life Debugger 第四阶段编程 Plan：局域网空闲自动同步 SummaryEvent》实现 Android -> Desktop 的局域网自动同步功能。

严格要求：

1. 第四阶段只实现已配对设备之间的 SummaryEvent 同步。
2. 不实现主分析设备切换。
3. 不实现跨设备 LLM 分析。
4. 不实现云端同步。
5. 不上传任何数据到开发者服务器。
6. 同步方向 MVP 先实现 Android -> Desktop。
7. Android 端只能发送 SummaryEvent。
8. 不允许发送 raw_usage_events。
9. 不允许发送截图、聊天内容、输入内容。
10. Desktop 端必须校验 from_device_id 是否已配对。
11. Desktop 端必须校验 to_device_id 是否为本机。
12. Desktop 端必须根据 event_id 幂等去重。
13. Android 同步成功后必须更新 sync_status = synced。
14. Android 同步失败后必须更新 sync_status = failed。
15. 必须支持手动“立即同步”。
16. 必须支持 WorkManager 自动同步。
17. 自动同步必须低频，不允许实时轮询。
18. Desktop Timeline 必须能显示 Android 同步事件。
19. DeviceSync 页面必须显示同步状态。
20. 先实现最近地址重试，再考虑 mDNS / NSD 自动发现。
```

------

## 23. 第四阶段最终 Demo

目标 Demo：

```text
1. 用户已经完成第三阶段配对。
2. Android 端已有今日手机使用 SummaryEvent。
3. Desktop 端打开 Life Debugger，并启动 SyncServer。
4. Android 端进入 Devices & Sync。
5. Android 显示：待同步 12 条。
6. 用户点击“立即同步”。
7. Android 显示：同步成功，已同步 12 条。
8. Desktop 显示：刚刚接收 Pixel Phone 的 12 条摘要。
9. Desktop Timeline 显示 Android 手机事件。
10. 用户再次点击同步，Desktop 不重复插入。
11. Android 后续在 Wi-Fi 下自动同步新事件。
```

演示文案：

```text
Life Debugger 已完成局域网摘要同步。
当前阶段只同步 SummaryEvent，不同步原始事件、不上传开发者服务器。
下一阶段将实现：用户选择主分析设备，并基于跨设备摘要生成统一 LLM 复盘。
```

------

## 24. 第四阶段完成后的下一步

第四阶段完成后，进入第五阶段：

```text
主分析设备切换 + 跨设备 LLM 复盘
```

第五阶段将复用第四阶段成果：

```text
paired_devices
SyncBundle
SyncClient
SyncServer
summary_events.origin_device_id
summary_events.origin_device_type
sync_status
sync_bundles
```

第五阶段新增：

```text
主分析设备选择 UI
双向同步方向
跨设备 LLMAnalysisPackage
Android + Desktop 混合时间线
跨设备 Review
复盘结果回传
```

因此第四阶段必须保留：

```text
origin_device_id
origin_device_type
to_device_id
from_device_id
sync_status
sync_bundles
metadata_json
```

不要把同步逻辑写死到只能 Android -> Desktop 的数据结构中。MVP 可以只跑 Android -> Desktop，但模型设计要支持未来 Desktop -> Android。