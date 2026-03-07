import React, { useEffect, useState } from 'react';

interface Props {
    targetDid: string;
}

export const TrustIndicator: React.FC<Props> = ({ targetDid }) => {
    const [score, setScore] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchScore = async () => {
            try {
                const res = await fetch(`/api/trust/score/${targetDid}`, {
                    headers: {
                        'Authorization': `Bearer ${localStorage.getItem('token')}`
                    }
                });
                if (res.ok) {
                    const data = await res.json();
                    setScore(data.score);
                }
            } catch (e) {
                console.error('Failed to fetch trust score', e);
            } finally {
                setLoading(false);
            }
        };

        if (targetDid) fetchScore();
    }, [targetDid]);

    if (loading) return <div className="w-2 h-2 rounded-full bg-gray-600 animate-pulse" title="Calculating Trust..." />;

    // Scale: -1.0 to 1.0
    // Positive values: Green
    // Zero: Gray (Neutral)
    // Negative values: Red/Orange

    let colorClass = 'bg-gray-500';
    let label = 'Neutral';

    if (score! > 0.7) {
        colorClass = 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]';
        label = 'Highly Trusted';
    } else if (score! > 0.3) {
        colorClass = 'bg-green-400';
        label = 'Trusted';
    } else if (score! < -0.7) {
        colorClass = 'bg-red-600 shadow-[0_0_8px_rgba(220,38,38,0.6)]';
        label = 'Distrusted / Blocked';
    } else if (score! < -0.3) {
        colorClass = 'bg-orange-500';
        label = 'Caution';
    }

    return (
        <div className="flex items-center gap-1.5 group relative cursor-help">
            <div className={`w-2.5 h-2.5 rounded-full ${colorClass} transition-all duration-300`} />
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block z-50">
                <div className="bg-black/90 text-white text-[10px] px-2 py-1 rounded border border-gray-700 whitespace-nowrap backdrop-blur-sm">
                    Sovereign Trust: <span className="font-bold">{label}</span> ({score?.toFixed(2)})
                </div>
            </div>
        </div>
    );
};
