import { useEffect, useState } from 'react'
import { backendApi, type ServerSummary, type MessageSummary } from '../lib/backendApi'
import type { AtpSession } from '../lib/atp'

type Props = { session: AtpSession }

export function ServersView({ session }: Props) {
  const [servers, setServers] = useState<ServerSummary[]>([])
  const [selectedChannelId, setSelectedChannelId] = useState<number | null>(null)
  const [selectedChannelName, setSelectedChannelName] = useState('')
  const [messages, setMessages] = useState<MessageSummary[]>([])
  const [newMsg, setNewMsg] = useState('')
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)
  const [createServerName, setCreateServerName] = useState('')
  const sess = { accessJwt: session.accessJwt, did: session.did, handle: session.handle }

  const loadServers = () => {
    backendApi.listServers(sess).then(setServers, (e) => setErr(e?.message ?? 'Failed')).finally(() => setLoading(false))
  }

  useEffect(() => loadServers(), [])

  useEffect(() => {
    if (!selectedChannelId) { setMessages([]); return }
    backendApi.getMessages(selectedChannelId, sess).then(setMessages, () => setMessages([]))
  }, [selectedChannelId])

  const handleCreateServer = (e: React.FormEvent) => {
    e.preventDefault()
    if (!createServerName.trim()) return
    backendApi.createServer(sess, createServerName.trim()).then(() => {
      setCreateServerName('')
      loadServers()
    }, (e) => setErr(e?.message))
  }

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedChannelId || !newMsg.trim()) return
    backendApi.postMessage(selectedChannelId, sess, newMsg.trim()).then(() => {
      setNewMsg('')
      backendApi.getMessages(selectedChannelId, sess).then(setMessages)
    })
  }

  return (
    <div style={{ padding: 16, display: 'flex', gap: 16, height: '100%' }}>
      <div style={{ minWidth: 200 }}>
        <h3 style={{ marginBottom: 8 }}>Servers</h3>
        <form onSubmit={handleCreateServer} style={{ marginBottom: 12 }}>
          <input
            value={createServerName}
            onChange={(e) => setCreateServerName(e.target.value)}
            placeholder="New server name"
            style={{ width: '100%', padding: 8, marginBottom: 4, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }}
          />
          <button type="submit" style={{ padding: '6px 12px', background: 'var(--accent)', color: 'white' }}>Create</button>
        </form>
        {err && <p style={{ color: 'var(--danger)', fontSize: 12 }}>{err}</p>}
        {servers.map((s) => (
          <div key={s.id} style={{ marginBottom: 8 }}>
            <div style={{ fontWeight: 600, marginBottom: 4 }}>{s.name}</div>
            {s.channels?.map((c) => (
              <button
                key={c.id}
                type="button"
                onClick={() => { setSelectedChannelId(c.id); setSelectedChannelName(c.name) }}
                style={{
                  display: 'block',
                  width: '100%',
                  padding: '6px 12px',
                  textAlign: 'left',
                  background: selectedChannelId === c.id ? 'var(--bg-elevated)' : 'transparent',
                  border: 'none',
                  color: 'var(--text-secondary)',
                  borderRadius: 4,
                }}
              >
                # {c.name}
              </button>
            ))}
          </div>
        ))}
        {!loading && servers.length === 0 && <p style={{ color: 'var(--text-muted)', fontSize: 14 }}>Create a server to get started.</p>}
      </div>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        {selectedChannelId ? (
          <>
            <div style={{ marginBottom: 12, fontWeight: 600 }}># {selectedChannelName}</div>
            <div style={{ flex: 1, overflow: 'auto', marginBottom: 12 }}>
              {messages.slice().reverse().map((m) => (
                <div key={m.id} style={{ marginBottom: 8 }}>
                  <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>{m.authorHandle || m.authorDid}</span>
                  <span style={{ marginLeft: 8 }}>{m.content}</span>
                </div>
              ))}
            </div>
            <form onSubmit={handleSendMessage}>
              <input
                value={newMsg}
                onChange={(e) => setNewMsg(e.target.value)}
                placeholder="Message..."
                style={{ width: '100%', padding: 10, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 6, color: 'var(--text-primary)' }}
              />
              <button type="submit" style={{ marginTop: 8, padding: '8px 16px', background: 'var(--accent)', color: 'white' }}>Send</button>
            </form>
          </>
        ) : (
          <p style={{ color: 'var(--text-muted)' }}>Select a channel.</p>
        )}
      </div>
    </div>
  )
}

