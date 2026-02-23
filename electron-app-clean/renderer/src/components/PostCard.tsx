import type { AppBskyFeedDefs } from '@atproto/api'
import { getAtpAgent } from '../lib/atp'

type FeedPost = AppBskyFeedDefs.FeedViewPost

type Props = {
  fp: FeedPost
  onOpenThread: (uri: string) => void
  onReply: (uri: string, cid: string) => void
  onRefresh: () => void
}

export function PostCard({ fp, onOpenThread, onReply, onRefresh }: Props) {
  const post = fp.post
  const author = post.author
  const record = post.record as { text?: string }
  const text = record?.text ?? ''
  const viewer = (post as { viewer?: { like?: string; repost?: string } }).viewer
  const agent = getAtpAgent()

  const handleLike = async () => {
    try {
      if (viewer?.like) {
        await agent.deleteLike(viewer.like)
      } else {
        await agent.like(post.uri, post.cid)
      }
      onRefresh()
    } catch {
      // ignore
    }
  }

  const handleRepost = async () => {
    try {
      if (viewer?.repost) {
        await agent.deleteRepost(viewer.repost)
      } else {
        await agent.repost(post.uri, post.cid)
      }
      onRefresh()
    } catch {
      // ignore
    }
  }

  return (
    <article
      style={{
        padding: 16,
        marginBottom: 12,
        background: 'var(--bg-secondary)',
        borderRadius: 8,
        border: '1px solid var(--border)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
        <a
          href="#"
          onClick={(e) => { e.preventDefault(); onOpenThread(post.uri) }}
          style={{ display: 'flex', alignItems: 'center', gap: 12, textDecoration: 'none', color: 'inherit' }}
        >
          <div
            style={{
              width: 40,
              height: 40,
              borderRadius: 20,
              background: 'var(--bg-elevated)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              overflow: 'hidden',
            }}
          >
            {author.avatar ? (
              <img
                src={author.avatar}
                alt=""
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            ) : (
              <span style={{ fontSize: 18 }}>👤</span>
            )}
          </div>
          <div>
            <strong style={{ display: 'block' }}>{author.displayName ?? author.handle}</strong>
            <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>@{author.handle}</span>
          </div>
        </a>
      </div>
      <p
        style={{ whiteSpace: 'pre-wrap', lineHeight: 1.5, marginBottom: 12 }}
        onClick={() => onOpenThread(post.uri)}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => e.key === 'Enter' && onOpenThread(post.uri)}
      >
        {text}
      </p>
      <div style={{ display: 'flex', gap: 16, fontSize: 14 }}>
        <button
          type="button"
          onClick={() => onReply(post.uri, post.cid)}
          style={{
            background: 'none',
            color: 'var(--text-secondary)',
            padding: 4,
          }}
        >
          💬 Reply
        </button>
        <button
          type="button"
          onClick={handleRepost}
          style={{
            background: 'none',
            color: viewer?.repost ? 'var(--success)' : 'var(--text-secondary)',
            padding: 4,
          }}
        >
          🔁 Repost
        </button>
        <button
          type="button"
          onClick={handleLike}
          style={{
            background: 'none',
            color: viewer?.like ? 'var(--danger)' : 'var(--text-secondary)',
            padding: 4,
          }}
        >
          ❤️ Like
        </button>
        <button
          type="button"
          onClick={() => onOpenThread(post.uri)}
          style={{ background: 'none', color: 'var(--text-secondary)', padding: 4 }}
        >
          Thread
        </button>
      </div>
    </article>
  )
}
