import { useEffect, useState } from 'react'
import { Card } from '../../components/common/Card'
import { generateDailyReview, getLatestReview } from '../../api/desktopApi'
import type { LlmReview } from '../../types/summary'

type ReviewJson = {
  daily_summary?: unknown
  time_distribution?: unknown
  attention_shifts?: unknown
  suggestions?: unknown
  [key: string]: unknown
}

export function ReviewPage() {
  const [review, setReview] = useState<LlmReview | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  async function load() {
    setError('')
    setMessage('')
    await getLatestReview()
      .then(setReview)
      .catch((reason) => setError(String(reason)))
  }

  async function generate() {
    setBusy(true)
    setError('')
    setMessage('正在生成今日复盘，云端模型返回前请稍等...')
    try {
      const nextReview = await generateDailyReview()
      setReview(nextReview)
      setMessage('今日复盘已生成。')
    } catch (reason) {
      setError(String(reason))
      setMessage('')
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const parsed = parseReview(review)

  return (
    <div className="stack">
      <header>
        <p className="eyebrow">Review</p>
        <h1>每日复盘</h1>
      </header>
      <div className="actions">
        <button className="primary" onClick={() => void generate()} disabled={busy}>
          {busy ? '生成中...' : '生成今日复盘'}
        </button>
        <button onClick={() => void load()} disabled={busy}>刷新复盘</button>
      </div>
      {message && <Card title="状态">{message}</Card>}
      {error && <Card title="复盘失败">{error}</Card>}
      {!review && !error && (
        <Card title="等待复盘">
          <p>请先在 LLM 配置页保存 API Key，然后点击本页“生成今日复盘”。</p>
          <p>LLM 调用失败时这里只展示错误，不会生成规则伪复盘。</p>
        </Card>
      )}
      {review && parsed && (
        <>
          <Card title="今日总结">
            <ReviewValue value={parsed.daily_summary ?? '模型没有返回 daily_summary。'} />
          </Card>
          <div className="grid review-grid">
            <Card title="时间分布">
              <ReviewValue value={parsed.time_distribution ?? '暂无时间分布。'} />
            </Card>
            <Card title="注意力切换">
              <ReviewValue value={parsed.attention_shifts ?? '暂无注意力切换分析。'} />
            </Card>
          </div>
          <Card title="建议">
            <ReviewValue value={parsed.suggestions ?? '暂无建议。'} />
          </Card>
          <Card title="来源">
            <p>{review.provider} / {review.model}</p>
            <small>{new Date(review.createdAtMs).toLocaleString()}</small>
          </Card>
        </>
      )}
      {review && !parsed && (
        <Card title="复盘结果">
          <p>复盘已生成，但结果不是可解析的 JSON。下面是原始内容：</p>
          <pre>{review.contentJson}</pre>
        </Card>
      )}
    </div>
  )
}

function parseReview(review: LlmReview | null): ReviewJson | null {
  if (!review) return null
  try {
    return JSON.parse(review.contentJson) as ReviewJson
  } catch {
    return null
  }
}

function ReviewValue({ value }: { value: unknown }) {
  if (Array.isArray(value)) {
    if (value.length === 0) return <p>暂无。</p>
    return (
      <ul className="review-list">
        {value.map((item, index) => (
          <li key={index}><ReviewValue value={item} /></li>
        ))}
      </ul>
    )
  }

  if (value && typeof value === 'object') {
    return (
      <div className="review-kv">
        {Object.entries(value as Record<string, unknown>).map(([key, item]) => (
          <div key={key}>
            <strong>{labelize(key)}</strong>
            <ReviewValue value={item} />
          </div>
        ))}
      </div>
    )
  }

  return <p>{String(value ?? '暂无。')}</p>
}

function labelize(key: string) {
  return key.replaceAll('_', ' ')
}
