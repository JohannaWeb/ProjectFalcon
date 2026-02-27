// type Props = ... (imports handled)

export type View = 'feed' | 'dms' | 'explore' | 'notifications' | 'search' | 'profile' | 'protocol'

const HOME_CHANNELS: { id: View; name: string; icon: string }[] = [
  { id: 'feed', name: 'Home feed', icon: '📜' },
  { id: 'notifications', name: 'Notifications', icon: '🔔' },
  { id: 'search', name: 'Search', icon: '🔍' },
  { id: 'explore', name: 'Explore', icon: '🌐' },
  { id: 'dms', name: 'Messages', icon: '✉️' },
  { id: 'profile', name: 'My profile', icon: '👤' },
  { id: 'protocol', name: 'Protocol', icon: '📄' },
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
          width: 240,
          background: 'var(--bg-secondary)',
          display: 'flex',
          flexDirection: 'column',
          borderRight: '1px solid var(--border)',
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
          }}
        >
          Falcon
        </div>
        <nav style={{ padding: 8 }}>
          {HOME_CHANNELS.map((ch) => (
            <button
              key={ch.id}
              type="button"
              onClick={() => onViewChange(ch.id)}
              style={{
                width: '100%',
                padding: '8px 12px',
                textAlign: 'left',
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                background: view === ch.id ? 'var(--bg-elevated)' : 'transparent',
                color: view === ch.id ? 'var(--text-primary)' : 'var(--text-secondary)',
                borderRadius: 4,
                marginBottom: 2,
              }}
            >
              <span>{ch.icon}</span>
              {ch.name}
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
        width: 240,
        background: 'var(--bg-secondary)',
        display: 'flex',
        flexDirection: 'column',
        borderRight: '1px solid var(--border)',
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
        }}
      >
        {serverName}
      </div>
      <nav style={{ padding: 8 }}>
        {channels.map((c) => (
          <button
            key={c.id}
            type="button"
            onClick={() => onSelectChannel(c.id)}
            style={{
              width: '100%',
              padding: '8px 12px',
              textAlign: 'left',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              background: selectedChannelId === c.id ? 'var(--bg-elevated)' : 'transparent',
              color: selectedChannelId === c.id ? 'var(--text-primary)' : 'var(--text-secondary)',
              borderRadius: 4,
              marginBottom: 2,
            }}
          >
            <span style={{ opacity: 0.7 }}>#</span>
            {c.name}
          </button>
        ))}
        <button
          type="button"
          onClick={onCreateChannel}
          style={{
            width: '100%',
            padding: '8px 12px',
            textAlign: 'left',
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            background: 'transparent',
            color: 'var(--text-muted)',
            borderRadius: 4,
            marginTop: 4,
          }}
        >
          + Create Channel
        </button>
      </nav>
    </div>
  )
}
