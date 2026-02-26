import { useEffect, useState } from 'react'
import { getAtpAgent } from '../lib/atp'
import type { AppBskyActorDefs } from '@atproto/api'

type Props = { actor: string; meDid?: string }

export function ProfileView({ actor, meDid }: Props) {
  const [profile, setProfile] = useState<AppBskyActorDefs.ProfileViewDetailed | null>(null)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)
  const [following, setFollowing] = useState(false)
  const [followUri, setFollowUri] = useState<string | null>(null)
  const agent = getAtpAgent()
  const me = meDid

  useEffect(() => {
    setLoading(true)
    setErr(null)
    agent.getProfile({ actor }).then(
      (res) => {
        setProfile(res.data)
        const v = (res.data as { viewer?: { following?: string } }).viewer
        setFollowing(!!v?.following)
        setFollowUri(v?.following ?? null)
      },
      (e) => setErr(e instanceof Error ? e.message : 'Failed to load profile')
    ).finally(() => setLoading(false))
  }, [actor])

  const handleFollow = async () => {
    if (!profile?.did) return
    try {
      if (following && followUri) {
        await agent.deleteFollow(followUri)
        setFollowing(false)
        setFollowUri(null)
      } else {
        await agent.follow(profile.did)
        setFollowing(true)
        setFollowUri('pending')
      }
    } catch {
      // ignore
    }
  }

  if (loading) return <div style={{ padding: 24, color: 'var(--text-secondary)' }}>Loading…</div>
  if (err) return <div style={{ padding: 24, color: 'var(--danger)' }}>{err}</div>
  if (!profile) return null

  const isMe = me === profile.did

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 24, marginBottom: 24 }}>
        <div
          style={{
            width: 80,
            height: 80,
            borderRadius: 40,
            background: 'var(--bg-elevated)',
            overflow: 'hidden',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {profile.avatar ? (
            <img src={profile.avatar} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          ) : (
            <span style={{ fontSize: 36 }}>👤</span>
          )}
        </div>
        <div>
          <h1 style={{ fontSize: 24, marginBottom: 4 }}>{profile.displayName ?? profile.handle}</h1>
          <p style={{ color: 'var(--text-muted)', marginBottom: 8 }}>@{profile.handle}</p>
          {profile.description && (
            <p style={{ maxWidth: 480, lineHeight: 1.5, marginBottom: 12 }}>{profile.description}</p>
          )}
          <div style={{ display: 'flex', gap: 16, fontSize: 14, color: 'var(--text-secondary)' }}>
            <span>{profile.followersCount ?? 0} followers</span>
            <span>{profile.followsCount ?? 0} following</span>
          </div>
          {!isMe && (
            <button
              type="button"
              onClick={handleFollow}
              style={{
                marginTop: 12,
                padding: '8px 20px',
                background: following ? 'var(--bg-elevated)' : 'var(--accent)',
                color: 'var(--text-primary)',
                fontWeight: 600,
              }}
            >
              {following ? 'Unfollow' : 'Follow'}
            </button>
          )}
        </div>
      </div>
    </div>

      {
    isMe && (
      <div style={{ marginTop: 40, borderTop: '1px solid var(--border)', paddingTop: 24 }}>
        <h2 style={{ fontSize: 20, marginBottom: 16 }}>Sovereign Integration Vessels (SIV)</h2>
        <div style={{ background: 'var(--bg-elevated)', padding: 20, borderRadius: 12, maxWidth: 600 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
            <span style={{ color: 'var(--accent)', fontWeight: 'bold' }}>SIV-01</span>
            <span style={{ fontWeight: 600 }}>GitHub Integration</span>
          </div>

          <form onSubmit={async (e) => {
            e.preventDefault()
            const target = e.target as any
            const token = target.token.value
            const repo = target.repo.value

            const res = await fetch('/api/siv/configs', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
              },
              body: JSON.stringify({
                vesselType: 'github',
                token,
                config: { repo }
              })
            })

            if (res.ok) {
              alert('Sovereign Link Established.')
            }
          }}>
            <div style={{ marginBottom: 12 }}>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>Access Token</label>
              <input name="token" type="password" style={{ width: '100%', padding: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }} />
            </div>
            <div style={{ marginBottom: 16 }}>
              <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>Repository (owner/repo)</label>
              <input name="repo" placeholder="e.g. facebook/react" style={{ width: '100%', padding: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }} />
            </div>
            <button type="submit" style={{ padding: '8px 16px', background: 'var(--accent)', color: 'white', fontWeight: 600, borderRadius: 4 }}>
              Save Configuration
            </button>
          </form>
        </div>
      </div>
    )
  }
    </div >
  )
}
