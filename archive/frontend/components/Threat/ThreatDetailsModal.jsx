import React from 'react';
import RiskScoreBadge from './RiskScoreBadge';
import '../../styles/Threat.css';

const ThreatDetailsModal = ({ assessment, onClose }) => {
    if (!assessment) return null;

    const formatTimestamp = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            timeZoneName: 'short'
        });
    };

    const renderSection = (title, icon, content) => (
        <div className="detail-section">
            <h3 className="section-title">
                <span className="section-icon">{icon}</span>
                {title}
            </h3>
            <div className="section-content">{content}</div>
        </div>
    );

    const renderInfoRow = (label, value, highlight = false) => (
        <div className={`info-row ${highlight ? 'highlight' : ''}`}>
            <span className="info-label">{label}:</span>
            <span className="info-value">{value || 'N/A'}</span>
        </div>
    );

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="threat-details-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Threat Assessment Details</h2>
                    <button className="btn-close-modal" onClick={onClose}>×</button>
                </div>

                <div className="modal-body">
                    {/* Overall Risk Score Section */}
                    <div className="risk-overview">
                        <div className="risk-score-display">
                            <RiskScoreBadge score={assessment.riskScore} size="large" />
                        </div>
                        <div className="risk-status">
                            <div className="status-item">
                                <span className="status-label">Access Status:</span>
                                <span className={`status-value status-${assessment.allowed ? 'allowed' : 'blocked'}`}>
                                    {assessment.allowed ? '✓ Allowed' : '✗ Blocked'}
                                </span>
                            </div>
                            {assessment.accountLocked && (
                                <div className="status-item">
                                    <span className="status-label">Account Status:</span>
                                    <span className="status-value status-locked">🔒 Locked</span>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Risk Factors */}
                    {assessment.riskFactors && assessment.riskFactors.length > 0 && renderSection(
                        'Risk Factors',
                        '⚠',
                        <ul className="risk-factors-detailed">
                            {assessment.riskFactors.map((factor, index) => (
                                <li key={index} className="risk-factor">
                                    <span className="factor-bullet">•</span>
                                    <span className="factor-text">{factor}</span>
                                </li>
                            ))}
                        </ul>
                    )}

                    {/* User Information */}
                    {renderSection(
                        'User Information',
                        '👤',
                        <>
                            {renderInfoRow('Username', assessment.username)}
                            {renderInfoRow('User ID', assessment.userId)}
                            {renderInfoRow('Timestamp', formatTimestamp(assessment.timestamp))}
                        </>
                    )}

                    {/* Network Information */}
                    {renderSection(
                        'Network Information',
                        '🌐',
                        <>
                            {renderInfoRow('IP Address', assessment.ipAddress, true)}
                            {assessment.ipReputation && (
                                <>
                                    {renderInfoRow('IP Reputation', assessment.ipReputation.reputation)}
                                    {renderInfoRow('ISP', assessment.ipReputation.isp)}
                                    {renderInfoRow('Organization', assessment.ipReputation.organization)}
                                    {assessment.ipReputation.isProxy && renderInfoRow('Proxy Detected', 'Yes', true)}
                                    {assessment.ipReputation.isTor && renderInfoRow('Tor Network', 'Yes', true)}
                                    {assessment.ipReputation.isVpn && renderInfoRow('VPN Detected', 'Yes', true)}
                                </>
                            )}
                        </>
                    )}

                    {/* Geolocation Information */}
                    {assessment.geolocation && renderSection(
                        'Geographic Location',
                        '📍',
                        <>
                            {renderInfoRow('Country', assessment.geolocation.country)}
                            {renderInfoRow('Region', assessment.geolocation.region)}
                            {renderInfoRow('City', assessment.geolocation.city)}
                            {renderInfoRow('Postal Code', assessment.geolocation.postalCode)}
                            {renderInfoRow('Timezone', assessment.geolocation.timezone)}
                            {assessment.geolocation.latitude && assessment.geolocation.longitude && (
                                renderInfoRow(
                                    'Coordinates',
                                    `${assessment.geolocation.latitude}, ${assessment.geolocation.longitude}`
                                )
                            )}
                        </>
                    )}

                    {/* Device Information */}
                    {assessment.deviceFingerprint && renderSection(
                        'Device Information',
                        '💻',
                        <>
                            {renderInfoRow('User Agent', assessment.deviceFingerprint.userAgent)}
                            {renderInfoRow('Browser', assessment.deviceFingerprint.browser)}
                            {renderInfoRow('Operating System', assessment.deviceFingerprint.os)}
                            {renderInfoRow('Device Type', assessment.deviceFingerprint.deviceType)}
                            {renderInfoRow('Screen Resolution', assessment.deviceFingerprint.screenResolution)}
                            {renderInfoRow('Language', assessment.deviceFingerprint.language)}
                            {renderInfoRow('Timezone', assessment.deviceFingerprint.timezone)}
                        </>
                    )}

                    {/* Behavioral Analysis */}
                    {assessment.behavioralFlags && Object.keys(assessment.behavioralFlags).length > 0 && renderSection(
                        'Behavioral Analysis',
                        '🔍',
                        <>
                            {assessment.behavioralFlags.unusualLoginTime &&
                                renderInfoRow('Unusual Login Time', 'Detected', true)}
                            {assessment.behavioralFlags.rapidLoginAttempts &&
                                renderInfoRow('Rapid Login Attempts', 'Detected', true)}
                            {assessment.behavioralFlags.newLocation &&
                                renderInfoRow('New Location', 'Detected', true)}
                            {assessment.behavioralFlags.newDevice &&
                                renderInfoRow('New Device', 'Detected', true)}
                            {assessment.behavioralFlags.impossibleTravel &&
                                renderInfoRow('Impossible Travel', 'Detected', true)}
                        </>
                    )}

                    {/* Timeline */}
                    {renderSection(
                        'Timeline',
                        '🕐',
                        <>
                            {renderInfoRow('Assessment Time', formatTimestamp(assessment.timestamp))}
                            {assessment.createdAt && renderInfoRow('Record Created', formatTimestamp(assessment.createdAt))}
                            {assessment.lockExpiresAt && renderInfoRow('Lock Expires', formatTimestamp(assessment.lockExpiresAt))}
                        </>
                    )}

                    {/* Additional Metadata */}
                    {assessment.metadata && Object.keys(assessment.metadata).length > 0 && renderSection(
                        'Additional Information',
                        'ℹ',
                        <>
                            {Object.entries(assessment.metadata).map(([key, value]) =>
                                renderInfoRow(
                                    key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase()),
                                    typeof value === 'object' ? JSON.stringify(value) : String(value)
                                )
                            )}
                        </>
                    )}
                </div>

                <div className="modal-footer">
                    <button className="btn-close" onClick={onClose}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ThreatDetailsModal;
