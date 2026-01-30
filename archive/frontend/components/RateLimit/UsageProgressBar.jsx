import React from 'react';
import { useRateLimit } from '../../contexts/RateLimitContext';
import '../../styles/RateLimit.css';

const UsageProgressBar = () => {
    const { limits, getUsagePercentage } = useRateLimit();

    if (limits.limit === null || limits.remaining === null) {
        return null;
    }

    const percentage = getUsagePercentage();
    const used = limits.limit - limits.remaining;

    // Determine color based on usage
    const getColorClass = () => {
        if (percentage >= 90) return 'danger';
        if (percentage >= 70) return 'warning';
        return 'success';
    };

    const getStatusText = () => {
        if (percentage >= 90) return 'Critical';
        if (percentage >= 70) return 'High';
        return 'Normal';
    };

    return (
        <div className="usage-progress-bar">
            <div className="usage-header">
                <div className="usage-title">
                    <span>API Usage</span>
                    <span className={`usage-status ${getColorClass()}`}>
                        {getStatusText()}
                    </span>
                </div>
                <div className="usage-stats">
                    <span className="usage-percentage">{percentage}%</span>
                    <span className="usage-count">
                        {used} / {limits.limit} requests
                    </span>
                </div>
            </div>
            <div className="progress-bar-container">
                <div
                    className={`progress-bar-fill ${getColorClass()}`}
                    style={{ width: `${percentage}%` }}
                >
                    <div className="progress-bar-shine"></div>
                </div>
            </div>
            <div className="usage-footer">
                <span className="remaining-requests">
                    {limits.remaining} requests remaining
                </span>
                {limits.reset && (
                    <span className="reset-time">
                        Resets: {new Date(limits.reset * 1000).toLocaleTimeString()}
                    </span>
                )}
            </div>
        </div>
    );
};

export default UsageProgressBar;
