import React, { useState, useEffect } from 'react';
import { useMFA } from '../../contexts/MFAContext';
import MFASetupModal from './MFASetupModal';
import BackupCodesDisplay from './BackupCodesDisplay';
import TrustedDevicesList from './TrustedDevicesList';
import '../../styles/MFA.css';

function MFASettings() {
    const [showSetupModal, setShowSetupModal] = useState(false);
    const [showDisableConfirm, setShowDisableConfirm] = useState(false);
    const [showBackupCodes, setShowBackupCodes] = useState(false);
    const [newBackupCodes, setNewBackupCodes] = useState([]);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const {
        mfaEnabled,
        backupCodesRemaining,
        loading,
        disableMFA,
        regenerateBackupCodes,
        loadMFAStatus
    } = useMFA();

    useEffect(() => {
        loadMFAStatus();
    }, []);

    const handleEnableMFA = () => {
        setShowSetupModal(true);
    };

    const handleSetupSuccess = () => {
        setSuccess('MFA has been successfully enabled!');
        setTimeout(() => setSuccess(''), 5000);
        loadMFAStatus();
    };

    const handleDisableMFA = async () => {
        setError('');
        setSuccess('');

        const result = await disableMFA();

        if (result.success) {
            setSuccess('MFA has been disabled');
            setShowDisableConfirm(false);
            setTimeout(() => setSuccess(''), 5000);
        } else {
            setError(result.error);
        }
    };

    const handleRegenerateBackupCodes = async () => {
        setError('');
        setSuccess('');

        const result = await regenerateBackupCodes();

        if (result.success) {
            setNewBackupCodes(result.backupCodes);
            setShowBackupCodes(true);
            setSuccess('Backup codes have been regenerated');
            setTimeout(() => setSuccess(''), 5000);
        } else {
            setError(result.error);
        }
    };

    return (
        <div className="mfa-settings-container">
            <div className="settings-section">
                <div className="section-header">
                    <h2>Multi-Factor Authentication</h2>
                    <div className="mfa-status-badge">
                        {mfaEnabled ? (
                            <span className="status-enabled">Enabled</span>
                        ) : (
                            <span className="status-disabled">Disabled</span>
                        )}
                    </div>
                </div>

                <p className="section-description">
                    Add an extra layer of security to your account by requiring a verification code
                    from your authenticator app in addition to your password.
                </p>

                {error && <div className="error-message">{error}</div>}
                {success && <div className="success-message">{success}</div>}

                <div className="settings-actions">
                    {!mfaEnabled ? (
                        <button
                            type="button"
                            className="btn-primary"
                            onClick={handleEnableMFA}
                            disabled={loading}
                        >
                            Enable MFA
                        </button>
                    ) : (
                        <>
                            <button
                                type="button"
                                className="btn-danger"
                                onClick={() => setShowDisableConfirm(true)}
                                disabled={loading}
                            >
                                Disable MFA
                            </button>
                        </>
                    )}
                </div>
            </div>

            {mfaEnabled && (
                <>
                    <div className="settings-section">
                        <div className="section-header">
                            <h3>Backup Codes</h3>
                            <span className="backup-codes-count">
                                {backupCodesRemaining} remaining
                            </span>
                        </div>

                        <p className="section-description">
                            Backup codes can be used to access your account if you lose access to your
                            authenticator device. Each code can only be used once.
                        </p>

                        {backupCodesRemaining <= 2 && backupCodesRemaining > 0 && (
                            <div className="warning-message">
                                You're running low on backup codes. Consider regenerating them.
                            </div>
                        )}

                        {backupCodesRemaining === 0 && (
                            <div className="error-message">
                                You have no backup codes remaining. Generate new ones immediately to avoid being
                                locked out of your account.
                            </div>
                        )}

                        <div className="settings-actions">
                            <button
                                type="button"
                                className="btn-secondary"
                                onClick={handleRegenerateBackupCodes}
                                disabled={loading}
                            >
                                Regenerate Backup Codes
                            </button>
                        </div>

                        {showBackupCodes && newBackupCodes.length > 0 && (
                            <div className="backup-codes-section">
                                <div className="warning-message">
                                    <strong>Important:</strong> Save these new backup codes. Your old codes are now invalid.
                                </div>
                                <BackupCodesDisplay codes={newBackupCodes} />
                                <button
                                    type="button"
                                    className="btn-secondary"
                                    onClick={() => setShowBackupCodes(false)}
                                >
                                    Done
                                </button>
                            </div>
                        )}
                    </div>

                    <div className="settings-section">
                        <div className="section-header">
                            <h3>Trusted Devices</h3>
                        </div>

                        <p className="section-description">
                            Devices you've marked as trusted won't require an MFA code for 30 days.
                        </p>

                        <TrustedDevicesList />
                    </div>
                </>
            )}

            {/* MFA Setup Modal */}
            <MFASetupModal
                isOpen={showSetupModal}
                onClose={() => setShowSetupModal(false)}
                onSuccess={handleSetupSuccess}
            />

            {/* Disable Confirmation Modal */}
            {showDisableConfirm && (
                <div className="modal-overlay" onClick={() => setShowDisableConfirm(false)}>
                    <div className="modal-content confirm-dialog" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Disable Multi-Factor Authentication</h3>
                        </div>
                        <div className="modal-body">
                            <p>
                                Are you sure you want to disable MFA? This will make your account less secure.
                            </p>
                            <div className="warning-message">
                                <strong>Warning:</strong> All your backup codes and trusted devices will be removed.
                            </div>
                        </div>
                        <div className="modal-actions">
                            <button
                                type="button"
                                className="btn-danger"
                                onClick={handleDisableMFA}
                                disabled={loading}
                            >
                                {loading ? 'Disabling...' : 'Yes, Disable MFA'}
                            </button>
                            <button
                                type="button"
                                className="btn-secondary"
                                onClick={() => setShowDisableConfirm(false)}
                                disabled={loading}
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

export default MFASettings;
