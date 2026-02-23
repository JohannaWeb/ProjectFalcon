import { useEffect, useState, useCallback } from 'react'
import { getAtpAgent } from '../lib/atp'
import type { AppBskyFeedDefs } from '@atproto/api'
import { PostCard } from './PostCard'
import { PostComposer } from './PostComposer'
import { ThreadView } from './ThreadView'

type FeedPost = AppBskyFeedDefs.FeedViewPost

export function FeedView() {
  const [posts, setPosts] = useState<FeedPost[]>([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)
  const [threadUri, setThreadUri] = useState<string | null>(null)
  const [threadReplyTo, setThreadReplyTo] = useState<{ uri: string; cid: string } | null>(null)
  const [showComposer, setShowComposer] = useState(false)

  const load = useCallback(() => {
    const agent = getAtpAgent()
    agent.getTimeline({ limit: 30 }).then(
      (res) => {
        setPosts(res.data.feed ?? [])
        setErr(null)
      },
      (e) => setErr(e instanceof Error ? e.message : 'Failed to load feed')
    )
  }, [])

  useEffect(() => {
    setLoading(true)
    load()
    setLoading(false)
  }, [load])

  if (loading && posts.length === 0) {
    return (
      <div style={{ padding: 24, color: 'var(--text-secondary)' }}>
        Loading your feed…
      </div>
    )
  }

  if (err && posts.length === 0) {
    return (
      <div style={{ padding: 24, color: 'var(--danger)' }}>
        {err}
      </div>
    )
  }

  return (
    <div style={{ padding: 16 }}>
      {showComposer && (
        <div style={{ marginBottom: 16 }}>
          <PostComposer
            onSuccess={() => { setShowComposer(false); load() }}
            onCancel={() => setShowComposer(false)}
          />
        </div>
      )}
      {!showComposer && (
        <button
          type="button"
          onClick={() => setShowComposer(true)}
          style={{
            marginBottom: 16,
            padding: '10px 16px',
            background: 'var(--bg-secondary)',
            border: '1px solid var(--border)',
            color: 'var(--text-secondary)',
            borderRadius: 8,
            width: '100%',
            textAlign: 'left',
          }}
        >
          What's on your mind?
        </button>
      )}
      {posts.length === 0 && (
        <div style={{ padding: 24, color: 'var(--text-muted)' }}>
          No posts in your feed yet. Follow some people on Bluesky!
        </div>
      )}
      {posts.map((fp) => (
        <PostCard
          key={fp.post.uri}
          fp={fp}
          onOpenThread={(uri) => { setThreadUri(uri); setThreadReplyTo(null) }}
          onReply={(uri, cid) => { setThreadUri(uri); setThreadReplyTo({ uri, cid }) }}
          onRefresh={load}
        />
      ))}
      {threadUri && (
        <ThreadView
          uri={threadUri}
          onClose={() => { setThreadUri(null); setThreadReplyTo(null) }}
          initialReplyTo={threadReplyTo ?? undefined}
        />
      )}
    </div>
  )
}
