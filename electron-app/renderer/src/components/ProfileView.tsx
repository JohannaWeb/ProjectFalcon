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
          <p style={{ color: 'var(--text-muted)', marginBottom: 8 }}@{profile.handle}</p>
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
  )
}
