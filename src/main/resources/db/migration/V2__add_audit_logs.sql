-- ============================================================================
-- Flyway Migration V2: Audit Logs
-- ============================================================================
-- Description: Creates audit_logs table for tracking all user actions
-- Author: Enterprise Transformation Team
-- Date: 2026-02-03
-- ============================================================================

-- Create audit_logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    action          VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    old_value       TEXT,
    new_value       TEXT,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    request_url     VARCHAR(500),
    http_method     VARCHAR(10),
    status_code     INTEGER,
    error_message   TEXT,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to users table (nullable for anonymous actions)
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for efficient querying
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_user_action ON audit_logs(user_id, action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- Create index for IP address lookups (security monitoring)
CREATE INDEX idx_audit_logs_ip_address ON audit_logs(ip_address);

-- Add comments
COMMENT ON TABLE audit_logs IS 'Audit trail of all user actions for compliance and security';
COMMENT ON COLUMN audit_logs.id IS 'Primary key - auto-incrementing audit log ID';
COMMENT ON COLUMN audit_logs.user_id IS 'User who performed the action (NULL for anonymous)';
COMMENT ON COLUMN audit_logs.action IS 'Action performed (e.g., LOGIN, CREATE_USER, UPDATE_USER, DELETE_USER)';
COMMENT ON COLUMN audit_logs.entity_type IS 'Type of entity affected (e.g., USER, ROLE)';
COMMENT ON COLUMN audit_logs.entity_id IS 'ID of the affected entity';
COMMENT ON COLUMN audit_logs.old_value IS 'Previous value (for updates) in JSON format';
COMMENT ON COLUMN audit_logs.new_value IS 'New value (for creates/updates) in JSON format';
COMMENT ON COLUMN audit_logs.ip_address IS 'Client IP address (IPv4 or IPv6)';
COMMENT ON COLUMN audit_logs.user_agent IS 'Browser user agent string';
COMMENT ON COLUMN audit_logs.request_url IS 'API endpoint that was called';
COMMENT ON COLUMN audit_logs.http_method IS 'HTTP method (GET, POST, PUT, DELETE)';
COMMENT ON COLUMN audit_logs.status_code IS 'HTTP response status code';
COMMENT ON COLUMN audit_logs.error_message IS 'Error message if action failed';
COMMENT ON COLUMN audit_logs.created_at IS 'Timestamp when action occurred';
