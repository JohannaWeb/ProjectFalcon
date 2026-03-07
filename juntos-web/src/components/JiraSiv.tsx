import React from 'react';

interface JiraIssue {
    key: string;
    fields: {
        summary: string;
        status: { name: string };
        updated: string;
    };
}

export const JiraSiv: React.FC<{ issues: JiraIssue[] }> = ({ issues }) => {
    // Data is now passed from parent
    const loading = false;

    // Internal fetching removed

    if (loading) return <div className="p-4 text-gray-500 animate-pulse">Syncing with Jira...</div>;
    if (issues.length === 0) return <div className="p-4 text-gray-500 italic">No Jira activity found.</div>;

    return (
        <div className="flex flex-col gap-2 p-2 border-t border-gray-800">
            <div className="px-2 py-1 flex items-center gap-2">
                <span className="text-blue-500 font-bold">SIV-03</span>
                <span className="text-[10px] text-gray-500 uppercase tracking-widest font-semibold text-center italic">Jira Enterprise</span>
            </div>
            {issues.map(issue => (
                <div key={issue.key} className="p-3 bg-gray-800/30 rounded-lg hover:bg-gray-800/50 transition-colors border border-transparent hover:border-blue-500/30 group">
                    <div className="flex items-center justify-between mb-1">
                        <span className="text-xs font-medium text-blue-400">{issue.key}</span>
                        <span className="text-[10px] text-gray-500">{issue.fields.status.name}</span>
                    </div>
                    <div className="text-sm text-gray-200 group-hover:text-white font-medium line-clamp-2">
                        {issue.fields.summary}
                    </div>
                </div>
            ))}
        </div>
    );
};
