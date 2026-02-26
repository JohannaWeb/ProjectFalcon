import React, { useEffect, useState } from 'react';

interface LinearIssue {
    id: string;
    title: string;
    updatedAt: string;
    state: { name: string };
}

export const LinearSiv: React.FC<{ vesselType: string }> = ({ vesselType }) => {
    const [issues, setIssues] = useState<LinearIssue[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchActivity = async () => {
            try {
                const response = await fetch(`/api/siv/activity/${vesselType}`, {
                    headers: {
                        'Authorization': `Bearer ${localStorage.getItem('token')}`
                    }
                });
                if (response.ok) {
                    const data = await response.json();
                    setIssues(data);
                }
            } catch (error) {
                console.error('Failed to fetch Linear SIV activity', error);
            } finally {
                setLoading(false);
            }
        };

        fetchActivity();
        const interval = setInterval(fetchActivity, 60000);
        return () => clearInterval(interval);
    }, [vesselType]);

    if (loading) return <div className="p-4 text-gray-500 animate-pulse">Connecting to Linear...</div>;
    if (issues.length === 0) return <div className="p-4 text-gray-500 italic">No Linear issues detected.</div>;

    return (
        <div className="flex flex-col gap-2 p-2 border-t border-gray-800">
            <div className="px-2 py-1 flex items-center gap-2">
                <span className="text-purple-400 font-bold">SIV-02</span>
                <span className="text-[10px] text-gray-500 uppercase tracking-widest font-semibold text-center italic">Linear</span>
            </div>
            {issues.map(issue => (
                <div key={issue.id} className="p-3 bg-gray-800/30 rounded-lg hover:bg-gray-800/50 transition-colors border border-transparent hover:border-purple-500/30 group">
                    <div className="flex items-center justify-between mb-1">
                        <span className="text-xs font-medium text-purple-300">{issue.state.name}</span>
                        <span className="text-[10px] text-gray-500">
                            {new Date(issue.updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                    </div>
                    <div className="text-sm text-gray-200 group-hover:text-white font-medium line-clamp-2">
                        {issue.title}
                    </div>
                </div>
            ))}
        </div>
    );
};
