import { useState, useCallback } from 'react'
import { ThemeProvider } from './contexts/ThemeContext'
import { Login } from './components/Login'
import { Layout } from './components/Layout'
import { useAtpSession } from './hooks/useAtpSession'

export default function App() {
  const { session, login, logout, loading, error } = useAtpSession()
  const [loginError, setLoginError] = useState<string | null>(null)

  const handleLogin = useCallback(
    async (identifier: string, password: string) => {
      setLoginError(null)
      try {
        await login(identifier, password)
      } catch (e) {
        setLoginError(e instanceof Error ? e.message : 'Login failed')
      }
    },
    [login]
  )

  return (
    <ThemeProvider>
      {loading && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
          <span style={{ color: 'var(--text-secondary)' }}>Connecting to AT Protocol…</span>
        </div>
      )}
      {!loading && !session && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: 'var(--bg-tertiary)' }}>
          <Login onLogin={handleLogin} error={error ?? loginError} />
        </div>
      )}
      {!loading && session && <Layout session={session} onLogout={logout} />}
    </ThemeProvider>
  )
}
