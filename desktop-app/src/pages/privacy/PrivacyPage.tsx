import { Card } from '../../components/common/Card'

export function PrivacyPage() {
  return (
    <div className="stack">
      <header>
        <p className="eyebrow">Privacy</p>
        <h1>隐私设置</h1>
      </header>
      <Card title="默认策略">
        <p>默认允许应用名称进入 LLM，默认不允许进程名进入 LLM，窗口标题后续会支持泛化后进入。</p>
        <p>金融类、健康类、敏感窗口标题默认排除。</p>
      </Card>
    </div>
  )
}
