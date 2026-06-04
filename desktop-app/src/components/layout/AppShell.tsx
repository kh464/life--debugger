import type { ReactNode } from 'react'

export type PageKey =
  | 'onboarding'
  | 'dashboard'
  | 'timeline'
  | 'review'
  | 'intent'
  | 'collector'
  | 'devices'
  | 'llm'
  | 'privacy'
  | 'data'

const navItems: Array<{ key: PageKey; label: string }> = [
  { key: 'dashboard', label: '首页' },
  { key: 'timeline', label: '时间线' },
  { key: 'review', label: '复盘' },
  { key: 'intent', label: '意图' },
  { key: 'collector', label: '采集状态' },
  { key: 'devices', label: '设备' },
  { key: 'llm', label: 'LLM 配置' },
  { key: 'privacy', label: '隐私' },
  { key: 'data', label: '数据' }
]

export function AppShell({
  page,
  onNavigate,
  children
}: {
  page: PageKey
  onNavigate: (page: PageKey) => void
  children: ReactNode
}) {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">LD</span>
          <div>
            <strong>Life Debugger</strong>
            <small>Desktop MVP</small>
          </div>
        </div>
        <nav>
          {navItems.map((item) => (
            <button key={item.key} className={page === item.key ? 'active' : ''} onClick={() => onNavigate(item.key)}>
              {item.label}
            </button>
          ))}
        </nav>
      </aside>
      <main className="page">{children}</main>
    </div>
  )
}
