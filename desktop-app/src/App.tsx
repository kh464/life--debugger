import { useState } from 'react'
import { AppShell, type PageKey } from './components/layout/AppShell'
import { OnboardingPage } from './pages/onboarding/OnboardingPage'
import { DashboardPage } from './pages/dashboard/DashboardPage'
import { TimelinePage } from './pages/timeline/TimelinePage'
import { ReviewPage } from './pages/review/ReviewPage'
import { IntentPage } from './pages/intent/IntentPage'
import { CollectorStatusPage } from './pages/collector/CollectorStatusPage'
import { DeviceSyncPage } from './pages/devices/DeviceSyncPage'
import { LlmSettingsPage } from './pages/llm/LlmSettingsPage'
import { PrivacyPage } from './pages/privacy/PrivacyPage'
import { DataManagementPage } from './pages/data/DataManagementPage'
import { setCollectorRunning } from './api/desktopApi'

export function App() {
  const [page, setPage] = useState<PageKey>('onboarding')

  async function startDesktopLoop() {
    await setCollectorRunning(true)
    setPage('dashboard')
  }

  return (
    <AppShell page={page} onNavigate={setPage}>
      {page === 'onboarding' && <OnboardingPage onStart={() => void startDesktopLoop()} />}
      {page === 'dashboard' && <DashboardPage onPreview={() => setPage('llm')} />}
      {page === 'timeline' && <TimelinePage />}
      {page === 'review' && <ReviewPage />}
      {page === 'intent' && <IntentPage />}
      {page === 'collector' && <CollectorStatusPage />}
      {page === 'devices' && <DeviceSyncPage />}
      {page === 'llm' && <LlmSettingsPage />}
      {page === 'privacy' && <PrivacyPage />}
      {page === 'data' && <DataManagementPage />}
    </AppShell>
  )
}
