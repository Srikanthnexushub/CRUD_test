import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import '../../styles/RateLimit.css';

const RateLimitConfigModal = ({ isOpen, onClose, onSuccess }) => {
    const [formData, setFormData] = useState({
        type: 'IP_ADDRESS',
        identifier: '',
        expiresAt: '',
        reason: '',
    });
    const [whitelists, setWhitelists] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    useEffect(() => {
        if (isOpen) {
            fetchWhitelists();
        }
    }, [isOpen]);

    const fetchWhitelists = async () => {
        try {
            const response = await api.getActiveWhitelists();
            setWhitelists(response.data);
        } catch (err) {
            console.error('Failed to fetch whitelists:', err);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(null);

        try {
            // Validate form
            if (!formData.identifier.trim()) {
                throw new Error('Identifier is required');
            }
            if (!formData.reason.trim()) {
                throw new Error('Reason is required');
            }

            // Format expiration date
            const expiresAt = formData.expiresAt
                ? new Date(formData.expiresAt).toISOString()
                : null;

            const payload = {
                type: formData.type,
                identifier: formData.identifier.trim(),
                expiresAt,
                reason: formData.reason.trim(),
            };

            await api.addWhitelist(payload);

            setSuccess('Whitelist entry added successfully');
            setFormData({
                type: 'IP_ADDRESS',
                identifier: '',
                expiresAt: '',
                reason: '',
            });

            // Refresh whitelist table
            await fetchWhitelists();

            if (onSuccess) {
                onSuccess();
            }

            // Clear success message after 3s
            setTimeout(() => setSuccess(null), 3000);
        } catch (err) {
            setError(err.message || 'Failed to add whitelist entry');
        } finally {
            setLoading(false);
        }
    };

    const handleRemove = async (id) => {
        if (!confirm('Are you sure you want to remove this whitelist entry?')) {
            return;
        }

        try {
            await api.removeWhitelist(id);
            setSuccess('Whitelist entry removed successfully');
            await fetchWhitelists();

            if (onSuccess) {
                onSuccess();
            }

            setTimeout(() => setSuccess(null), 3000);
        } catch (err) {
            setError('Failed to remove whitelist entry');
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'Never';
        return new Date(dateString).toLocaleString();
    };

    // Get minimum date for expiration (today)
    const getMinDate = () => {
        const today = new Date();
        return today.toISOString().split('T')[0];
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="rate-limit-config-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Rate Limit Configuration</h2>
                    <button className="modal-close" onClick={onClose}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M6 18L18 6M6 6l12 12" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                        </svg>
                    </button>
                </div>

                <div className="modal-body">
                    {error && (
                        <div className="alert alert-error">
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd"/>
                            </svg>
                            {error}
                        </div>
                    )}

                    {success && (
                        <div className="alert alert-success">
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd"/>
                            </svg>
                            {success}
                        </div>
                    )}

                    {/* Add Whitelist Form */}
                    <form onSubmit={handleSubmit} className="whitelist-form">
                        <h3>Add Whitelist Entry</h3>

                        <div className="form-row">
                            <div className="form-group">
                                <label htmlFor="type">Type</label>
                                <select
                                    id="type"
                                    name="type"
                                    value={formData.type}
                                    onChange={handleInputChange}
                                    required
                                >
                                    <option value="IP_ADDRESS">IP Address</option>
                                    <option value="USER_ID">User ID</option>
                                </select>
                            </div>

                            <div className="form-group">
                                <label htmlFor="identifier">
                                    {formData.type === 'IP_ADDRESS' ? 'IP Address' : 'User ID'}
                                </label>
                                <input
                                    type="text"
                                    id="identifier"
                                    name="identifier"
                                    value={formData.identifier}
                                    onChange={handleInputChange}
                                    placeholder={formData.type === 'IP_ADDRESS' ? '192.168.1.1' : 'user123'}
                                    required
                                />
                            </div>
                        </div>

                        <div className="form-group">
                            <label htmlFor="expiresAt">Expiration Date (Optional)</label>
                            <input
                                type="datetime-local"
                                id="expiresAt"
                                name="expiresAt"
                                value={formData.expiresAt}
                                onChange={handleInputChange}
                                min={getMinDate()}
                            />
                            <small>Leave empty for permanent whitelist</small>
                        </div>

                        <div className="form-group">
                            <label htmlFor="reason">Reason</label>
                            <textarea
                                id="reason"
                                name="reason"
                                value={formData.reason}
                                onChange={handleInputChange}
                                placeholder="Enter the reason for whitelisting..."
                                rows="3"
                                required
                            ></textarea>
                        </div>

                        <button type="submit" className="btn btn-primary" disabled={loading}>
                            {loading ? 'Adding...' : 'Add Whitelist'}
                        </button>
                    </form>

                    {/* Active Whitelists Table */}
                    <div className="whitelist-table-section">
                        <h3>Active Whitelists</h3>
                        {whitelists.length === 0 ? (
                            <p className="empty-state">No active whitelists</p>
                        ) : (
                            <div className="table-container">
                                <table className="whitelist-table">
                                    <thead>
                                        <tr>
                                            <th>Type</th>
                                            <th>Identifier</th>
                                            <th>Reason</th>
                                            <th>Created</th>
                                            <th>Expires</th>
                                            <th>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {whitelists.map((entry) => (
                                            <tr key={entry.id}>
                                                <td>
                                                    <span className={`badge ${entry.type.toLowerCase()}`}>
                                                        {entry.type.replace('_', ' ')}
                                                    </span>
                                                </td>
                                                <td><code>{entry.identifier}</code></td>
                                                <td>{entry.reason}</td>
                                                <td>{formatDate(entry.createdAt)}</td>
                                                <td>
                                                    <span className={entry.expiresAt ? '' : 'permanent'}>
                                                        {formatDate(entry.expiresAt)}
                                                    </span>
                                                </td>
                                                <td>
                                                    <button
                                                        className="btn btn-danger btn-sm"
                                                        onClick={() => handleRemove(entry.id)}
                                                    >
                                                        Remove
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </div>

                <div className="modal-footer">
                    <button className="btn btn-secondary" onClick={onClose}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default RateLimitConfigModal;
