import { useEffect, useState, useRef } from 'react'
import { chatApi } from '../lib/atp'
import { type Session } from '../lib/backendApi'
import { Avatar } from './ChannelView'

type Props = {
    convoId: string
    session: Session
    onBack: () => void
}

export function ConvoThread({ convoId, session, onBack }: Props) {
    const [messages, setMessages] = useState<any[]>([])
    const [convo, setConvo] = useState<any | null>(null)
    const [input, setInput] = useState('')
    const [loading, setLoading] = useState(true)
    const scrollRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        setLoading(true)
        Promise.all([
            chatApi.getConvo(convoId),
            chatApi.getMessages(convoId, 50)
        ]).then(([cRes, mRes]) => {
            if (cRes.convo) setConvo(cRes.convo as any)
            if (mRes.messages) {
                // messages are returned from newest to oldest usually, reverse them for chat UI
                const msgs = mRes.messages.slice().reverse()
                setMessages(msgs)
            }
        }).catch((err) => {
            console.error('Failed to load convo thread', err)
        }).finally(() => setLoading(false))
    }, [convoId, session.accessJwt])

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight
        }
    }, [messages])

    const handleSend = async (e: React.FormEvent) => {
        e.preventDefault()
        const content = input.trim()
        if (!content) return

        // Optimistic UI update could go here
        setInput('')
        try {
            await chatApi.sendMessage(convoId, content)
            chatApi.getMessages(convoId, 50).then(mRes => {
                if (mRes.messages) setMessages(mRes.messages.slice().reverse())
            })
        } catch (err) {
            console.error('Failed to send', err)
            setInput(content) // restore input
        }
    }

    const otherParticipants = convo?.members.filter((p: any) => p.did !== session.did) || []
    const title = otherParticipants.length > 0 ? otherParticipants[0].handle : 'Note to self'
    const displayAvatarHash = otherParticipants.length > 0 ? otherParticipants[0].handle : session.handle

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            {/* Header */}
            <div style={{
                padding: '12px 20px',
                borderBottom: '1px solid var(--border)',
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                background: 'var(--bg-surface)'
            }}>
                <button
                    onClick={onBack}
                    style={{
                        background: 'none',
                        border: 'none',
                        color: 'var(--text-muted)',
                        cursor: 'pointer',
                        padding: 4,
                        display: 'flex',
                        alignItems: 'center'
                    }}
                >
                    ←
                </button>
                <Avatar handle={displayAvatarHash} size={32} />
                <div>
                    <div style={{ fontWeight: 600, fontSize: 14 }}>{title}</div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Direct Message</div>
                </div>
            </div>

            {/* Messages */}
            <div
                ref={scrollRef}
                style={{ flex: 1, overflow: 'auto', padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 16 }}
            >
                {loading && <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>Loading messages...</p>}
                {messages.map((m: any) => {
                    if (m.$type !== 'chat.bsky.convo.defs#messageView') {
                        // skip non-message items (e.g. deleted messages)
                        return null
                    }
                    const isMe = m.sender?.did === session.did
                    return (
                        <div
                            key={m.id}
                            style={{
                                display: 'flex',
                                flexDirection: isMe ? 'row-reverse' : 'row',
                                gap: 12,
                                alignItems: 'flex-start'
                            }}
                        >
                            {!isMe && <Avatar handle={m.sender?.handle || m.sender?.did || ''} size={32} />}
                            <div style={{
                                maxWidth: '70%',
                                padding: '10px 14px',
                                borderRadius: 12,
                                background: isMe ? 'var(--accent)' : 'var(--bg-elevated)',
                                color: isMe ? '#fff' : 'var(--text-primary)',
                                fontSize: 14,
                                lineHeight: 1.5,
                                border: isMe ? 'none' : '1px solid var(--border)'
                            }}>
                                {m.text}
                            </div>
                        </div>
                    )
                })}
            </div>

            {/* Compose */}
            <div style={{ padding: '12px 20px 16px', borderTop: '1px solid var(--border)' }}>
                <form onSubmit={handleSend} style={{ display: 'flex', gap: 8 }}>
                    <input
                        value={input}
                        onChange={e => setInput(e.target.value)}
                        placeholder="Type a message..."
                        style={{
                            flex: 1,
                            padding: '10px 14px',
                            borderRadius: 8,
                            background: 'var(--bg-elevated)',
                            border: '1px solid var(--border)',
                            color: 'var(--text-primary)',
                            fontSize: 14
                        }}
                    />
                    <button
                        type="submit"
                        disabled={!input.trim()}
                        style={{
                            padding: '8px 16px',
                            background: 'var(--accent)',
                            color: '#fff',
                            borderRadius: 8,
                            fontWeight: 600,
                            fontSize: 14,
                            border: 'none',
                            cursor: 'pointer'
                        }}
                    >
                        Send
                    </button>
                </form>
            </div>
        </div>
    )
}
