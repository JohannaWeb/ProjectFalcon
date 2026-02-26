import React from 'react'
import type { AppBskyActorDefs } from '@atproto/api'
import { TrustIndicator } from './TrustIndicator'

type Props = {
    profile: AppBskyActorDefs.ProfileViewDetailed
    isMe: boolean
    following: boolean
    handleFollow: () => void
}

export function ProfileHeader({ profile, isMe, following, handleFollow }: Props) {
    return (
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
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 4 }}>
                    <h1 style={{ fontSize: 24, margin: 0 }}>{profile.displayName ?? profile.handle}</h1>
                    <TrustIndicator targetDid={profile.did} />
                </div>
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
                            cursor: 'pointer',
                            border: 'none',
                            borderRadius: 4,
                        }}
                    >
                        {following ? 'Unfollow' : 'Follow'}
                    </button>
                )}
            </div>
        </div>
    )
}
