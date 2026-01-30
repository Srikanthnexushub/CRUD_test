import React, { useState, useEffect } from 'react';
import { useRateLimit } from '../../contexts/RateLimitContext';
import '../../styles/RateLimit.css';

const RateLimitToast = () => {
    const { exceeded, limits, resetExceeded, getSecondsUntilReset } = useRateLimit();
    const [countdown, setCountdown] = useState(0);
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        if (exceeded) {
            setVisible(true);
            setCountdown(getSecondsUntilReset());
        } else {
            // Delay hiding to allow fade-out animation
            const timer = setTimeout(() => setVisible(false), 300);
            return () => clearTimeout(timer);
        }
    }, [exceeded, getSecondsUntilReset]);

    // Update countdown timer
    useEffect(() => {
        if (!exceeded || countdown <= 0) return;

        const interval = setInterval(() => {
            const remaining = getSecondsUntilReset();
            setCountdown(remaining);

            if (remaining <= 0) {
                clearInterval(interval);
            }
        }, 1000);

        return () => clearInterval(interval);
    }, [exceeded, countdown, getSecondsUntilReset]);

    const handleDismiss = () => {
        setVisible(false);
        resetExceeded();
    };

    const formatTime = (seconds) => {
        if (seconds < 60) {
            return `${seconds}s`;
        }
        const minutes = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${minutes}m ${secs}s`;
    };

    if (!visible) return null;

    return (
        <div className={`rate-limit-toast ${exceeded ? 'show' : 'hide'}`}>
            <div className="toast-content">
                <div className="toast-icon">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                </div>
                <div className="toast-body">
                    <h4>Rate Limit Exceeded</h4>
                    <p>You have reached the request limit of {limits.limit} requests.</p>
                    <div className="toast-countdown">
                        <span>Resets in: </span>
                        <strong>{formatTime(countdown)}</strong>
                    </div>
                </div>
                <button className="toast-dismiss" onClick={handleDismiss} aria-label="Dismiss">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd"/>
                    </svg>
                </button>
            </div>
            <div className="toast-progress">
                <div
                    className="toast-progress-bar"
                    style={{ width: `${(countdown / (limits.reset - Math.floor(Date.now() / 1000) + countdown)) * 100}%` }}
                ></div>
            </div>
        </div>
    );
};

export default RateLimitToast;
