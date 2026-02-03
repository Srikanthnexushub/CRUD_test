-- ============================================================================
-- Flyway Migration V6: Refresh Tokens
-- ============================================================================
-- Description: Creates refresh_tokens table for JWT token refresh mechanism
-- Author: Enterprise Transformation Team
-- Date: 2026-02-03
-- ============================================================================

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMP(6) NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      TIMESTAMP(6),
    replaced_by     VARCHAR(255),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),

    -- Foreign key to users table
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens(user_id, revoked_at)
    WHERE revoked_at IS NULL;

-- Create function to revoke all tokens for a user
CREATE OR REPLACE FUNCTION revoke_all_user_tokens(p_user_id BIGINT)
RETURNS INTEGER AS $$
DECLARE
    v_revoked_count INTEGER;
BEGIN
    UPDATE refresh_tokens
    SET revoked_at = CURRENT_TIMESTAMP
    WHERE user_id = p_user_id
      AND revoked_at IS NULL
      AND expires_at > CURRENT_TIMESTAMP;

    GET DIAGNOSTICS v_revoked_count = ROW_COUNT;
    RETURN v_revoked_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to clean up expired tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS INTEGER AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    DELETE FROM refresh_tokens
    WHERE expires_at < CURRENT_TIMESTAMP
       OR revoked_at < (CURRENT_TIMESTAMP - INTERVAL '30 days');

    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to count active tokens for a user
CREATE OR REPLACE FUNCTION count_active_tokens(p_user_id BIGINT)
RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM refresh_tokens
    WHERE user_id = p_user_id
      AND revoked_at IS NULL
      AND expires_at > CURRENT_TIMESTAMP;

    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- Add comments
COMMENT ON TABLE refresh_tokens IS 'Refresh tokens for JWT token rotation and renewal';
COMMENT ON COLUMN refresh_tokens.id IS 'Primary key - auto-incrementing token ID';
COMMENT ON COLUMN refresh_tokens.user_id IS 'User ID who owns this token';
COMMENT ON COLUMN refresh_tokens.token IS 'Unique refresh token (UUID or secure random string)';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token expiration timestamp (typically 7 days)';
COMMENT ON COLUMN refresh_tokens.created_at IS 'When token was created';
COMMENT ON COLUMN refresh_tokens.revoked_at IS 'When token was revoked (NULL if still valid)';
COMMENT ON COLUMN refresh_tokens.replaced_by IS 'Token that replaced this one (for token rotation)';
COMMENT ON COLUMN refresh_tokens.ip_address IS 'IP address when token was issued';
COMMENT ON COLUMN refresh_tokens.user_agent IS 'User agent when token was issued';

COMMENT ON FUNCTION revoke_all_user_tokens(BIGINT) IS 'Revokes all active tokens for a user (e.g., on logout or password change)';
COMMENT ON FUNCTION cleanup_expired_tokens() IS 'Removes expired and old revoked tokens';
COMMENT ON FUNCTION count_active_tokens(BIGINT) IS 'Counts active (non-revoked, non-expired) tokens for a user';
