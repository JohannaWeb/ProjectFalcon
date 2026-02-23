import { useState } from 'react'
import { getAtpAgent } from '../lib/atp'
import type { AppBskyActorDefs } from '@atproto/api'

type Profile = AppBskyActorDefs.ProfileViewBasic

export function SearchView() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Profile[]>([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const agent = getAtpAgent()

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const q = query.trim()
    if (!q) return
    setLoading(true)
    setSearched(true)
    agent.searchActors({ q, limit: 30 }).then(
      (res) => {
        setResults(res.data.actors ?? [])
      },
      () => setResults([])
    ).finally(() => setLoading(false))
  }

  return (
    <div style={{ padding: 16 }}>
      <form onSubmit={handleSearch} style={{ marginBottom: 16 }}>
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search users by handle or name…"
          style={{
            width: '100%',
            padding: 12,
            background: 'var(--bg-secondary)',
            border: '1px solid var(--border)',
            borderRadius: 8,
            color: 'var(--text-primary)',
          }}
        />
        <button
          type="submit"
          disabled={loading}
          style={{ marginTop: 8, padding: '8px 16px', background: 'var(--accent)', color: 'white', fontWeight: 600 }}
        >
          {loading ? 'Searching…' : 'Search'}
        </button>
      </form>
      {searched && !loading && results.length === 0 && (
        <p style={{ color: 'var(--text-muted)' }}>No users found.</p>
      )}
      {results.map((actor) => (
        <a
          key={actor.did}
          href="#"
          onClick={(e) => { e.preventDefault(); window.dispatchEvent(new CustomEvent('falcon:navigate', { detail: { view: 'profile', actor: actor.handle } })) }}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            padding: 12,
            marginBottom: 8,
            background: 'var(--bg-secondary)',
            borderRadius: 8,
            border: '1px solid var(--border)',
            color: 'inherit',
            textDecoration: 'none',
          }}
        >
          {actor.avatar ? (
            <img src={actor.avatar} alt="" style={{ width: 48, height: 48, borderRadius: 24, objectFit: 'cover' }} />
          ) : (
            <div style={{ width: 48, height: 48, borderRadius: 24, background: 'var(--bg-elevated)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>👤</div>
          )}
          <div>
            <strong>{actor.displayName ?? actor.handle}</strong>
            <br />
            <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>@{actor.handle}</span>
          </div>
        </a>
      ))}
    </div>
  )
}
