export type SummaryEvent = {
  eventId: string
  deviceId: string
  deviceType: 'desktop'
  source: 'desktop_window_tracker' | 'manual_intent'
  startTime: number
  endTime?: number
  durationSeconds?: number
  category?: string
  activityType?: string
  appLabel?: string
  windowTitle?: string
  processName?: string
  summary: string
  confidence: number
  privacyLevel: 'metadata' | 'summary_only' | 'user_input' | 'sensitive_masked'
  llmAllowed: boolean
}

export type DashboardState = {
  totalDesktopMinutes: number
  productiveMinutes: number
  summaryEventCount: number
  appSwitchCount: number
  topCategories: string
  llmConfigured: boolean
  collectorRunning: boolean
}

export type CollectorStatus = {
  running: boolean
  currentApp: string
  windowTitle?: string
  processName?: string
  idleSeconds: number
}

export type LlmConfig = {
  provider: string
  baseUrl: string
  model: string
  apiKeyStored: boolean
}

export type LlmReview = {
  reviewDate: string
  provider: string
  model: string
  contentJson: string
  createdAtMs: number
}
