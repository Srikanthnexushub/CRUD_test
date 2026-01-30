import React from 'react';
import '../../styles/Threat.css';

const RiskScoreBadge = ({ score, size = 'medium', showLabel = true }) => {
    const getRiskLevel = (score) => {
        if (score < 40) return { level: 'LOW', color: 'green' };
        if (score < 60) return { level: 'MEDIUM', color: 'yellow' };
        if (score < 80) return { level: 'HIGH', color: 'orange' };
        return { level: 'CRITICAL', color: 'red' };
    };

    const { level, color } = getRiskLevel(score);

    return (
        <div className={`risk-score-badge risk-${color} size-${size}`}>
            <div className="risk-score-value">{score}</div>
            {showLabel && <div className="risk-score-label">{level}</div>}
        </div>
    );
};

export default RiskScoreBadge;
