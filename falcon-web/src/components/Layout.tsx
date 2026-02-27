import { useState, useEffect, useCallback } from 'react'
import { useTheme } from '../contexts/ThemeContext'
import { ServerList, type ServerId } from './ServerList'
import { ChannelList } from './ChannelList'
import { FeedView } from './FeedView'
import { ProfileView } from './ProfileView'
import { NotificationsView } from './NotificationsView'
import { SearchView } from './SearchView'
import { ExploreView } from './ExploreView'
import { ChannelView } from './ChannelView'
import { ProtocolView } from './ProtocolView'
import { CreateServerModal } from './CreateServerModal'
import { CreateChannelModal } from './CreateChannelModal'
import { InviteModal } from './InviteModal'
import type { AtpSession } from '../lib/atp'
import type { View } from './ChannelList'
import { backendApi, type ServerSummary } from '../lib/backendApi'

type Props = {
  session: AtpSession
  onLogout: () => void
}

const VIEW_TITLES: Record<View, string> = {
  feed: 'Home feed',
  dms: 'Messages',
  explore: 'Explore',
  notifications: 'Notifications',
  search: 'Search',
  profile: 'Profile',
  protocol: 'Technical Protocol',
}

export function Layout({ session, onLogout }: Props) {
  const [servers, setServers] = useState<ServerSummary[]>([])
  const [selectedServerId, setSelectedServerId] = useState<ServerId | null>('home')
  const [selectedChannelId, setSelectedChannelId] = useState<number | null>(null)
  const [view, setView] = useState<View>('feed')
  const [profileActor, setProfileActor] = useState<string>(session.handle)
  const [showCreateServer, setShowCreateServer] = useState(false)
  const [showCreateChannel, setShowCreateChannel] = useState(false)
  const [showInvite, setShowInvite] = useState(false)

  const sess = { accessJwt: session.accessJwt, did: session.did, handle: session.handle }

  const loadServers = useCallback(() => {
    backendApi.listServers(sess).then(setServers, () => setServers([]))
  }, [session.did, session.handle])

  useEffect(() => loadServers(), [loadServers])

  useEffect(() => {
    const handler = (e: CustomEvent<{ view: 'profile'; actor: string }>) => {
      if (e.detail?.view === 'profile' && e.detail?.actor) {
        setProfileActor(e.detail.actor)
        setView('profile')
        setSelectedServerId('home')
      }
    }
    window.addEventListener('atdiscord:navigate', handler as EventListener)
    return () => window.removeEventListener('atdiscord:navigate', handler as EventListener)
  }, [])

  useEffect(() => {
    if (view === 'profile') setProfileActor((a) => a || session.handle)
  }, [view, session.handle])

  const { theme, setTheme } = useTheme()
  const toggleTheme = useCallback(() => setTheme(theme === 'dark' ? 'light' : 'dark'), [theme, setTheme])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.shiftKey && e.key === 'L') {
        e.preventDefault()
        toggleTheme()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [toggleTheme])

  const currentServer = typeof selectedServerId === 'number' ? servers.find((s) => s.id === selectedServerId) : null
  const channels = currentServer?.channels ?? []

  const handleCreateServer = useCallback(
    async (name: string) => {
      const res = await backendApi.createServer(sess, name)
      await loadServers()
      setSelectedServerId(res.id)
      setSelectedChannelId(res.channelId)
    },
    [sess, loadServers]
  )

  const handleCreateChannel = useCallback(
    async (name: string) => {
      if (typeof selectedServerId !== 'number') return
      const ch = await backendApi.createChannel(selectedServerId, sess, name)
      await loadServers()
      setSelectedChannelId(ch.id)
    },
    [selectedServerId, sess, loadServers]
  )

  const headerTitle =
    selectedServerId === 'home'
      ? VIEW_TITLES[view]
      : currentServer && selectedChannelId
        ? `# ${channels.find((c) => c.id === selectedChannelId)?.name ?? 'channel'}`
        : currentServer
          ? currentServer.name
          : 'ATDiscord'

  const handleInvite = useCallback(
    async (handle: string) => {
      if (typeof selectedServerId !== 'number') return
      await backendApi.inviteToServer(selectedServerId, sess, handle)
    },
    [selectedServerId, sess]
  )

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <ServerList
        servers={servers}
        selectedId={selectedServerId}
        onSelect={(id) => {
          setSelectedServerId(id)
          if (id === 'home') {
            setSelectedChannelId(null)
          } else {
            const s = servers.find((x) => x.id === (id as number))
            setSelectedChannelId(s?.channels?.[0]?.id ?? null)
          }
        }}
        onCreateServer={() => setShowCreateServer(true)}
        session={session}
        onLogout={onLogout}
      />

      {selectedServerId === 'home' ? (
        <ChannelList mode="home" view={view} onViewChange={setView} />
      ) : currentServer ? (
        <ChannelList
          mode="server"
          serverName={currentServer.name}
          channels={channels}
          selectedChannelId={selectedChannelId}
          onSelectChannel={setSelectedChannelId}
          onCreateChannel={() => setShowCreateChannel(true)}
        />
      ) : (
        <div style={{ width: 240, background: 'var(--bg-secondary)', borderRight: '1px solid var(--border)' }} />
      )}

      <main
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          minWidth: 0,
          background: 'var(--bg-primary)',
        }}
      >
        <header
          style={{
            height: 48,
            borderBottom: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 16px',
            gap: 8,
          }}
        >
          <span style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>{headerTitle}</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {typeof selectedServerId === 'number' && currentServer && (
              <button
                type="button"
                onClick={() => setShowInvite(true)}
                style={{ padding: '4px 10px', background: 'var(--bg-elevated)', color: 'var(--text-secondary)', fontSize: 14 }}
              >
                Invite
              </button>
            )}
            <button
              type="button"
              onClick={toggleTheme}
              title="Toggle theme (Ctrl+Shift+L)"
              style={{ padding: '4px 10px', background: 'var(--bg-elevated)', color: 'var(--text-secondary)', fontSize: 14 }}
            >
              {theme === 'dark' ? '☀️ Light' : '🌙 Dark'}
            </button>
          </div>
        </header>
        <div style={{ flex: 1, overflow: 'auto' }}>
          {selectedServerId === 'home' && (
            <>
              {view === 'feed' && <FeedView />}
              {view === 'dms' && (
                <div style={{ padding: 24, color: 'var(--text-muted)' }}>DMs on AT Protocol — coming soon</div>
              )}
              {view === 'explore' && <ExploreView />}
              {view === 'notifications' && <NotificationsView />}
              {view === 'search' && <SearchView />}
              {view === 'profile' && <ProfileView actor={profileActor} meDid={session.did} />}
              {view === 'protocol' && <ProtocolView />}
            </>
          )}
          {typeof selectedServerId === 'number' && selectedChannelId && (
            <ChannelView
              channelId={selectedChannelId}
              channelName={channels.find((c) => c.id === selectedChannelId)?.name ?? ''}
              session={session}
            />
          )}
          {typeof selectedServerId === 'number' && !selectedChannelId && channels.length === 0 && (
            <div style={{ padding: 24, color: 'var(--text-muted)' }}>Create a channel to get started.</div>
          )}
          {typeof selectedServerId === 'number' && !selectedChannelId && channels.length > 0 && (
            <div style={{ padding: 24, color: 'var(--text-muted)' }}>Select a channel.</div>
          )}
        </div>
      </main>

      {showCreateServer && (
        <CreateServerModal
          onClose={() => setShowCreateServer(false)}
          onCreate={handleCreateServer}
        />
      )}
      {showCreateChannel && currentServer && (
        <CreateChannelModal
          serverName={currentServer.name}
          onClose={() => setShowCreateChannel(false)}
          onCreate={handleCreateChannel}
        />
      )}
      {showInvite && currentServer && (
        <InviteModal
          serverName={currentServer.name}
          onClose={() => setShowInvite(false)}
          onInvite={handleInvite}
        />
      )}
    </div>
  )
}

