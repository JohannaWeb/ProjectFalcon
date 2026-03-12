import { AtpAgent, type AtpSessionData } from '@atproto/api'

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
  } as AtpSession
}
export const chatApi = {
  listConvos: async () => {
    // We use a proxy to tell the PDS to forward this to the chat service
    const res = await getAtpAgent().api.chat.bsky.convo.listConvos({}, { headers: { 'atproto-proxy': 'did:web:api.bsky.chat#bsky_chat' } })
    return res.data
  },
  getConvo: async (convoId: string) => {
    const res = await getAtpAgent().api.chat.bsky.convo.getConvo({ convoId }, { headers: { 'atproto-proxy': 'did:web:api.bsky.chat#bsky_chat' } })
    return res.data
  },
  getMessages: async (convoId: string, limit = 50) => {
    const res = await getAtpAgent().api.chat.bsky.convo.getMessages({ convoId, limit }, { headers: { 'atproto-proxy': 'did:web:api.bsky.chat#bsky_chat' } })
    return res.data
  },
  getConvoForMembers: async (members: string[]) => {
    const res = await getAtpAgent().api.chat.bsky.convo.getConvoForMembers({ members }, { headers: { 'atproto-proxy': 'did:web:api.bsky.chat#bsky_chat' } })
    return res.data
  },
  sendMessage: async (convoId: string, text: string) => {
    const res = await getAtpAgent().api.chat.bsky.convo.sendMessage({ convoId, message: { text } }, { headers: { 'atproto-proxy': 'did:web:api.bsky.chat#bsky_chat' }, encoding: 'application/json' })
    return res.data
  }
}
