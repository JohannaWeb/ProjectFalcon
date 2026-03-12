import { useEffect, useState } from 'react'
import { chatApi } from '../lib/atp'
import { type Session } from '../lib/backendApi'
import { Avatar } from './ChannelView'

type Props = {
    session: Session
    onSelectConvo: (convoId: string) => void
}

export function DMsView({ session, onSelectConvo }: Props) {
    const [convos, setConvos] = useState<any[]>([])
    const [loading, setLoading] = useState(true)
    const [tokenError, setTokenError] = useState(false)
    const [newParticipant, setNewParticipant] = useState('')
    const [newMessage, setNewMessage] = useState('')
    const [sending, setSending] = useState(false)

    const loadConvos = () => {
        setLoading(true)
        chatApi.listConvos()
            .then(res => {
                setConvos(res.convos)
                setTokenError(false)
            })
            .catch((err) => {
                console.error('List convos error:', err)
                if (String(err).includes('Bad token method') || String(err).includes('bad_token')) {
                    setTokenError(true)
                }
                setConvos([])
            })
            .finally(() => setLoading(false))
    }

    useEffect(() => {
        loadConvos()
    }, [session.accessJwt])

    const handleStartConvo = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!newParticipant.trim() || !newMessage.trim()) return
        setSending(true)
        try {
            const convoRes = await chatApi.getConvoForMembers([newParticipant.trim()])
            if (convoRes.convo) {
                await chatApi.sendMessage(convoRes.convo.id, newMessage.trim())
                setNewParticipant('')
                setNewMessage('')
                loadConvos() // refresh list
                onSelectConvo(convoRes.convo.id)
            }
        } catch (err) {
            console.error('Failed to start convo', err)
            alert('Could not start conversation. Ensure you entered a valid DID and they allow DMs from you.')
        } finally {
            setSending(false)
        }
    }

    return (
        <div style={{ padding: 20 }}>
            <h2 style={{ fontSize: 20, fontWeight: 700, marginBottom: 20, color: 'var(--text-primary)' }}>Direct Messages</h2>

            {tokenError && (
                <div style={{ padding: 16, background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.2)', borderRadius: 8, color: '#ef4444', marginBottom: 24 }}>
                    <h3 style={{ fontWeight: 600, marginBottom: 8 }}>Authentication Error</h3>
                    <p style={{ fontSize: 14, lineHeight: 1.5 }}>
                        Bluesky's Direct Messages API requires you to log in with your <strong>main account password</strong>.
                        It looks like you logged in with an App Password, which does not have permission to access DMs.
                        Please log out and log back in with your main password to use this feature.
                    </p>
                </div>
            )}

            <form onSubmit={handleStartConvo} style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
                <input
                    value={newParticipant}
                    onChange={e => setNewParticipant(e.target.value)}
                    placeholder="User DID (e.g. did:plc:...)"
                    style={{ flex: 1, padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg-elevated)', color: 'var(--text-primary)', fontSize: 14 }}
                />
                <input
                    value={newMessage}
                    onChange={e => setNewMessage(e.target.value)}
                    placeholder="Say hello..."
                    style={{ flex: 2, padding: '10px 14px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--bg-elevated)', color: 'var(--text-primary)', fontSize: 14 }}
                />
                <button
                    type="submit"
                    disabled={!newParticipant.trim() || !newMessage.trim() || sending}
                    style={{ padding: '10px 20px', borderRadius: 8, background: 'var(--accent)', color: '#fff', fontWeight: 600, border: 'none', cursor: 'pointer', fontSize: 14 }}
                >
                    {sending ? 'Sending...' : 'Start'}
                </button>
            </form>

            {loading && <p style={{ color: 'var(--text-muted)' }}>Loading conversations...</p>}

            {!loading && convos.length === 0 && (
                <div style={{ textAlign: 'center', paddingTop: 40 }}>
                    <p style={{ color: 'var(--text-muted)' }}>No conversations yet.</p>
                </div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                {convos.map(convo => {
                    const otherParticipants = convo.members.filter((p: any) => p.did !== session.did)
                    const displayHandle = otherParticipants.length > 0 ? otherParticipants[0].handle : 'Note to self'

                    let lastMessageText = 'Click to chat'
                    if (convo.lastMessage) {
                        const lm = convo.lastMessage as any
                        if (lm.text) lastMessageText = lm.text
                        else lastMessageText = 'New message'
                    }

                    return (
                        <button
                            key={convo.id}
                            onClick={() => onSelectConvo(convo.id)}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 12,
                                padding: '10px 12px',
                                borderRadius: 8,
                                background: 'var(--bg-elevated)',
                                border: '1px solid var(--border)',
                                cursor: 'pointer',
                                textAlign: 'left',
                                transition: 'background 0.2s',
                            }}
                            onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--bg-hover)')}
                            onMouseLeave={(e) => (e.currentTarget.style.background = 'var(--bg-elevated)')}
                        >
                            <Avatar handle={displayHandle} size={40} />
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: 14 }}>
                                    {displayHandle}
                                </div>
                                <div style={{ fontSize: 12, color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                    {lastMessageText}
                                </div>
                            </div>
                        </button>
                    )
                })}
            </div>
        </div>
    )
}
