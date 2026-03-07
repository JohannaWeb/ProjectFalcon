import React, { useState } from 'react'

export function JiraSivCard() {
    const [loading, setLoading] = useState(false)

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        setLoading(true)
        const formData = new FormData(e.currentTarget)
        const token = formData.get('token')
        const host = formData.get('host')
        const project = formData.get('project')

        try {
            const res = await fetch('/api/siv/configs', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                body: JSON.stringify({ vesselType: 'jira', token, config: { host, project } })
            })
            if (res.ok) alert('Jira Link Established.')
            else alert('Failed to link Jira.')
        } catch (err) {
            alert('Error linking Jira.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div style={{ background: 'var(--bg-elevated)', padding: 20, borderRadius: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <span style={{ color: '#0052cc', fontWeight: 'bold' }}>SIV-03</span>
                <span style={{ fontWeight: 600 }}>Jira</span>
            </div>
            <form onSubmit={handleSubmit}>
                <div style={{ marginBottom: 12 }}>
                    <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>API Token</label>
                    <input
                        name="token"
                        type="password"
                        required
                        style={{ width: '100%', padding: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }}
                    />
                </div>
                <div style={{ marginBottom: 12 }}>
                    <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>Atlassian Host</label>
                    <input
                        name="host"
                        placeholder="your-domain.atlassian.net"
                        required
                        style={{ width: '100%', padding: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }}
                    />
                </div>
                <div style={{ marginBottom: 16 }}>
                    <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>Project Key</label>
                    <input
                        name="project"
                        placeholder="e.g. PROJ"
                        required
                        style={{ width: '100%', padding: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }}
                    />
                </div>
                <button
                    type="submit"
                    disabled={loading}
                    style={{ width: '100%', padding: '8px 16px', background: '#0052cc', color: 'white', fontWeight: 600, borderRadius: 4, opacity: loading ? 0.7 : 1, cursor: loading ? 'not-allowed' : 'pointer' }}
                >
                    {loading ? 'Linking...' : 'Link Jira'}
                </button>
            </form>
        </div>
    )
}
