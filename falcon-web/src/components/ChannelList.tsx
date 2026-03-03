export type View = 'feed' | 'dms' | 'explore' | 'notifications' | 'search' | 'profile' | 'protocol'

const PRIMARY_NAV: { id: View; name: string }[] = [
  { id: 'feed', name: 'Feed' },
  { id: 'explore', name: 'Explore' },
  { id: 'profile', name: 'Profile' },
]

const SECONDARY_NAV: { id: View; name: string }[] = [
  { id: 'notifications', name: 'Notifications' },
  { id: 'search', name: 'Search' },
  { id: 'dms', name: 'Messages' },
  { id: 'protocol', name: 'Protocol' },
]

type Props =
  | {
      mode: 'home'
      view: View
      onViewChange: (v: View) => void
    }
  | {
      mode: 'server'
      serverName: string
      channels: { id: number; name: string }[]
      selectedChannelId: number | null
      onSelectChannel: (id: number) => void
      onCreateChannel: () => void
    }

export function ChannelList(props: Props) {
  if (props.mode === 'home') {
    const { view, onViewChange } = props
    return (
      <div
        style={{
          width: 220,
          background: 'var(--bg-secondary)',
          display: 'flex',
          flexDirection: 'column',
          borderRight: '1px solid var(--border)',
          flexShrink: 0,
        }}
      >
        <div
          style={{
            height: 48,
            padding: '0 16px',
            display: 'flex',
            alignItems: 'center',
            borderBottom: '1px solid var(--border)',
            fontWeight: 600,
            fontSize: 15,
            letterSpacing: '-0.01em',
            flexShrink: 0,
          }}
        >
          Falcon
        </div>
        <nav style={{ padding: '8px 8px', overflowY: 'auto', flex: 1 }}>
          {PRIMARY_NAV.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`nav-item${view === item.id ? ' active' : ''}`}
              onClick={() => onViewChange(item.id)}
            >
              {item.name}
            </button>
          ))}

          <div style={{ height: 1, background: 'var(--border)', margin: '8px 4px' }} />

          {SECONDARY_NAV.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`nav-item${view === item.id ? ' active' : ''}`}
              onClick={() => onViewChange(item.id)}
            >
              {item.name}
            </button>
          ))}
        </nav>
      </div>
    )
  }

  const { serverName, channels, selectedChannelId, onSelectChannel, onCreateChannel } = props
  return (
    <div
      style={{
        width: 220,
        background: 'var(--bg-secondary)',
        display: 'flex',
        flexDirection: 'column',
        borderRight: '1px solid var(--border)',
        flexShrink: 0,
      }}
    >
      <div
        style={{
          height: 48,
          padding: '0 16px',
          display: 'flex',
          alignItems: 'center',
          borderBottom: '1px solid var(--border)',
          fontWeight: 600,
          fontSize: 15,
          letterSpacing: '-0.01em',
          flexShrink: 0,
          overflow: 'hidden',
          whiteSpace: 'nowrap',
          textOverflow: 'ellipsis',
        }}
      >
        {serverName}
      </div>
      <nav style={{ padding: '8px 8px', overflowY: 'auto', flex: 1 }}>
        <div className="section-label">Channels</div>
        {channels.map((c) => (
          <button
            key={c.id}
            type="button"
            className={`nav-item${selectedChannelId === c.id ? ' active' : ''}`}
            onClick={() => onSelectChannel(c.id)}
          >
            <span style={{ color: 'var(--text-muted)', fontWeight: 400, flexShrink: 0 }}>#</span>
            {c.name}
          </button>
        ))}
        <button
          type="button"
          className="nav-item"
          onClick={onCreateChannel}
          style={{ color: 'var(--text-muted)', marginTop: 4 }}
        >
          <span style={{ fontSize: 16, lineHeight: 1 }}>+</span>
          New channel
        </button>
      </nav>
    </div>
  )
}
