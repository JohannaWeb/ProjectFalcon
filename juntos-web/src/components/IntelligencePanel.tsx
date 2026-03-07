import React, { useEffect, useState } from 'react';
import { GitHubSiv } from './GitHubSiv';
import { LinearSiv } from './LinearSiv';
import { JiraSiv } from './JiraSiv';
import { VercelSiv } from './VercelSiv';

export const IntelligencePanel: React.FC = () => {
    const [intelligence, setIntelligence] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchIntelligence = async () => {
            try {
                const res = await fetch('/api/siv/intelligence', {
                    headers: {
                        'Authorization': `Bearer ${localStorage.getItem('token')}`
                    }
                });
                if (res.ok) {
                    setIntelligence(await res.json());
                }
            } catch (e) {
                console.error('Failed to fetch intelligence', e);
            } finally {
                setLoading(false);
            }
        };
        fetchIntelligence();
        const interval = setInterval(fetchIntelligence, 30000); // Faster refresh for real-time feel
        return () => clearInterval(interval);
    }, []);

    if (loading && !intelligence) return <div className="p-4 text-gray-500 animate-pulse">Syncing Sovereignty...</div>;
    if (!intelligence || !intelligence.activities || Object.keys(intelligence.activities).length === 0) return null;

    return (
        <div className="siv-panel bg-gray-900/50 border-l border-gray-800 h-full overflow-y-auto w-80">
            <div className="p-4 border-b border-gray-800 flex items-center justify-between">
                <span className="text-xs text-gray-400 uppercase tracking-widest font-bold">Intelligence Feed</span>
                <span className="text-[10px] bg-blue-500/20 text-blue-400 px-2 py-0.5 rounded-full border border-blue-500/30">Active Sovereignty</span>
            </div>
            <div className="flex flex-col">
                {intelligence && intelligence.activities && Object.keys(intelligence.activities).map(type => {
                    const data = intelligence.activities[type];
                    switch (type) {
                        case 'github': return <GitHubSiv key="github" events={data} />;
                        case 'linear': return <LinearSiv key="linear" issues={data} />;
                        case 'jira': return <JiraSiv key="jira" issues={data} />;
                        case 'vercel': return <VercelSiv key="vercel" deployments={data} />;
                        default: return null;
                    }
                })}
            </div>
        </div>
    );
};
