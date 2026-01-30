import React, { useState, useEffect } from 'react';
import {
    RiskScoreBadge,
    ThreatAlertModal,
    SessionDetailsCard,
    ThreatIntelligencePanel,
    GeographicHeatmap,
    ThreatDetailsModal
} from './index';
import api from '../../services/api';

/**
 * Example 1: Admin Dashboard with Threat Intelligence
 * Full-featured admin view with statistics, map, and detailed assessments
 */
export const AdminThreatDashboard = () => {
    return (
        <div className="admin-dashboard">
            <div className="dashboard-header">
                <h1>Security Operations Center - Threat Intelligence</h1>
            </div>

            {/* Main Threat Intelligence Panel */}
            <section className="threat-panel-section">
                <ThreatIntelligencePanel apiBaseUrl="/api/threat" />
            </section>

            {/* Geographic Threat Map */}
            <section className="threat-map-section">
                <GeographicHeatmap apiBaseUrl="/api/threat" />
            </section>
        </div>
    );
};

/**
 * Example 2: Login Flow with Threat Detection
 * Handles account locking and threat alerts during login
 */
export const LoginWithThreatDetection = () => {
    const [credentials, setCredentials] = useState({ username: '', password: '' });
    const [showThreatAlert, setShowThreatAlert] = useState(false);
    const [threatAssessment, setThreatAssessment] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        try {
            const response = await api.login(credentials);

            // Check if login was blocked due to threat detection
            if (response.data.accountLocked && response.data.threatAssessment) {
                setThreatAssessment(response.data.threatAssessment);
                setShowThreatAlert(true);
            } else {
                // Successful login
                localStorage.setItem('token', response.data.token);
                localStorage.setItem('user', JSON.stringify(response.data.user));
                window.location.href = '/dashboard';
            }
        } catch (err) {
            setError(err.message || 'Login failed');
        } finally {
            setLoading(false);
        }
    };

    const handleContactSupport = () => {
        // Redirect to support or open support modal
        window.location.href = '/support?issue=account-locked';
    };

    return (
        <div className="login-page">
            <form onSubmit={handleLogin} className="login-form">
                <h2>Login</h2>

                <input
                    type="text"
                    placeholder="Username"
                    value={credentials.username}
                    onChange={(e) => setCredentials({ ...credentials, username: e.target.value })}
                    required
                />

                <input
                    type="password"
                    placeholder="Password"
                    value={credentials.password}
                    onChange={(e) => setCredentials({ ...credentials, password: e.target.value })}
                    required
                />

                {error && <div className="error-message">{error}</div>}

                <button type="submit" disabled={loading}>
                    {loading ? 'Logging in...' : 'Login'}
                </button>
            </form>

            {/* Threat Alert Modal */}
            {showThreatAlert && threatAssessment && (
                <ThreatAlertModal
                    threatAssessment={threatAssessment}
                    onClose={() => setShowThreatAlert(false)}
                    onContactSupport={handleContactSupport}
                />
            )}
        </div>
    );
};

/**
 * Example 3: User Profile - Active Sessions with Threat Info
 * Shows user's active sessions with risk indicators
 */
export const UserActiveSessions = () => {
    const [sessions, setSessions] = useState([]);
    const [selectedSession, setSelectedSession] = useState(null);
    const [showDetailsModal, setShowDetailsModal] = useState(false);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadSessions();
    }, []);

    const loadSessions = async () => {
        try {
            setLoading(true);
            // Assuming there's an endpoint for user sessions
            const response = await fetch('/api/sessions/active', {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                }
            });
            const data = await response.json();
            setSessions(data);
        } catch (error) {
            console.error('Error loading sessions:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleViewDetails = (session) => {
        if (session.threatAssessment) {
            setSelectedSession(session.threatAssessment);
            setShowDetailsModal(true);
        }
    };

    if (loading) {
        return <div className="loading">Loading sessions...</div>;
    }

    return (
        <div className="user-sessions-page">
            <h2>Your Active Sessions</h2>
            <p className="subtitle">Monitor your login activity and security status</p>

            <div className="sessions-grid">
                {sessions.length === 0 ? (
                    <div className="no-sessions">No active sessions found</div>
                ) : (
                    sessions.map(session => (
                        <SessionDetailsCard
                            key={session.id}
                            session={session}
                            onViewDetails={handleViewDetails}
                        />
                    ))
                )}
            </div>

            {/* Threat Details Modal */}
            {showDetailsModal && selectedSession && (
                <ThreatDetailsModal
                    assessment={selectedSession}
                    onClose={() => {
                        setShowDetailsModal(false);
                        setSelectedSession(null);
                    }}
                />
            )}
        </div>
    );
};

/**
 * Example 4: Security Dashboard Widget
 * Simple widget showing current threat level
 */
export const ThreatLevelWidget = () => {
    const [currentThreat, setCurrentThreat] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadCurrentThreat();
        const interval = setInterval(loadCurrentThreat, 60000); // Refresh every minute
        return () => clearInterval(interval);
    }, []);

    const loadCurrentThreat = async () => {
        try {
            const response = await api.getThreatAssessmentForCurrentSession();
            setCurrentThreat(response.data);
        } catch (error) {
            console.error('Error loading threat assessment:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return <div className="widget-loading">Loading...</div>;
    }

    if (!currentThreat) {
        return (
            <div className="threat-widget">
                <h3>Security Status</h3>
                <div className="status-good">
                    <span className="icon">✓</span>
                    <p>No threats detected</p>
                </div>
            </div>
        );
    }

    return (
        <div className="threat-widget">
            <h3>Current Threat Level</h3>
            <RiskScoreBadge score={currentThreat.riskScore} size="large" />
            {currentThreat.riskFactors && currentThreat.riskFactors.length > 0 && (
                <div className="widget-factors">
                    <p className="factors-title">Active Concerns:</p>
                    <ul>
                        {currentThreat.riskFactors.slice(0, 3).map((factor, index) => (
                            <li key={index}>{factor}</li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
};

/**
 * Example 5: Risk Score Comparison Table
 * Shows multiple users/sessions with risk scores for comparison
 */
export const RiskScoreComparison = ({ assessments }) => {
    const [sortBy, setSortBy] = useState('score'); // 'score' or 'time'
    const [sortOrder, setSortOrder] = useState('desc');

    const sortedAssessments = [...assessments].sort((a, b) => {
        const multiplier = sortOrder === 'desc' ? -1 : 1;
        if (sortBy === 'score') {
            return multiplier * (a.riskScore - b.riskScore);
        } else {
            return multiplier * (new Date(a.timestamp) - new Date(b.timestamp));
        }
    });

    return (
        <div className="risk-comparison-table">
            <div className="table-controls">
                <label>
                    Sort by:
                    <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
                        <option value="score">Risk Score</option>
                        <option value="time">Time</option>
                    </select>
                </label>
                <label>
                    Order:
                    <select value={sortOrder} onChange={(e) => setSortOrder(e.target.value)}>
                        <option value="desc">Descending</option>
                        <option value="asc">Ascending</option>
                    </select>
                </label>
            </div>

            <table className="comparison-table">
                <thead>
                    <tr>
                        <th>User</th>
                        <th>IP Address</th>
                        <th>Location</th>
                        <th>Risk Score</th>
                        <th>Time</th>
                    </tr>
                </thead>
                <tbody>
                    {sortedAssessments.map((assessment) => (
                        <tr key={assessment.id}>
                            <td>{assessment.username}</td>
                            <td className="mono">{assessment.ipAddress}</td>
                            <td>
                                {assessment.geolocation
                                    ? `${assessment.geolocation.city}, ${assessment.geolocation.country}`
                                    : 'Unknown'}
                            </td>
                            <td>
                                <RiskScoreBadge
                                    score={assessment.riskScore}
                                    size="small"
                                    showLabel={false}
                                />
                            </td>
                            <td>{new Date(assessment.timestamp).toLocaleString()}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

/**
 * Example 6: Real-time Threat Feed
 * Live stream of threat events with WebSocket integration
 */
export const RealTimeThreatFeed = () => {
    const [threats, setThreats] = useState([]);
    const [connected, setConnected] = useState(false);
    const maxThreats = 50;

    useEffect(() => {
        // Initialize with recent threats
        loadRecentThreats();

        // Setup WebSocket connection (if you have WebSocket support)
        // const ws = new WebSocket('ws://localhost:8080/ws/threats');
        // ws.onopen = () => setConnected(true);
        // ws.onmessage = (event) => {
        //     const newThreat = JSON.parse(event.data);
        //     setThreats(prev => [newThreat, ...prev].slice(0, maxThreats));
        // };
        // ws.onerror = () => setConnected(false);
        // return () => ws.close();

        // For now, use polling
        const interval = setInterval(loadRecentThreats, 30000);
        return () => clearInterval(interval);
    }, []);

    const loadRecentThreats = async () => {
        try {
            const response = await api.getThreatAssessments({ limit: maxThreats });
            setThreats(response.data);
            setConnected(true);
        } catch (error) {
            console.error('Error loading threats:', error);
            setConnected(false);
        }
    };

    return (
        <div className="threat-feed">
            <div className="feed-header">
                <h3>Real-time Threat Feed</h3>
                <div className={`connection-indicator ${connected ? 'connected' : 'disconnected'}`}>
                    <span className="dot"></span>
                    {connected ? 'Live' : 'Disconnected'}
                </div>
            </div>

            <div className="feed-list">
                {threats.map((threat) => (
                    <div
                        key={threat.id}
                        className={`feed-item ${threat.riskScore >= 60 ? 'high-risk' : ''}`}
                    >
                        <div className="feed-item-header">
                            <RiskScoreBadge score={threat.riskScore} size="small" />
                            <span className="feed-time">
                                {new Date(threat.timestamp).toLocaleTimeString()}
                            </span>
                        </div>
                        <div className="feed-item-content">
                            <strong>{threat.username}</strong> from {threat.ipAddress}
                            {threat.geolocation && (
                                <span className="feed-location">
                                    {' '}
                                    ({threat.geolocation.city}, {threat.geolocation.country})
                                </span>
                            )}
                        </div>
                        {threat.riskFactors && threat.riskFactors.length > 0 && (
                            <div className="feed-factors">
                                {threat.riskFactors[0]}
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
};

// Export all examples
export default {
    AdminThreatDashboard,
    LoginWithThreatDetection,
    UserActiveSessions,
    ThreatLevelWidget,
    RiskScoreComparison,
    RealTimeThreatFeed
};
