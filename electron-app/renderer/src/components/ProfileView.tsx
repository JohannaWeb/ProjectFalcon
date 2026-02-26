import React, { useEffect, useState } from 'react'
import { getAtpAgent } from '../lib/atp'
import type { AppBskyActorDefs } from '@atproto/api'
import { ProfileHeader } from './ProfileHeader'
import { GitHubSivCard } from './siv/GitHubSivCard'
import { LinearSivCard } from './siv/LinearSivCard'
import { JiraSivCard } from './siv/JiraSivCard'
import { VercelSivCard } from './siv/VercelSivCard'

type Props = { actor: string; meDid?: string }

export function ProfileView({ actor, meDid }: Props) {
  const [profile, setProfile] = useState<AppBskyActorDefs.ProfileViewDetailed | null>(null)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)
  const [following, setFollowing] = useState(false)
  const [followUri, setFollowUri] = useState<string | null>(null)
  const agent = getAtpAgent()

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
  }, [actor, agent])

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

  const isMe = meDid === profile.did

  return (
    <div style={{ padding: 24 }}>
      <ProfileHeader
        profile={profile}
        isMe={isMe}
        following={following}
        handleFollow={handleFollow}
      />

      {isMe && (
        <div style={{ marginTop: 40, borderTop: '1px solid var(--border)', paddingTop: 24 }}>
          <h2 style={{ fontSize: 20, marginBottom: 16 }}>Sovereign Integration Vessels (SIV)</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 20 }}>
            <GitHubSivCard />
            <LinearSivCard />
            <JiraSivCard />
            <VercelSivCard />
          </div>
        </div>
      )}
    </div>
  )
}
