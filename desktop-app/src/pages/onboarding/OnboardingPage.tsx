import { Card } from '../../components/common/Card'

export function OnboardingPage({ onStart }: { onStart: () => void }) {
  return (
    <div className="stack">
      <div className="hero">
        <p className="eyebrow">Desktop 单端闭环</p>
        <h1>用本地摘要复盘你的电脑使用模式。</h1>
        <p>第二阶段只采集活动应用、窗口标题和空闲状态，不读取键盘输入、文件内容、截图、录屏、麦克风或摄像头。</p>
        <button className="primary" onClick={onStart}>进入首页</button>
      </div>
      <Card title="目标流程">
        <p>Window Activity → Desktop SummaryEvent → LLM Preview → LLM Review。</p>
      </Card>
    </div>
  )
}
