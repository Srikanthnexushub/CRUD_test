import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import '../../styles/Notifications.css';

const EmailLogTable = () => {
    const [emails, setEmails] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [filters, setFilters] = useState({
        status: '',
        search: '',
    });
    const [pagination, setPagination] = useState({
        page: 0,
        size: 20,
        totalPages: 0,
        totalElements: 0,
    });
    const [sorting, setSorting] = useState({
        sortBy: 'sentAt',
        sortDirection: 'desc',
    });
    const [selectedEmail, setSelectedEmail] = useState(null);
    const [showTemplateModal, setShowTemplateModal] = useState(false);
    const [retryingEmail, setRetryingEmail] = useState(null);

    useEffect(() => {
        loadEmails();
    }, [filters, pagination.page, pagination.size, sorting]);

    const loadEmails = async () => {
        try {
            setLoading(true);
            setError(null);

            const params = {
                page: pagination.page,
                size: pagination.size,
                sortBy: sorting.sortBy,
                sortDirection: sorting.sortDirection,
            };

            if (filters.status) {
                params.status = filters.status;
            }
            if (filters.search) {
                params.search = filters.search;
            }

            const response = await api.getEmailLogs(params);
            setEmails(response.data.content || []);
            setPagination(prev => ({
                ...prev,
                totalPages: response.data.totalPages || 0,
                totalElements: response.data.totalElements || 0,
            }));
        } catch (err) {
            console.error('Failed to load email logs:', err);
            setError(err.message || 'Failed to load email logs');
        } finally {
            setLoading(false);
        }
    };

    const handleFilterChange = (field, value) => {
        setFilters(prev => ({ ...prev, [field]: value }));
        setPagination(prev => ({ ...prev, page: 0 })); // Reset to first page
    };

    const handleSort = (field) => {
        setSorting(prev => ({
            sortBy: field,
            sortDirection: prev.sortBy === field && prev.sortDirection === 'asc' ? 'desc' : 'asc',
        }));
    };

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < pagination.totalPages) {
            setPagination(prev => ({ ...prev, page: newPage }));
        }
    };

    const handleViewTemplate = (email) => {
        setSelectedEmail(email);
        setShowTemplateModal(true);
    };

    const handleRetry = async (emailId) => {
        if (!window.confirm('Are you sure you want to retry sending this email?')) {
            return;
        }

        setRetryingEmail(emailId);

        try {
            await api.retryEmail(emailId);
            await loadEmails();
        } catch (err) {
            console.error('Failed to retry email:', err);
            alert(err.message || 'Failed to retry email');
        } finally {
            setRetryingEmail(null);
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

    const getSortIcon = (field) => {
        if (sorting.sortBy !== field) {
            return <i className="fas fa-sort"></i>;
        }
        return sorting.sortDirection === 'asc'
            ? <i className="fas fa-sort-up"></i>
            : <i className="fas fa-sort-down"></i>;
    };

    return (
        <div className="email-log-table">
            <div className="table-header">
                <h2>Email History</h2>
                <div className="table-actions">
                    <select
                        value={filters.status}
                        onChange={(e) => handleFilterChange('status', e.target.value)}
                        className="filter-select"
                    >
                        <option value="">All Status</option>
                        <option value="PENDING">Pending</option>
                        <option value="SENT">Sent</option>
                        <option value="FAILED">Failed</option>
                    </select>

                    <input
                        type="text"
                        placeholder="Search by recipient or subject..."
                        value={filters.search}
                        onChange={(e) => handleFilterChange('search', e.target.value)}
                        className="search-input"
                    />

                    <button
                        className="btn-refresh"
                        onClick={loadEmails}
                        disabled={loading}
                    >
                        <i className="fas fa-sync-alt"></i>
                        {loading ? ' Loading...' : ' Refresh'}
                    </button>
                </div>
            </div>

            {error && (
                <div className="alert alert-error">
                    {error}
                </div>
            )}

            <div className="table-container">
                <table className="email-table">
                    <thead>
                        <tr>
                            <th onClick={() => handleSort('recipientEmail')} className="sortable">
                                Recipient {getSortIcon('recipientEmail')}
                            </th>
                            <th onClick={() => handleSort('subject')} className="sortable">
                                Subject {getSortIcon('subject')}
                            </th>
                            <th onClick={() => handleSort('status')} className="sortable">
                                Status {getSortIcon('status')}
                            </th>
                            <th onClick={() => handleSort('sentAt')} className="sortable">
                                Sent At {getSortIcon('sentAt')}
                            </th>
                            <th>Attempts</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading && emails.length === 0 ? (
                            <tr>
                                <td colSpan="6" className="loading-cell">
                                    <div className="loading-spinner">Loading emails...</div>
                                </td>
                            </tr>
                        ) : emails.length === 0 ? (
                            <tr>
                                <td colSpan="6" className="empty-cell">
                                    <div className="empty-state">
                                        <i className="fas fa-inbox"></i>
                                        <p>No emails found</p>
                                    </div>
                                </td>
                            </tr>
                        ) : (
                            emails.map((email) => (
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
                                        {email.errorMessage && (
                                            <span
                                                className="error-tooltip"
                                                title={email.errorMessage}
                                            >
                                                <i className="fas fa-exclamation-triangle"></i>
                                            </span>
                                        )}
                                    </td>
                                    <td className="action-buttons">
                                        <button
                                            className="btn-icon btn-view"
                                            onClick={() => handleViewTemplate(email)}
                                            title="View Template"
                                        >
                                            <i className="fas fa-eye"></i>
                                        </button>
                                        {email.status === 'FAILED' && (
                                            <button
                                                className="btn-icon btn-retry"
                                                onClick={() => handleRetry(email.id)}
                                                disabled={retryingEmail === email.id}
                                                title="Retry"
                                            >
                                                <i className={`fas fa-redo ${retryingEmail === email.id ? 'fa-spin' : ''}`}></i>
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {pagination.totalPages > 1 && (
                <div className="pagination">
                    <div className="pagination-info">
                        Showing {pagination.page * pagination.size + 1} to{' '}
                        {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of{' '}
                        {pagination.totalElements} emails
                    </div>
                    <div className="pagination-controls">
                        <button
                            onClick={() => handlePageChange(0)}
                            disabled={pagination.page === 0}
                            className="btn-page"
                        >
                            <i className="fas fa-angle-double-left"></i>
                        </button>
                        <button
                            onClick={() => handlePageChange(pagination.page - 1)}
                            disabled={pagination.page === 0}
                            className="btn-page"
                        >
                            <i className="fas fa-angle-left"></i>
                        </button>
                        <span className="page-numbers">
                            Page {pagination.page + 1} of {pagination.totalPages}
                        </span>
                        <button
                            onClick={() => handlePageChange(pagination.page + 1)}
                            disabled={pagination.page >= pagination.totalPages - 1}
                            className="btn-page"
                        >
                            <i className="fas fa-angle-right"></i>
                        </button>
                        <button
                            onClick={() => handlePageChange(pagination.totalPages - 1)}
                            disabled={pagination.page >= pagination.totalPages - 1}
                            className="btn-page"
                        >
                            <i className="fas fa-angle-double-right"></i>
                        </button>
                    </div>
                </div>
            )}

            {showTemplateModal && selectedEmail && (
                <div className="modal-overlay" onClick={() => setShowTemplateModal(false)}>
                    <div className="modal-content template-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Email Template</h3>
                            <button
                                className="btn-close"
                                onClick={() => setShowTemplateModal(false)}
                            >
                                <i className="fas fa-times"></i>
                            </button>
                        </div>
                        <div className="modal-body">
                            <div className="email-details">
                                <div className="detail-row">
                                    <strong>To:</strong> {selectedEmail.recipientEmail}
                                </div>
                                <div className="detail-row">
                                    <strong>Subject:</strong> {selectedEmail.subject}
                                </div>
                                <div className="detail-row">
                                    <strong>Status:</strong>{' '}
                                    <span className={`status-badge ${getStatusColor(selectedEmail.status)}`}>
                                        {selectedEmail.status}
                                    </span>
                                </div>
                                <div className="detail-row">
                                    <strong>Sent At:</strong> {formatDate(selectedEmail.sentAt)}
                                </div>
                            </div>
                            <div className="email-body-preview">
                                <h4>Email Body:</h4>
                                <iframe
                                    srcDoc={selectedEmail.body}
                                    title="Email Preview"
                                    className="email-preview-iframe"
                                />
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default EmailLogTable;
