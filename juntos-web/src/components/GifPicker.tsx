import { useState, useEffect } from 'react'

type Gif = {
    id: string
    title: string
    images: {
        fixed_height_small: {
            url: string
        }
    }
}

type Props = {
    onSelect: (url: string) => void
    onClose: () => void
}

const GIPHY_API_KEY = 'dc6zaTOxFJmzC' // Public Beta Key (Rate limited, for demo purposes)

export function GifPicker({ onSelect, onClose }: Props) {
    const [search, setSearch] = useState('cats')
    const [gifs, setGifs] = useState<Gif[]>([])
    const [loading, setLoading] = useState(false)

    const fetchGifs = async (query: string) => {
        setLoading(true)
        try {
            const resp = await fetch(
                `https://api.giphy.com/v1/gifs/search?api_key=${GIPHY_API_KEY}&q=${encodeURIComponent(query)}&limit=20`
            )
            const json = await resp.json()
            setGifs(json.data || [])
        } catch (err) {
            console.error('Failed to fetch GIFs', err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchGifs(search)
    }, [])

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault()
        fetchGifs(search)
    }

    return (
        <div style={{
            position: 'absolute',
            bottom: '100%',
            left: 0,
            width: 300,
            height: 400,
            background: 'var(--bg-secondary)',
            border: '1px solid var(--border)',
            borderRadius: 8,
            display: 'flex',
            flexDirection: 'column',
            zIndex: 1000,
            boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
            marginBottom: 8
        }}>
            <div style={{ padding: 12, borderBottom: '1px solid var(--border)', display: 'flex', gap: 8 }}>
                <form onSubmit={handleSearch} style={{ flex: 1 }}>
                    <input
                        autoFocus
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        placeholder="Search GIFs..."
                        style={{
                            width: '100%',
                            padding: '6px 10px',
                            background: 'var(--bg-tertiary)',
                            border: '1px solid var(--border)',
                            borderRadius: 4,
                            color: 'var(--text-primary)'
                        }}
                    />
                </form>
                <button onClick={onClose} style={{ color: 'var(--text-muted)', fontSize: 20 }}>×</button>
            </div>

            <div style={{ flex: 1, overflowY: 'auto', padding: 8, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                {loading && <p style={{ gridColumn: 'span 2', textAlign: 'center', color: 'var(--text-muted)' }}>Fetching GIFs...</p>}
                {gifs.map((gif) => (
                    <img
                        key={gif.id}
                        src={gif.images.fixed_height_small.url}
                        alt={gif.title}
                        onClick={() => onSelect(gif.images.fixed_height_small.url)}
                        style={{
                            width: '100%',
                            height: 100,
                            objectFit: 'cover',
                            borderRadius: 4,
                            cursor: 'pointer',
                            border: '2px solid transparent'
                        }}
                        onMouseOver={(e) => e.currentTarget.style.borderColor = 'var(--accent)'}
                        onMouseOut={(e) => e.currentTarget.style.borderColor = 'transparent'}
                    />
                ))}
            </div>

            <div style={{ padding: 8, textAlign: 'center', fontSize: 10, color: 'var(--text-muted)' }}>
                Powered by GIPHY
            </div>
        </div>
    )
}
