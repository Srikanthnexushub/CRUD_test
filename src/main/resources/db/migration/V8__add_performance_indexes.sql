-- ============================================================================
-- Flyway Migration V8: Performance Indexes
-- ============================================================================
-- Description: Adds additional indexes and optimizations for performance
-- Author: Enterprise Transformation Team
-- Date: 2026-02-03
-- ============================================================================

-- ============================================================================
-- Composite Indexes for Common Query Patterns
-- ============================================================================

-- Users table: Composite index for authentication queries
CREATE INDEX IF NOT EXISTS idx_users_username_locked
    ON users(username, is_account_locked)
    WHERE is_account_locked = FALSE;

-- Users table: Index for locked account cleanup
CREATE INDEX IF NOT EXISTS idx_users_locked_until
    ON users(account_locked_until)
    WHERE account_locked_until IS NOT NULL AND is_account_locked = TRUE;

-- Audit logs: Composite index for user action queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_created
    ON audit_logs(user_id, created_at DESC)
    WHERE user_id IS NOT NULL;

-- Audit logs: Index for security event queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_security_events
    ON audit_logs(action, created_at DESC)
    WHERE action IN ('LOGIN_FAILED', 'ACCOUNT_LOCKED', 'UNAUTHORIZED_ACCESS');

-- ============================================================================
-- Partial Indexes for Filtered Queries
-- ============================================================================

-- MFA settings: Index for active MFA users
CREATE INDEX IF NOT EXISTS idx_mfa_settings_verified
    ON mfa_settings(user_id)
    WHERE is_verified = TRUE;

-- Trusted devices: Index for valid (non-expired) devices
CREATE INDEX IF NOT EXISTS idx_trusted_devices_valid
    ON trusted_devices(user_id, trusted_until)
    WHERE trusted_until > CURRENT_TIMESTAMP;

-- Login attempts: Index for recent failed attempts
CREATE INDEX IF NOT EXISTS idx_login_attempts_recent_failures
    ON login_attempts(username, created_at DESC)
    WHERE success = FALSE;

-- Refresh tokens: Index for active tokens only
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active
    ON refresh_tokens(user_id, expires_at DESC)
    WHERE revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP;

-- Email notifications: Index for failed emails needing retry
CREATE INDEX IF NOT EXISTS idx_email_notifications_failed_retry
    ON email_notifications(created_at)
    WHERE status = 'FAILED' AND attempts < max_attempts;

-- ============================================================================
-- Covering Indexes (Include columns for index-only scans)
-- ============================================================================

-- Users: Include role for authorization checks
CREATE INDEX IF NOT EXISTS idx_users_username_include_role
    ON users(username) INCLUDE (role, is_account_locked);

-- Audit logs: Include common columns for reporting
CREATE INDEX IF NOT EXISTS idx_audit_logs_reporting
    ON audit_logs(created_at DESC)
    INCLUDE (user_id, action, entity_type, status_code);

-- ============================================================================
-- BRIN Indexes for Large Time-Series Data
-- ============================================================================
-- BRIN (Block Range INdex) indexes are efficient for large tables with
-- sequential data (like timestamps). They use much less space than B-tree.

-- Audit logs: BRIN index on created_at for range queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at_brin
    ON audit_logs USING brin(created_at);

-- Login attempts: BRIN index for time-based queries
CREATE INDEX IF NOT EXISTS idx_login_attempts_created_at_brin
    ON login_attempts USING brin(created_at);

-- Password history: BRIN index for historical data
CREATE INDEX IF NOT EXISTS idx_password_history_created_at_brin
    ON password_history USING brin(created_at);

-- ============================================================================
-- Text Search Indexes
-- ============================================================================

-- Audit logs: Full-text search on error messages
CREATE INDEX IF NOT EXISTS idx_audit_logs_error_message_fts
    ON audit_logs USING gin(to_tsvector('english', COALESCE(error_message, '')));

-- Email notifications: Full-text search on subject and body
CREATE INDEX IF NOT EXISTS idx_email_notifications_content_fts
    ON email_notifications USING gin(
        to_tsvector('english', subject || ' ' || COALESCE(body_text, ''))
    );

-- ============================================================================
-- Statistics and Query Planner Hints
-- ============================================================================

-- Update table statistics for better query planning
ANALYZE users;
ANALYZE audit_logs;
ANALYZE mfa_settings;
ANALYZE trusted_devices;
ANALYZE login_attempts;
ANALYZE password_history;
ANALYZE refresh_tokens;
ANALYZE email_notifications;

-- ============================================================================
-- Maintenance Functions
-- ============================================================================

-- Create function to reindex all tables
CREATE OR REPLACE FUNCTION reindex_all_tables()
RETURNS VOID AS $$
BEGIN
    REINDEX TABLE users;
    REINDEX TABLE audit_logs;
    REINDEX TABLE mfa_settings;
    REINDEX TABLE trusted_devices;
    REINDEX TABLE login_attempts;
    REINDEX TABLE password_history;
    REINDEX TABLE refresh_tokens;
    REINDEX TABLE email_notifications;

    -- Update statistics after reindexing
    ANALYZE users;
    ANALYZE audit_logs;
    ANALYZE mfa_settings;
    ANALYZE trusted_devices;
    ANALYZE login_attempts;
    ANALYZE password_history;
    ANALYZE refresh_tokens;
    ANALYZE email_notifications;
END;
$$ LANGUAGE plpgsql;

-- Create function to get index usage statistics
CREATE OR REPLACE FUNCTION get_index_usage_stats()
RETURNS TABLE (
    schemaname TEXT,
    tablename TEXT,
    indexname TEXT,
    idx_scan BIGINT,
    idx_tup_read BIGINT,
    idx_tup_fetch BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        pg_stat_user_indexes.schemaname::TEXT,
        pg_stat_user_indexes.relname::TEXT,
        pg_stat_user_indexes.indexrelname::TEXT,
        pg_stat_user_indexes.idx_scan,
        pg_stat_user_indexes.idx_tup_read,
        pg_stat_user_indexes.idx_tup_fetch
    FROM pg_stat_user_indexes
    ORDER BY pg_stat_user_indexes.idx_scan DESC;
END;
$$ LANGUAGE plpgsql;

-- Create function to identify unused indexes
CREATE OR REPLACE FUNCTION get_unused_indexes()
RETURNS TABLE (
    schemaname TEXT,
    tablename TEXT,
    indexname TEXT,
    index_size TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        pg_stat_user_indexes.schemaname::TEXT,
        pg_stat_user_indexes.relname::TEXT,
        pg_stat_user_indexes.indexrelname::TEXT,
        pg_size_pretty(pg_relation_size(pg_stat_user_indexes.indexrelid))::TEXT
    FROM pg_stat_user_indexes
    WHERE pg_stat_user_indexes.idx_scan = 0
      AND pg_stat_user_indexes.schemaname = 'public'
    ORDER BY pg_relation_size(pg_stat_user_indexes.indexrelid) DESC;
END;
$$ LANGUAGE plpgsql;

-- Add comments
COMMENT ON FUNCTION reindex_all_tables() IS 'Reindexes all application tables and updates statistics';
COMMENT ON FUNCTION get_index_usage_stats() IS 'Returns index usage statistics for monitoring';
COMMENT ON FUNCTION get_unused_indexes() IS 'Identifies potentially unused indexes that could be dropped';
