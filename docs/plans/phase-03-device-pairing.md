# Life Debugger 第三阶段编码 Plan：手机与电脑第一次配对认证

## 0. 阶段目标

第三阶段只实现 Android 端与 Desktop 端的第一次配对认证。

本阶段目标：

```text
Android App 与 Desktop App 可以在同一局域网内完成一次安全配对。
配对完成后，双方保存彼此设备身份。
设备页可以显示已配对设备。
用户可以解除配对。
```

本阶段不实现：

```text
自动同步 SummaryEvent
局域网空闲同步
mDNS 自动发现
主分析设备切换
跨设备时间线合并
LLM 跨设备复盘
复盘结果回传
后台自动连接
实时同步
云端账号
远程服务器
```

本阶段成功标准：

> 用户打开 Desktop，生成配对二维码；用户用 Android 扫码后，双方完成配对，并在各自设备页看到对方设备。

------

## 1. 与前两个阶段的关系

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

第三阶段只在这两个单端应用之间增加：

```text
设备身份生成
配对二维码
局域网临时配对服务
配对请求
配对确认
可信设备保存
设备页展示
解除配对
```

不要在第三阶段改动第一、第二阶段的核心采集和 LLM 分析逻辑。

------

## 2. 第三阶段范围

### 2.1 必须实现

```text
1. Android 端设备身份生成
2. Desktop 端设备身份生成
3. Android 端 paired_devices 表
4. Desktop 端 paired_devices 表
5. Desktop 端生成配对二维码
6. Desktop 端启动临时配对 HTTP 服务
7. Android 端扫码读取二维码
8. Android 端向 Desktop 发起配对请求
9. Desktop 端校验 pairing_token
10. Desktop 端保存 Android 设备信息
11. Android 端保存 Desktop 设备信息
12. 双方设备页展示已配对设备
13. 用户可以解除配对
14. 配对失败错误提示
15. 配对成功提示
```

### 2.2 建议实现

```text
1. Android 端也能生成二维码
2. Desktop 端也能扫描或输入配对码
3. 配对 token 过期时间
4. 配对请求签名或 challenge 校验
5. 配对服务超时自动关闭
6. 设备名自定义
```

### 2.3 暂不实现

```text
1. mDNS / Bonjour / Zeroconf 自动发现
2. SyncBundle 传输
3. SummaryEvent 跨设备同步
4. 主分析设备切换
5. 跨设备 LLM 分析
6. 空闲同步调度器
7. 设备在线状态持续检测
```

------

## 3. 设备身份模型

每个设备首次启动时，都必须生成自己的本地设备身份。

### 3.1 DeviceIdentity

Android 与 Desktop 都使用统一设备身份结构。

```json
{
  "device_id": "uuid",
  "device_name": "Ko-PC",
  "device_type": "desktop",
  "public_key": "base64_public_key",
  "created_at": 1780305000000
}
```

字段说明：

```text
device_id：本地生成的 UUID，长期稳定。
device_name：用户可见设备名。
device_type：android 或 desktop。
public_key：设备公钥，用于后续可信设备识别。
created_at：设备身份创建时间。
```

### 3.2 设备身份生成规则

```text
首次启动时检查本地是否已有 device_id。
如果没有，则生成：
- device_id
- device_name
- key pair
- created_at

后续启动不得重复生成。
除非用户在数据管理页选择“重置本设备身份”。
```

### 3.3 Key Pair 要求

MVP 可选两档实现。

#### 简化实现

```text
生成随机 public_key / private_key 占位。
本阶段只完成设备身份和配对流程。
第四阶段同步前再完善签名与加密。
```

#### 推荐实现

```text
Android 使用 Android Keystore 生成密钥对。
Desktop 使用系统安全存储或本地安全文件保存私钥。
public_key 可以写入普通数据库。
private_key 不能明文写入普通数据库。
```

建议第三阶段直接采用推荐实现，但如果 Codex 实现困难，可以先使用简化实现跑通配对流程。

------

## 4. 数据库设计

### 4.1 Android：device_identity

```sql
CREATE TABLE device_identity (
  device_id TEXT PRIMARY KEY,
  device_name TEXT NOT NULL,
  device_type TEXT NOT NULL,
  public_key TEXT NOT NULL,
  private_key_ref TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
```

### 4.2 Desktop：device_identity

```sql
CREATE TABLE device_identity (
  device_id TEXT PRIMARY KEY,
  device_name TEXT NOT NULL,
  device_type TEXT NOT NULL,
  public_key TEXT NOT NULL,
  private_key_ref TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
```

### 4.3 Android：paired_devices

```sql
CREATE TABLE paired_devices (
  paired_device_id TEXT PRIMARY KEY,
  paired_device_name TEXT NOT NULL,
  paired_device_type TEXT NOT NULL,
  public_key TEXT NOT NULL,
  last_known_host TEXT,
  last_known_port INTEGER,
  pairing_status TEXT NOT NULL,
  trust_level TEXT DEFAULT 'trusted',
  paired_at INTEGER NOT NULL,
  last_seen_at INTEGER,
  last_sync_at INTEGER,
  metadata_json TEXT
);
```

### 4.4 Desktop：paired_devices

```sql
CREATE TABLE paired_devices (
  paired_device_id TEXT PRIMARY KEY,
  paired_device_name TEXT NOT NULL,
  paired_device_type TEXT NOT NULL,
  public_key TEXT NOT NULL,
  last_known_host TEXT,
  last_known_port INTEGER,
  pairing_status TEXT NOT NULL,
  trust_level TEXT DEFAULT 'trusted',
  paired_at INTEGER NOT NULL,
  last_seen_at INTEGER,
  last_sync_at INTEGER,
  metadata_json TEXT
);
```

### 4.5 pairing_sessions

配对发起方需要保存临时配对会话。

Android 与 Desktop 都可以有此表。

```sql
CREATE TABLE pairing_sessions (
  session_id TEXT PRIMARY KEY,
  pairing_token TEXT NOT NULL,
  local_device_id TEXT NOT NULL,
  local_device_type TEXT NOT NULL,
  local_host TEXT,
  local_port INTEGER,
  expires_at INTEGER NOT NULL,
  status TEXT NOT NULL,
  created_at INTEGER NOT NULL
);
```

status 可选值：

```text
created
waiting
paired
expired
cancelled
failed
```

------

## 5. 配对协议

### 5.1 协议版本

本阶段协议名：

```text
lifedbg-pairing-v1
```

所有配对二维码与配对请求都必须携带：

```json
{
  "protocol": "lifedbg-pairing-v1"
}
```

这样后续协议升级时可以兼容。

------

## 6. Desktop 生成二维码配对流程

这是第三阶段的主流程。

### 6.1 用户流程

```text
1. 用户打开 Desktop App。
2. 进入 Devices & Sync 页面。
3. 点击“添加 Android 设备”。
4. Desktop 生成临时 pairing_session。
5. Desktop 启动本地 HTTP 配对服务。
6. Desktop 显示二维码。
7. 用户打开 Android App。
8. Android 进入 Devices & Sync 页面。
9. Android 点击“扫描电脑二维码”。
10. Android 扫描二维码。
11. Android 向 Desktop 发送配对请求。
12. Desktop 校验请求。
13. Desktop 保存 Android 设备。
14. Android 保存 Desktop 设备。
15. 双方显示配对成功。
```

------

## 7. 二维码内容

Desktop 生成二维码时，二维码内包含 PairingQrPayload。

```json
{
  "protocol": "lifedbg-pairing-v1",
  "session_id": "uuid",
  "device_id": "desktop_001",
  "device_name": "Ko-PC",
  "device_type": "desktop",
  "host": "192.168.1.8",
  "port": 58231,
  "public_key": "base64_public_key",
  "pairing_token": "random_token",
  "expires_at": 1780305600000
}
```

字段说明：

```text
protocol：协议版本。
session_id：本次临时配对会话 ID。
device_id：发起配对的设备 ID。
device_name：发起配对的设备名称。
device_type：desktop。
host：Desktop 在局域网中的 IP。
port：Desktop 临时配对服务端口。
public_key：Desktop 公钥。
pairing_token：一次性随机 token。
expires_at：二维码过期时间。
```

### 7.1 pairing_token 要求

```text
长度至少 32 bytes。
使用安全随机数生成。
只在本地保存。
只允许使用一次。
默认 5 分钟过期。
配对成功后立即失效。
用户取消配对后立即失效。
```

------

## 8. Android 扫码后发起配对请求

### 8.1 PairRequest

Android 扫描二维码后，向 Desktop 发起：

```http
POST http://{host}:{port}/api/pair
```

请求体：

```json
{
  "protocol": "lifedbg-pairing-v1",
  "session_id": "uuid",
  "pairing_token": "random_token",
  "device_id": "android_phone_001",
  "device_name": "Pixel Phone",
  "device_type": "android",
  "public_key": "android_base64_public_key",
  "timestamp": 1780305300000
}
```

### 8.2 Desktop 校验逻辑

Desktop 收到请求后校验：

```text
1. protocol 是否等于 lifedbg-pairing-v1。
2. session_id 是否存在。
3. pairing_session 状态是否为 waiting。
4. pairing_token 是否匹配。
5. pairing_token 是否未过期。
6. device_id 是否存在。
7. public_key 是否存在。
8. device_type 是否为 android。
9. 是否已经配对过相同 device_id。
```

如果通过：

```text
1. 保存 Android 到 paired_devices。
2. 将 pairing_session 状态改为 paired。
3. 返回 PairResponse。
```

如果失败：

```text
返回明确错误码和错误信息。
不要保存设备。
```

------

## 9. PairResponse

Desktop 返回给 Android：

```json
{
  "status": "ok",
  "protocol": "lifedbg-pairing-v1",
  "session_id": "uuid",
  "device_id": "desktop_001",
  "device_name": "Ko-PC",
  "device_type": "desktop",
  "public_key": "desktop_base64_public_key",
  "paired_at": 1780305303000,
  "message": "paired"
}
```

Android 收到后：

```text
1. 校验 status = ok。
2. 校验 protocol。
3. 校验 session_id。
4. 保存 Desktop 到 paired_devices。
5. 显示配对成功。
```

------

## 10. 错误码设计

PairResponse 失败格式：

```json
{
  "status": "error",
  "error_code": "TOKEN_EXPIRED",
  "message": "Pairing token has expired."
}
```

错误码：

```text
INVALID_PROTOCOL
SESSION_NOT_FOUND
SESSION_NOT_WAITING
TOKEN_INVALID
TOKEN_EXPIRED
DEVICE_TYPE_NOT_ALLOWED
PUBLIC_KEY_MISSING
ALREADY_PAIRED
SERVER_UNREACHABLE
NETWORK_ERROR
UNKNOWN_ERROR
```

UI 友好提示：

```text
二维码已过期，请在电脑端重新生成。
配对码无效，请重新扫描。
电脑端无法连接，请确认手机和电脑在同一个 Wi-Fi。
该设备已经配对过。
配对失败，请重试。
```

------

## 11. Android 模块设计

在第一阶段 Android 项目基础上新增：

```text
sync/
  DeviceIdentityManager.kt
  PairingQrScanner.kt
  PairingClient.kt
  PairingRepository.kt
  PairedDeviceRepository.kt

ui/
  devices/
    DeviceSyncScreen.kt
    ScanPairingQrScreen.kt
    PairedDeviceListScreen.kt
    PairingResultScreen.kt
```

### 11.1 DeviceIdentityManager.kt

职责：

```text
生成和读取本机设备身份。
```

接口：

```kotlin
class DeviceIdentityManager(
    private val repository: DeviceIdentityRepository,
    private val keyStore: DeviceKeyStore
) {
    suspend fun getOrCreateIdentity(): DeviceIdentity
    suspend fun getIdentity(): DeviceIdentity?
    suspend fun resetIdentity()
}
```

### 11.2 PairingQrScanner.kt

职责：

```text
扫描 Desktop 端二维码。
解析 PairingQrPayload。
校验协议字段。
```

接口：

```kotlin
class PairingQrScanner {
    fun parsePayload(rawText: String): Result<PairingQrPayload>
}
```

### 11.3 PairingClient.kt

职责：

```text
向 Desktop 临时配对服务发送 PairRequest。
接收 PairResponse。
```

接口：

```kotlin
class PairingClient(
    private val httpClient: OkHttpClient
) {
    suspend fun pairWithDesktop(
        qrPayload: PairingQrPayload,
        localIdentity: DeviceIdentity
    ): Result<PairResponse>
}
```

### 11.4 PairingRepository.kt

职责：

```text
协调扫码、请求、保存配对设备。
```

接口：

```kotlin
class PairingRepository(
    private val identityManager: DeviceIdentityManager,
    private val pairingClient: PairingClient,
    private val pairedDeviceRepository: PairedDeviceRepository
) {
    suspend fun pairByQrPayload(rawQrText: String): Result<PairedDevice>
}
```

### 11.5 PairedDeviceRepository.kt

职责：

```text
保存、查询、删除已配对设备。
```

接口：

```kotlin
class PairedDeviceRepository(
    private val dao: PairedDeviceDao
) {
    suspend fun upsert(device: PairedDevice)
    suspend fun getAll(): List<PairedDevice>
    suspend fun delete(deviceId: String)
    suspend fun isPaired(deviceId: String): Boolean
}
```

------

## 12. Desktop 模块设计

在第二阶段 Desktop 项目基础上新增：

```text
src-tauri/src/sync/
  device_identity.rs
  device_key_store.rs
  pairing_session.rs
  pairing_server.rs
  pairing_qr_payload.rs
  paired_device_repository.rs

src-tauri/src/commands/
  device_commands.rs
  pairing_commands.rs

src/pages/devices/
  DeviceSyncPage.tsx
  PairingQrDialog.tsx
  PairedDeviceList.tsx
  PairingStatusCard.tsx
```

### 12.1 device_identity.rs

职责：

```text
生成和读取 Desktop 本机设备身份。
```

接口：

```rust
pub struct DeviceIdentityManager;

impl DeviceIdentityManager {
    pub fn get_or_create_identity(&self) -> Result<DeviceIdentity, String>;
    pub fn get_identity(&self) -> Result<Option<DeviceIdentity>, String>;
    pub fn reset_identity(&self) -> Result<(), String>;
}
```

### 12.2 pairing_session.rs

职责：

```text
创建临时配对会话。
生成 pairing_token。
设置过期时间。
维护 session 状态。
```

接口：

```rust
pub struct PairingSessionManager;

impl PairingSessionManager {
    pub fn create_session(&self, local_identity: DeviceIdentity) -> Result<PairingSession, String>;
    pub fn get_session(&self, session_id: &str) -> Result<Option<PairingSession>, String>;
    pub fn mark_paired(&self, session_id: &str) -> Result<(), String>;
    pub fn cancel_session(&self, session_id: &str) -> Result<(), String>;
    pub fn expire_old_sessions(&self) -> Result<(), String>;
}
```

### 12.3 pairing_server.rs

职责：

```text
启动本地临时 HTTP 服务。
提供 /api/pair 接口。
校验 PairRequest。
返回 PairResponse。
```

接口：

```rust
pub struct PairingServer;

impl PairingServer {
    pub async fn start(&self, port: Option<u16>) -> Result<PairingServerInfo, String>;
    pub async fn stop(&self) -> Result<(), String>;
    pub fn is_running(&self) -> bool;
}
```

### 12.4 pairing_qr_payload.rs

职责：

```text
根据 PairingSession 和 DeviceIdentity 构造二维码 payload。
```

接口：

```rust
pub struct PairingQrPayloadBuilder;

impl PairingQrPayloadBuilder {
    pub fn build(
        identity: DeviceIdentity,
        session: PairingSession,
        host: String,
        port: u16
    ) -> Result<PairingQrPayload, String>;
}
```

### 12.5 paired_device_repository.rs

职责：

```text
保存、查询、删除已配对设备。
```

接口：

```rust
pub struct PairedDeviceRepository;

impl PairedDeviceRepository {
    pub fn upsert(&self, device: PairedDevice) -> Result<(), String>;
    pub fn get_all(&self) -> Result<Vec<PairedDevice>, String>;
    pub fn delete(&self, device_id: &str) -> Result<(), String>;
    pub fn is_paired(&self, device_id: &str) -> Result<bool, String>;
}
```

------

## 13. Desktop Tauri Commands

### 13.1 设备身份命令

```rust
#[tauri::command]
async fn get_local_device_identity() -> Result<DeviceIdentity, String>;

#[tauri::command]
async fn update_local_device_name(device_name: String) -> Result<DeviceIdentity, String>;

#[tauri::command]
async fn reset_local_device_identity() -> Result<(), String>;
```

### 13.2 配对命令

```rust
#[tauri::command]
async fn start_pairing_session() -> Result<PairingQrPayload, String>;

#[tauri::command]
async fn cancel_pairing_session(session_id: String) -> Result<(), String>;

#[tauri::command]
async fn get_pairing_session_status(session_id: String) -> Result<PairingSessionStatus, String>;
```

### 13.3 已配对设备命令

```rust
#[tauri::command]
async fn get_paired_devices() -> Result<Vec<PairedDevice>, String>;

#[tauri::command]
async fn remove_paired_device(device_id: String) -> Result<(), String>;
```

------

## 14. Desktop 本地 HTTP 接口

Desktop 临时配对服务至少提供一个接口。

### 14.1 POST /api/pair

请求：

```json
{
  "protocol": "lifedbg-pairing-v1",
  "session_id": "uuid",
  "pairing_token": "random_token",
  "device_id": "android_phone_001",
  "device_name": "Pixel Phone",
  "device_type": "android",
  "public_key": "android_base64_public_key",
  "timestamp": 1780305300000
}
```

成功响应：

```json
{
  "status": "ok",
  "protocol": "lifedbg-pairing-v1",
  "session_id": "uuid",
  "device_id": "desktop_001",
  "device_name": "Ko-PC",
  "device_type": "desktop",
  "public_key": "desktop_base64_public_key",
  "paired_at": 1780305303000,
  "message": "paired"
}
```

失败响应：

```json
{
  "status": "error",
  "error_code": "TOKEN_EXPIRED",
  "message": "Pairing token has expired."
}
```

------

## 15. Android UI 编程计划

### 15.1 DeviceSyncScreen

入口位置：

```text
Settings -> Devices & Sync
```

展示内容：

```text
本机设备名
本机设备类型：Android
本机设备 ID，默认折叠
已配对设备数量
已配对设备列表
```

按钮：

```text
扫描电脑二维码
刷新设备列表
解除配对
```

### 15.2 ScanPairingQrScreen

功能：

```text
打开相机扫码。
解析二维码。
显示待配对设备信息。
用户确认后发起配对。
```

扫码成功后的确认页：

```text
即将连接设备：

设备名：Ko-PC
设备类型：Desktop
局域网地址：192.168.1.8:58231

请确认这是你自己的电脑。
```

按钮：

```text
确认配对
取消
```

### 15.3 PairingResultScreen

成功状态：

```text
配对成功
你的手机已连接到 Ko-PC。
后续阶段将支持局域网自动同步摘要。
```

失败状态：

```text
配对失败
原因：二维码已过期 / 无法连接电脑 / token 无效。
```

按钮：

```text
重新扫描
返回设备页
```

------

## 16. Desktop UI 编程计划

### 16.1 DeviceSyncPage

入口位置：

```text
Sidebar -> Devices
或 Settings -> Devices & Sync
```

展示内容：

```text
本机设备名
本机设备类型：Desktop
本机设备 ID，默认折叠
已配对设备列表
当前配对会话状态
```

按钮：

```text
添加 Android 设备
刷新设备列表
解除配对
修改本机设备名
```

### 16.2 PairingQrDialog

点击“添加 Android 设备”后弹出。

展示内容：

```text
二维码
配对码过期倒计时
当前电脑名称
当前局域网地址
```

提示文案：

```text
请在 Android App 中打开“设备与同步”，扫描此二维码完成配对。
二维码 5 分钟后过期。
```

按钮：

```text
取消配对
重新生成二维码
```

### 16.3 PairedDeviceList

展示字段：

```text
设备名
设备类型
配对时间
最近连接时间
信任状态
```

操作：

```text
解除配对
查看设备详情
```

------

## 17. 安全设计

### 17.1 第三阶段最低安全要求

```text
pairing_token 必须随机生成。
pairing_token 必须有过期时间。
pairing_token 只能使用一次。
配对接口只在本地局域网监听。
配对服务只有用户点击“添加设备”后才启动。
配对成功或取消后关闭配对服务。
不要将 private_key 写入普通数据库。
不要将 pairing_token 打印到日志。
不要将配对请求中的敏感字段打印到日志。
```

### 17.2 用户确认

Android 扫码后必须显示 Desktop 设备信息，让用户确认：

```text
设备名
设备类型
局域网地址
```

用户点击“确认配对”后才发送 PairRequest。

### 17.3 已配对设备覆盖策略

如果同一个 device_id 已经配对过：

推荐策略：

```text
提示用户：该设备已配对，是否更新设备信息？
```

MVP 简化策略：

```text
直接返回 ALREADY_PAIRED。
用户需要先解除旧配对，再重新配对。
```

建议第三阶段采用 MVP 简化策略。

------

## 18. 网络设计

### 18.1 Desktop 本地服务绑定地址

MVP 推荐：

```text
监听 0.0.0.0:{random_port}
```

二维码中填入：

```text
当前局域网 IPv4 地址
```

如果存在多个网卡：

```text
优先选择非 loopback、非虚拟网卡、局域网 IPv4。
如果无法判断，允许用户在 UI 中切换 IP。
```

### 18.2 端口选择

```text
默认随机选择 50000 - 60000 之间可用端口。
避免固定端口冲突。
端口写入二维码。
```

### 18.3 防火墙提示

如果 Android 无法连接 Desktop：

UI 提示：

```text
无法连接电脑。请确认：
1. 手机和电脑在同一个 Wi-Fi；
2. 电脑端配对二维码仍未过期；
3. Windows 防火墙没有阻止 Life Debugger；
4. 电脑端应用仍在运行。
```

------

## 19. 第三阶段开发顺序

### Step 1：统一设备身份模型

任务：

```text
1. Android 新增 DeviceIdentity 表和 Repository。
2. Desktop 新增 DeviceIdentity 表和 Repository。
3. Android 实现 getOrCreateIdentity。
4. Desktop 实现 get_or_create_identity。
5. UI 展示本机设备名和设备类型。
```

验收：

```text
Android 和 Desktop 首次启动时都能生成稳定 device_id。
重启应用后 device_id 不变化。
```

------

### Step 2：实现 paired_devices 表

任务：

```text
1. Android 新增 PairedDeviceEntity、Dao、Repository。
2. Desktop 新增 paired_devices repository。
3. 两端 UI 能展示空的已配对设备列表。
4. 实现解除配对接口。
```

验收：

```text
两端都能显示“暂无已配对设备”。
插入测试设备后，UI 能显示。
用户可以删除测试设备。
```

------

### Step 3：Desktop 创建 PairingSession

任务：

```text
1. Desktop 实现 pairing_sessions 表。
2. 实现 PairingSessionManager。
3. 生成 session_id。
4. 生成 pairing_token。
5. 设置 expires_at = 当前时间 + 5 分钟。
6. 状态设为 waiting。
```

验收：

```text
点击“添加 Android 设备”后，Desktop 能创建 waiting 状态的 pairing_session。
5 分钟后会过期。
```

------

### Step 4：Desktop 启动临时 PairingServer

任务：

```text
1. 实现本地 HTTP 服务。
2. 随机选择可用端口。
3. 监听 /api/pair。
4. 支持启动和停止。
5. 配对会话取消或成功后停止服务。
```

验收：

```text
Desktop 点击“添加 Android 设备”后，本地 HTTP 服务启动。
取消后服务关闭。
```

------

### Step 5：Desktop 生成二维码 Payload

任务：

```text
1. 获取本机 DeviceIdentity。
2. 获取局域网 IP。
3. 获取 PairingSession。
4. 构造 PairingQrPayload。
5. 前端生成二维码。
6. PairingQrDialog 显示倒计时。
```

验收：

```text
Desktop 可以显示二维码。
二维码内容是合法 JSON。
二维码包含 protocol、host、port、device_id、public_key、pairing_token、expires_at。
```

------

### Step 6：Android 扫码解析二维码

任务：

```text
1. 接入二维码扫描库。
2. 实现 ScanPairingQrScreen。
3. 解析二维码 JSON。
4. 校验 protocol。
5. 校验 expires_at。
6. 展示待配对 Desktop 信息。
```

验收：

```text
Android 可以扫描 Desktop 二维码。
扫码后显示电脑设备名、设备类型、局域网地址。
```

------

### Step 7：Android 发送 PairRequest

任务：

```text
1. Android 获取本机 DeviceIdentity。
2. 构造 PairRequest。
3. POST 到 Desktop /api/pair。
4. 处理成功响应。
5. 处理失败响应。
```

验收：

```text
Android 点击“确认配对”后，可以向 Desktop 发起请求。
网络失败、token 过期、协议错误时显示明确错误。
```

------

### Step 8：Desktop 校验 PairRequest

任务：

```text
1. 校验 protocol。
2. 校验 session_id。
3. 校验 pairing_token。
4. 校验 expires_at。
5. 校验 device_type = android。
6. 校验 public_key 不为空。
7. 检查是否已经配对。
8. 保存 Android 到 paired_devices。
9. session 状态改为 paired。
10. 返回 PairResponse。
```

验收：

```text
Desktop 收到合法 PairRequest 后，paired_devices 中出现 Android 设备。
非法请求不会保存设备。
```

------

### Step 9：Android 保存 Desktop 设备

任务：

```text
1. Android 收到 PairResponse。
2. 校验 status = ok。
3. 校验 protocol。
4. 保存 Desktop 到 paired_devices。
5. 跳转 PairingResultScreen。
```

验收：

```text
Android 配对成功后，paired_devices 中出现 Desktop 设备。
Android 设备页显示 Ko-PC。
```

------

### Step 10：双方设备页联调

任务：

```text
1. Desktop DeviceSyncPage 展示 Android。
2. Android DeviceSyncScreen 展示 Desktop。
3. 双方支持解除配对。
4. 解除配对后设备列表更新。
```

验收：

```text
完成配对后，手机和电脑都能看到对方。
解除配对后，对方从本地列表消失。
```

------

### Step 11：错误处理与 UI 打磨

任务：

```text
1. 二维码过期提示。
2. 配对服务未启动提示。
3. 局域网不可达提示。
4. 已配对提示。
5. 用户取消提示。
6. Desktop 防火墙提示。
7. 连接超时提示。
```

验收：

```text
普通用户配对失败时能知道下一步该怎么做。
```

------

## 20. 测试计划

### 20.1 单元测试

Android 测试：

```text
PairingQrPayload 解析
protocol 校验
expires_at 校验
PairRequest 构造
PairResponse 解析
PairedDeviceRepository 插入/删除
```

Desktop 测试：

```text
PairingSession 创建
pairing_token 生成
token 过期判断
PairRequest 校验
PairedDeviceRepository 插入/删除
PairingQrPayload 构造
```

------

### 20.2 集成测试

测试场景：

```text
1. Desktop 生成二维码，Android 扫码成功。
2. Android 发送 PairRequest，Desktop 保存 Android。
3. Desktop 返回 PairResponse，Android 保存 Desktop。
4. 二维码过期后扫码，Android 提示过期。
5. token 错误，Desktop 返回 TOKEN_INVALID。
6. 重复配对，Desktop 返回 ALREADY_PAIRED。
7. Desktop 取消配对后，Android 请求失败。
8. Android 和 Desktop 不在同一 Wi-Fi，Android 显示无法连接。
```

------

### 20.3 手动测试流程

```text
1. 启动 Desktop App。
2. 进入 Devices & Sync。
3. 点击“添加 Android 设备”。
4. 看到二维码和 5 分钟倒计时。
5. 启动 Android App。
6. 进入 Devices & Sync。
7. 点击“扫描电脑二维码”。
8. 扫描 Desktop 二维码。
9. Android 显示 Ko-PC 信息。
10. 点击确认配对。
11. Android 显示配对成功。
12. Desktop 显示 Android 设备。
13. Android 显示 Desktop 设备。
14. 在 Android 上解除配对。
15. 在 Desktop 上解除配对。
```

------

## 21. 第三阶段验收清单

### 21.1 功能验收

```text
[ ] Android 可以生成本机 device_id
[ ] Desktop 可以生成本机 device_id
[ ] Android 重启后 device_id 不变化
[ ] Desktop 重启后 device_id 不变化
[ ] Desktop 可以创建 pairing_session
[ ] Desktop 可以启动临时配对服务
[ ] Desktop 可以生成二维码
[ ] 二维码包含完整 PairingQrPayload
[ ] Android 可以扫描二维码
[ ] Android 可以解析 PairingQrPayload
[ ] Android 可以显示待配对 Desktop 信息
[ ] Android 可以发送 PairRequest
[ ] Desktop 可以接收 /api/pair
[ ] Desktop 可以校验 pairing_token
[ ] Desktop 可以保存 Android 设备
[ ] Desktop 可以返回 PairResponse
[ ] Android 可以保存 Desktop 设备
[ ] Desktop 设备页可以显示 Android
[ ] Android 设备页可以显示 Desktop
[ ] 双方都可以解除配对
[ ] 二维码过期后不能配对
[ ] 重复配对有明确提示
[ ] 网络错误有明确提示
```

### 21.2 非功能验收

```text
[ ] 不上传任何数据到开发者服务器
[ ] 不同步 SummaryEvent
[ ] 不传输 raw_usage_events
[ ] 不传输 raw_desktop_events
[ ] 不调用 LLM
[ ] 不使用云端账号
[ ] pairing_token 不写入日志
[ ] private_key 不写入普通数据库
[ ] 配对服务只在用户主动点击添加设备后启动
[ ] 配对成功或取消后，配对服务关闭
```

------

## 22. 第三阶段 Codex 执行提示词

可以直接把下面内容给 Codex：

```text
请根据《Life Debugger 第三阶段编码 Plan：手机与电脑第一次配对认证》实现 Android 与 Desktop 的第一次配对功能。

严格要求：

1. 第三阶段只实现配对认证，不实现 SummaryEvent 同步。
2. 不实现 mDNS 自动发现。
3. 不实现空闲自动同步。
4. 不实现主分析设备切换。
5. Android 与 Desktop 都必须生成稳定 device_id。
6. Android 与 Desktop 都必须保存 paired_devices。
7. Desktop 必须能生成配对二维码。
8. Desktop 必须启动临时本地 HTTP 配对服务。
9. Android 必须能扫描 Desktop 二维码。
10. Android 必须能向 Desktop /api/pair 发送 PairRequest。
11. Desktop 必须校验 protocol、session_id、pairing_token、expires_at。
12. 配对成功后，Desktop 保存 Android 设备，Android 保存 Desktop 设备。
13. 双方设备页必须显示已配对设备。
14. 双方必须支持解除配对。
15. pairing_token 必须随机、一次性、5 分钟过期。
16. 配对服务只在用户主动添加设备时启动。
17. 配对成功、取消或过期后，配对服务应关闭或会话失效。
18. 不要将 private_key 存入普通数据库。
19. 不要将 pairing_token 打印到日志。
20. 先实现 Desktop 生成二维码、Android 扫码配对的主流程，再考虑 Android 生成二维码。
```

------

## 23. 第三阶段最终 Demo

目标 Demo：

```text
1. 用户打开 Desktop App。
2. 用户进入 Devices & Sync 页面。
3. 用户点击“添加 Android 设备”。
4. Desktop 显示二维码和倒计时。
5. 用户打开 Android App。
6. 用户进入 Devices & Sync 页面。
7. 用户点击“扫描电脑二维码”。
8. Android 扫描 Desktop 二维码。
9. Android 显示：
   即将连接 Ko-PC，请确认这是你的电脑。
10. 用户点击确认配对。
11. Android 显示配对成功。
12. Desktop 显示已配对 Android 手机。
13. Android 显示已配对 Desktop 电脑。
14. 用户可以在任一端解除配对。
```

演示文案：

```text
Life Debugger 已完成手机与电脑的可信设备配对。
当前阶段不会同步任何行为数据。
下一阶段将实现：配对后，同一局域网中设备空闲时自动同步 SummaryEvent 摘要。
```

------

## 24. 第三阶段完成后的下一步

第三阶段完成后，进入第四阶段：

```text
局域网空闲自动同步 SummaryEvent
```

第四阶段需要复用第三阶段产物：

```text
device_id
public_key
paired_devices
last_known_host
last_known_port
pairing trust relationship
```

第四阶段将新增：

```text
mDNS / 局域网发现
SyncBundle
SyncClient
SyncServer
IdleSyncScheduler
幂等去重
sync_status 更新
```

因此第三阶段要保留以下字段，方便后续扩展：

```text
last_known_host
last_known_port
last_seen_at
last_sync_at
trust_level
metadata_json
```

不要在第三阶段删除这些字段，即便暂时不用。