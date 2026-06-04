import { useState } from 'react'
import { Card } from '../../components/common/Card'
import { clearRawEvents, clearSummaryEvents, exportTodaySummaryEventsJson } from '../../api/desktopApi'

export function DataManagementPage() {
  const [message, setMessage] = useState('')
  const [exportJson, setExportJson] = useState('')

  async function clearRaw() {
    const result = await clearRawEvents()
    setMessage(`已清空 raw_desktop_events：${result.deleted} 条。`)
  }

  async function clearSummaries() {
    const result = await clearSummaryEvents()
    setMessage(`已清空 summary_events：${result.deleted} 条。`)
    setExportJson('')
  }

  async function exportSummaries() {
    const json = await exportTodaySummaryEventsJson()
    setExportJson(json)
    setMessage('已生成今日 SummaryEvent JSON。')
  }

  return (
    <div className="stack">
      <header>
        <p className="eyebrow">Data</p>
        <h1>数据管理</h1>
      </header>
      <Card title="本地数据">
        <p>数据保存在本机 SQLite。raw desktop events 不会发送给 LLM；导出内容只包含 SummaryEvent。</p>
      </Card>
      {message && <Card title="状态">{message}</Card>}
      <div className="actions">
        <button onClick={() => void clearRaw()}>清空原始桌面事件</button>
        <button onClick={() => void clearSummaries()}>清空摘要事件</button>
        <button className="primary" onClick={() => void exportSummaries()}>导出今日摘要 JSON</button>
      </div>
      {exportJson && <Card title="今日 SummaryEvent JSON"><pre>{exportJson}</pre></Card>}
    </div>
  )
}
