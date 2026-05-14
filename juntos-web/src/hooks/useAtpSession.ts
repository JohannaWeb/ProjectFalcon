import { useState, useEffect, useCallback } from 'react'
import {
  getAtpAgent,
  loadPersistedSession,
  persistSession,
  type AtpSession,
} from '../lib/atp'
import {
  initBskyOAuth,
  signInWithBluesky,
  signOutBluesky,
  getServiceAuthToken,
  type OAuthSession,
} from '../lib/bskyOAuth'
import { BACKEND_URL } from '../lib/backendApi'

export type { AtpSession }

const GOOGLE_SESSION_KEY = 'juntos_google_session'
const OAUTH_DID_KEY = 'juntos_oauth_did'

type StoredGoogleSession = { did: string; handle: string; accessJwt: string }

function loadGoogleSession(): StoredGoogleSession | null {
  try {
    const raw = localStorage.getItem(GOOGLE_SESSION_KEY)
    return raw ? (JSON.parse(raw) as StoredGoogleSession) : null
  } catch {
    return null
  }
}

function saveGoogleSession(s: StoredGoogleSession) {
  localStorage.setItem(GOOGLE_SESSION_KEY, JSON.stringify(s))
}

function clearGoogleSession() {
  localStorage.removeItem(GOOGLE_SESSION_KEY)
}

function googleOAuthUrl(): string {
  return `${BACKEND_URL || ''}/api/auth/google`
}

function extractGoogleTokenFromUrl(): string | null {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('google_token')
  if (token) {
    const clean = new URL(window.location.href)
    clean.searchParams.delete('google_token')
    window.history.replaceState({}, '', clean.toString())
  }
  return token
}

function decodeJwtPayload(token: string): Record<string, unknown> {
  try {
    return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return {}
  }
}

export function useAtpSession() {
  const [session, setSession] = useState<AtpSession | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [oauthSessionRef, setOauthSessionRef] = useState<OAuthSession | null>(null)

  useEffect(() => {
    let cancelled = false

    async function init() {
      // 1. Google token arriving in URL after OAuth redirect
      const googleToken = extractGoogleTokenFromUrl()
      if (googleToken) {
        const payload = decodeJwtPayload(googleToken)
        const did = (payload.sub as string) ?? ''
        const handle = (payload.email as string) ?? did
        const stored: StoredGoogleSession = { did, handle, accessJwt: googleToken }
        saveGoogleSession(stored)
        if (!cancelled) {
          setSession({ did, handle, accessJwt: googleToken } as any)
          setLoading(false)
        }
        return
      }

      // 2. Stored Google session
      const gSess = loadGoogleSession()
      if (gSess) {
        if (!cancelled) {
          setSession({ did: gSess.did, handle: gSess.handle, accessJwt: gSess.accessJwt } as any)
          setLoading(false)
        }
        return
      }

      // 3. Bluesky OAuth (popup flow stores session in IndexedDB via the client)
      try {
        const oauthSess = await initBskyOAuth()
        if (oauthSess && !cancelled) {
          setOauthSessionRef(oauthSess)
          localStorage.setItem(OAUTH_DID_KEY, oauthSess.did)
          const token = await getServiceAuthToken(oauthSess)
          if (!cancelled) {
            setSession({ did: oauthSess.did, handle: oauthSess.did, accessJwt: token } as any)
            setLoading(false)
          }
          return
        }
      } catch {
        // OAuth init failed — not a fatal error, fall through
      }

      // 4. AT Protocol app-password session (existing)
      const persisted = loadPersistedSession()
      if (persisted?.accessJwt && persisted?.refreshJwt) {
        try {
          const agent = getAtpAgent()
          agent.resumeSession(persisted)
          if (!cancelled) {
            setSession({
              ...persisted,
              handle: persisted.handle ?? '',
              did: persisted.did ?? '',
            } as AtpSession)
          }
        } catch {
          persistSession(null)
        }
      }

      if (!cancelled) setLoading(false)
    }

    init()
    return () => { cancelled = true }
  }, [])

  const login = useCallback(async (identifier: string, password: string) => {
    setError(null)
    const agent = getAtpAgent()
    const { data } = await agent.login({ identifier, password })
    persistSession(data as any)
    setSession({ ...data, handle: data.handle ?? '', did: data.did ?? '' } as any)
  }, [])

  const loginWithBluesky = useCallback(async (handle: string) => {
    setError(null)
    try {
      const oauthSess = await signInWithBluesky(handle)
      setOauthSessionRef(oauthSess)
      localStorage.setItem(OAUTH_DID_KEY, oauthSess.did)
      const token = await getServiceAuthToken(oauthSess)
      setSession({ did: oauthSess.did, handle, accessJwt: token } as any)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Bluesky sign-in failed')
      throw e
    }
  }, [])

  const loginWithGoogle = useCallback(() => {
    window.location.href = googleOAuthUrl()
  }, [])

  const logout = useCallback(async () => {
    persistSession(null)
    clearGoogleSession()
    localStorage.removeItem(OAUTH_DID_KEY)
    if (oauthSessionRef) {
      await signOutBluesky(oauthSessionRef.did).catch(() => {})
      setOauthSessionRef(null)
    }
    setSession(null)
    setError(null)
  }, [oauthSessionRef])

  return { session, login, loginWithBluesky, loginWithGoogle, logout, loading, error }
}
