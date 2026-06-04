import { invoke } from '@tauri-apps/api/core'
import type { CollectorStatus, DashboardState, LlmConfig, LlmReview, SummaryEvent } from '../types/summary'

export type DeviceIdentity = {
  deviceId: string
  deviceName: string
  deviceType: string
  publicKey: string
  privateKeyRef?: string | null
  createdAt: number
  updatedAt: number
}

export type PairedDevice = {
  pairedDeviceId: string
  pairedDeviceName: string
  pairedDeviceType: string
  publicKey: string
  lastKnownHost?: string | null
  lastKnownPort?: number | null
  pairingStatus: string
  trustLevel: string
  pairedAt: number
  lastSeenAt?: number | null
  lastSyncAt?: number | null
  metadataJson?: string | null
}

export type PairingQrPayload = {
  protocol: string
  sessionId: string
  deviceId: string
  deviceName: string
  deviceType: string
  host: string
  port: number
  publicKey: string
  pairingToken: string
  expiresAt: number
}

export type PairingSessionStatus = {
  sessionId: string
  status: string
  expiresAt: number
  pairedDevices: PairedDevice[]
}

const now = Date.now()
const isTauri = typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window

let collectorRunning = false
let events: SummaryEvent[] = [
  {
    eventId: 'demo-1',
    deviceId: 'desktop-local',
    deviceType: 'desktop',
    source: 'desktop_window_tracker',
    startTime: now - 1000 * 60 * 90,
    endTime: now - 1000 * 60 * 48,
    durationSeconds: 42 * 60,
    category: 'coding',
    activityType: 'productive',
    appLabel: 'VS Code',
    windowTitle: 'life-debugger',
    processName: 'Code.exe',
    summary: '桌面端进行了约 42 分钟的编码活动。',
    confidence: 0.9,
    privacyLevel: 'metadata',
    llmAllowed: true
  }
]

export async function getDashboard(): Promise<DashboardState> {
  if (isTauri) {
    return invoke<DashboardState>('get_dashboard')
  }

  const totalDesktopMinutes = Math.round(events.reduce((sum, event) => sum + (event.durationSeconds ?? 0), 0) / 60)
  return {
    totalDesktopMinutes,
    productiveMinutes: Math.round(
      events
        .filter((event) => event.activityType === 'productive')
        .reduce((sum, event) => sum + (event.durationSeconds ?? 0), 0) / 60
    ),
    summaryEventCount: events.length,
    appSwitchCount: Math.max(events.length - 1, 0),
    topCategories: events.map((event) => event.category ?? 'unknown').join(', ') || '暂无',
    llmConfigured: false,
    collectorRunning
  }
}

export async function getTimeline(): Promise<SummaryEvent[]> {
  if (isTauri) {
    return invoke<SummaryEvent[]>('get_today_summary_events')
  }

  return events
}

export async function refreshToday(): Promise<{ generated: number }> {
  if (isTauri) {
    return invoke<{ generated: number }>('refresh_today')
  }

  const generated: SummaryEvent = {
    eventId: `demo-${Date.now()}`,
    deviceId: 'desktop-local',
    deviceType: 'desktop',
    source: 'desktop_window_tracker',
    startTime: Date.now() - 1000 * 60 * 25,
    endTime: Date.now(),
    durationSeconds: 25 * 60,
    category: 'browser',
    activityType: 'consumption',
    appLabel: 'Chrome',
    windowTitle: 'technical docs',
    processName: 'chrome.exe',
    summary: '桌面端浏览技术文档约 25 分钟。',
    confidence: 0.82,
    privacyLevel: 'metadata',
    llmAllowed: true
  }
  events = [...events, generated]
  return { generated: 1 }
}

export async function getCollectorStatus(): Promise<CollectorStatus> {
  if (isTauri) {
    return invoke<CollectorStatus>('get_collector_status')
  }

  return {
    running: collectorRunning,
    currentApp: collectorRunning ? 'VS Code' : '未启动',
    windowTitle: collectorRunning ? 'Life Debugger Desktop' : undefined,
    processName: collectorRunning ? 'Code.exe' : undefined,
    idleSeconds: 0
  }
}

export async function setCollectorRunning(running: boolean): Promise<void> {
  if (isTauri) {
    await invoke('set_collector_running', { running })
    return
  }

  collectorRunning = running
}

export async function buildLlmPreview(): Promise<string> {
  if (isTauri) {
    return invoke<string>('build_llm_preview')
  }

  return JSON.stringify(
    {
      date: new Date().toISOString().slice(0, 10),
      device: { device_id: 'desktop-local', device_type: 'desktop', role: 'primary_analysis' },
      events: events
        .filter((event) => event.llmAllowed)
        .map((event) => ({
          start_time: event.startTime,
          end_time: event.endTime,
          duration_seconds: event.durationSeconds,
          category: event.category,
          activity_type: event.activityType,
          app_label: event.appLabel,
          window_title: event.windowTitle,
          summary: event.summary,
          privacy_level: event.privacyLevel
        })),
      privacy_note:
        'Only user-previewable SummaryEvent data is included. Raw desktop events, keystrokes, screenshots, microphone, camera and file contents are not collected or sent.'
    },
    null,
    2
  )
}

export async function getLlmConfig(): Promise<LlmConfig> {
  if (isTauri) {
    return invoke<LlmConfig>('get_llm_config')
  }

  return {
    provider: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com',
    model: 'deepseek-chat',
    apiKeyStored: false
  }
}

export async function saveLlmConfig(input: {
  provider: string
  baseUrl: string
  model: string
  apiKey?: string
}): Promise<LlmConfig> {
  if (isTauri) {
    return invoke<LlmConfig>('save_llm_config_json', {
      requestJson: JSON.stringify(input)
    })
  }

  return {
    provider: input.provider,
    baseUrl: input.baseUrl,
    model: input.model,
    apiKeyStored: Boolean(input.apiKey)
  }
}

export async function testLlmConnection(): Promise<string> {
  if (isTauri) {
    return invoke<string>('test_llm_connection')
  }

  return 'Browser preview mode: connection test is available in the Tauri app.'
}

export async function generateDailyReview(): Promise<LlmReview> {
  if (isTauri) {
    return invoke<LlmReview>('generate_daily_review')
  }

  return {
    reviewDate: new Date().toISOString().slice(0, 10),
    provider: 'Demo',
    model: 'mock',
    contentJson: JSON.stringify(
      {
        daily_summary: '浏览器预览模式下的示例复盘。',
        time_distribution: [],
        attention_shifts: [],
        suggestions: ['在 Tauri App 中配置真实 LLM 后生成正式复盘。']
      },
      null,
      2
    ),
    createdAtMs: Date.now()
  }
}

export async function getLatestReview(): Promise<LlmReview | null> {
  if (isTauri) {
    return invoke<LlmReview | null>('get_latest_review')
  }

  return null
}

export async function recordManualIntent(intentLabel: string): Promise<SummaryEvent> {
  if (isTauri) {
    return invoke<SummaryEvent>('record_manual_intent', { intentLabel })
  }

  const event: SummaryEvent = {
    eventId: `intent-${Date.now()}`,
    deviceId: 'desktop-local',
    deviceType: 'desktop',
    source: 'manual_intent',
    startTime: Date.now(),
    endTime: Date.now(),
    durationSeconds: 0,
    category: intentLabel,
    activityType: 'intent',
    appLabel: 'Manual Intent',
    summary: `用户记录了当前意图：${intentLabel}。`,
    confidence: 1,
    privacyLevel: 'user_input',
    llmAllowed: true
  }
  events = [...events, event]
  return event
}

export async function clearRawEvents(): Promise<{ deleted: number }> {
  if (isTauri) {
    return invoke<{ deleted: number }>('clear_raw_events')
  }

  return { deleted: 0 }
}

export async function clearSummaryEvents(): Promise<{ deleted: number }> {
  if (isTauri) {
    return invoke<{ deleted: number }>('clear_summary_events_command')
  }

  const deleted = events.length
  events = []
  return { deleted }
}

export async function exportTodaySummaryEventsJson(): Promise<string> {
  if (isTauri) {
    return invoke<string>('export_today_summary_events_json')
  }

  return JSON.stringify(events, null, 2)
}

const browserIdentity: DeviceIdentity = {
  deviceId: 'browser-preview-desktop',
  deviceName: 'Browser Preview Desktop',
  deviceType: 'desktop',
  publicKey: 'browser-preview-public-key',
  privateKeyRef: null,
  createdAt: now,
  updatedAt: now
}

let browserPairedDevices: PairedDevice[] = []

export async function getLocalDeviceIdentity(): Promise<DeviceIdentity> {
  if (isTauri) {
    return invoke<DeviceIdentity>('get_local_device_identity')
  }
  return browserIdentity
}

export async function updateLocalDeviceName(deviceName: string): Promise<DeviceIdentity> {
  if (isTauri) {
    return invoke<DeviceIdentity>('update_local_device_name', { deviceName })
  }
  browserIdentity.deviceName = deviceName
  browserIdentity.updatedAt = Date.now()
  return browserIdentity
}

export async function getPairedDevices(): Promise<PairedDevice[]> {
  if (isTauri) {
    return invoke<PairedDevice[]>('get_paired_devices')
  }
  return browserPairedDevices
}

export async function removePairedDevice(deviceId: string): Promise<void> {
  if (isTauri) {
    await invoke('remove_paired_device', { deviceId })
    return
  }
  browserPairedDevices = browserPairedDevices.filter((device) => device.pairedDeviceId !== deviceId)
}

export async function startPairingSession(): Promise<PairingQrPayload> {
  if (isTauri) {
    return invoke<PairingQrPayload>('start_pairing_session')
  }
  return {
    protocol: 'lifedbg-pairing-v1',
    sessionId: `browser-session-${Date.now()}`,
    deviceId: browserIdentity.deviceId,
    deviceName: browserIdentity.deviceName,
    deviceType: browserIdentity.deviceType,
    host: '127.0.0.1',
    port: 58231,
    publicKey: browserIdentity.publicKey,
    pairingToken: 'browser-preview-token-not-for-real-pairing',
    expiresAt: Date.now() + 5 * 60 * 1000
  }
}

export async function cancelPairingSession(sessionId: string): Promise<void> {
  if (isTauri) {
    await invoke('cancel_pairing_session', { sessionId })
  }
}

export async function getPairingSessionStatus(sessionId: string): Promise<PairingSessionStatus> {
  if (isTauri) {
    return invoke<PairingSessionStatus>('get_pairing_session_status', { sessionId })
  }
  return {
    sessionId,
    status: 'waiting',
    expiresAt: Date.now() + 5 * 60 * 1000,
    pairedDevices: browserPairedDevices
  }
}
