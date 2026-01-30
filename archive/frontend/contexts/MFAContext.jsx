import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
import api from '../services/api';
import { useAuth } from './AuthContext';

const MFAContext = createContext(null);

export const MFAProvider = ({ children }) => {
    const [mfaRequired, setMfaRequired] = useState(false);
    const [mfaEnabled, setMfaEnabled] = useState(false);
    const [backupCodesRemaining, setBackupCodesRemaining] = useState(0);
    const [trustDeviceToken, setTrustDeviceToken] = useState(null);
    const [loading, setLoading] = useState(false);
    const [pendingCredentials, setPendingCredentials] = useState(null);

    const { isAuthenticated, token } = useAuth();

    // Load MFA status when user is authenticated
    useEffect(() => {
        if (isAuthenticated() && token) {
            loadMFAStatus();
        }
    }, [token, isAuthenticated]);

    const loadMFAStatus = async () => {
        try {
            const response = await api.getMFAStatus();
            setMfaEnabled(response.data.enabled);
            setBackupCodesRemaining(response.data.backupCodesRemaining || 0);
        } catch (error) {
            console.error('Failed to load MFA status:', error);
        }
    };

    const setupMFA = async () => {
        try {
            setLoading(true);
            const response = await api.setupMFA();
            return {
                success: true,
                data: {
                    secret: response.data.secret,
                    qrCodeUrl: response.data.qrCodeUrl,
                    backupCodes: response.data.backupCodes
                }
            };
        } catch (error) {
            return {
                success: false,
                error: error.message || 'Failed to setup MFA'
            };
        } finally {
            setLoading(false);
        }
    };

    const verifyMFASetup = async (code) => {
        try {
            setLoading(true);
            const response = await api.verifyMFASetup(code);
            if (response.data.success) {
                setMfaEnabled(true);
                setBackupCodesRemaining(response.data.backupCodesRemaining || 10);
                await loadMFAStatus();
            }
            return {
                success: true,
                data: response.data
            };
        } catch (error) {
            return {
                success: false,
                error: error.message || 'Invalid verification code'
            };
        } finally {
            setLoading(false);
        }
    };

    const verifyMFACode = async (code, trustDevice = false) => {
        try {
            setLoading(true);
            const response = await api.verifyMFALogin(code, trustDevice);

            if (response.data.token) {
                if (trustDevice && response.data.trustToken) {
                    setTrustDeviceToken(response.data.trustToken);
                    localStorage.setItem('trustDeviceToken', response.data.trustToken);
                }
                setMfaRequired(false);
                return {
                    success: true,
                    token: response.data.token,
                    user: response.data.user
                };
            }

            return {
                success: false,
                error: 'Verification failed'
            };
        } catch (error) {
            return {
                success: false,
                error: error.message || 'Invalid MFA code'
            };
        } finally {
            setLoading(false);
        }
    };

    const verifyBackupCode = async (code) => {
        try {
            setLoading(true);
            const response = await api.verifyBackupCode(code);

            if (response.data.token) {
                setMfaRequired(false);
                setBackupCodesRemaining(prev => Math.max(0, prev - 1));
                return {
                    success: true,
                    token: response.data.token,
                    user: response.data.user,
                    codesRemaining: response.data.backupCodesRemaining
                };
            }

            return {
                success: false,
                error: 'Verification failed'
            };
        } catch (error) {
            return {
                success: false,
                error: error.message || 'Invalid backup code'
            };
        } finally {
            setLoading(false);
        }
    };

    const disableMFA = async () => {
        try {
            setLoading(true);
            await api.disableMFA();
            setMfaEnabled(false);
            setBackupCodesRemaining(0);
            return { success: true };
        } catch (error) {
            return {
                success: false,
                error: error.message || 'Failed to disable MFA'
            };
        } finally {
            setLoading(false);
        }
    };

    const regenerateBackupCodes = async () => {
        try {
            setLoading(true);
            const response = await api.regenerateBackupCodes();
            setBackupCodesRemaining(response.data.backupCodes?.length || 10);
            return {
                success: true,
                backupCodes: response.data.backupCodes
            };
        } catch (error) {
            return {
                success: false,
                error: error.message || 'Failed to regenerate backup codes'
            };
        } finally {
            setLoading(false);
        }
    };

    const requireMFA = (credentials) => {
        setMfaRequired(true);
        setPendingCredentials(credentials);
    };

    const clearMFARequirement = () => {
        setMfaRequired(false);
        setPendingCredentials(null);
    };

    const value = {
        mfaRequired,
        mfaEnabled,
        backupCodesRemaining,
        trustDeviceToken,
        loading,
        pendingCredentials,
        setupMFA,
        verifyMFASetup,
        verifyMFACode,
        verifyBackupCode,
        disableMFA,
        regenerateBackupCodes,
        requireMFA,
        clearMFARequirement,
        loadMFAStatus
    };

    return <MFAContext.Provider value={value}>{children}</MFAContext.Provider>;
};

export const useMFA = () => {
    const context = useContext(MFAContext);
    if (!context) {
        throw new Error('useMFA must be used within an MFAProvider');
    }
    return context;
};
