import React from 'react';
import RiskScoreBadge from './RiskScoreBadge';
import '../../styles/Threat.css';

const SessionDetailsCard = ({ session, onViewDetails }) => {
    const formatTimestamp = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    };

    const getDeviceIcon = (device) => {
        if (!device) return '💻';
        const lower = device.toLowerCase();
        if (lower.includes('mobile') || lower.includes('android') || lower.includes('iphone')) return '📱';
        if (lower.includes('tablet') || lower.includes('ipad')) return '📱';
        return '💻';
    };

    return (
        <div className={`session-details-card ${session.threatAssessment ? 'has-threat' : ''}`}>
            <div className="session-header">
                <div className="session-device">
                    <span className="device-icon">{getDeviceIcon(session.device)}</span>
                    <div className="device-info">
                        <div className="device-name">{session.device || 'Unknown Device'}</div>
                        <div className="session-time">{formatTimestamp(session.loginTime)}</div>
                    </div>
                </div>
                {session.threatAssessment && (
                    <RiskScoreBadge
                        score={session.threatAssessment.riskScore}
                        size="small"
                    />
                )}
            </div>

            <div className="session-details">
                <div className="detail-row">
                    <span className="detail-icon">🌐</span>
                    <div className="detail-content">
                        <span className="detail-label">IP Address</span>
                        <span className="detail-value">{session.ipAddress}</span>
                    </div>
                </div>

                {session.location && (
                    <div className="detail-row">
                        <span className="detail-icon">📍</span>
                        <div className="detail-content">
                            <span className="detail-label">Location</span>
                            <span className="detail-value">
                                {session.location.city && session.location.country
                                    ? `${session.location.city}, ${session.location.country}`
                                    : session.location.country || 'Unknown'}
                            </span>
                        </div>
                    </div>
                )}

                {session.threatAssessment && session.threatAssessment.riskFactors &&
                 session.threatAssessment.riskFactors.length > 0 && (
                    <div className="threat-factors">
                        <div className="factors-header">
                            <span className="factors-icon">⚠</span>
                            <span className="factors-label">Threat Indicators</span>
                        </div>
                        <ul className="factors-list">
                            {session.threatAssessment.riskFactors.slice(0, 3).map((factor, index) => (
                                <li key={index} className="factor-item">{factor}</li>
                            ))}
                            {session.threatAssessment.riskFactors.length > 3 && (
                                <li className="factor-item more">
                                    +{session.threatAssessment.riskFactors.length - 3} more
                                </li>
                            )}
                        </ul>
                    </div>
                )}
            </div>

            {session.threatAssessment && onViewDetails && (
                <div className="session-footer">
                    <button
                        className="btn-view-details"
                        onClick={() => onViewDetails(session)}
                    >
                        View Full Details
                    </button>
                </div>
            )}
        </div>
    );
};

export default SessionDetailsCard;
