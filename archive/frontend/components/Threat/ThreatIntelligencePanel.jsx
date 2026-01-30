import React, { useState, useEffect } from 'react';
import RiskScoreBadge from './RiskScoreBadge';
import ThreatDetailsModal from './ThreatDetailsModal';
import '../../styles/Threat.css';

const ThreatIntelligencePanel = ({ apiBaseUrl = '/api/threat' }) => {
    const [assessments, setAssessments] = useState([]);
    const [filteredAssessments, setFilteredAssessments] = useState([]);
    const [statistics, setStatistics] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedAssessment, setSelectedAssessment] = useState(null);
    const [showDetailsModal, setShowDetailsModal] = useState(false);
    const [filters, setFilters] = useState({
        highRiskOnly: false,
        timeRange: '24h'
    });

    useEffect(() => {
        loadData();
        const interval = setInterval(loadData, 30000); // Refresh every 30 seconds
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        applyFilters();
    }, [assessments, filters]);

    const loadData = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            const headers = {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            };

            // Load recent assessments (fixed path: /assessments instead of /assessments/recent)
            const assessmentsResponse = await fetch(`${apiBaseUrl}/assessments?page=0&size=50`, { headers });
            if (!assessmentsResponse.ok) {
                if (assessmentsResponse.status === 403) {
                    throw new Error('Access denied - Admin role required');
                }
                throw new Error('Failed to load threat assessments');
            }
            const assessmentsData = await assessmentsResponse.json();

            // Load statistics (fixed path: /stats instead of /statistics)
            const statsResponse = await fetch(`${apiBaseUrl}/stats`, { headers });
            if (!statsResponse.ok) {
                if (statsResponse.status === 403) {
                    throw new Error('Access denied - Admin role required');
                }
                throw new Error('Failed to load statistics');
            }
            const statsData = await statsResponse.json();

            // Handle response structure from backend
            // Backend returns: { success: true, assessments: [...], totalPages, currentPage, totalElements }
            const assessmentsList = assessmentsData.assessments || assessmentsData.content || assessmentsData || [];
            const stats = statsData.stats || statsData || null;

            console.log('Loaded assessments:', assessmentsList.length);
            setAssessments(assessmentsList);
            setStatistics(stats);
            setError(null);
        } catch (err) {
            console.error('Error loading threat intelligence data:', err);
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const applyFilters = () => {
        let filtered = [...assessments];

        // Filter by high risk only
        if (filters.highRiskOnly) {
            filtered = filtered.filter(a => a.riskScore >= 60);
        }

        // Filter by time range
        const now = new Date();
        const timeLimit = new Date();
        if (filters.timeRange === '24h') {
            timeLimit.setHours(now.getHours() - 24);
        } else if (filters.timeRange === '7d') {
            timeLimit.setDate(now.getDate() - 7);
        } else if (filters.timeRange === '30d') {
            timeLimit.setDate(now.getDate() - 30);
        }

        filtered = filtered.filter(a => new Date(a.timestamp) >= timeLimit);

        setFilteredAssessments(filtered);
    };

    const handleToggleAccountLock = async (userId, currentlyLocked) => {
        try {
            const token = localStorage.getItem('token');
            const endpoint = currentlyLocked
                ? `${apiBaseUrl}/account/${userId}/unlock`
                : `${apiBaseUrl}/account/${userId}/lock`;

            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) throw new Error('Failed to toggle account lock');

            // Reload data
            await loadData();
        } catch (err) {
            console.error('Error toggling account lock:', err);
            alert('Failed to toggle account lock: ' + err.message);
        }
    };

    const formatTimestamp = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const handleViewDetails = (assessment) => {
        setSelectedAssessment(assessment);
        setShowDetailsModal(true);
    };

    if (loading && assessments.length === 0) {
        return <div className="threat-panel-loading">Loading threat intelligence data...</div>;
    }

    return (
        <div className="threat-intelligence-panel">
            <div className="panel-header">
                <h2>Threat Intelligence Dashboard</h2>
                <button className="btn-refresh" onClick={loadData} disabled={loading}>
                    {loading ? '↻' : '⟳'} Refresh
                </button>
            </div>

            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠</span>
                    {error}
                </div>
            )}

            {/* Statistics Cards */}
            {statistics && (
                <div className="statistics-grid">
                    <div className="stat-card stat-primary">
                        <div className="stat-icon">📊</div>
                        <div className="stat-content">
                            <div className="stat-value">{statistics.totalAssessments24h || 0}</div>
                            <div className="stat-label">Assessments (24h)</div>
                        </div>
                    </div>
                    <div className="stat-card stat-warning">
                        <div className="stat-icon">⚠</div>
                        <div className="stat-content">
                            <div className="stat-value">{statistics.highRiskCount24h || 0}</div>
                            <div className="stat-label">High Risk (24h)</div>
                        </div>
                    </div>
                    <div className="stat-card stat-danger">
                        <div className="stat-icon">🔒</div>
                        <div className="stat-content">
                            <div className="stat-value">{statistics.blockedLogins24h || 0}</div>
                            <div className="stat-label">Blocked Logins (24h)</div>
                        </div>
                    </div>
                    <div className="stat-card stat-info">
                        <div className="stat-icon">📈</div>
                        <div className="stat-content">
                            <div className="stat-value">
                                {statistics.averageRiskScore ? statistics.averageRiskScore.toFixed(1) : '0.0'}
                            </div>
                            <div className="stat-label">Average Risk Score</div>
                        </div>
                    </div>
                </div>
            )}

            {/* Filters */}
            <div className="filters-bar">
                <div className="filter-group">
                    <label className="filter-checkbox">
                        <input
                            type="checkbox"
                            checked={filters.highRiskOnly}
                            onChange={(e) => setFilters({ ...filters, highRiskOnly: e.target.checked })}
                        />
                        <span>High Risk Only (60+)</span>
                    </label>
                </div>
                <div className="filter-group">
                    <label>Time Range:</label>
                    <select
                        value={filters.timeRange}
                        onChange={(e) => setFilters({ ...filters, timeRange: e.target.value })}
                        className="filter-select"
                    >
                        <option value="24h">Last 24 Hours</option>
                        <option value="7d">Last 7 Days</option>
                        <option value="30d">Last 30 Days</option>
                        <option value="all">All Time</option>
                    </select>
                </div>
            </div>

            {/* Assessments Table */}
            <div className="assessments-table-container">
                <table className="assessments-table">
                    <thead>
                        <tr>
                            <th>Timestamp</th>
                            <th>User</th>
                            <th>IP Address</th>
                            <th>Location</th>
                            <th>Risk Score</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredAssessments.length === 0 ? (
                            <tr>
                                <td colSpan="7" className="no-data">
                                    No threat assessments found
                                </td>
                            </tr>
                        ) : (
                            filteredAssessments.map((assessment) => (
                                <tr key={assessment.id} className={assessment.accountLocked ? 'locked-row' : ''}>
                                    <td>{formatTimestamp(assessment.timestamp)}</td>
                                    <td className="user-cell">
                                        <div>{assessment.username || 'Unknown'}</div>
                                        {assessment.accountLocked && (
                                            <span className="locked-badge">🔒 Locked</span>
                                        )}
                                    </td>
                                    <td className="ip-cell">{assessment.ipAddress}</td>
                                    <td>
                                        {assessment.geolocation
                                            ? `${assessment.geolocation.city || ''}, ${assessment.geolocation.country || 'Unknown'}`
                                            : 'Unknown'}
                                    </td>
                                    <td>
                                        <RiskScoreBadge score={assessment.riskScore} size="small" showLabel={false} />
                                    </td>
                                    <td>
                                        <span className={`status-badge status-${assessment.allowed ? 'allowed' : 'blocked'}`}>
                                            {assessment.allowed ? 'Allowed' : 'Blocked'}
                                        </span>
                                    </td>
                                    <td className="actions-cell">
                                        <button
                                            className="btn-action btn-view"
                                            onClick={() => handleViewDetails(assessment)}
                                            title="View Details"
                                        >
                                            👁
                                        </button>
                                        {assessment.userId && (
                                            <button
                                                className={`btn-action ${assessment.accountLocked ? 'btn-unlock' : 'btn-lock'}`}
                                                onClick={() => handleToggleAccountLock(assessment.userId, assessment.accountLocked)}
                                                title={assessment.accountLocked ? 'Unlock Account' : 'Lock Account'}
                                            >
                                                {assessment.accountLocked ? '🔓' : '🔒'}
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* Details Modal */}
            {showDetailsModal && selectedAssessment && (
                <ThreatDetailsModal
                    assessment={selectedAssessment}
                    onClose={() => {
                        setShowDetailsModal(false);
                        setSelectedAssessment(null);
                    }}
                />
            )}
        </div>
    );
};

export default ThreatIntelligencePanel;
