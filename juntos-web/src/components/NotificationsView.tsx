import { useEffect, useState } from 'react'
import { getAtpAgent } from '../lib/atp'
import type { AppBskyNotificationListNotifications } from '@atproto/api'

type Notification = AppBskyNotificationListNotifications.Notification

export function NotificationsView() {
  const [items, setItems] = useState<Notification[]>([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)
  const [unreadCount, setUnreadCount] = useState(0)
  const agent = getAtpAgent()

  const load = () => {
    agent.listNotifications({ limit: 50 }).then(
      (res) => {
        setItems(res.data.notifications ?? [])
        setErr(null)
      },
      (e) => setErr(e instanceof Error ? e.message : 'Failed to load notifications')
    ).finally(() => setLoading(false))
    const a = agent as { countUnreadNotifications?: () => Promise<{ data?: { count?: number } }> }
    a.countUnreadNotifications?.().then?.(
      (r) => setUnreadCount(r?.data?.count ?? 0)
    ).catch(() => {})
  }

  useEffect(() => load(), [])

  const markRead = () => {
    const a = agent as { updateSeenNotifications?: () => Promise<unknown> }
    a.updateSeenNotifications?.().then?.(() => load()).catch(() => {})
  }

  if (loading && items.length === 0) {
    return <div style={{ padding: 24, color: 'var(--text-secondary)' }}>Loading notifications…</div>
  }
  if (err && items.length === 0) {
    return <div style={{ padding: 24, color: 'var(--danger)' }}>{err}</div>
  }

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <span style={{ fontWeight: 600 }}>Notifications</span>
        {(unreadCount > 0 || items.length > 0) && (
          <button type="button" onClick={markRead} style={{ padding: '4px 12px', background: 'var(--bg-elevated)', fontSize: 12 }}>
            Mark all read
          </button>
        )}
      </div>
      {items.length === 0 && (
        <p style={{ color: 'var(--text-muted)' }}>No notifications yet.</p>
      )}
      {items.map((n) => {
        const reason = (n as { reason?: string }).reason ?? 'unknown'
        const author = (n as { author?: { displayName?: string; handle?: string; avatar?: string } }).author
        const cid = (n as { cid?: string }).cid
        const uri = (n as { uri?: string }).uri
        return (
          <div
            key={`${uri}-${cid}`}
            style={{
              padding: 12,
              marginBottom: 8,
              background: 'var(--bg-secondary)',
              borderRadius: 8,
              border: '1px solid var(--border)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              {author?.avatar && (
                <img src={author.avatar} alt="" style={{ width: 36, height: 36, borderRadius: 18, objectFit: 'cover' }} />
              )}
              <div>
                <strong>{author?.displayName ?? author?.handle ?? 'Someone'}</strong>
                <span style={{ color: 'var(--text-muted)', marginLeft: 8 }}>
                  {reason === 'like' && 'liked your post'}
                  {reason === 'repost' && 'reposted your post'}
                  {reason === 'follow' && 'followed you'}
                  {reason === 'reply' && 'replied to your post'}
                  {reason === 'mention' && 'mentioned you'}
                  {!['like', 'repost', 'follow', 'reply', 'mention'].includes(reason) && reason}
                </span>
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}
