import React, { useState } from 'react'

export function GitHubSivCard() {
    const [loading, setLoading] = useState(false)

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
            if (res.ok) alert('GitHub Link Established.')
            else alert('Failed to link GitHub.')
        } catch (err) {
            alert('Error linking GitHub.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div style={{ background: 'var(--bg-elevated)', padding: 20, borderRadius: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <span style={{ color: 'var(--accent)', fontWeight: 'bold' }}>SIV-01</span>
                <span style={{ fontWeight: 600 }}>GitHub</span>
            </div>
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
