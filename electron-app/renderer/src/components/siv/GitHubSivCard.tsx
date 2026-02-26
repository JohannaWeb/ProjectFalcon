import React, { useState } from 'react'

type ToastState = { message: string; type: 'success' | 'error' } | null

export function GitHubSivCard() {
    const [loading, setLoading] = useState(false)
    const [toast, setToast] = useState<ToastState>(null)

    const showToast = (message: string, type: 'success' | 'error') => {
        setToast({ message, type })
        setTimeout(() => setToast(null), 3500)
    }

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        setLoading(true)
        const formData = new FormData(e.currentTarget)
        const token = formData.get('token')
        const repo = formData.get('repo')

        try {
            const res = await fetch('/api/siv/configs', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                body: JSON.stringify({ vesselType: 'github', token, config: { repo } })
            })
            if (res.ok) showToast('GitHub linked successfully.', 'success')
            else showToast('Failed to link GitHub. Check your token and try again.', 'error')
        } catch {
            showToast('Network error — could not reach the server.', 'error')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div style={{ background: 'var(--bg-elevated)', padding: 20, borderRadius: 12, position: 'relative' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <span style={{ color: 'var(--accent)', fontWeight: 'bold' }}>SIV-01</span>
                <span style={{ fontWeight: 600 }}>GitHub</span>
            </div>

            {toast && (
                <div style={{
                    padding: '8px 12px',
                    marginBottom: 12,
                    borderRadius: 6,
                    fontSize: 13,
                    background: toast.type === 'success' ? 'var(--success, #1a3a2a)' : 'var(--error, #3a1a1a)',
                    color: toast.type === 'success' ? 'var(--success-text, #4ade80)' : 'var(--error-text, #f87171)',
                    border: `1px solid ${toast.type === 'success' ? 'var(--success-border, #166534)' : 'var(--error-border, #7f1d1d)'}`,
                    transition: 'opacity 0.2s ease'
                }}>
                    {toast.message}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <div style={{ marginBottom: 12 }}>
                    <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>Access Token</label>
                    <input
                        name="token"
                        type="password"
                        required
                        style={{ width: '100%', padding: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }}
                    />
                </div>
                <div style={{ marginBottom: 16 }}>
                    <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>Repository (owner/repo)</label>
                    <input
                        name="repo"
                        placeholder="e.g. facebook/react"
                        required
                        style={{ width: '100%', padding: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }}
                    />
                </div>
                <button
                    type="submit"
                    disabled={loading}
                    style={{ width: '100%', padding: '8px 16px', background: 'var(--accent)', color: 'white', fontWeight: 600, borderRadius: 4, opacity: loading ? 0.7 : 1, cursor: loading ? 'not-allowed' : 'pointer' }}
                >
                    {loading ? 'Linking...' : 'Link GitHub'}
                </button>
            </form>
        </div>
    )
}
