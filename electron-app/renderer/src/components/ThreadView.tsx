import { useEffect, useState } from 'react'
import { getAtpAgent } from '../lib/atp'
import type { AppBskyFeedDefs } from '@atproto/api'
import { PostCard } from './PostCard'
import { PostComposer } from './PostComposer'

type ThreadNode = AppBskyFeedDefs.ThreadViewPost

type Props = {
  uri: string
  onClose: () => void
  initialReplyTo?: { uri: string; cid: string }
}

function ThreadPost({ node, onOpenThread, onReply, onRefresh }: {
  node: ThreadNode
  onOpenThread: (uri: string) => void
  onReply: (uri: string, cid: string) => void
  onRefresh: () => void
}) {
  if (!node || node.$type !== 'app.bsky.feed.defs#threadViewPost') return null
  const post = node.post
  const author = post.author
  const record = (post.record as { text?: string }) ?? {}
  const text = record.text ?? ''

  return (
    <div style={{ marginLeft: 24, borderLeft: '2px solid var(--border)', paddingLeft: 12, marginBottom: 12 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 16,
            background: 'var(--bg-elevated)',
            overflow: 'hidden',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {author.avatar ? (
            <img src={author.avatar} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          ) : (
            <span style={{ fontSize: 14 }}>👤</span>
          )}
        </div>
        <strong style={{ fontSize: 14 }}>{author.displayName ?? author.handle}</strong>
        <span style={{ fontSize: 12, color: 'var(--text-muted)' }}@{author.handle}</span>
      </div>
      <p style={{ whiteSpace: 'pre-wrap', fontSize: 14, marginBottom: 8 }}>{text}</p>
      <div style={{ display: 'flex', gap: 12, fontSize: 12 }}>
        <button type="button" onClick={() => onReply(post.uri, post.cid)} style={{ background: 'none', color: 'var(--text-secondary)' }}>Reply</button>
        <button type="button" onClick={() => onOpenThread(post.uri)} style={{ background: 'none', color: 'var(--text-secondary)' }}>Thread</button>
      </div>
      {node.replies?.length ? (
        <div style={{ marginTop: 8 }}>
          {node.replies.map((r) => (
            <ThreadPost
              key={(r as ThreadNode).post?.uri ?? Math.random()}
              node={r as ThreadNode}
              onOpenThread={onOpenThread}
              onReply={onReply}
              onRefresh={onRefresh}
            />
          ))}
        </div>
      ) : null}
    </div>
  )
}

export function ThreadView({ uri: initialUri, onClose, initialReplyTo }: Props) {
  const [threadUri, setThreadUri] = useState(initialUri)
  const [thread, setThread] = useState<ThreadNode | null>(null)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)
  const [replyTo, setReplyTo] = useState<{ uri: string; cid: string } | null>(initialReplyTo ?? null)
  const agent = getAtpAgent()

  const load = (u?: string) => {
    const target = u ?? threadUri
    setLoading(true)
    setErr(null)
    agent.getPostThread({ uri: target }).then(
      (res) => {
        const node = res.data.thread
        if (node && (node as ThreadNode).$type === 'app.bsky.feed.defs#threadViewPost') {
          setThread(node as ThreadNode)
        } else {
          setThread(null)
        }
      },
      (e) => setErr(e instanceof Error ? e.message : 'Failed to load thread')
    ).finally(() => setLoading(false))
  }

  useEffect(() => {
    setThreadUri(initialUri)
    setReplyTo(initialReplyTo ?? null)
    load(initialUri)
  }, [initialUri, initialReplyTo])

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.6)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 100,
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: 'var(--bg-primary)',
          borderRadius: 12,
          maxWidth: 560,
          width: '90%',
          maxHeight: '85vh',
          overflow: 'auto',
          padding: 24,
          boxShadow: '0 16px 48px rgba(0,0,0,0.4)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h2 style={{ fontSize: 18 }}>Thread</h2>
          <button onClick={onClose} style={{ padding: '4px 12px', background: 'var(--bg-elevated)' }}>Close</button>
        </div>
        {loading && <p style={{ color: 'var(--text-muted)' }}>Loading…</p>}
        {err && <p style={{ color: 'var(--danger)' }}>{err}</p>}
        {thread && !loading && (
          <>
            {thread.post && (
              <PostCard
                fp={{ post: thread.post, reply: thread.reply, reason: thread.reason }}
                onOpenThread={(u) => { if (u !== threadUri) setThreadUri(u); load(u) }}
                onReply={setReplyTo}
                onRefresh={() => load()}
              />
            )}
            {replyTo && (
              <div style={{ marginTop: 16 }}>
                <PostComposer
                  replyTo={replyTo}
                  onSuccess={() => { setReplyTo(null); load() }}
                  onCancel={() => setReplyTo(null)}
                  placeholder="Write a reply…"
                  compact
                />
              </div>
            )}
            {thread.replies?.map((r) => (
              <ThreadPost
                key={(r as ThreadNode).post?.uri ?? Math.random()}
                node={r as ThreadNode}
                onOpenThread={(u) => { setThreadUri(u); load(u) }}
                onReply={setReplyTo}
                onRefresh={() => load()}
              />
            ))}
          </>
        )}
      </div>
    </div>
  )
}
