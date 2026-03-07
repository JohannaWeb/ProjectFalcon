import React from 'react';

interface VercelDeployment {
    uid: string;
    name: string;
    url: string;
    state: string;
    created: number;
    creator: { username: string };
}

export const VercelSiv: React.FC<{ deployments: VercelDeployment[] }> = ({ deployments }) => {
    // Data is now passed from parent
    const loading = false;

    // Internal fetching removed

    if (loading) return <div className="p-4 text-gray-500 animate-pulse">Checking Vercel deployments...</div>;
    if (deployments.length === 0) return <div className="p-4 text-gray-500 italic">No Vercel activity detected.</div>;

    const getStatusColor = (state: string) => {
        switch (state) {
            case 'READY': return 'text-green-400';
            case 'ERROR': return 'text-red-400';
            case 'BUILDING': return 'text-yellow-400';
            default: return 'text-gray-400';
        }
    };

    return (
        <div className="flex flex-col gap-2 p-2 border-t border-gray-800">
            <div className="px-2 py-1 flex items-center gap-2">
                <span className="text-white font-bold">SIV-04</span>
                <span className="text-[10px] text-gray-500 uppercase tracking-widest font-semibold text-center italic">Vercel</span>
            </div>
            {deployments.map(dep => (
                <div key={dep.uid} className="p-3 bg-gray-800/30 rounded-lg hover:bg-gray-800/50 transition-colors border border-transparent hover:border-white/30 group">
                    <div className="flex items-center justify-between mb-1">
                        <span className={`text-xs font-medium ${getStatusColor(dep.state)}`}>{dep.state}</span>
                        <span className="text-[10px] text-gray-500">
                            {new Date(dep.created).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                    </div>
                    <div className="text-sm text-gray-200 truncate font-mono bg-black/20 p-1 rounded mb-1">
                        {dep.url}
                    </div>
                    <div className="text-[10px] text-gray-400">
                        by <span className="text-gray-200">{dep.creator.username}</span>
                    </div>
                </div>
            ))}
        </div>
    );
};
