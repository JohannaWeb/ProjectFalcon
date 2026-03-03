import { useState } from 'react'

type Props = {
  onLogin: (identifier: string, password: string) => Promise<void>
  error: string | null
}

export function Login({ onLogin, error }: Props) {
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!identifier.trim() || !password) return
    setSubmitting(true)
    try {
      await onLogin(identifier.trim(), password)
    } finally {
      setSubmitting(false)
    }
  }

  const inputStyle: React.CSSProperties = {
    width: '100%',
    padding: '9px 12px',
    background: 'var(--bg-tertiary)',
    color: 'var(--text-primary)',
    border: '1px solid var(--border)',
    borderRadius: 8,
    fontSize: 14,
    transition: 'border-color 0.15s',
  }

  return (
    <div
      style={{
        width: '100%',
        maxWidth: 380,
        padding: '36px 32px',
        background: 'var(--bg-secondary)',
        borderRadius: 12,
        border: '1px solid var(--border)',
        boxShadow: '0 8px 32px rgba(0,0,0,0.3)',
      }}
    >
      <div style={{ marginBottom: 28, textAlign: 'center' }}>
        <div
          style={{
            width: 40,
            height: 40,
            borderRadius: 10,
            background: 'var(--accent)',
            color: '#fff',
            fontSize: 18,
            fontWeight: 700,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 16px',
          }}
        >
          F
        </div>
        <h1 style={{ fontSize: 20, fontWeight: 600, letterSpacing: '-0.02em', marginBottom: 6 }}>
          Sign in to Falcon
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>
          Use your Bluesky handle and app password
        </p>
      </div>

      <form onSubmit={handleSubmit}>
        <label
          style={{ display: 'block', marginBottom: 6, color: 'var(--text-secondary)', fontSize: 12, fontWeight: 500 }}
        >
          Handle or email
        </label>
        <input
          type="text"
          value={identifier}
          onChange={(e) => setIdentifier(e.target.value)}
          placeholder="you.bsky.social"
          autoComplete="username"
          required
          style={{ ...inputStyle, marginBottom: 16 }}
        />

        <label
          style={{ display: 'block', marginBottom: 6, color: 'var(--text-secondary)', fontSize: 12, fontWeight: 500 }}
        >
          App password
        </label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="xxxx-xxxx-xxxx-xxxx"
          autoComplete="current-password"
          required
          style={{ ...inputStyle, marginBottom: error ? 12 : 20 }}
        />

        {error && (
          <p style={{ color: 'var(--danger)', fontSize: 13, marginBottom: 16 }}>{error}</p>
        )}

        <button
          type="submit"
          disabled={submitting}
          style={{
            width: '100%',
            padding: '10px 0',
            background: 'var(--accent)',
            color: '#fff',
            fontWeight: 600,
            fontSize: 14,
            borderRadius: 8,
          }}
        >
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>

      <p style={{ marginTop: 20, fontSize: 12, color: 'var(--text-muted)', textAlign: 'center' }}>
        Create an app password at{' '}
        <span style={{ color: 'var(--text-secondary)' }}>Bluesky → Settings → App passwords</span>
      </p>
    </div>
  )
}
