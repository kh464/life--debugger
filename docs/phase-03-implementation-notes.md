# 第三阶段实现记录

## 已完成

- Desktop 已生成稳定本机 `device_identity`，并新增 `paired_devices`、`pairing_sessions` 表。
- Desktop 新增 Devices 页面，可展示本机身份、修改设备名、展示已配对设备、解除本地配对。
- Desktop 可主动创建 5 分钟有效的一次性配对会话，并启动临时局域网 HTTP 服务。
- Desktop 配对服务提供 `POST /api/pair`，校验 `protocol`、`session_id`、`pairing_token`、过期时间、设备类型和公钥。
- Desktop 可生成 `lifedbg-pairing-v1` payload，并以前端二维码和可复制 JSON 两种形式展示。
- Android 已生成稳定本机 `device_identity`，并新增 `paired_devices` 表与 Room 1 -> 2 migration。
- Android 新增 Devices & Sync 页面，可展示本机身份、修改设备名、展示已配对设备、解除本地配对。
- Android 可扫描 Desktop 二维码，解析并展示待配对电脑信息，用户确认后发送 PairRequest。
- Android 保留粘贴 Desktop 二维码 payload 的调试入口，便于模拟器和 USB `adb reverse` 测试。
- 配对成功后，Desktop 保存 Android 设备，Android 保存 Desktop 设备。
- Android 已接入 CameraX + ML Kit QR 扫码，扫码只在用户主动打开设备配对页面时运行。
- Android 已增加常见配对失败文案，包括二维码过期、重复配对、token 无效、网络连接失败和超时。
- Desktop Devices 页面已增加连接诊断提示，说明固定端口、防火墙、公用 Wi-Fi 和 USB 调试方案。

## 阶段边界

- 未实现 SummaryEvent 同步。
- 未实现 mDNS / Bonjour 自动发现。
- 未实现后台空闲同步。
- 未实现跨设备时间线和跨设备 LLM 复盘。
- 未向 LLM、云端或开发者服务器发送任何配对或行为数据。

## 安全说明

- `pairing_token` 使用安全随机值，默认 5 分钟过期，配对成功后会话进入 `paired` 状态。
- Desktop 临时配对服务只在用户点击添加 Android 设备后启动。
- 普通数据库只保存 `public_key`，不保存私钥。
- 第三阶段暂使用随机 `public_key` 占位；真实签名/加密密钥应在第四阶段同步前接入系统安全存储。
- Android 为局域网临时配对允许 cleartext HTTP；当前仅用于访问 Desktop 的临时 `/api/pair`。

## 待验收

- Windows 防火墙首次放行提示需要在真机局域网环境手动确认。
- 需要手动联调：Desktop 运行开发版，Android 真机扫描 Desktop 二维码后确认配对。
- 需要分别验证：专用 Wi-Fi、公用 Wi-Fi、手机热点、USB `adb reverse` 四种网络路径下的表现。

## 配对网络风险记录

阶段三当前采用“Android 主动连接 Desktop 临时 HTTP 端口”的局域网直连方案。该方案隐私边界清晰、实现简单，但在真实使用场景中存在稳定性风险：

- Windows 防火墙可能拦截手机访问 Desktop 端口，尤其当前网络被系统识别为“公用网络”时。
- 学校、公司、酒店、商场等公用 Wi-Fi 可能开启客户端隔离，导致同一 Wi-Fi 下手机也无法访问电脑。
- 每次使用随机端口会增加防火墙放行成本；当前已调整为优先使用固定端口 `58231`，仅端口被占用时回退随机端口。
- USB `adb reverse` 只能作为开发调试手段，不适合作为普通用户的正式配对路径。

业内常见解决思路：

- 局域网直连作为首选路径，但在桌面端主动引导用户完成防火墙授权。
- 增加连接诊断页，明确区分“端口未监听”“防火墙拦截”“Wi-Fi 客户端隔离”“二维码过期”等原因。
- 引入 mDNS / Bonjour / UDP 广播等局域网发现能力，但它们仍可能被公用 Wi-Fi 阻断。
- 提供云端或中继握手作为兜底：云端只负责设备发现和临时配对握手，不上传原始行为数据或 SummaryEvent。
- 在长期方案中，可以保留本地优先同步，同时允许用户选择“使用中继完成配对”。

后续建议：

- 第四阶段前先补充 Desktop 连接诊断和防火墙提示。
- 若产品面向普通用户，应避免把“手机必须直连电脑端口”作为唯一配对方案。
- 若引入中继服务，必须继续遵守本地优先原则：中继只传递配对握手元数据，不传输 raw events、SummaryEvent、LLM payload 或 API Key。

## 已运行验证

- `desktop-app`: `npm install`
- `desktop-app`: `npm run build`
- `desktop-app/src-tauri`: `cargo check`
- `desktop-app/src-tauri`: `cargo test`
- `android-app`: `.\gradlew.bat :app:assembleDebug --no-daemon`
- `android-app`: 接入 CameraX + ML Kit 后重新运行 `.\gradlew.bat :app:assembleDebug --no-daemon`
