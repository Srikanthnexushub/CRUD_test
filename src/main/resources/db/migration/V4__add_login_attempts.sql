-- ============================================================================
-- Flyway Migration V4: Login Attempts
-- ============================================================================
-- Description: Creates login_attempts table for tracking and brute force protection
-- Author: Enterprise Transformation Team
-- Date: 2026-02-03
-- ============================================================================

-- Create login_attempts table
CREATE TABLE IF NOT EXISTS login_attempts (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL,
    ip_address      VARCHAR(45) NOT NULL,
    success         BOOLEAN NOT NULL,
    failure_reason  VARCHAR(100),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_login_attempts_username ON login_attempts(username);
CREATE INDEX idx_login_attempts_ip_address ON login_attempts(ip_address);
CREATE INDEX idx_login_attempts_created_at ON login_attempts(created_at DESC);
CREATE INDEX idx_login_attempts_username_success ON login_attempts(username, success, created_at DESC);
CREATE INDEX idx_login_attempts_ip_success ON login_attempts(ip_address, success, created_at DESC);

-- Create function to count recent failed login attempts by username
CREATE OR REPLACE FUNCTION count_recent_failed_logins(
    p_username VARCHAR(50),
    p_minutes INTEGER DEFAULT 15
)
RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM login_attempts
    WHERE username = p_username
      AND success = FALSE
      AND created_at > (CURRENT_TIMESTAMP - (p_minutes || ' minutes')::INTERVAL);

    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to count recent failed login attempts by IP address
CREATE OR REPLACE FUNCTION count_recent_failed_logins_by_ip(
    p_ip_address VARCHAR(45),
    p_minutes INTEGER DEFAULT 15
)
RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM login_attempts
    WHERE ip_address = p_ip_address
      AND success = FALSE
      AND created_at > (CURRENT_TIMESTAMP - (p_minutes || ' minutes')::INTERVAL);

    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to clean up old login attempts (retention: 90 days)
CREATE OR REPLACE FUNCTION cleanup_old_login_attempts()
RETURNS INTEGER AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    DELETE FROM login_attempts
    WHERE created_at < (CURRENT_TIMESTAMP - INTERVAL '90 days');

    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Add comments
COMMENT ON TABLE login_attempts IS 'Login attempts for security monitoring and brute force protection';
COMMENT ON COLUMN login_attempts.id IS 'Primary key - auto-incrementing login attempt ID';
COMMENT ON COLUMN login_attempts.username IS 'Username attempted (may not exist in users table)';
COMMENT ON COLUMN login_attempts.ip_address IS 'IP address of login attempt';
COMMENT ON COLUMN login_attempts.success IS 'Whether login was successful';
COMMENT ON COLUMN login_attempts.failure_reason IS 'Reason for failure (e.g., INVALID_PASSWORD, USER_NOT_FOUND, ACCOUNT_LOCKED)';
COMMENT ON COLUMN login_attempts.user_agent IS 'Browser user agent string';
COMMENT ON COLUMN login_attempts.created_at IS 'Timestamp of login attempt';

COMMENT ON FUNCTION count_recent_failed_logins(VARCHAR, INTEGER) IS 'Counts failed login attempts for a username within specified minutes';
COMMENT ON FUNCTION count_recent_failed_logins_by_ip(VARCHAR, INTEGER) IS 'Counts failed login attempts from an IP within specified minutes';
COMMENT ON FUNCTION cleanup_old_login_attempts() IS 'Removes login attempts older than 90 days';
