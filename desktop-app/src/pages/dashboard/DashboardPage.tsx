import { useEffect, useState } from 'react'
import { Card } from '../../components/common/Card'
import { getDashboard, refreshToday } from '../../api/desktopApi'
import type { DashboardState } from '../../types/summary'

export function DashboardPage({ onPreview }: { onPreview: () => void }) {
  const [state, setState] = useState<DashboardState | null>(null)
  const [message, setMessage] = useState('')

  async function load() {
    setState(await getDashboard())
  }

  useEffect(() => {
    void load()
  }, [])

  async function refresh() {
    const result = await refreshToday()
    setMessage(`已生成 ${result.generated} 条桌面摘要。`)
    await load()
  }

  return (
    <div className="stack">
      <header>
        <p className="eyebrow">Dashboard</p>
        <h1>今日桌面概览</h1>
      </header>
      <div className="grid">
        <Card title="桌面使用">{state?.totalDesktopMinutes ?? 0} 分钟</Card>
        <Card title="生产性时间">{state?.productiveMinutes ?? 0} 分钟</Card>
        <Card title="摘要事件">{state?.summaryEventCount ?? 0} 条</Card>
        <Card title="采集器">{state?.collectorRunning ? '运行中' : '未启动'}</Card>
      </div>
      <Card title="主要类别">{state?.topCategories ?? '暂无'}</Card>
      {message && <Card title="状态">{message}</Card>}
      <div className="actions">
        <button className="primary" onClick={refresh}>刷新今日数据</button>
        <button onClick={onPreview}>预览 LLM 输入</button>
      </div>
    </div>
  )
}
