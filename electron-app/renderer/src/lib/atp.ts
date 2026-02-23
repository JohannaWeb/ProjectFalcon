import { AtpAgent, AtpSessionData } from '@atproto/api'

const DEFAULT_SERVICE = 'https://bsky.social'

let agent: AtpAgent | null = null

export type AtpSession = AtpSessionData & { handle: string; did: string }

function getAgent(): AtpAgent {
  if (!agent) {
    agent = new AtpAgent({ service: DEFAULT_SERVICE })
  }
  return agent
}

export function getAtpAgent(): AtpAgent {
  return getAgent()
}

export function persistSession(data: AtpSessionData | null): void {
  if (data) {
    try {
      localStorage.setItem('atp_session', JSON.stringify(data))
    } catch {
      // ignore
    }
  } else {
    localStorage.removeItem('atp_session')
  }
}

export function loadPersistedSession(): AtpSessionData | null {
  try {
    const raw = localStorage.getItem('atp_session')
    if (!raw) return null
    return JSON.parse(raw) as AtpSessionData
  } catch {
    return null
  }
}

export function resumeSession(data: AtpSessionData): AtpSession | null {
  const a = getAgent()
  a.resumeSession(data)
  return {
    ...data,
    handle: data.handle ?? '',
    did: data.did ?? '',
  }
}
