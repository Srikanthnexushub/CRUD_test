import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';
import '../styles/AuditLogs.css';

function AuditLogs() {
    const [auditLogs, setAuditLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    // Filters
    const [filters, setFilters] = useState({
        searchTerm: '',
        eventType: '',
        status: '',
        startDate: '',
        endDate: '',
    });

    const { user, isAdmin } = useAuth();

    useEffect(() => {
        if (!isAdmin()) {
            setError('You do not have permission to view audit logs');
            setLoading(false);
            return;
        }
        loadAuditLogs();
    }, [currentPage, filters]);

    const loadAuditLogs = async () => {
        try {
            setLoading(true);
            setError('');

            const params = {
                page: currentPage,
                size: 20,
                sortBy: 'timestamp',
                sortDirection: 'DESC',
                ...filters,
            };

            const response = await api.getAuditLogs(params);
            setAuditLogs(response.data.content);
            setTotalPages(response.data.totalPages);
            setTotalElements(response.data.totalElements);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load audit logs');
        } finally {
            setLoading(false);
        }
    };

    const handleFilterChange = (e) => {
        const { name, value } = e.target;
        setFilters(prev => ({
            ...prev,
            [name]: value
        }));
        setCurrentPage(0); // Reset to first page when filters change
    };

    const handleSearch = (e) => {
        e.preventDefault();
        setCurrentPage(0);
        loadAuditLogs();
    };

    const clearFilters = () => {
        setFilters({
            searchTerm: '',
            eventType: '',
            status: '',
            startDate: '',
            endDate: '',
        });
        setCurrentPage(0);
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleString();
    };

    const getEventTypeClass = (eventType) => {
        if (eventType.includes('FAILURE') || eventType.includes('DENIED')) return 'event-danger';
        if (eventType.includes('SUCCESS') || eventType.includes('CREATED')) return 'event-success';
        if (eventType.includes('SUSPICIOUS')) return 'event-warning';
        return 'event-info';
    };

    if (!isAdmin()) {
        return (
            <div className="audit-logs-container">
                <div className="error-message">
                    You do not have permission to view audit logs. This feature is only available to administrators.
                </div>
            </div>
        );
    }

    return (
        <div className="audit-logs-container">
            <div className="audit-header">
                <h2>Audit Logs</h2>
                <p className="subtitle">Monitor system activities and security events</p>
            </div>

            <div className="filters-section">
                <form onSubmit={handleSearch} className="filters-form">
                    <div className="filter-row">
                        <input
                            type="text"
                            name="searchTerm"
                            placeholder="Search by action or username..."
                            value={filters.searchTerm}
                            onChange={handleFilterChange}
                            className="filter-input search-input"
                        />

                        <select
                            name="eventType"
                            value={filters.eventType}
                            onChange={handleFilterChange}
                            className="filter-input"
                        >
                            <option value="">All Event Types</option>
                            <option value="LOGIN_SUCCESS">Login Success</option>
                            <option value="LOGIN_FAILURE">Login Failure</option>
                            <option value="LOGOUT">Logout</option>
                            <option value="USER_CREATED">User Created</option>
                            <option value="USER_UPDATED">User Updated</option>
                            <option value="USER_DELETED">User Deleted</option>
                            <option value="USER_VIEWED">User Viewed</option>
                            <option value="ACCESS_DENIED">Access Denied</option>
                            <option value="SUSPICIOUS_ACTIVITY">Suspicious Activity</option>
                        </select>

                        <select
                            name="status"
                            value={filters.status}
                            onChange={handleFilterChange}
                            className="filter-input"
                        >
                            <option value="">All Statuses</option>
                            <option value="SUCCESS">Success</option>
                            <option value="FAILURE">Failure</option>
                            <option value="WARNING">Warning</option>
                            <option value="ERROR">Error</option>
                        </select>
                    </div>

                    <div className="filter-row">
                        <input
                            type="datetime-local"
                            name="startDate"
                            value={filters.startDate}
                            onChange={handleFilterChange}
                            className="filter-input"
                            placeholder="Start Date"
                        />

                        <input
                            type="datetime-local"
                            name="endDate"
                            value={filters.endDate}
                            onChange={handleFilterChange}
                            className="filter-input"
                            placeholder="End Date"
                        />

                        <button type="submit" className="btn-search">
                            Search
                        </button>

                        <button type="button" onClick={clearFilters} className="btn-clear">
                            Clear
                        </button>
                    </div>
                </form>

                <div className="results-info">
                    Total: {totalElements} records
                </div>
            </div>

            {error && (
                <div className="error-message">{error}</div>
            )}

            {loading ? (
                <div className="loading">Loading audit logs...</div>
            ) : (
                <>
                    <div className="audit-table-container">
                        <table className="audit-table">
                            <thead>
                                <tr>
                                    <th>Timestamp</th>
                                    <th>User</th>
                                    <th>Event Type</th>
                                    <th>Action</th>
                                    <th>Status</th>
                                    <th>IP Address</th>
                                    <th>Details</th>
                                </tr>
                            </thead>
                            <tbody>
                                {auditLogs.length === 0 ? (
                                    <tr>
                                        <td colSpan="7" className="no-data">
                                            No audit logs found
                                        </td>
                                    </tr>
                                ) : (
                                    auditLogs.map((log) => (
                                        <tr key={log.id}>
                                            <td className="timestamp">
                                                {formatDate(log.timestamp)}
                                            </td>
                                            <td>
                                                <div className="user-info">
                                                    <strong>{log.username || 'System'}</strong>
                                                    {log.userRole && (
                                                        <span className="role-tag">{log.userRole}</span>
                                                    )}
                                                </div>
                                            </td>
                                            <td>
                                                <span className={`event-badge ${getEventTypeClass(log.eventType)}`}>
                                                    {log.eventType.replace(/_/g, ' ')}
                                                </span>
                                            </td>
                                            <td className="action-cell">{log.action}</td>
                                            <td>
                                                <span className={`status-badge status-${log.status.toLowerCase()}`}>
                                                    {log.status}
                                                </span>
                                            </td>
                                            <td className="ip-cell">{log.ipAddress || '-'}</td>
                                            <td className="details-cell">
                                                {log.details && (
                                                    <details>
                                                        <summary>View</summary>
                                                        <div className="details-content">
                                                            {log.details}
                                                            {log.errorMessage && (
                                                                <div className="error-detail">
                                                                    Error: {log.errorMessage}
                                                                </div>
                                                            )}
                                                        </div>
                                                    </details>
                                                )}
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>

                    {totalPages > 1 && (
                        <div className="pagination">
                            <button
                                onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                                disabled={currentPage === 0}
                                className="pagination-btn"
                            >
                                Previous
                            </button>

                            <span className="page-info">
                                Page {currentPage + 1} of {totalPages}
                            </span>

                            <button
                                onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                                disabled={currentPage === totalPages - 1}
                                className="pagination-btn"
                            >
                                Next
                            </button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

export default AuditLogs;
