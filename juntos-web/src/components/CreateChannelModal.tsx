import { useState } from 'react'

type Props = {
  serverName: string
  onClose: () => void
  onCreate: (name: string) => Promise<void>
}

export function CreateChannelModal({ serverName, onClose, onCreate }: Props) {
  const [name, setName] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = name.trim()
    if (!trimmed) return
    setSubmitting(true)
    setError(null)
    try {
      await onCreate(trimmed)
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create channel')
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
        <h2 style={{ marginBottom: 16 }}>Create Channel</h2>
        <p style={{ color: 'var(--text-muted)', marginBottom: 16, fontSize: 14 }}>in {serverName}</p>
        <form onSubmit={handleSubmit}>
          <label style={{ display: 'block', marginBottom: 8, color: 'var(--text-secondary)', fontSize: 14 }}>
            Channel name
          </label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="general"
            autoFocus
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
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <button type="button" onClick={onClose} style={{ padding: '8px 16px', background: 'var(--bg-elevated)', color: 'var(--text-secondary)' }}>
              Cancel
            </button>
            <button type="submit" disabled={submitting} style={{ padding: '8px 16px', background: 'var(--accent)', color: 'white', fontWeight: 600 }}>
              {submitting ? 'Creating…' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
