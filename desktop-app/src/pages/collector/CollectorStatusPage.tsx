import { useEffect, useState } from 'react'
import { Card } from '../../components/common/Card'
import { getCollectorStatus, setCollectorRunning } from '../../api/desktopApi'
import type { CollectorStatus } from '../../types/summary'

export function CollectorStatusPage() {
  const [status, setStatus] = useState<CollectorStatus | null>(null)

  async function load() {
    setStatus(await getCollectorStatus())
  }

  useEffect(() => {
    void load()
  }, [])

  async function toggle() {
    await setCollectorRunning(!status?.running)
    await load()
  }

  return (
    <div className="stack">
      <header>
        <p className="eyebrow">Collector</p>
        <h1>采集状态</h1>
      </header>
      <Card title="当前活动窗口">
        <p>状态：{status?.running ? '运行中' : '未启动'}</p>
        <p>应用：{status?.currentApp}</p>
        <p>标题：{status?.windowTitle ?? '暂无'}</p>
        <p>进程：{status?.processName ?? '暂无'}</p>
        <p>空闲：{status?.idleSeconds ?? 0} 秒</p>
      </Card>
      <button className="primary" onClick={toggle}>{status?.running ? '停止采集' : '开始采集'}</button>
    </div>
  )
}
