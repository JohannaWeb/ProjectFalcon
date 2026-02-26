import React from 'react';

interface GitHubEvent {
    id: string;
    type: string;
    actor: { login: string };
    repo: { name: string };
    created_at: string;
    payload: any;
}

export const GitHubSiv: React.FC<{ events: GitHubEvent[] }> = ({ events }) => {
    // Internal loading is no longer needed as we fetch aggregate data
    const loading = false;

    // Data is now passed from parent

    if (loading) return <div className="p-4 text-gray-500 animate-pulse">Sovereign Link initializing...</div>;
    if (events.length === 0) return <div className="p-4 text-gray-500 italic">No recent sovereign activity detected.</div>;

    return (
        <div className="flex flex-col gap-2 p-2 border-t border-gray-800">
            <div className="px-2 py-1 flex items-center gap-2">
                <span className="text-blue-400 font-bold">SIV-01</span>
                <span className="text-[10px] text-gray-500 uppercase tracking-widest font-semibold text-center italic">GitHub</span>
            </div>
            {events.map(event => (
                <div key={event.id} className="p-3 bg-gray-800/30 rounded-lg hover:bg-gray-800/50 transition-colors border border-transparent hover:border-blue-500/30 group">
                    <div className="flex items-center justify-between mb-1">
                        <span className="text-xs font-medium text-blue-300">{event.type.replace('Event', '')}</span>
                        <span className="text-[10px] text-gray-500">{new Date(event.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    </div>
                    <div className="text-sm text-gray-200">
                        <span className="font-semibold text-white group-hover:text-blue-200">{event.actor.login}</span>
                        <span className="mx-1 text-gray-400">at</span>
                        <span className="text-xs text-gray-300">{event.repo.name.split('/')[1]}</span>
                    </div>
                </div>
            ))}
        </div>
    );
};
