import { useEffect, useRef, useState } from 'react'
import { Card } from '../../components/common/Card'
import {
  buildLlmPreview,
  getLlmConfig,
  saveLlmConfig,
  testLlmConnection
} from '../../api/desktopApi'

export function LlmSettingsPage() {
  const [provider, setProvider] = useState('DeepSeek')
  const [baseUrl, setBaseUrl] = useState('https://api.deepseek.com')
  const [model, setModel] = useState('deepseek-chat')
  const [apiKey, setApiKey] = useState('')
  const [apiKeyStored, setApiKeyStored] = useState(false)
  const [preview, setPreview] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const apiKeyInputRef = useRef<HTMLInputElement>(null)

  function currentApiKey() {
    return apiKey || apiKeyInputRef.current?.value || ''
  }

  useEffect(() => {
    void getLlmConfig().then((config) => {
      setProvider(config.provider)
      setBaseUrl(config.baseUrl)
      setModel(config.model)
      setApiKeyStored(config.apiKeyStored)
    })
  }, [])

  async function save() {
    setBusy(true)
    setMessage('')
    try {
      const key = currentApiKey()
      if (!key.trim() && !apiKeyStored) {
        setMessage('请先输入 API Key。')
        return
      }
      const config = await saveLlmConfig({ provider, baseUrl, model, apiKey: key })
      setApiKeyStored(config.apiKeyStored)
      if (config.apiKeyStored) {
        setApiKey('')
        setMessage('配置已保存，API Key 已写入本地安全存储。')
      } else {
        setMessage('Provider 配置已保存，但 API Key 尚未写入本地安全存储，请重新输入 API Key。')
      }
    } catch (error) {
      setMessage(String(error))
    } finally {
      setBusy(false)
    }
  }

  async function saveIfNeeded() {
    const key = currentApiKey()
    if (!key.trim() && apiKeyStored) {
      return
    }
    if (!key.trim()) {
      throw new Error('请先输入 API Key。')
    }

    const config = await saveLlmConfig({ provider, baseUrl, model, apiKey: key })
    setApiKeyStored(config.apiKeyStored)
    if (!config.apiKeyStored) {
      throw new Error('API Key 尚未写入本地安全存储，请重新输入 API Key。')
    }
    setApiKey('')
  }

  async function test() {
    setBusy(true)
    setMessage('正在测试 LLM 连接...')
    try {
      await saveIfNeeded()
      setMessage(await testLlmConnection())
    } catch (error) {
      handleCredentialError(error)
    } finally {
      setBusy(false)
    }
  }

  async function previewPayload() {
    setPreview(await buildLlmPreview())
  }

  function handleCredentialError(error: unknown) {
    const text = String(error)
    if (text.includes('credential') || text.includes('secure storage') || text.includes('No matching entry')) {
      setApiKeyStored(false)
      setMessage(`${text}。请重新输入 API Key 后再测试。`)
      return
    }
    setMessage(text)
  }

  return (
    <div className="stack">
      <header>
        <p className="eyebrow">LLM</p>
        <h1>LLM 配置与输入预览</h1>
      </header>
      <div className="form">
        <label>Provider<input value={provider} onChange={(event) => setProvider(event.target.value)} /></label>
        <label>Base URL<input value={baseUrl} onChange={(event) => setBaseUrl(event.target.value)} /></label>
        <label>Model<input value={model} onChange={(event) => setModel(event.target.value)} /></label>
        <label>
          API Key
          <input
            placeholder={apiKeyStored ? '已安全保存，留空则不修改' : '保存到本地安全存储'}
            type="password"
            value={apiKey}
            ref={apiKeyInputRef}
            onChange={(event) => setApiKey(event.target.value)}
          />
        </label>
      </div>
      <div className="actions">
        <button onClick={save} disabled={busy}>保存配置</button>
        <button onClick={test} disabled={busy}>测试连接</button>
        <button onClick={previewPayload}>预览 LLM 输入</button>
      </div>
      {message && <Card title="状态">{message}</Card>}
      {preview && <Card title="LLM 输入包"><pre>{preview}</pre></Card>}
    </div>
  )
}
