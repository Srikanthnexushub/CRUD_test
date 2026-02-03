-- ============================================================================
-- Flyway Migration V1: Initial Schema
-- ============================================================================
-- Description: Creates the users table with authentication and security fields
-- Author: Enterprise Transformation Team
-- Date: 2026-02-03
-- ============================================================================

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id                      BIGSERIAL PRIMARY KEY,
    username                VARCHAR(50) NOT NULL UNIQUE,
    email                   VARCHAR(100) NOT NULL UNIQUE,
    password_hash           VARCHAR(60) NOT NULL,
    role                    VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    mfa_enabled             BOOLEAN NOT NULL DEFAULT FALSE,
    is_account_locked       BOOLEAN NOT NULL DEFAULT FALSE,
    account_locked_until    TIMESTAMP(6),
    lock_reason             VARCHAR(500),
    created_at              TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN')),
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- Create indexes for frequently queried columns
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at on row update
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default admin user (password: admin123)
-- BCrypt hash with strength 12
INSERT INTO users (username, email, password_hash, role, created_at, updated_at)
VALUES (
    'admin',
    'admin@crudtest.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lW1z9p4MvYFe', -- admin123
    'ROLE_ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- Add comments for documentation
COMMENT ON TABLE users IS 'User accounts with authentication and authorization';
COMMENT ON COLUMN users.id IS 'Primary key - auto-incrementing user ID';
COMMENT ON COLUMN users.username IS 'Unique username (3-50 chars, alphanumeric + underscore)';
COMMENT ON COLUMN users.email IS 'Unique email address';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password (strength 12)';
COMMENT ON COLUMN users.role IS 'User role for authorization (ROLE_USER or ROLE_ADMIN)';
COMMENT ON COLUMN users.mfa_enabled IS 'Whether multi-factor authentication is enabled';
COMMENT ON COLUMN users.is_account_locked IS 'Whether the account is currently locked';
COMMENT ON COLUMN users.account_locked_until IS 'Timestamp when account lock expires';
COMMENT ON COLUMN users.lock_reason IS 'Reason for account lock';
COMMENT ON COLUMN users.created_at IS 'Timestamp when user was created';
COMMENT ON COLUMN users.updated_at IS 'Timestamp when user was last updated (auto-updated)';
