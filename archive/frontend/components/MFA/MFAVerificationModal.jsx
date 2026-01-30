import React, { useState, useEffect, useRef } from 'react';
import { useMFA } from '../../contexts/MFAContext';
import '../../styles/MFA.css';

function MFAVerificationModal({ isOpen, onSuccess, onCancel }) {
    const [verificationCode, setVerificationCode] = useState('');
    const [useBackupCode, setUseBackupCode] = useState(false);
    const [trustDevice, setTrustDevice] = useState(false);
    const [error, setError] = useState('');
    const inputRef = useRef(null);

    const { verifyMFACode, verifyBackupCode, loading, backupCodesRemaining } = useMFA();

    useEffect(() => {
        if (isOpen && inputRef.current) {
            inputRef.current.focus();
        }
    }, [isOpen]);

    // Auto-submit when 6 digits are entered (TOTP mode only)
    useEffect(() => {
        if (!useBackupCode && verificationCode.length === 6) {
            handleVerify();
        }
    }, [verificationCode, useBackupCode]);

    const handleVerificationChange = (e) => {
        const value = e.target.value.replace(/\D/g, '');

        if (useBackupCode) {
            // Backup codes are typically 8 digits
            setVerificationCode(value.slice(0, 8));
        } else {
            // TOTP codes are 6 digits
            setVerificationCode(value.slice(0, 6));
        }

        setError('');
    };

    const handleVerify = async (e) => {
        if (e) e.preventDefault();

        const expectedLength = useBackupCode ? 8 : 6;
        if (verificationCode.length !== expectedLength) {
            setError(`Please enter a ${expectedLength}-digit code`);
            return;
        }

        const result = useBackupCode
            ? await verifyBackupCode(verificationCode)
            : await verifyMFACode(verificationCode, trustDevice);

        if (result.success) {
            // Success - pass token and user data to parent
            if (onSuccess) {
                onSuccess(result.token, result.user);
            }
            handleClose();
        } else {
            setError(result.error);
            setVerificationCode('');
            if (inputRef.current) {
                inputRef.current.focus();
            }
        }
    };

    const handleToggleBackupCode = () => {
        setUseBackupCode(!useBackupCode);
        setVerificationCode('');
        setError('');
        if (inputRef.current) {
            inputRef.current.focus();
        }
    };

    const handleClose = () => {
        setVerificationCode('');
        setUseBackupCode(false);
        setTrustDevice(false);
        setError('');
        if (onCancel) {
            onCancel();
        }
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay">
            <div className="modal-content mfa-verification-modal">
                <div className="modal-header">
                    <h2>Two-Factor Authentication</h2>
                </div>

                <div className="modal-body">
                    <form onSubmit={handleVerify}>
                        {!useBackupCode ? (
                            <>
                                <p className="instruction-text">
                                    Enter the 6-digit code from your authenticator app:
                                </p>

                                <div className="verification-input-container">
                                    <input
                                        ref={inputRef}
                                        type="text"
                                        inputMode="numeric"
                                        pattern="[0-9]*"
                                        maxLength="6"
                                        className="verification-input large"
                                        value={verificationCode}
                                        onChange={handleVerificationChange}
                                        placeholder="000000"
                                        disabled={loading}
                                        autoComplete="one-time-code"
                                    />
                                </div>

                                <div className="trust-device-section">
                                    <label className="checkbox-label">
                                        <input
                                            type="checkbox"
                                            checked={trustDevice}
                                            onChange={(e) => setTrustDevice(e.target.checked)}
                                            disabled={loading}
                                        />
                                        <span>Trust this device for 30 days</span>
                                    </label>
                                </div>
                            </>
                        ) : (
                            <>
                                <p className="instruction-text">
                                    Enter one of your 8-digit backup codes:
                                </p>

                                <div className="verification-input-container">
                                    <input
                                        ref={inputRef}
                                        type="text"
                                        inputMode="numeric"
                                        pattern="[0-9]*"
                                        maxLength="8"
                                        className="verification-input large"
                                        value={verificationCode}
                                        onChange={handleVerificationChange}
                                        placeholder="00000000"
                                        disabled={loading}
                                        autoComplete="off"
                                    />
                                </div>

                                {backupCodesRemaining > 0 && (
                                    <p className="backup-codes-remaining">
                                        {backupCodesRemaining} backup code{backupCodesRemaining !== 1 ? 's' : ''} remaining
                                    </p>
                                )}

                                {backupCodesRemaining <= 2 && backupCodesRemaining > 0 && (
                                    <div className="warning-message">
                                        Warning: You're running low on backup codes. Consider regenerating them after login.
                                    </div>
                                )}
                            </>
                        )}

                        {error && <div className="error-message">{error}</div>}

                        <div className="modal-actions vertical">
                            <button
                                type="submit"
                                className="btn-primary full-width"
                                disabled={
                                    loading ||
                                    (useBackupCode ? verificationCode.length !== 8 : verificationCode.length !== 6)
                                }
                            >
                                {loading ? 'Verifying...' : 'Verify'}
                            </button>

                            <button
                                type="button"
                                className="btn-link"
                                onClick={handleToggleBackupCode}
                                disabled={loading}
                            >
                                {useBackupCode
                                    ? 'Use authenticator code instead'
                                    : 'Use backup code instead'}
                            </button>

                            <button
                                type="button"
                                className="btn-secondary full-width"
                                onClick={handleClose}
                                disabled={loading}
                            >
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default MFAVerificationModal;
