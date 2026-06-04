import { useState } from 'react'
import { Card } from '../../components/common/Card'
import { recordManualIntent } from '../../api/desktopApi'

const options = ['编码', '写作', '查资料', '会议', '沟通', '阅读', '设计', '休息', '娱乐', '其他']

export function IntentPage() {
  const [message, setMessage] = useState('')

  async function record(option: string) {
    try {
      await recordManualIntent(option)
      setMessage(`已记录：准备${option}`)
    } catch (error) {
      setMessage(String(error))
    }
  }

  return (
    <div className="stack">
      <header>
        <p className="eyebrow">Intent</p>
        <h1>记录当前意图</h1>
      </header>
      <div className="chips">
        {options.map((option) => (
          <button key={option} onClick={() => void record(option)}>{option}</button>
        ))}
      </div>
      {message && <Card title="状态">{message}</Card>}
    </div>
  )
}
