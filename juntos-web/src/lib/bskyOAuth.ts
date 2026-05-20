import { BrowserOAuthClient, type OAuthSession } from '@atproto/oauth-client-browser'

export type { OAuthSession }

let client: BrowserOAuthClient | null = null

function getClient(): BrowserOAuthClient {
  if (!client) {
    const isProd = import.meta.env.PROD
    client = new BrowserOAuthClient({
      handleResolver: 'https://bsky.social',
      clientMetadata: isProd
        ? {
            client_id: 'https://juntos.chat/client-metadata.json',
            client_name: 'Juntos',
            client_uri: 'https://juntos.chat',
            redirect_uris: ['https://juntos.chat/'],
            scope: 'atproto transition:generic',
            grant_types: ['authorization_code', 'refresh_token'],
            response_types: ['code'],
            application_type: 'web',
            dpop_bound_access_tokens: true,
            token_endpoint_auth_method: 'none',
          }
        : {
            client_id: 'http://localhost',
            redirect_uris: [`http://127.0.0.1`],
            scope: 'atproto transition:generic',
            grant_types: ['authorization_code', 'refresh_token'],
            response_types: ['code'],
            application_type: 'web',
            dpop_bound_access_tokens: true,
            token_endpoint_auth_method: 'none',
          },
    })
  }
  return client
}

export async function initBskyOAuth(): Promise<OAuthSession | undefined> {
  const c = getClient()
  const result = await c.init()
  return result?.session
}

export async function signInWithBluesky(handle: string): Promise<OAuthSession> {
  const c = getClient()
  try {
    return await c.signInPopup(handle)
  } catch {
    // Popup blocked — fall back to redirect (will return never, page navigates away)
    await c.signInRedirect(handle)
    throw new Error('unreachable')
  }
}

export async function signOutBluesky(did: string): Promise<void> {
  const c = getClient()
  try {
    await c.revoke(did)
  } catch {
    // best-effort
  }
}

export async function getServiceAuthToken(session: OAuthSession): Promise<string> {
  const aud = import.meta.env.PROD ? 'did:web:juntos.chat' : 'did:web:localhost'
  const params = new URLSearchParams({ aud, lxm: 'app.juntos.server.list' })
  const res = await session.fetchHandler(`/xrpc/com.atproto.server.getServiceAuth?${params}`)
  if (!res.ok) throw new Error(`getServiceAuth ${res.status}: ${await res.text()}`)
  const json: { token: string } = await res.json()
  return json.token
}
