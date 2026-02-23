import { useState } from 'react'

type Props = {
  serverName: string
  onClose: () => void
  onInvite: (handle: string) => Promise<void>
}

export function InviteModal({ serverName, onClose, onInvite }: Props) {
  const [handle, setHandle] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = handle.trim()
    if (!trimmed) return
    setSubmitting(true)
    setError(null)
    try {
      await onInvite(trimmed)
      setSuccess(true)
      setHandle('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invite failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.6)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 200,
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: 'var(--bg-secondary)',
          borderRadius: 12,
          padding: 24,
          width: '100%',
          maxWidth: 400,
          boxShadow: '0 16px 48px rgba(0,0,0,0.4)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 style={{ marginBottom: 16 }}>Invite to {serverName}</h2>
        <p style={{ color: 'var(--text-muted)', marginBottom: 16, fontSize: 14 }}>
          Enter a Bluesky handle (e.g. user.bsky.social) to add them to this server.
        </p>
        <form onSubmit={handleSubmit}>
          <input
            type="text"
            value={handle}
            onChange={(e) => setHandle(e.target.value)}
            placeholder="handle.bsky.social"
            style={{
              width: '100%',
              padding: 10,
              marginBottom: 16,
              background: 'var(--bg-tertiary)',
              border: '1px solid var(--border)',
              borderRadius: 6,
              color: 'var(--text-primary)',
            }}
          />
          {error && <p style={{ color: 'var(--danger)', marginBottom: 12, fontSize: 14 }}>{error}</p>}
          {success && <p style={{ color: 'var(--success)', marginBottom: 12, fontSize: 14 }}>Invite sent.</p>}
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <button type="button" onClick={onClose} style={{ padding: '8px 16px', background: 'var(--bg-elevated)', color: 'var(--text-secondary)' }}>
              Done
            </button>
            <button type="submit" disabled={submitting} style={{ padding: '8px 16px', background: 'var(--accent)', color: 'white', fontWeight: 600 }}>
              {submitting ? 'Inviting…' : 'Invite'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
