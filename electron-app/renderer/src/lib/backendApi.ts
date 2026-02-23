const BACKEND_URL = 'http://localhost:8080'

export type Session = { did: string; handle: string }

async function request<T>(
  path: string,
  opts: { method?: string; body?: unknown; session?: Session } = {}
): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (opts.session) {
    headers['X-User-Did'] = opts.session.did
    headers['X-User-Handle'] = opts.session.handle ?? ''
  }
  const res = await fetch(`${BACKEND_URL}${path}`, {
    method: opts.method ?? 'GET',
    headers,
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  })
  if (!res.ok) throw new Error(await res.text().catch(() => res.statusText))
  return res.json()
}

export type ServerSummary = { id: number; name: string; ownerDid: string; channels: { id: number; name: string }[] }
export type ChannelSummary = { id: number; name: string; serverId: number }
export type MessageSummary = { id: number; content: string; authorDid: string; authorHandle: string; createdAt: string }

export const backendApi = {
  listServers: (session: Session) => request<ServerSummary[]>('/api/servers', { session }),
  getServer: (serverId: number, session: Session) => request<ServerSummary>(`/api/servers/${serverId}`, { session }),
  createServer: (session: Session, name: string) =>
    request<{ id: number; name: string; channelId: number }>('/api/servers', { method: 'POST', session, body: { name } }),
  listChannels: (serverId: number, session: Session) =>
    request<ChannelSummary[]>(`/api/channels/server/${serverId}`, { session }),
  createChannel: (serverId: number, session: Session, name: string) =>
    request<{ id: number; name: string }>(`/api/channels/server/${serverId}`, { method: 'POST', session, body: { name } }),
  getMessages: (channelId: number, session: Session, limit = 50) =>
    request<MessageSummary[]>(`/api/channels/${channelId}/messages?limit=${limit}`, { session }),
  postMessage: (channelId: number, session: Session, content: string) =>
    request<MessageSummary>(`/api/channels/${channelId}/messages`, { method: 'POST', session, body: { content } }),
  inviteToServer: (serverId: number, session: Session, handle: string) =>
    request<{ did: string; handle: string }>(`/api/servers/${serverId}/invite`, { method: 'POST', session, body: { handle } }),
}
