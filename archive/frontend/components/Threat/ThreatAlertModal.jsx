import React, { useState, useEffect } from 'react';
import RiskScoreBadge from './RiskScoreBadge';
import '../../styles/Threat.css';

const ThreatAlertModal = ({ threatAssessment, onClose, onContactSupport }) => {
    const [countdown, setCountdown] = useState(0);

    useEffect(() => {
        if (threatAssessment?.accountLocked && threatAssessment?.lockExpiresAt) {
            const calculateCountdown = () => {
                const now = new Date().getTime();
                const lockExpires = new Date(threatAssessment.lockExpiresAt).getTime();
                const remaining = Math.max(0, Math.floor((lockExpires - now) / 1000));
                setCountdown(remaining);

                if (remaining === 0) {
                    // Account unlocked, close modal
                    setTimeout(() => {
                        onClose();
                        window.location.reload(); // Refresh to allow login
                    }, 2000);
                }
            };

            calculateCountdown();
            const interval = setInterval(calculateCountdown, 1000);

            return () => clearInterval(interval);
        }
    }, [threatAssessment, onClose]);

    const formatCountdown = (seconds) => {
        const minutes = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${minutes}:${secs.toString().padStart(2, '0')}`;
    };

    if (!threatAssessment) return null;

    return (
        <div className="modal-overlay">
            <div className="threat-alert-modal">
                <div className="modal-header alert-header">
                    <div className="alert-icon">
                        <span className="icon-warning">⚠</span>
                    </div>
                    <h2>Account Temporarily Locked</h2>
                </div>

                <div className="modal-body">
                    <div className="risk-score-section">
                        <p className="alert-message">
                            Your account has been temporarily locked due to suspicious activity.
                        </p>
                        <div className="risk-display">
                            <span className="risk-label">Threat Level:</span>
                            <RiskScoreBadge score={threatAssessment.riskScore} size="large" />
                        </div>
                    </div>

                    {threatAssessment.riskFactors && threatAssessment.riskFactors.length > 0 && (
                        <div className="risk-factors-section">
                            <h3>Security Concerns Detected:</h3>
                            <ul className="risk-factors-list">
                                {threatAssessment.riskFactors.map((factor, index) => (
                                    <li key={index} className="risk-factor-item">
                                        <span className="factor-icon">•</span>
                                        <span className="factor-text">{factor}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}

                    {countdown > 0 && (
                        <div className="countdown-section">
                            <p className="countdown-label">Account will unlock in:</p>
                            <div className="countdown-timer">{formatCountdown(countdown)}</div>
                        </div>
                    )}

                    {countdown === 0 && (
                        <div className="unlock-message">
                            <span className="success-icon">✓</span>
                            <p>Your account has been unlocked. You may now try again.</p>
                        </div>
                    )}

                    <div className="support-section">
                        <p className="support-text">
                            If you believe this is an error or need immediate access, please contact support.
                        </p>
                        <button
                            className="btn-support"
                            onClick={onContactSupport}
                        >
                            Contact Support
                        </button>
                    </div>
                </div>

                <div className="modal-footer">
                    <button
                        className="btn-close"
                        onClick={onClose}
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ThreatAlertModal;
