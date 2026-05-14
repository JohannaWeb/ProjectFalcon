import { useState } from 'react'

type Props = {
  onLogin: (identifier: string, password: string) => Promise<void>
  onLoginWithBluesky: (handle: string) => Promise<void>
  onLoginWithGoogle: () => void
  error: string | null
}

export function Login({ onLogin, onLoginWithBluesky, onLoginWithGoogle, error }: Props) {
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [bskyHandle, setBskyHandle] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [bskySubmitting, setBskySubmitting] = useState(false)

  const inputStyle: React.CSSProperties = {
    width: '100%',
    padding: '9px 12px',
    background: 'var(--bg-tertiary)',
    color: 'var(--text-primary)',
    border: '1px solid var(--border)',
    borderRadius: 8,
    fontSize: 14,
    boxSizing: 'border-box',
  }

  const handleBskySubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!bskyHandle.trim()) return
    setBskySubmitting(true)
    try {
      await onLoginWithBluesky(bskyHandle.trim())
    } finally {
      setBskySubmitting(false)
    }
  }

  const handlePasswordSubmit = async (e: React.FormEvent) => {
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
        maxWidth: 400,
        padding: '36px 32px',
        background: 'var(--bg-secondary)',
        borderRadius: 12,
        border: '1px solid var(--border)',
        boxShadow: '0 8px 32px rgba(0,0,0,0.3)',
      }}
    >
      {/* Logo + heading */}
      <div style={{ marginBottom: 28, textAlign: 'center' }}>
        <div
          style={{
            width: 40, height: 40, borderRadius: 10,
            background: 'var(--accent)', color: '#fff',
            fontSize: 18, fontWeight: 700,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 16px',
          }}
        >J</div>
        <h1 style={{ fontSize: 20, fontWeight: 600, letterSpacing: '-0.02em', marginBottom: 4 }}>
          Sign in to Juntos
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>
          Join the conversation
        </p>
      </div>

      {/* Bluesky OAuth */}
      <form onSubmit={handleBskySubmit} style={{ marginBottom: 12 }}>
        <input
          type="text"
          value={bskyHandle}
          onChange={(e) => setBskyHandle(e.target.value)}
          placeholder="your-handle.bsky.social"
          style={{ ...inputStyle, marginBottom: 8 }}
        />
        <button
          type="submit"
          disabled={bskySubmitting || !bskyHandle.trim()}
          style={{
            width: '100%', padding: '10px 0',
            background: '#0085ff', color: '#fff',
            fontWeight: 600, fontSize: 14, borderRadius: 8,
            border: 'none', cursor: 'pointer', opacity: bskySubmitting ? 0.7 : 1,
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          }}
        >
          <svg width="16" height="16" viewBox="0 0 568 501" fill="white" aria-hidden>
            <path d="M123.121 33.664C188.241 82.553 258.281 181.68 284 234.873c25.719-53.192 95.759-152.32 160.879-201.209C491.866-1.706 568-28.622 568 57.829c0 17.455-10.008 146.729-15.873 167.656-20.381 72.638-94.136 91.179-159.649 79.98 114.506 19.107 143.684 82.19 80.757 145.118-119.206 122.524-171.396-30.755-184.5-70.175-2.681-7.816-3.937-11.447-3.935-8.33.002 3.118-1.253 6.749-3.934 14.565-13.104 39.42-65.294 192.699-184.5 70.175-62.927-62.928-33.749-126.011 80.757-145.118-65.513 11.199-139.268-7.342-159.649-79.98C10.008 204.558 0 75.284 0 57.829c0-86.451 76.134-59.535 123.121-24.165z" />
          </svg>
          {bskySubmitting ? 'Opening…' : 'Continue with Bluesky'}
        </button>
      </form>

      {/* Google OAuth */}
      <button
        type="button"
        onClick={onLoginWithGoogle}
        style={{
          width: '100%', padding: '10px 0',
          background: 'var(--bg-primary)', color: 'var(--text-primary)',
          fontWeight: 600, fontSize: 14, borderRadius: 8,
          border: '1px solid var(--border)', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        }}
      >
        <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden>
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
        </svg>
        Continue with Google
      </button>

      {/* Divider */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '24px 0' }}>
        <div style={{ flex: 1, height: 1, background: 'var(--border)' }} />
        <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>or use app password</span>
        <div style={{ flex: 1, height: 1, background: 'var(--border)' }} />
      </div>

      {/* Legacy app-password form */}
      <form onSubmit={handlePasswordSubmit}>
        <label style={{ display: 'block', marginBottom: 6, color: 'var(--text-secondary)', fontSize: 12, fontWeight: 500 }}>
          Handle or email
        </label>
        <input
          type="text"
          value={identifier}
          onChange={(e) => setIdentifier(e.target.value)}
          placeholder="you.bsky.social"
          autoComplete="username"
          style={{ ...inputStyle, marginBottom: 16 }}
        />

        <label style={{ display: 'block', marginBottom: 6, color: 'var(--text-secondary)', fontSize: 12, fontWeight: 500 }}>
          App password
        </label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="xxxx-xxxx-xxxx-xxxx"
          autoComplete="current-password"
          style={{ ...inputStyle, marginBottom: error ? 12 : 20 }}
        />

        {error && (
          <p style={{ color: 'var(--danger)', fontSize: 13, marginBottom: 16 }}>{error}</p>
        )}

        <button
          type="submit"
          disabled={submitting}
          style={{
            width: '100%', padding: '10px 0',
            background: 'var(--accent)', color: '#fff',
            fontWeight: 600, fontSize: 14, borderRadius: 8,
          }}
        >
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  )
}
