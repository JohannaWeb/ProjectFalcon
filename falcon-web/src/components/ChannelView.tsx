import { useEffect, useState } from 'react'
import { backendApi, getRealtimeWsUrl, type MessageSummary } from '../lib/backendApi'
import type { AtpSession } from '../lib/atp'
import { IntelligencePanel } from './IntelligencePanel'
import { GifPicker } from './GifPicker'

type Props = {
  channelId: number
  channelName: string
  session: AtpSession
}

const AVATAR_COLORS = ['#2563eb', '#1d4ed8', '#0369a1', '#0f766e', '#334155', '#1e3a5f', '#374151', '#1e40af']

function avatarColor(handle: string) {
  let hash = 0
  for (const c of handle) hash = (hash * 31 + c.charCodeAt(0)) & 0x7fffffff
  return AVATAR_COLORS[hash % AVATAR_COLORS.length]
}

export function Avatar({ handle, size = 32 }: { handle: string; size?: number }) {
  const initials = handle.replace(/\..*$/, '').slice(0, 2).toUpperCase()
  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: 8,
        background: avatarColor(handle),
        color: '#fff',
        fontSize: size * 0.38,
        fontWeight: 600,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
        letterSpacing: '0.02em',
      }}
    >
      {initials}
    </div>
  )
}

function formatTime(iso: string) {
  const d = new Date(iso)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  if (diffMins < 1) return 'just now'
  if (diffMins < 60) return `${diffMins}m ago`
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return `${diffHours}h ago`
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

export function ChannelView({ channelId, channelName, session }: Props) {
  const [messages, setMessages] = useState<MessageSummary[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(true)
  const [showIntelligence] = useState(true)
  const [showGifs, setShowGifs] = useState(false)
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
    ws.onopen = () => ws?.send(JSON.stringify({ type: 'subscribe', channelId }))
    ws.onmessage = (event) => {
      try {
        const envelope = JSON.parse(event.data) as {
          type?: string
          channelId?: number
          payload?: MessageSummary
        }
        if (envelope.type !== 'message.created' || envelope.channelId !== channelId || !envelope.payload) return
        const incoming = envelope.payload
        setMessages((prev) => (prev.some((m) => m.id === incoming.id) ? prev : [incoming, ...prev]))
      } catch { /* ignore */ }
    }
    return () => {
      if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify({ type: 'unsubscribe', channelId }))
      ws?.close()
    }
  }, [channelId, session.accessJwt, session.did, session.handle])

  const handleSend = (e?: React.FormEvent, contentOverride?: string) => {
    if (e) e.preventDefault()
    const text = contentOverride || input.trim()
    if (!text) return
    setInput('')
    backendApi.postMessage(channelId, sess, text).then((created) => {
      setMessages((prev) => (prev.some((m) => m.id === created.id) ? prev : [created, ...prev]))
    })
  }

  const renderContent = (content: string) => {
    if (content.match(/^https?:\/\/.*\.giphy\.com\/.*$/)) {
      return (
        <img
          src={content}
          alt="GIF"
          style={{ maxWidth: 360, maxHeight: 280, borderRadius: 8, marginTop: 6, display: 'block' }}
        />
      )
    }
    return <p style={{ marginTop: 3, color: 'var(--text-primary)', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>{content}</p>
  }

  return (
    <div style={{ display: 'flex', height: '100%' }}>
      <div style={{ display: 'flex', flexDirection: 'column', flex: 1, minWidth: 0 }}>
        {/* Messages */}
        <div style={{ flex: 1, overflow: 'auto', padding: '16px 20px' }}>
          {loading && (
            <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>Loading…</p>
          )}
          {!loading && messages.length === 0 && (
            <div style={{ paddingTop: 40, textAlign: 'center' }}>
              <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                No messages yet in <strong style={{ color: 'var(--text-secondary)' }}>#{channelName}</strong>
              </p>
            </div>
          )}
          {messages.slice().reverse().map((m) => {
            const handle = m.authorHandle || m.authorDid.slice(0, 12)
            return (
              <div
                key={m.id}
                style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'flex-start' }}
              >
                <Avatar handle={handle} />
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 2 }}>
                    <span style={{ fontWeight: 600, fontSize: 13.5, color: 'var(--text-primary)' }}>
                      {handle}
                    </span>
                    <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                      {formatTime(m.createdAt)}
                    </span>
                  </div>
                  {renderContent(m.content)}
                </div>
              </div>
            )
          })}
        </div>

        {/* Compose */}
        <div style={{ padding: '12px 20px 16px', borderTop: '1px solid var(--border)', position: 'relative' }}>
          {showGifs && (
            <GifPicker
              onSelect={(url) => { handleSend(undefined, url); setShowGifs(false) }}
              onClose={() => setShowGifs(false)}
            />
          )}
          <form onSubmit={handleSend} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <div
              style={{
                flex: 1,
                display: 'flex',
                alignItems: 'center',
                background: 'var(--bg-elevated)',
                borderRadius: 8,
                border: '1px solid var(--border)',
                padding: '0 12px',
              }}
            >
              <input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder={`Message #${channelName}`}
                style={{
                  flex: 1,
                  padding: '10px 0',
                  background: 'transparent',
                  color: 'var(--text-primary)',
                  fontSize: 14,
                }}
              />
              <button
                type="button"
                onClick={() => setShowGifs(!showGifs)}
                title="GIF"
                style={{
                  background: 'none',
                  color: 'var(--text-muted)',
                  fontSize: 13,
                  fontWeight: 500,
                  padding: '4px 6px',
                  borderRadius: 4,
                  flexShrink: 0,
                }}
              >
                GIF
              </button>
            </div>
            <button
              type="submit"
              disabled={!input.trim()}
              style={{
                padding: '9px 18px',
                background: 'var(--accent)',
                color: '#fff',
                fontWeight: 600,
                fontSize: 13.5,
                borderRadius: 8,
                flexShrink: 0,
              }}
            >
              Send
            </button>
          </form>
        </div>
      </div>

      {showIntelligence && <IntelligencePanel />}
    </div>
  )
}
