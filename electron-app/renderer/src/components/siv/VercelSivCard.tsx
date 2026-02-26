import React, { useState } from 'react'

export function VercelSivCard() {
    const [loading, setLoading] = useState(false)

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        setLoading(true)
        const formData = new FormData(e.currentTarget)
        const token = formData.get('token')
        const projectId = formData.get('projectId')
        const teamId = formData.get('teamId')

        try {
            const res = await fetch('/api/siv/configs', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                body: JSON.stringify({ vesselType: 'vercel', token, config: { projectId, teamId } })
            })
            if (res.ok) alert('Vercel Link Established.')
            else alert('Failed to link Vercel.')
        } catch (err) {
            alert('Error linking Vercel.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div style={{ background: 'var(--bg-elevated)', padding: 20, borderRadius: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <span style={{ color: '#fff', fontWeight: 'bold' }}>SIV-04</span>
                <span style={{ fontWeight: 600 }}>Vercel</span>
            </div>
            <form onSubmit={handleSubmit}>
                <div style={{ marginBottom: 12 }}>
                    <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>Vercel Token</label>
                    <input
                        name="token"
                        type="password"
                        required
                        style={{ width: '100%', padding: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }}
                    />
                </div>
                <div style={{ marginBottom: 12 }}>
                    <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>Project ID</label>
                    <input
                        name="projectId"
                        placeholder="prj_..."
                        required
                        style={{ width: '100%', padding: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }}
                    />
                </div>
                <div style={{ marginBottom: 16 }}>
                    <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>Team ID (Optional)</label>
                    <input
                        name="teamId"
                        placeholder="team_..."
                        style={{ width: '100%', padding: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text-primary)' }}
                    />
                </div>
                <button
                    type="submit"
                    disabled={loading}
                    style={{ width: '100%', padding: '8px 16px', background: '#fff', color: '#000', fontWeight: 600, borderRadius: 4, opacity: loading ? 0.7 : 1, cursor: loading ? 'not-allowed' : 'pointer' }}
                >
                    {loading ? 'Linking...' : 'Link Vercel'}
                </button>
            </form>
        </div>
    )
}
