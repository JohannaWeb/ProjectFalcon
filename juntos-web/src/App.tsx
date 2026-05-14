import { useCallback } from 'react'
import { ThemeProvider } from './contexts/ThemeContext'
import { Login } from './components/Login'
import { Layout } from './components/Layout'
import { useAtpSession } from './hooks/useAtpSession'

export default function App() {
  const { session, login, loginWithBluesky, loginWithGoogle, logout, loading, error } = useAtpSession()

  const handleLogin = useCallback(
    async (identifier: string, password: string) => {
      await login(identifier, password)
    },
    [login]
  )

  return (
    <ThemeProvider>
      {loading && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
          <span style={{ color: 'var(--text-secondary)' }}>Connecting…</span>
        </div>
      )}
      {!loading && !session && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: 'var(--bg-tertiary)' }}>
          <Login
            onLogin={handleLogin}
            onLoginWithBluesky={loginWithBluesky}
            onLoginWithGoogle={loginWithGoogle}
            error={error}
          />
        </div>
      )}
      {!loading && session && <Layout session={session} onLogout={logout} />}
    </ThemeProvider>
  )
}
