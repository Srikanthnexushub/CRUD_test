import React, { createContext, useState, useContext, useEffect } from 'react';
import api from '../services/api';

const NotificationContext = createContext(null);

export const NotificationProvider = ({ children }) => {
    const [preferences, setPreferences] = useState({
        emailOnUserCreated: true,
        emailOnUserUpdated: true,
        emailOnUserDeleted: true,
        emailOnSecurityEvent: true,
        emailOnSystemError: true,
        dailyDigestEnabled: false,
        digestTime: '09:00'
    });
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // Load preferences on mount
    useEffect(() => {
        loadPreferences();
    }, []);

    const loadPreferences = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.getNotificationPreferences();
            if (response.data) {
                setPreferences(response.data);
            }
        } catch (err) {
            console.error('Failed to load notification preferences:', err);
            setError(err.message || 'Failed to load preferences');
        } finally {
            setLoading(false);
        }
    };

    const updatePreferences = async (newPreferences) => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.updateNotificationPreferences(newPreferences);
            setPreferences(response.data);
            return { success: true, data: response.data };
        } catch (err) {
            console.error('Failed to update preferences:', err);
            setError(err.message || 'Failed to update preferences');
            return { success: false, error: err.message };
        } finally {
            setLoading(false);
        }
    };

    const sendTestEmail = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.sendTestEmail();
            return { success: true, message: response.data.message || 'Test email sent successfully' };
        } catch (err) {
            console.error('Failed to send test email:', err);
            setError(err.message || 'Failed to send test email');
            return { success: false, error: err.message };
        } finally {
            setLoading(false);
        }
    };

    const loadUnreadCount = async () => {
        try {
            const response = await api.getUnreadNotificationCount();
            setUnreadCount(response.data.count || 0);
        } catch (err) {
            console.error('Failed to load unread count:', err);
        }
    };

    const markAsRead = async (notificationId) => {
        try {
            await api.markNotificationAsRead(notificationId);
            await loadUnreadCount();
            return { success: true };
        } catch (err) {
            console.error('Failed to mark notification as read:', err);
            return { success: false, error: err.message };
        }
    };

    const value = {
        preferences,
        unreadCount,
        loading,
        error,
        updatePreferences,
        sendTestEmail,
        loadPreferences,
        loadUnreadCount,
        markAsRead,
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
};

export const useNotifications = () => {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotifications must be used within a NotificationProvider');
    }
    return context;
};
