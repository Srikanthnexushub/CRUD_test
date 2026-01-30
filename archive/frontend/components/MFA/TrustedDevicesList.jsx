import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import '../../styles/MFA.css';

function TrustedDevicesList() {
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [removingDevice, setRemovingDevice] = useState(null);

    useEffect(() => {
        loadDevices();
    }, []);

    const loadDevices = async () => {
        try {
            setLoading(true);
            setError('');
            const response = await api.getTrustedDevices();
            setDevices(response.data || []);
        } catch (err) {
            setError(err.message || 'Failed to load trusted devices');
        } finally {
            setLoading(false);
        }
    };

    const handleRemoveDevice = async (deviceId) => {
        try {
            setError('');
            await api.removeTrustedDevice(deviceId);
            setRemovingDevice(null);
            loadDevices();
        } catch (err) {
            setError(err.message || 'Failed to remove device');
        }
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const isExpiringSoon = (expirationDate) => {
        const expiration = new Date(expirationDate);
        const now = new Date();
        const daysUntilExpiration = Math.ceil((expiration - now) / (1000 * 60 * 60 * 24));
        return daysUntilExpiration <= 7 && daysUntilExpiration > 0;
    };

    const isExpired = (expirationDate) => {
        return new Date(expirationDate) < new Date();
    };

    const getDeviceIcon = (deviceInfo) => {
        const info = deviceInfo?.toLowerCase() || '';
        if (info.includes('mobile') || info.includes('android') || info.includes('iphone')) {
            return '📱';
        } else if (info.includes('tablet') || info.includes('ipad')) {
            return '📱';
        } else {
            return '💻';
        }
    };

    if (loading) {
        return <div className="loading-text">Loading trusted devices...</div>;
    }

    if (error) {
        return <div className="error-message">{error}</div>;
    }

    if (devices.length === 0) {
        return (
            <div className="empty-state">
                <p>No trusted devices found.</p>
                <p className="empty-state-hint">
                    When you log in with MFA enabled, you can choose to trust a device for 30 days.
                </p>
            </div>
        );
    }

    return (
        <div className="trusted-devices-list">
            {devices.map((device) => (
                <div
                    key={device.id}
                    className={`device-card ${isExpired(device.expiresAt) ? 'expired' : ''}`}
                >
                    <div className="device-card-header">
                        <div className="device-icon">{getDeviceIcon(device.deviceInfo)}</div>
                        <div className="device-info">
                            <h4 className="device-name">
                                {device.deviceName || 'Unknown Device'}
                            </h4>
                            <p className="device-details">
                                {device.ipAddress && (
                                    <span className="device-ip">IP: {device.ipAddress}</span>
                                )}
                                {device.location && (
                                    <span className="device-location"> • {device.location}</span>
                                )}
                            </p>
                        </div>
                    </div>

                    <div className="device-card-body">
                        <div className="device-metadata">
                            <div className="metadata-item">
                                <span className="metadata-label">Last Used:</span>
                                <span className="metadata-value">
                                    {device.lastUsedAt ? formatDate(device.lastUsedAt) : 'Never'}
                                </span>
                            </div>

                            <div className="metadata-item">
                                <span className="metadata-label">Added:</span>
                                <span className="metadata-value">
                                    {formatDate(device.createdAt)}
                                </span>
                            </div>

                            <div className="metadata-item">
                                <span className="metadata-label">Expires:</span>
                                <span className={`metadata-value ${isExpiringSoon(device.expiresAt) ? 'expiring-soon' : ''} ${isExpired(device.expiresAt) ? 'expired' : ''}`}>
                                    {formatDate(device.expiresAt)}
                                    {isExpired(device.expiresAt) && ' (Expired)'}
                                    {isExpiringSoon(device.expiresAt) && ' (Expiring Soon)'}
                                </span>
                            </div>
                        </div>

                        {device.deviceInfo && (
                            <div className="device-fingerprint">
                                <details>
                                    <summary>Device Details</summary>
                                    <pre className="fingerprint-data">{device.deviceInfo}</pre>
                                </details>
                            </div>
                        )}
                    </div>

                    <div className="device-card-actions">
                        <button
                            type="button"
                            className="btn-remove-device"
                            onClick={() => setRemovingDevice(device)}
                        >
                            Remove
                        </button>
                    </div>
                </div>
            ))}

            {/* Remove Device Confirmation Modal */}
            {removingDevice && (
                <div className="modal-overlay" onClick={() => setRemovingDevice(null)}>
                    <div className="modal-content confirm-dialog" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Remove Trusted Device</h3>
                        </div>
                        <div className="modal-body">
                            <p>
                                Are you sure you want to remove this trusted device?
                            </p>
                            <div className="device-info-summary">
                                <strong>{removingDevice.deviceName || 'Unknown Device'}</strong>
                                {removingDevice.ipAddress && (
                                    <div>IP: {removingDevice.ipAddress}</div>
                                )}
                            </div>
                            <p className="warning-text">
                                You will need to enter an MFA code the next time you log in from this device.
                            </p>
                        </div>
                        <div className="modal-actions">
                            <button
                                type="button"
                                className="btn-danger"
                                onClick={() => handleRemoveDevice(removingDevice.id)}
                            >
                                Remove Device
                            </button>
                            <button
                                type="button"
                                className="btn-secondary"
                                onClick={() => setRemovingDevice(null)}
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default TrustedDevicesList;
