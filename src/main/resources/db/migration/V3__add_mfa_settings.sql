-- ============================================================================
-- Flyway Migration V3: MFA Settings
-- ============================================================================
-- Description: Creates mfa_settings table for TOTP multi-factor authentication
-- Author: Enterprise Transformation Team
-- Date: 2026-02-03
-- ============================================================================

-- Create mfa_settings table
CREATE TABLE IF NOT EXISTS mfa_settings (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE,
    secret              VARCHAR(32) NOT NULL,
    backup_codes        TEXT[],
    is_verified         BOOLEAN NOT NULL DEFAULT FALSE,
    recovery_email      VARCHAR(100),
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at         TIMESTAMP(6),
    last_used_at        TIMESTAMP(6),

    -- Foreign key to users table
    CONSTRAINT fk_mfa_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Create trusted_devices table for device management
CREATE TABLE IF NOT EXISTS trusted_devices (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    device_fingerprint  VARCHAR(255) NOT NULL,
    device_name         VARCHAR(100),
    device_type         VARCHAR(50),
    browser             VARCHAR(100),
    os                  VARCHAR(100),
    ip_address          VARCHAR(45),
    trusted_until       TIMESTAMP(6) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at        TIMESTAMP(6),

    -- Foreign key to users table
    CONSTRAINT fk_trusted_device_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,

    -- Unique constraint on user_id + device_fingerprint
    CONSTRAINT uk_user_device UNIQUE (user_id, device_fingerprint)
);

-- Create indexes
CREATE INDEX idx_mfa_settings_user_id ON mfa_settings(user_id);
CREATE INDEX idx_trusted_devices_user_id ON trusted_devices(user_id);
CREATE INDEX idx_trusted_devices_fingerprint ON trusted_devices(device_fingerprint);
CREATE INDEX idx_trusted_devices_trusted_until ON trusted_devices(trusted_until);

-- Add comments
COMMENT ON TABLE mfa_settings IS 'Multi-factor authentication settings for users (TOTP)';
COMMENT ON COLUMN mfa_settings.id IS 'Primary key - auto-incrementing MFA settings ID';
COMMENT ON COLUMN mfa_settings.user_id IS 'User ID (one-to-one relationship)';
COMMENT ON COLUMN mfa_settings.secret IS 'Base32-encoded TOTP secret key';
COMMENT ON COLUMN mfa_settings.backup_codes IS 'Array of single-use backup codes (hashed)';
COMMENT ON COLUMN mfa_settings.is_verified IS 'Whether MFA setup has been verified';
COMMENT ON COLUMN mfa_settings.recovery_email IS 'Alternative email for account recovery';
COMMENT ON COLUMN mfa_settings.created_at IS 'When MFA was first set up';
COMMENT ON COLUMN mfa_settings.verified_at IS 'When MFA setup was verified';
COMMENT ON COLUMN mfa_settings.last_used_at IS 'Last successful MFA verification';

COMMENT ON TABLE trusted_devices IS 'Trusted devices that can skip MFA for a period';
COMMENT ON COLUMN trusted_devices.id IS 'Primary key - auto-incrementing device ID';
COMMENT ON COLUMN trusted_devices.user_id IS 'User ID who owns this trusted device';
COMMENT ON COLUMN trusted_devices.device_fingerprint IS 'Unique device identifier';
COMMENT ON COLUMN trusted_devices.device_name IS 'User-friendly device name';
COMMENT ON COLUMN trusted_devices.device_type IS 'Device type (mobile, tablet, desktop)';
COMMENT ON COLUMN trusted_devices.browser IS 'Browser name and version';
COMMENT ON COLUMN trusted_devices.os IS 'Operating system';
COMMENT ON COLUMN trusted_devices.ip_address IS 'IP address when device was trusted';
COMMENT ON COLUMN trusted_devices.trusted_until IS 'Expiration timestamp (typically 30 days)';
COMMENT ON COLUMN trusted_devices.created_at IS 'When device was first trusted';
COMMENT ON COLUMN trusted_devices.last_used_at IS 'Last time device was used';
