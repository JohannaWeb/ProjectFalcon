import React, { useEffect, useState } from 'react'

interface AiFact {
    factType: 'TAG' | 'SUMMARY' | 'HIGHLIGHT' | 'WARNING'
    content: string
    confidence: number | null
    agentDid: string
    createdAt: string
}

interface AiSivCardProps {
    facts?: AiFact[]
    userDid?: string
}

const FACT_STYLES: Record<AiFact['factType'], { bg: string; text: string; label: string }> = {
    TAG: { bg: 'var(--bg-tertiary, #1e2a3a)', text: 'var(--accent, #60a5fa)', label: '' },
    SUMMARY: { bg: 'var(--bg-elevated, #1a2035)', text: 'var(--text-secondary, #94a3b8)', label: '💬' },
    HIGHLIGHT: { bg: 'rgba(34,197,94,0.08)', text: '#4ade80', label: '✨' },
    WARNING: { bg: 'rgba(239,68,68,0.08)', text: '#f87171', label: '⚠️' },
}

export function AiSivCard({ facts = [], userDid }: AiSivCardProps) {
    const [isExpanded, setIsExpanded] = useState(true)

    const tags = facts.filter(f => f.factType === 'TAG')
    const summaries = facts.filter(f => f.factType === 'SUMMARY')
    const highlights = facts.filter(f => f.factType === 'HIGHLIGHT')
    const warnings = facts.filter(f => f.factType === 'WARNING')

    const latestSummary = summaries[0]
    const agentDid = facts[0]?.agentDid

    if (!facts.length) {
        return (
            <div style={{ padding: '12px 16px', background: 'var(--bg-elevated)', borderRadius: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                    <span style={{ color: 'var(--accent)', fontWeight: 'bold', fontSize: 12 }}>AI</span>
                    <span style={{ fontWeight: 600 }}>Sovereign Intelligence</span>
                </div>
                <p style={{ fontSize: 12, color: 'var(--text-muted)', fontStyle: 'italic', margin: 0 }}>
                    No AI insights yet. Configure an AI backend to start analysing the firehose.
                </p>
            </div>
        )
    }

    return (
        <div style={{
            background: 'var(--bg-elevated, #111827)',
            borderRadius: 12,
            border: '1px solid rgba(96,165,250,0.15)',
            overflow: 'hidden',
        }}>
            {/* Header */}
            <div
                onClick={() => setIsExpanded(e => !e)}
                style={{
                    display: 'flex', alignItems: 'center', gap: 10,
                    padding: '10px 14px', cursor: 'pointer',
                    borderBottom: isExpanded ? '1px solid rgba(255,255,255,0.05)' : 'none',
                }}
            >
                <div style={{
                    width: 28, height: 28, borderRadius: '50%',
                    background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 13, flexShrink: 0
                }}>🤖</div>
                <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
                        <span style={{ fontWeight: 700, fontSize: 13, color: 'var(--text-primary, #f1f5f9)' }}>
                            Sovereign AI
                        </span>
                        <span style={{ fontSize: 10, color: 'var(--text-muted, #64748b)', fontFamily: 'monospace' }}>
                            {agentDid?.slice(0, 28)}…
                        </span>
                    </div>
                </div>
                <span style={{ fontSize: 10, color: 'var(--text-muted)', userSelect: 'none' }}>
                    {isExpanded ? '▲' : '▼'}
                </span>
            </div>

            {isExpanded && (
                <div style={{ padding: 14 }}>
                    {/* Warnings — shown first */}
                    {warnings.map((w, i) => (
                        <div key={i} style={{
                            padding: '8px 12px', borderRadius: 8, marginBottom: 8,
                            background: FACT_STYLES.WARNING.bg,
                            border: '1px solid rgba(239,68,68,0.2)',
                        }}>
                            <div style={{ fontSize: 11, fontWeight: 700, color: FACT_STYLES.WARNING.text, marginBottom: 3 }}>
                                ⚠️ Content Warning
                            </div>
                            <div style={{ fontSize: 12, color: '#fca5a5' }}>{w.content}</div>
                        </div>
                    ))}

                    {/* Latest summary */}
                    {latestSummary && (
                        <div style={{
                            padding: '8px 12px', borderRadius: 8, marginBottom: 12,
                            background: 'rgba(99,102,241,0.08)',
                            border: '1px solid rgba(99,102,241,0.15)',
                        }}>
                            <div style={{ fontSize: 10, fontWeight: 600, color: '#818cf8', marginBottom: 4, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                                💬 Channel Summary
                            </div>
                            <div style={{ fontSize: 13, color: 'var(--text-secondary, #94a3b8)', lineHeight: 1.5 }}>
                                {latestSummary.content}
                            </div>
                        </div>
                    )}

                    {/* Tags */}
                    {tags.length > 0 && (
                        <div>
                            <div style={{ fontSize: 10, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 6 }}>
                                Topics
                            </div>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                                {tags.map((t, i) => (
                                    <span key={i} style={{
                                        padding: '3px 10px', borderRadius: 999,
                                        fontSize: 11, fontWeight: 600,
                                        background: 'rgba(96,165,250,0.1)',
                                        color: '#60a5fa',
                                        border: '1px solid rgba(96,165,250,0.2)',
                                    }}>
                                        #{t.content}
                                    </span>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Highlights */}
                    {highlights.map((h, i) => (
                        <div key={i} style={{
                            marginTop: 8, padding: '7px 12px', borderRadius: 8,
                            background: FACT_STYLES.HIGHLIGHT.bg,
                            border: '1px solid rgba(34,197,94,0.2)',
                            fontSize: 12, color: '#86efac'
                        }}>
                            ✨ {h.content}
                        </div>
                    ))}

                    {/* Agent attribution */}
                    <div style={{ marginTop: 10, fontSize: 10, color: 'var(--text-muted)', opacity: 0.6 }}>
                        Signed by <span style={{ fontFamily: 'monospace' }}>{agentDid}</span>
                    </div>
                </div>
            )}
        </div>
    )
}
