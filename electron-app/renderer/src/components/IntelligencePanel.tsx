import React, { useEffect, useState } from 'react';
import { GitHubSiv } from './GitHubSiv';
import { LinearSiv } from './LinearSiv';
import { JiraSiv } from './JiraSiv';
import { VercelSiv } from './VercelSiv';

export const IntelligencePanel: React.FC = () => {
    const [activeConfigs, setActiveConfigs] = useState<any[]>([]);

    useEffect(() => {
        const fetchConfigs = async () => {
            const res = await fetch('/api/siv/configs', {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                }
            });
            if (res.ok) {
                setActiveConfigs(await res.json());
            }
        };
        fetchConfigs();
    }, []);

    if (activeConfigs.length === 0) return null;

    return (
        <div className="siv-panel bg-gray-900/50 border-l border-gray-800 h-full overflow-y-auto w-80">
            <div className="p-4 border-b border-gray-800 flex items-center justify-between">
                <span className="text-xs text-gray-400 uppercase tracking-widest font-bold">Intelligence Feed</span>
                <span className="text-[10px] bg-blue-500/20 text-blue-400 px-2 py-0.5 rounded-full border border-blue-500/30">Active Sovereignty</span>
            </div>
            <div className="flex flex-col">
                {activeConfigs.map(config => {
                    switch (config.vesselType) {
                        case 'github': return <GitHubSiv key="github" vesselType="github" />;
                        case 'linear': return <LinearSiv key="linear" vesselType="linear" />;
                        case 'jira': return <JiraSiv key="jira" vesselType="jira" />;
                        case 'vercel': return <VercelSiv key="vercel" vesselType="vercel" />;
                        default: return null;
                    }
                })}
            </div>
        </div>
    );
};
