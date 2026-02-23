import { useState, useEffect, useCallback } from 'react'
import {
  getAtpAgent,
  loadPersistedSession,
  persistSession,
  type AtpSession,
} from '../lib/atp'

export function useAtpSession() {
  const [session, setSession] = useState<AtpSession | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const persisted = loadPersistedSession()
    if (!persisted?.accessJwt || !persisted?.refreshJwt) {
      setLoading(false)
      return
    }
    try {
      const agent = getAtpAgent()
      agent.resumeSession(persisted)
      setSession({
        ...persisted,
        handle: persisted.handle ?? '',
        did: persisted.did ?? '',
      })
      setError(null)
    } catch {
      persistSession(null)
      setSession(null)
    }
    setLoading(false)
  }, [])

  const login = useCallback(async (identifier: string, password: string) => {
    setError(null)
    const agent = getAtpAgent()
    const { data } = await agent.login({ identifier, password })
    persistSession(data)
    setSession({
      ...data,
      handle: data.handle ?? '',
      did: data.did ?? '',
    })
  }, [])

  const logout = useCallback(() => {
    persistSession(null)
    setSession(null)
    setError(null)
  }, [])

  return { session, login, logout, loading, error }
}
