import { useEffect, useState } from 'react'
import { backendApi, getRealtimeWsUrl, type MessageSummary } from '../lib/backendApi'
import type { AtpSession } from '../lib/atp'

type Props = {
  channelId: number
  channelName: string
  session: AtpSession
}

import { GitHubSiv } from './GitHubSiv'

export function ChannelView({ channelId, channelName, session }: Props) {
  const [messages, setMessages] = useState<MessageSummary[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(true)
  const [showSiv, setShowSiv] = useState(true) // We can make this toggleable
  const sess = { accessJwt: session.accessJwt, did: session.did, handle: session.handle }

  const load = () => {
    setLoading(true)
    backendApi.getMessages(channelId, sess).then(setMessages, () => setMessages([])).finally(() => setLoading(false))
  }

  useEffect(() => load(), [channelId])

  useEffect(() => {
    let ws: WebSocket | null = null

    try {
      ws = new WebSocket(getRealtimeWsUrl(sess))
    } catch {
      return
    }

    ws.onopen = () => {
      ws?.send(JSON.stringify({ type: 'subscribe', channelId }))
    }

    ws.onmessage = (event) => {
      try {
        const envelope = JSON.parse(event.data) as {
          type?: string
          channelId?: number
          payload?: MessageSummary
        }
        if (envelope.type !== 'message.created' || envelope.channelId !== channelId || !envelope.payload) {
          return
        }
        const incoming = envelope.payload
        setMessages((prev) => (prev.some((m) => m.id === incoming.id) ? prev : [incoming, ...prev]))
      } catch {
        // Ignore malformed events.
      }
    }

    return () => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'unsubscribe', channelId }))
      }
      ws?.close()
    }
  }, [channelId, session.accessJwt, session.did, session.handle])

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault()
    const text = input.trim()
    if (!text) return
    setInput('')
    backendApi.postMessage(channelId, sess, text).then((created) => {
      setMessages((prev) => (prev.some((m) => m.id === created.id) ? prev : [created, ...prev]))
    })
  }

  return (
    <div style={{ display: 'flex', height: '100%' }}>
      <div style={{ display: 'flex', flexDirection: 'column', flex: 1, padding: 16 }}>
        <div style={{ flex: 1, overflow: 'auto', marginBottom: 16 }}>
          {loading && <p style={{ color: 'var(--text-muted)' }}>Loading…</p>}
          {!loading && messages.length === 0 && (
            <p style={{ color: 'var(--text-muted)' }}>No messages yet. Say hello!</p>
          )}
          {messages.slice().reverse().map((m) => (
            <div
              key={m.id}
              style={{
                marginBottom: 12,
                padding: '8px 0',
                borderBottom: '1px solid var(--border)',
              }}
            >
              <span style={{ fontWeight: 600, color: 'var(--text-primary)', marginRight: 8 }}>
                {m.authorHandle || m.authorDid.slice(0, 12)}
              </span>
              <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                {new Date(m.createdAt).toLocaleString()}
              </span>
              <p style={{ marginTop: 4, whiteSpace: 'pre-wrap' }}>{m.content}</p>
            </div>
          ))}
        </div>
        <form onSubmit={handleSend}>
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={`Message #${channelName}`}
            style={{
              width: '100%',
              padding: 12,
              background: 'var(--bg-tertiary)',
              border: '1px solid var(--border)',
              borderRadius: 8,
              color: 'var(--text-primary)',
            }}
          />
          <button
            type="submit"
            style={{ marginTop: 8, padding: '8px 16px', background: 'var(--accent)', color: 'white', fontWeight: 600 }}
          >
            Send
          </button>
        </form>
      </div>

      {showSiv && <GitHubSiv vesselType="github" />}
    </div>
  )
}

