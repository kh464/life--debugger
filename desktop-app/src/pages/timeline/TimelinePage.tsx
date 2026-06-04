import { useEffect, useState } from 'react'
import { Card } from '../../components/common/Card'
import { getTimeline } from '../../api/desktopApi'
import type { SummaryEvent } from '../../types/summary'

export function TimelinePage() {
  const [events, setEvents] = useState<SummaryEvent[]>([])

  useEffect(() => {
    void getTimeline().then(setEvents)
  }, [])

  return (
    <div className="stack">
      <header>
        <p className="eyebrow">Timeline</p>
        <h1>今日桌面时间线</h1>
      </header>
      {events.map((event) => (
        <Card key={event.eventId} title={`${new Date(event.startTime).toLocaleTimeString()} · ${event.appLabel ?? 'Unknown'}`}>
          <p>{event.summary}</p>
          <small>{event.category} / {event.activityType} / LLM: {event.llmAllowed ? '允许' : '排除'}</small>
        </Card>
      ))}
    </div>
  )
}
