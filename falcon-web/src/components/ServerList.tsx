import type { AtpSession } from '../lib/atp'
import type { ServerSummary } from '../lib/backendApi'

export type ServerId = 'home' | number

type Props = {
  servers: ServerSummary[]
  selectedId: ServerId | null
  onSelect: (id: ServerId) => void
  onCreateServer: () => void
  session: AtpSession
  onLogout: () => void
}

export function ServerList({ servers, selectedId, onSelect, onCreateServer, session, onLogout }: Props) {
  return (
    <div
      style={{
        width: 72,
        background: 'var(--bg-tertiary)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        paddingTop: 12,
        gap: 8,
      }}
    >
      <button
        type="button"
        onClick={() => onSelect('home')}
        title="Home"
        style={{
          width: 48,
          height: 48,
          borderRadius: 24,
          background: selectedId === 'home' ? 'var(--accent)' : 'var(--bg-secondary)',
          color: 'var(--text-primary)',
          fontSize: 20,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        🏠
      </button>
      <div style={{ width: 32, height: 1, background: 'var(--border)' }} />
      {servers.map((s) => {
        const id = s.id as number
        const isSelected = selectedId === id
        return (
          <button
            key={s.id}
            type="button"
            onClick={() => onSelect(id)}
            title={s.name}
            style={{
              width: 48,
              height: 48,
              borderRadius: 24,
              background: isSelected ? 'var(--accent)' : 'var(--bg-secondary)',
              color: 'var(--text-primary)',
              fontSize: 18,
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {s.name.slice(0, 1).toUpperCase()}
          </button>
        )
      })}
      <button
        type="button"
        onClick={onCreateServer}
        title="Add Server"
        style={{
          width: 48,
          height: 48,
          borderRadius: 24,
          background: 'var(--bg-secondary)',
          color: 'var(--text-secondary)',
          fontSize: 22,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        +
      </button>
      <div style={{ flex: 1, minHeight: 16 }} />
      <button
        type="button"
        onClick={onLogout}
        title={`${session.handle} — Log out`}
        style={{
          width: 48,
          height: 48,
          borderRadius: 24,
          background: 'var(--bg-secondary)',
          color: 'var(--text-primary)',
          fontSize: 18,
        }}
      >
        👤
      </button>
    </div>
  )
}
