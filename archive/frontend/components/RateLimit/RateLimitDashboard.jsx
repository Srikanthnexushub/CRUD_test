import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import RequestRateGauge from './RequestRateGauge';
import '../../styles/RateLimit.css';

const RateLimitDashboard = () => {
    const [stats, setStats] = useState({
        violations24h: 0,
        violations7d: 0,
        totalRequests24h: 0,
        activeWhitelists: 0,
    });
    const [recentViolations, setRecentViolations] = useState([]);
    const [topEndpoints, setTopEndpoints] = useState([]);
    const [config, setConfig] = useState({
        defaultLimit: 100,
        windowSeconds: 60,
        enabled: true,
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchDashboardData();
        const interval = setInterval(fetchDashboardData, 30000); // Refresh every 30s
        return () => clearInterval(interval);
    }, []);

    const fetchDashboardData = async () => {
        try {
            setLoading(true);
            const [statsRes, violationsRes, endpointsRes, configRes] = await Promise.all([
                api.getRateLimitStats(),
                api.getRecentViolations(10),
                api.getTopBlockedEndpoints(5),
                api.getRateLimitConfig(),
            ]);

            // Extract stats from response (handle nested structure)
            setStats(statsRes.data?.stats || statsRes.data || {
                violations24h: 0,
                violations7d: 0,
                totalRequests24h: 0,
                activeWhitelists: 0,
            });

            setRecentViolations(violationsRes.data?.violations || violationsRes.data || []);
            setTopEndpoints(endpointsRes.data?.endpoints || endpointsRes.data || []);
            setConfig(configRes.data?.config || configRes.data || config);
            setError(null);
        } catch (err) {
            setError('Failed to load dashboard data');
            console.error('Dashboard error:', err);
        } finally {
            setLoading(false);
        }
    };

    const formatTimestamp = (timestamp) => {
        return new Date(timestamp).toLocaleString();
    };

    const formatDuration = (seconds) => {
        if (seconds < 60) return `${seconds}s`;
        if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
        return `${Math.floor(seconds / 3600)}h`;
    };

    if (loading && !stats.violations24h) {
        return <div className="rate-limit-dashboard loading">Loading dashboard...</div>;
    }

    if (error) {
        return (
            <div className="rate-limit-dashboard error">
                <p>{error}</p>
                <button onClick={fetchDashboardData}>Retry</button>
            </div>
        );
    }

    return (
        <div className="rate-limit-dashboard">
            <div className="dashboard-header">
                <h2>Rate Limit Dashboard</h2>
                <button className="refresh-btn" onClick={fetchDashboardData}>
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd"/>
                    </svg>
                    Refresh
                </button>
            </div>

            {/* Statistics Cards */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-icon violations">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                        </svg>
                    </div>
                    <div className="stat-content">
                        <h3>Violations (24h)</h3>
                        <p className="stat-value">{stats.violations24h}</p>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-icon violations-week">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
                        </svg>
                    </div>
                    <div className="stat-content">
                        <h3>Violations (7d)</h3>
                        <p className="stat-value">{stats.violations7d}</p>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-icon requests">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M7 12l3-3 3 3 4-4M8 21l4-4 4 4M3 4h18M4 4h16v12a1 1 0 01-1 1H5a1 1 0 01-1-1V4z"/>
                        </svg>
                    </div>
                    <div className="stat-content">
                        <h3>Total Requests (24h)</h3>
                        <p className="stat-value">{stats.totalRequests24h.toLocaleString()}</p>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-icon whitelists">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
                        </svg>
                    </div>
                    <div className="stat-content">
                        <h3>Active Whitelists</h3>
                        <p className="stat-value">{stats.activeWhitelists}</p>
                    </div>
                </div>
            </div>

            {/* Gauge */}
            <div className="dashboard-row">
                <RequestRateGauge title="Current System Load" />
            </div>

            {/* Recent Violations Table */}
            <div className="dashboard-section">
                <h3>Recent Violations</h3>
                {recentViolations.length === 0 ? (
                    <p className="empty-state">No recent violations</p>
                ) : (
                    <div className="table-container">
                        <table className="violations-table">
                            <thead>
                                <tr>
                                    <th>Timestamp</th>
                                    <th>IP Address</th>
                                    <th>User</th>
                                    <th>Endpoint</th>
                                    <th>Limit</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentViolations.map((violation, index) => (
                                    <tr key={index}>
                                        <td>{formatTimestamp(violation.timestamp)}</td>
                                        <td><code>{violation.ipAddress}</code></td>
                                        <td>{violation.username || 'Anonymous'}</td>
                                        <td><code>{violation.endpoint}</code></td>
                                        <td>{violation.limit} req/{formatDuration(violation.windowSeconds)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Top Blocked Endpoints Chart */}
            <div className="dashboard-section">
                <h3>Top Blocked Endpoints</h3>
                {topEndpoints.length === 0 ? (
                    <p className="empty-state">No blocked endpoints</p>
                ) : (
                    <div className="endpoints-chart">
                        {topEndpoints.map((endpoint, index) => (
                            <div key={index} className="endpoint-bar">
                                <div className="endpoint-info">
                                    <span className="endpoint-path"><code>{endpoint.path}</code></span>
                                    <span className="endpoint-count">{endpoint.count} violations</span>
                                </div>
                                <div className="endpoint-bar-container">
                                    <div
                                        className="endpoint-bar-fill"
                                        style={{
                                            width: `${(endpoint.count / topEndpoints[0].count) * 100}%`
                                        }}
                                    ></div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Configuration Summary */}
            <div className="dashboard-section config-summary">
                <h3>Configuration Summary</h3>
                <div className="config-grid">
                    <div className="config-item">
                        <span className="config-label">Status:</span>
                        <span className={`config-value ${config.enabled ? 'enabled' : 'disabled'}`}>
                            {config.enabled ? 'Enabled' : 'Disabled'}
                        </span>
                    </div>
                    <div className="config-item">
                        <span className="config-label">Default Limit:</span>
                        <span className="config-value">{config.defaultLimit} requests</span>
                    </div>
                    <div className="config-item">
                        <span className="config-label">Time Window:</span>
                        <span className="config-value">{formatDuration(config.windowSeconds)}</span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default RateLimitDashboard;
