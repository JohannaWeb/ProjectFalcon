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

  return (
    <div
      style={{
        width: '100%',
        maxWidth: 420,
        padding: 32,
        background: 'var(--bg-secondary)',
        borderRadius: 8,
        boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
      }}
    >
      <h1
        style={{
          fontSize: 24,
          fontWeight: 600,
          marginBottom: 8,
          textAlign: 'center',
        }}
      >
        Falcon
      </h1>
      <p style={{ color: 'var(--text-secondary)', textAlign: 'center', marginBottom: 24 }}>
        Sign in with your Bluesky / AT Protocol account
      </p>
      <form onSubmit={handleSubmit}>
        <label style={{ display: 'block', marginBottom: 8, color: 'var(--text-secondary)', fontSize: 12 }}>
          Handle or email
        </label>
        <input
          type="text"
          value={identifier}
          onChange={(e) => setIdentifier(e.target.value)}
          placeholder="you.bsky.social"
          autoComplete="username"
          required
          style={{
            width: '100%',
            padding: '10px 12px',
            marginBottom: 16,
            background: 'var(--bg-tertiary)',
            color: 'var(--text-primary)',
            border: '1px solid var(--border)',
          }}
        />
        <label style={{ display: 'block', marginBottom: 8, color: 'var(--text-secondary)', fontSize: 12 }}>
          App password
        </label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="xxxx-xxxx-xxxx-xxxx"
          autoComplete="current-password"
          required
          style={{
            width: '100%',
            padding: '10px 12px',
            marginBottom: 24,
            background: 'var(--bg-tertiary)',
            color: 'var(--text-primary)',
            border: '1px solid var(--border)',
          }}
        />
        {error && (
          <p style={{ color: 'var(--danger)', marginBottom: 16, fontSize: 14 }}>{error}</p>
        )}
        <button
          type="submit"
          disabled={submitting}
          style={{
            width: '100%',
            padding: 12,
            background: 'var(--accent)',
            color: 'white',
            fontWeight: 600,
          }}
        >
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p style={{ marginTop: 16, fontSize: 12, color: 'var(--text-muted)', textAlign: 'center' }}>
        Use an app password from Bluesky Settings → App passwords
      </p>
    </div>
  )
}
