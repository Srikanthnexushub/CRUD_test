import React, { useState, useEffect } from 'react';
import { useNotifications } from '../../contexts/NotificationContext';
import '../../styles/Notifications.css';

const NotificationPreferences = () => {
    const { preferences, loading, error, updatePreferences, sendTestEmail } = useNotifications();
    const [localPreferences, setLocalPreferences] = useState(preferences);
    const [saveStatus, setSaveStatus] = useState(null);
    const [testEmailStatus, setTestEmailStatus] = useState(null);
    const [isSaving, setIsSaving] = useState(false);
    const [isSendingTest, setIsSendingTest] = useState(false);

    useEffect(() => {
        setLocalPreferences(preferences);
    }, [preferences]);

    const handleToggle = (field) => {
        setLocalPreferences(prev => ({
            ...prev,
            [field]: !prev[field]
        }));
    };

    const handleTimeChange = (e) => {
        setLocalPreferences(prev => ({
            ...prev,
            digestTime: e.target.value
        }));
    };

    const handleSave = async () => {
        setIsSaving(true);
        setSaveStatus(null);

        const result = await updatePreferences(localPreferences);

        if (result.success) {
            setSaveStatus({ type: 'success', message: 'Preferences saved successfully!' });
        } else {
            setSaveStatus({ type: 'error', message: result.error || 'Failed to save preferences' });
        }

        setIsSaving(false);

        // Clear status after 3 seconds
        setTimeout(() => setSaveStatus(null), 3000);
    };

    const handleTestEmail = async () => {
        setIsSendingTest(true);
        setTestEmailStatus(null);

        const result = await sendTestEmail();

        if (result.success) {
            setTestEmailStatus({ type: 'success', message: result.message });
        } else {
            setTestEmailStatus({ type: 'error', message: result.error || 'Failed to send test email' });
        }

        setIsSendingTest(false);

        // Clear status after 5 seconds
        setTimeout(() => setTestEmailStatus(null), 5000);
    };

    const hasChanges = JSON.stringify(preferences) !== JSON.stringify(localPreferences);

    return (
        <div className="notification-preferences">
            <div className="preferences-header">
                <h2>Notification Preferences</h2>
                <p className="preferences-subtitle">Manage your email notification settings</p>
            </div>

            {error && (
                <div className="alert alert-error">
                    {error}
                </div>
            )}

            <div className="preferences-content">
                <section className="preferences-section">
                    <h3>User Activity Notifications</h3>
                    <p className="section-description">Receive emails when users perform actions</p>

                    <div className="preference-item">
                        <div className="preference-info">
                            <label htmlFor="emailOnUserCreated">User Created</label>
                            <span className="preference-description">
                                Get notified when a new user is registered
                            </span>
                        </div>
                        <label className="toggle-switch">
                            <input
                                type="checkbox"
                                id="emailOnUserCreated"
                                checked={localPreferences.emailOnUserCreated}
                                onChange={() => handleToggle('emailOnUserCreated')}
                                disabled={loading}
                            />
                            <span className="toggle-slider"></span>
                        </label>
                    </div>

                    <div className="preference-item">
                        <div className="preference-info">
                            <label htmlFor="emailOnUserUpdated">User Updated</label>
                            <span className="preference-description">
                                Get notified when user information is modified
                            </span>
                        </div>
                        <label className="toggle-switch">
                            <input
                                type="checkbox"
                                id="emailOnUserUpdated"
                                checked={localPreferences.emailOnUserUpdated}
                                onChange={() => handleToggle('emailOnUserUpdated')}
                                disabled={loading}
                            />
                            <span className="toggle-slider"></span>
                        </label>
                    </div>

                    <div className="preference-item">
                        <div className="preference-info">
                            <label htmlFor="emailOnUserDeleted">User Deleted</label>
                            <span className="preference-description">
                                Get notified when a user account is removed
                            </span>
                        </div>
                        <label className="toggle-switch">
                            <input
                                type="checkbox"
                                id="emailOnUserDeleted"
                                checked={localPreferences.emailOnUserDeleted}
                                onChange={() => handleToggle('emailOnUserDeleted')}
                                disabled={loading}
                            />
                            <span className="toggle-slider"></span>
                        </label>
                    </div>
                </section>

                <section className="preferences-section">
                    <h3>System Notifications</h3>
                    <p className="section-description">Critical alerts and system events</p>

                    <div className="preference-item">
                        <div className="preference-info">
                            <label htmlFor="emailOnSecurityEvent">Security Events</label>
                            <span className="preference-description">
                                Get notified about failed login attempts and security issues
                            </span>
                        </div>
                        <label className="toggle-switch">
                            <input
                                type="checkbox"
                                id="emailOnSecurityEvent"
                                checked={localPreferences.emailOnSecurityEvent}
                                onChange={() => handleToggle('emailOnSecurityEvent')}
                                disabled={loading}
                            />
                            <span className="toggle-slider"></span>
                        </label>
                    </div>

                    <div className="preference-item">
                        <div className="preference-info">
                            <label htmlFor="emailOnSystemError">System Errors</label>
                            <span className="preference-description">
                                Get notified about application errors and failures
                            </span>
                        </div>
                        <label className="toggle-switch">
                            <input
                                type="checkbox"
                                id="emailOnSystemError"
                                checked={localPreferences.emailOnSystemError}
                                onChange={() => handleToggle('emailOnSystemError')}
                                disabled={loading}
                            />
                            <span className="toggle-slider"></span>
                        </label>
                    </div>
                </section>

                <section className="preferences-section">
                    <h3>Daily Digest</h3>
                    <p className="section-description">Receive a summary of daily activities</p>

                    <div className="preference-item">
                        <div className="preference-info">
                            <label htmlFor="dailyDigestEnabled">Enable Daily Digest</label>
                            <span className="preference-description">
                                Get a daily summary email with all activities
                            </span>
                        </div>
                        <label className="toggle-switch">
                            <input
                                type="checkbox"
                                id="dailyDigestEnabled"
                                checked={localPreferences.dailyDigestEnabled}
                                onChange={() => handleToggle('dailyDigestEnabled')}
                                disabled={loading}
                            />
                            <span className="toggle-slider"></span>
                        </label>
                    </div>

                    {localPreferences.dailyDigestEnabled && (
                        <div className="preference-item">
                            <div className="preference-info">
                                <label htmlFor="digestTime">Delivery Time</label>
                                <span className="preference-description">
                                    Choose when to receive your daily digest
                                </span>
                            </div>
                            <input
                                type="time"
                                id="digestTime"
                                value={localPreferences.digestTime}
                                onChange={handleTimeChange}
                                disabled={loading}
                                className="time-input"
                            />
                        </div>
                    )}
                </section>

                <section className="preferences-section">
                    <h3>Test Email</h3>
                    <p className="section-description">Send a test email to verify your configuration</p>

                    <button
                        className="btn-test-email"
                        onClick={handleTestEmail}
                        disabled={isSendingTest || loading}
                    >
                        {isSendingTest ? 'Sending...' : 'Send Test Email'}
                    </button>

                    {testEmailStatus && (
                        <div className={`alert alert-${testEmailStatus.type}`}>
                            {testEmailStatus.message}
                        </div>
                    )}
                </section>
            </div>

            <div className="preferences-footer">
                {saveStatus && (
                    <div className={`save-status save-status-${saveStatus.type}`}>
                        {saveStatus.message}
                    </div>
                )}
                <button
                    className="btn-save"
                    onClick={handleSave}
                    disabled={!hasChanges || isSaving || loading}
                >
                    {isSaving ? 'Saving...' : 'Save Preferences'}
                </button>
            </div>
        </div>
    );
};

export default NotificationPreferences;
