import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import '../../styles/Notifications.css';

const EmailDashboard = () => {
    const [stats, setStats] = useState({
        totalEmails: 0,
        pendingEmails: 0,
        sentEmails: 0,
        failedEmails: 0,
        queueSize: 0,
    });
    const [recentEmails, setRecentEmails] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [retryingAll, setRetryingAll] = useState(false);
    const [retryStatus, setRetryStatus] = useState(null);

    useEffect(() => {
        loadDashboardData();
        const interval = setInterval(loadDashboardData, 30000); // Refresh every 30 seconds
        return () => clearInterval(interval);
    }, []);

    const loadDashboardData = async () => {
        try {
            setLoading(true);
            setError(null);

            const [statsResponse, emailsResponse] = await Promise.all([
                api.getEmailStats(),
                api.getRecentEmails(10)
            ]);

            setStats(statsResponse.data);
            setRecentEmails(emailsResponse.data);
        } catch (err) {
            console.error('Failed to load dashboard data:', err);
            setError(err.message || 'Failed to load dashboard data');
        } finally {
            setLoading(false);
        }
    };

    const handleRetryAll = async () => {
        if (!window.confirm('Are you sure you want to retry all failed emails?')) {
            return;
        }

        setRetryingAll(true);
        setRetryStatus(null);

        try {
            const response = await api.retryFailedEmails();
            setRetryStatus({
                type: 'success',
                message: `Successfully queued ${response.data.count || 0} emails for retry`
            });
            await loadDashboardData();
        } catch (err) {
            console.error('Failed to retry emails:', err);
            setRetryStatus({
                type: 'error',
                message: err.message || 'Failed to retry failed emails'
            });
        } finally {
            setRetryingAll(false);
            setTimeout(() => setRetryStatus(null), 5000);
        }
    };

    const getStatusColor = (status) => {
        switch (status.toLowerCase()) {
            case 'sent':
                return 'status-success';
            case 'failed':
                return 'status-error';
            case 'pending':
                return 'status-pending';
            default:
                return 'status-default';
        }
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleString();
    };

    const calculateSuccessRate = () => {
        const total = stats.sentEmails + stats.failedEmails;
        if (total === 0) return 0;
        return ((stats.sentEmails / total) * 100).toFixed(1);
    };

    if (loading && recentEmails.length === 0) {
        return (
            <div className="email-dashboard">
                <div className="loading-spinner">Loading dashboard...</div>
            </div>
        );
    }

    return (
        <div className="email-dashboard">
            <div className="dashboard-header">
                <h2>Email Dashboard</h2>
                <button
                    className="btn-refresh"
                    onClick={loadDashboardData}
                    disabled={loading}
                >
                    {loading ? 'Refreshing...' : 'Refresh'}
                </button>
            </div>

            {error && (
                <div className="alert alert-error">
                    {error}
                </div>
            )}

            {retryStatus && (
                <div className={`alert alert-${retryStatus.type}`}>
                    {retryStatus.message}
                </div>
            )}

            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-icon stat-icon-total">
                        <i className="fas fa-envelope"></i>
                    </div>
                    <div className="stat-content">
                        <div className="stat-label">Total Emails</div>
                        <div className="stat-value">{stats.totalEmails}</div>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-icon stat-icon-pending">
                        <i className="fas fa-clock"></i>
                    </div>
                    <div className="stat-content">
                        <div className="stat-label">Pending</div>
                        <div className="stat-value">{stats.pendingEmails}</div>
                        <div className="stat-subtitle">Queue: {stats.queueSize}</div>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-icon stat-icon-success">
                        <i className="fas fa-check-circle"></i>
                    </div>
                    <div className="stat-content">
                        <div className="stat-label">Sent</div>
                        <div className="stat-value">{stats.sentEmails}</div>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-icon stat-icon-error">
                        <i className="fas fa-exclamation-circle"></i>
                    </div>
                    <div className="stat-content">
                        <div className="stat-label">Failed</div>
                        <div className="stat-value">{stats.failedEmails}</div>
                    </div>
                </div>

                <div className="stat-card stat-card-wide">
                    <div className="stat-icon stat-icon-rate">
                        <i className="fas fa-chart-line"></i>
                    </div>
                    <div className="stat-content">
                        <div className="stat-label">Success Rate</div>
                        <div className="stat-value">{calculateSuccessRate()}%</div>
                        <div className="stat-subtitle">
                            {stats.sentEmails} of {stats.sentEmails + stats.failedEmails} delivered
                        </div>
                    </div>
                </div>
            </div>

            <div className="dashboard-section">
                <div className="section-header">
                    <h3>Recent Emails</h3>
                    {stats.failedEmails > 0 && (
                        <button
                            className="btn-retry-all"
                            onClick={handleRetryAll}
                            disabled={retryingAll}
                        >
                            {retryingAll ? 'Retrying...' : `Retry All Failed (${stats.failedEmails})`}
                        </button>
                    )}
                </div>

                {recentEmails.length === 0 ? (
                    <div className="empty-state">
                        <i className="fas fa-inbox"></i>
                        <p>No emails sent yet</p>
                    </div>
                ) : (
                    <div className="recent-emails-table">
                        <table>
                            <thead>
                                <tr>
                                    <th>Recipient</th>
                                    <th>Subject</th>
                                    <th>Status</th>
                                    <th>Sent At</th>
                                    <th>Attempts</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentEmails.map((email) => (
                                    <tr key={email.id}>
                                        <td>{email.recipientEmail}</td>
                                        <td className="email-subject">{email.subject}</td>
                                        <td>
                                            <span className={`status-badge ${getStatusColor(email.status)}`}>
                                                {email.status}
                                            </span>
                                        </td>
                                        <td>{formatDate(email.sentAt)}</td>
                                        <td>
                                            <span className="attempts-badge">
                                                {email.attempts}/{email.maxAttempts}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            <div className="dashboard-footer">
                <div className="footer-info">
                    <i className="fas fa-info-circle"></i>
                    <span>Dashboard updates automatically every 30 seconds</span>
                </div>
            </div>
        </div>
    );
};

export default EmailDashboard;
