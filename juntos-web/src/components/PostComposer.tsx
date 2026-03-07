import { useState, useRef } from 'react'
import { getAtpAgent } from '../lib/atp'

type Props = {
  onSuccess: () => void
  onCancel?: () => void
  replyTo?: { uri: string; cid: string }
  placeholder?: string
  compact?: boolean
}

export function PostComposer({ onSuccess, onCancel, replyTo, placeholder = 'What\'s on your mind?', compact }: Props) {
  const [text, setText] = useState('')
  const [posting, setPosting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [imageBlob, setImageBlob] = useState<{ blob: { ref: { $link: string }; mimeType: string }; alt?: string } | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const agent = getAtpAgent()

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file || !file.type.startsWith('image/')) return
    setError(null)
    try {
      const data = new Uint8Array(await file.arrayBuffer())
      const res = await agent.uploadBlob(data, { encoding: file.type })
      setImageBlob({
        blob: res.data.blob as { ref: { $link: string }; mimeType: string },
        alt: file.name,
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to upload image')
    }
    e.target.value = ''
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = text.trim()
    if (!trimmed && !imageBlob) return
    setPosting(true)
    setError(null)
    try {
      const record: { text: string; createdAt: string; embed?: unknown; reply?: { parent: { uri: string; cid: string }; root: { uri: string; cid: string } } } = {
        text: trimmed || ' ',
        createdAt: new Date().toISOString(),
      }
      if (imageBlob) {
        record.embed = {
          $type: 'app.bsky.embed.images',
          images: [{ image: imageBlob.blob, alt: imageBlob.alt ?? '' }],
        }
      }
      if (replyTo) {
        record.reply = {
          parent: { uri: replyTo.uri, cid: replyTo.cid },
          root: { uri: replyTo.uri, cid: replyTo.cid },
        }
      }
      await agent.post(record as Parameters<typeof agent.post>[0])
      setText('')
      setImageBlob(null)
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to post')
    } finally {
      setPosting(false)
    }
  }

  const formStyle: React.CSSProperties = {
    padding: compact ? 12 : 16,
    background: 'var(--bg-secondary)',
    borderRadius: 8,
    border: '1px solid var(--border)',
  }

  return (
    <form onSubmit={handleSubmit} style={formStyle}>
      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={placeholder}
        rows={compact ? 2 : 4}
        maxLength={300}
        style={{
          width: '100%',
          padding: 12,
          marginBottom: 8,
          background: 'var(--bg-tertiary)',
          color: 'var(--text-primary)',
          border: '1px solid var(--border)',
          borderRadius: 6,
          resize: 'vertical',
        }}
      />
      {imageBlob && (
        <div style={{ marginBottom: 8, position: 'relative', display: 'inline-block' }}>
          <span style={{ color: 'var(--text-muted)', fontSize: 14 }}>Image attached</span>
          <button
            type="button"
            onClick={() => setImageBlob(null)}
            style={{
              position: 'absolute',
              top: 4,
              right: 4,
              background: 'var(--danger)',
              color: 'white',
              padding: '2px 8px',
              fontSize: 12,
            }}
          >
            Remove
          </button>
        </div>
      )}
      {error && <p style={{ color: 'var(--danger)', marginBottom: 8, fontSize: 14 }}>{error}</p>}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={handleImageChange}
          style={{ display: 'none' }}
        />
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          style={{ padding: '6px 12px', background: 'var(--bg-elevated)', color: 'var(--text-secondary)' }}
        >
          📷 Image
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} style={{ padding: '6px 12px', background: 'var(--bg-elevated)', color: 'var(--text-secondary)' }}>
            Cancel
          </button>
        )}
        <button
          type="submit"
          disabled={posting || (!text.trim() && !imageBlob)}
          style={{ marginLeft: 'auto', padding: '6px 16px', background: 'var(--accent)', color: 'white', fontWeight: 600 }}
        >
          {posting ? 'Posting…' : replyTo ? 'Reply' : 'Post'}
        </button>
      </div>
    </form>
  )
}
