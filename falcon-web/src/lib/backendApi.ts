/**
 * Falcon backend API via AT Protocol XRPC (app.falcon.* lexicons).
 * All methods require a Session (AT access JWT + did + handle).
 */
export const BACKEND_URL = 'http://localhost:8080'

export type Session = { accessJwt: string; did: string; handle: string }

/** Lexicon-shaped types (app.falcon.defs) */
export type ServerSummary = { id: number; name: string; ownerDid: string; channels: { id: number; name: string }[] }
export type ChannelSummary = { id: number; name: string; serverId?: number }
export type MessageSummary = { id: number; content: string; authorDid: string; authorHandle: string; createdAt: string }

async function xrpcQuery<T>(
  nsid: string,
  params: Record<string, string | number> | undefined,
  session: Session
): Promise<T> {
  const url = new URL(`${BACKEND_URL}/xrpc/${nsid}`)
  if (params) {
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, String(v)))
  }
  const res = await fetch(url.toString(), {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${session.accessJwt}`,
    },
  })
  if (!res.ok) throw new Error(await res.text().catch(() => res.statusText))
  return res.json()
}

async function xrpcProcedure<T>(
  nsid: string,
  params: Record<string, string | number> | undefined,
  body: unknown,
  session: Session
): Promise<T> {
  const url = new URL(`${BACKEND_URL}/xrpc/${nsid}`)
  if (params) {
    Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, String(v)))
  }
  const res = await fetch(url.toString(), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${session.accessJwt}`,
    },
    body: body != null ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) throw new Error(await res.text().catch(() => res.statusText))
  return res.json()
}

export function getRealtimeWsUrl(session: Session): string {
  const wsUrl = new URL(BACKEND_URL)
  wsUrl.protocol = wsUrl.protocol === 'https:' ? 'wss:' : 'ws:'
  wsUrl.pathname = '/ws'
  wsUrl.search = ''
  wsUrl.searchParams.set('token', session.accessJwt)
  return wsUrl.toString()
}

export const backendApi = {
  listServers: (session: Session) =>
    xrpcQuery<ServerSummary[]>('app.falcon.server.list', undefined, session),

  getServer: (serverId: number, session: Session) =>
    xrpcQuery<ServerSummary>('app.falcon.server.get', { serverId }, session),

  createServer: (session: Session, name: string) =>
    xrpcProcedure<{ id: number; name: string; ownerDid: string; channelId: number }>(
      'app.falcon.server.create',
      undefined,
      { name },
      session
    ),

  listChannels: (serverId: number, session: Session) =>
    xrpcQuery<ChannelSummary[]>('app.falcon.channel.list', { serverId }, session),

  createChannel: (serverId: number, session: Session, name: string) =>
    xrpcProcedure<ChannelSummary>('app.falcon.channel.create', { serverId }, { name }, session),

  getMessages: (channelId: number, session: Session, limit = 50) =>
    xrpcQuery<MessageSummary[]>('app.falcon.channel.getMessages', { channelId, limit }, session),

  postMessage: (channelId: number, session: Session, content: string) =>
    xrpcProcedure<MessageSummary>('app.falcon.channel.postMessage', { channelId }, { content }, session),

  inviteToServer: (serverId: number, session: Session, handle: string) =>
    xrpcProcedure<{ did: string; handle: string }>('app.falcon.server.invite', { serverId }, { handle }, session),
}
