import { useEffect, useState } from 'react'
import { getAtpAgent } from '../lib/atp'

type FeedGen = { uri: string; cid: string; value?: { displayName?: { en?: string }; description?: { en?: string } }; avatar?: string }

export function ExploreView() {
  const [feeds, setFeeds] = useState<FeedGen[]>([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)
  const [selectedUri, setSelectedUri] = useState<string | null>(null)
  const [feedPosts, setFeedPosts] = useState<unknown[]>([])
  const agent = getAtpAgent()

  useEffect(() => {
    agent.app.bsky.feed.getSuggestedFeeds?.({ limit: 20 }).then(
      (res: { data?: { feeds?: FeedGen[] } }) => {
        setFeeds(res.data?.feeds ?? [])
        setErr(null)
      },
      (e: Error) => setErr(e?.message ?? 'Failed to load feeds')
    ).finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!selectedUri) { setFeedPosts([]); return }
    agent.app.bsky.feed.getFeed?.({ feed: selectedUri, limit: 30 }).then(
      (res: { data?: { feed?: unknown[] } }) => setFeedPosts(res.data?.feed ?? []),
      () => setFeedPosts([])
    )
  }, [selectedUri])

  if (loading && feeds.length === 0) {
    return <div style={{ padding: 24, color: 'var(--text-secondary)' }}>Loading explore…</div>
  }
  if (err && feeds.length === 0) {
    return <div style={{ padding: 24, color: 'var(--danger)' }}>{err}</div>
  }

  return (
    <div style={{ padding: 16, display: 'flex', gap: 16, flexWrap: 'wrap' }}>
      <div style={{ minWidth: 240, flex: '0 0 240px' }}>
        <h3 style={{ marginBottom: 12 }}>Suggested feeds</h3>
        {feeds.map((f) => (
          <button
            key={f.uri}
            type="button"
            onClick={() => setSelectedUri(selectedUri === f.uri ? null : f.uri)}
            style={{
              width: '100%',
              padding: 12,
              marginBottom: 8,
              textAlign: 'left',
              background: selectedUri === f.uri ? 'var(--bg-elevated)' : 'var(--bg-secondary)',
              border: '1px solid var(--border)',
              borderRadius: 8,
              color: 'var(--text-primary)',
            }}
          >
            {f.value?.displayName?.en ?? f.uri.split('/').pop() ?? 'Feed'}
          </button>
        ))}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        {selectedUri ? (
          feedPosts.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>No posts in this feed.</p>
          ) : (
            <div>
              {feedPosts.map((fp: any, i: number) => (
                <div
                  key={fp.post?.uri ?? i}
                  style={{
                    padding: 12,
                    marginBottom: 8,
                    background: 'var(--bg-secondary)',
                    borderRadius: 8,
                    border: '1px solid var(--border)',
                  }}
                >
                  <strong>{fp.post?.author?.displayName ?? fp.post?.author?.handle ?? 'Unknown'}</strong>
                  <p style={{ whiteSpace: 'pre-wrap', marginTop: 4 }}>{fp.post?.record?.text ?? ''}</p>
                </div>
              ))}
            </div>
          )
        ) : (
          <p style={{ color: 'var(--text-muted)' }}>Select a feed to view posts.</p>
        )}
      </div>
    </div>
  )
}
