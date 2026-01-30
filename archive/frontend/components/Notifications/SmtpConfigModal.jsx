import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import '../../styles/Notifications.css';

const SmtpConfigModal = ({ isOpen, onClose, onSave }) => {
    const [config, setConfig] = useState({
        host: '',
        port: 587,
        username: '',
        password: '',
        fromEmail: '',
        fromName: '',
        enableTls: true,
        enableAuth: true,
    });
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [testing, setTesting] = useState(false);
    const [error, setError] = useState(null);
    const [testStatus, setTestStatus] = useState(null);
    const [showPassword, setShowPassword] = useState(false);

    useEffect(() => {
        if (isOpen) {
            loadConfig();
        }
    }, [isOpen]);

    const loadConfig = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.getSmtpConfig();
            if (response.data) {
                setConfig({
                    ...response.data,
                    password: '', // Don't show password for security
                });
            }
        } catch (err) {
            console.error('Failed to load SMTP config:', err);
            setError(err.message || 'Failed to load SMTP configuration');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (field, value) => {
        setConfig(prev => ({ ...prev, [field]: value }));
        setTestStatus(null);
    };

    const handleTestConnection = async () => {
        setTesting(true);
        setTestStatus(null);
        setError(null);

        try {
            const response = await api.testSmtpConnection(config);
            setTestStatus({
                type: 'success',
                message: response.data.message || 'Connection successful!',
            });
        } catch (err) {
            console.error('Connection test failed:', err);
            setTestStatus({
                type: 'error',
                message: err.message || 'Connection failed. Please check your settings.',
            });
        } finally {
            setTesting(false);
        }
    };

    const handleSave = async () => {
        // Validate required fields
        if (!config.host || !config.port || !config.fromEmail) {
            setError('Please fill in all required fields (Host, Port, From Email)');
            return;
        }

        // Validate email format
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(config.fromEmail)) {
            setError('Please enter a valid email address');
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const response = await api.updateSmtpConfig(config);
            if (onSave) {
                onSave(response.data);
            }
            onClose();
        } catch (err) {
            console.error('Failed to save SMTP config:', err);
            setError(err.message || 'Failed to save SMTP configuration');
        } finally {
            setSaving(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content smtp-config-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h3>SMTP Configuration</h3>
                    <button className="btn-close" onClick={onClose} disabled={saving}>
                        <i className="fas fa-times"></i>
                    </button>
                </div>

                <div className="modal-body">
                    {error && (
                        <div className="alert alert-error">
                            {error}
                        </div>
                    )}

                    {testStatus && (
                        <div className={`alert alert-${testStatus.type}`}>
                            <i className={`fas fa-${testStatus.type === 'success' ? 'check-circle' : 'exclamation-circle'}`}></i>
                            {testStatus.message}
                        </div>
                    )}

                    {loading ? (
                        <div className="loading-spinner">Loading configuration...</div>
                    ) : (
                        <form className="smtp-form">
                            <div className="form-section">
                                <h4>Server Settings</h4>

                                <div className="form-group">
                                    <label htmlFor="host">
                                        SMTP Host <span className="required">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        id="host"
                                        value={config.host}
                                        onChange={(e) => handleChange('host', e.target.value)}
                                        placeholder="smtp.gmail.com"
                                        disabled={saving}
                                        required
                                    />
                                    <span className="field-hint">e.g., smtp.gmail.com, smtp.office365.com</span>
                                </div>

                                <div className="form-group">
                                    <label htmlFor="port">
                                        SMTP Port <span className="required">*</span>
                                    </label>
                                    <input
                                        type="number"
                                        id="port"
                                        value={config.port}
                                        onChange={(e) => handleChange('port', parseInt(e.target.value))}
                                        placeholder="587"
                                        disabled={saving}
                                        required
                                    />
                                    <span className="field-hint">Common ports: 587 (TLS), 465 (SSL), 25 (unencrypted)</span>
                                </div>

                                <div className="form-row">
                                    <div className="form-group checkbox-group">
                                        <label>
                                            <input
                                                type="checkbox"
                                                checked={config.enableTls}
                                                onChange={(e) => handleChange('enableTls', e.target.checked)}
                                                disabled={saving}
                                            />
                                            <span>Enable TLS/SSL</span>
                                        </label>
                                    </div>

                                    <div className="form-group checkbox-group">
                                        <label>
                                            <input
                                                type="checkbox"
                                                checked={config.enableAuth}
                                                onChange={(e) => handleChange('enableAuth', e.target.checked)}
                                                disabled={saving}
                                            />
                                            <span>Enable Authentication</span>
                                        </label>
                                    </div>
                                </div>
                            </div>

                            {config.enableAuth && (
                                <div className="form-section">
                                    <h4>Authentication</h4>

                                    <div className="form-group">
                                        <label htmlFor="username">Username</label>
                                        <input
                                            type="text"
                                            id="username"
                                            value={config.username}
                                            onChange={(e) => handleChange('username', e.target.value)}
                                            placeholder="your-email@example.com"
                                            disabled={saving}
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="password">Password</label>
                                        <div className="password-input-group">
                                            <input
                                                type={showPassword ? 'text' : 'password'}
                                                id="password"
                                                value={config.password}
                                                onChange={(e) => handleChange('password', e.target.value)}
                                                placeholder="Leave empty to keep current password"
                                                disabled={saving}
                                            />
                                            <button
                                                type="button"
                                                className="btn-toggle-password"
                                                onClick={() => setShowPassword(!showPassword)}
                                                disabled={saving}
                                            >
                                                <i className={`fas fa-eye${showPassword ? '-slash' : ''}`}></i>
                                            </button>
                                        </div>
                                        <span className="field-hint">
                                            For Gmail, use an App Password (not your regular password)
                                        </span>
                                    </div>
                                </div>
                            )}

                            <div className="form-section">
                                <h4>Sender Information</h4>

                                <div className="form-group">
                                    <label htmlFor="fromEmail">
                                        From Email <span className="required">*</span>
                                    </label>
                                    <input
                                        type="email"
                                        id="fromEmail"
                                        value={config.fromEmail}
                                        onChange={(e) => handleChange('fromEmail', e.target.value)}
                                        placeholder="noreply@example.com"
                                        disabled={saving}
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="fromName">From Name</label>
                                    <input
                                        type="text"
                                        id="fromName"
                                        value={config.fromName}
                                        onChange={(e) => handleChange('fromName', e.target.value)}
                                        placeholder="Your Application Name"
                                        disabled={saving}
                                    />
                                    <span className="field-hint">
                                        This will be displayed as the sender name
                                    </span>
                                </div>
                            </div>

                            <div className="form-section">
                                <button
                                    type="button"
                                    className="btn-test-connection"
                                    onClick={handleTestConnection}
                                    disabled={testing || saving || !config.host || !config.port}
                                >
                                    {testing ? (
                                        <>
                                            <i className="fas fa-spinner fa-spin"></i>
                                            Testing Connection...
                                        </>
                                    ) : (
                                        <>
                                            <i className="fas fa-plug"></i>
                                            Test Connection
                                        </>
                                    )}
                                </button>
                            </div>
                        </form>
                    )}
                </div>

                <div className="modal-footer">
                    <button
                        className="btn-cancel"
                        onClick={onClose}
                        disabled={saving}
                    >
                        Cancel
                    </button>
                    <button
                        className="btn-save"
                        onClick={handleSave}
                        disabled={saving || loading}
                    >
                        {saving ? 'Saving...' : 'Save Configuration'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default SmtpConfigModal;
