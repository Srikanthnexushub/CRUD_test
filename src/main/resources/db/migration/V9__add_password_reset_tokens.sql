-- V9: Add password reset tokens table
-- Handles secure password reset flow with token-based authentication

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(45),
    user_agent TEXT,

    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_created_at ON password_reset_tokens(created_at);

-- Comments for documentation
COMMENT ON TABLE password_reset_tokens IS 'Stores password reset tokens with expiration for secure password recovery';
COMMENT ON COLUMN password_reset_tokens.token IS 'Unique token for password reset (UUID without hyphens)';
COMMENT ON COLUMN password_reset_tokens.user_id IS 'Reference to the user requesting password reset';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Token expiration timestamp (default: 30 minutes)';
COMMENT ON COLUMN password_reset_tokens.created_at IS 'Timestamp when token was created';
COMMENT ON COLUMN password_reset_tokens.used_at IS 'Timestamp when token was used';
COMMENT ON COLUMN password_reset_tokens.is_used IS 'Whether token has been used (one-time use)';
COMMENT ON COLUMN password_reset_tokens.ip_address IS 'IP address of the request initiator';
COMMENT ON COLUMN password_reset_tokens.user_agent IS 'User agent of the request initiator';
