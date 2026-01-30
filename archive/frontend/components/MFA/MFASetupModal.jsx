import React, { useState, useEffect } from 'react';
import { useMFA } from '../../contexts/MFAContext';
import BackupCodesDisplay from './BackupCodesDisplay';
import '../../styles/MFA.css';

function MFASetupModal({ isOpen, onClose, onSuccess }) {
    const [step, setStep] = useState(1); // 1: QR Code, 2: Verification, 3: Backup Codes
    const [qrCodeUrl, setQrCodeUrl] = useState('');
    const [secret, setSecret] = useState('');
    const [backupCodes, setBackupCodes] = useState([]);
    const [verificationCode, setVerificationCode] = useState('');
    const [error, setError] = useState('');
    const [copied, setCopied] = useState(false);

    const { setupMFA, verifyMFASetup, loading } = useMFA();

    useEffect(() => {
        if (isOpen && step === 1) {
            initializeSetup();
        }
    }, [isOpen]);

    const initializeSetup = async () => {
        setError('');
        const result = await setupMFA();

        if (result.success) {
            setQrCodeUrl(result.data.qrCodeUrl);
            setSecret(result.data.secret);
            setBackupCodes(result.data.backupCodes || []);
        } else {
            setError(result.error);
        }
    };

    const handleCopySecret = () => {
        navigator.clipboard.writeText(secret);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleVerificationChange = (e) => {
        const value = e.target.value.replace(/\D/g, '').slice(0, 6);
        setVerificationCode(value);
        setError('');
    };

    const handleVerify = async (e) => {
        e.preventDefault();

        if (verificationCode.length !== 6) {
            setError('Please enter a 6-digit code');
            return;
        }

        const result = await verifyMFASetup(verificationCode);

        if (result.success) {
            setStep(3); // Show backup codes
        } else {
            setError(result.error);
            setVerificationCode('');
        }
    };

    const handleComplete = () => {
        if (onSuccess) {
            onSuccess();
        }
        handleClose();
    };

    const handleClose = () => {
        setStep(1);
        setQrCodeUrl('');
        setSecret('');
        setBackupCodes([]);
        setVerificationCode('');
        setError('');
        setCopied(false);
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content mfa-setup-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Enable Multi-Factor Authentication</h2>
                    <button className="modal-close" onClick={handleClose}>&times;</button>
                </div>

                <div className="modal-body">
                    {step === 1 && (
                        <div className="mfa-setup-step">
                            <div className="step-indicator">
                                <span className="step-number active">1</span>
                                <span className="step-label">Scan QR Code</span>
                            </div>

                            <div className="qr-code-section">
                                <p className="instruction-text">
                                    Scan this QR code with your authenticator app (Google Authenticator, Authy, or similar):
                                </p>

                                {qrCodeUrl && (
                                    <div className="qr-code-container">
                                        <img src={qrCodeUrl} alt="MFA QR Code" className="qr-code-image" />
                                    </div>
                                )}

                                <div className="secret-key-section">
                                    <p className="secret-label">Or enter this secret key manually:</p>
                                    <div className="secret-key-container">
                                        <code className="secret-key">{secret}</code>
                                        <button
                                            type="button"
                                            className="btn-copy-secret"
                                            onClick={handleCopySecret}
                                        >
                                            {copied ? 'Copied!' : 'Copy'}
                                        </button>
                                    </div>
                                </div>
                            </div>

                            {error && <div className="error-message">{error}</div>}

                            <div className="modal-actions">
                                <button
                                    type="button"
                                    className="btn-primary"
                                    onClick={() => setStep(2)}
                                    disabled={!qrCodeUrl}
                                >
                                    Next: Verify Code
                                </button>
                                <button
                                    type="button"
                                    className="btn-secondary"
                                    onClick={handleClose}
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    )}

                    {step === 2 && (
                        <div className="mfa-setup-step">
                            <div className="step-indicator">
                                <span className="step-number active">2</span>
                                <span className="step-label">Verify Code</span>
                            </div>

                            <form onSubmit={handleVerify}>
                                <p className="instruction-text">
                                    Enter the 6-digit code from your authenticator app to verify the setup:
                                </p>

                                <div className="verification-input-container">
                                    <input
                                        type="text"
                                        inputMode="numeric"
                                        pattern="[0-9]*"
                                        maxLength="6"
                                        className="verification-input"
                                        value={verificationCode}
                                        onChange={handleVerificationChange}
                                        placeholder="000000"
                                        autoFocus
                                        disabled={loading}
                                    />
                                </div>

                                {error && <div className="error-message">{error}</div>}

                                <div className="modal-actions">
                                    <button
                                        type="submit"
                                        className="btn-primary"
                                        disabled={verificationCode.length !== 6 || loading}
                                    >
                                        {loading ? 'Verifying...' : 'Verify & Enable MFA'}
                                    </button>
                                    <button
                                        type="button"
                                        className="btn-secondary"
                                        onClick={() => setStep(1)}
                                        disabled={loading}
                                    >
                                        Back
                                    </button>
                                </div>
                            </form>
                        </div>
                    )}

                    {step === 3 && (
                        <div className="mfa-setup-step">
                            <div className="step-indicator">
                                <span className="step-number active">3</span>
                                <span className="step-label">Save Backup Codes</span>
                            </div>

                            <div className="backup-codes-section">
                                <div className="success-message">
                                    MFA has been successfully enabled!
                                </div>

                                <p className="instruction-text warning">
                                    <strong>Important:</strong> Save these backup codes in a secure location.
                                    You can use them to access your account if you lose your authenticator device.
                                </p>

                                <BackupCodesDisplay codes={backupCodes} />
                            </div>

                            <div className="modal-actions">
                                <button
                                    type="button"
                                    className="btn-primary"
                                    onClick={handleComplete}
                                >
                                    Done
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

export default MFASetupModal;
