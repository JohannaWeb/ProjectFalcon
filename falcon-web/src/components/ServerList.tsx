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
  const initials = (name: string) => name.slice(0, 2).toUpperCase()

  return (
    <div
      style={{
        width: 60,
        background: 'var(--bg-tertiary)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        padding: '12px 0',
        gap: 6,
        borderRight: '1px solid var(--border)',
        flexShrink: 0,
      }}
    >
      {/* Home */}
      <button
        type="button"
        className={`workspace-btn${selectedId === 'home' ? ' active' : ''}`}
        onClick={() => onSelect('home')}
        title="Home"
        style={{ fontSize: 16, fontWeight: 700, marginBottom: 2 }}
      >
        F
      </button>

      <div style={{ width: 24, height: 1, background: 'var(--border)', margin: '2px 0' }} />

      {/* Workspaces */}
      {servers.map((s) => {
        const id = s.id as number
        return (
          <button
            key={s.id}
            type="button"
            className={`workspace-btn${selectedId === id ? ' active' : ''}`}
            onClick={() => onSelect(id)}
            title={s.name}
          >
            {initials(s.name)}
          </button>
        )
      })}

      {/* New workspace */}
      <button
        type="button"
        onClick={onCreateServer}
        title="New workspace"
        style={{
          width: 36,
          height: 36,
          borderRadius: 10,
          background: 'transparent',
          border: '1.5px dashed var(--border)',
          color: 'var(--text-muted)',
          fontSize: 18,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          transition: 'border-color 0.12s, color 0.12s',
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.borderColor = 'var(--accent)'
          e.currentTarget.style.color = 'var(--accent)'
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.borderColor = 'var(--border)'
          e.currentTarget.style.color = 'var(--text-muted)'
        }}
      >
        +
      </button>

      <div style={{ flex: 1 }} />

      {/* User avatar */}
      <button
        type="button"
        className="workspace-btn"
        onClick={onLogout}
        title={`${session.handle} — Sign out`}
        style={{ fontSize: 11 }}
      >
        {initials(session.handle)}
      </button>
    </div>
  )
}
