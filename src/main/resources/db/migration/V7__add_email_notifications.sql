-- ============================================================================
-- Flyway Migration V7: Email Notifications
-- ============================================================================
-- Description: Creates email_notifications table for email queue and tracking
-- Author: Enterprise Transformation Team
-- Date: 2026-02-03
-- ============================================================================

-- Create email notification status enum type
CREATE TYPE email_status AS ENUM ('PENDING', 'SENDING', 'SENT', 'FAILED', 'CANCELLED');

-- Create email notification type enum
CREATE TYPE email_type AS ENUM (
    'WELCOME',
    'PASSWORD_RESET',
    'PASSWORD_CHANGED',
    'EMAIL_CHANGED',
    'LOGIN_ALERT',
    'ACCOUNT_LOCKED',
    'ACCOUNT_UNLOCKED',
    'MFA_ENABLED',
    'MFA_DISABLED',
    'NEW_DEVICE_LOGIN',
    'SECURITY_ALERT'
);

-- Create email_notifications table
CREATE TABLE IF NOT EXISTS email_notifications (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT,
    email_type          email_type NOT NULL,
    recipient_email     VARCHAR(100) NOT NULL,
    subject             VARCHAR(200) NOT NULL,
    body_text           TEXT,
    body_html           TEXT,
    status              email_status NOT NULL DEFAULT 'PENDING',
    attempts            INTEGER NOT NULL DEFAULT 0,
    max_attempts        INTEGER NOT NULL DEFAULT 3,
    last_attempt_at     TIMESTAMP(6),
    sent_at             TIMESTAMP(6),
    error_message       TEXT,
    metadata            JSONB,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to users table (nullable for system emails)
    CONSTRAINT fk_email_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes
CREATE INDEX idx_email_notifications_user_id ON email_notifications(user_id);
CREATE INDEX idx_email_notifications_status ON email_notifications(status);
CREATE INDEX idx_email_notifications_created_at ON email_notifications(created_at DESC);
CREATE INDEX idx_email_notifications_recipient ON email_notifications(recipient_email);
CREATE INDEX idx_email_notifications_type ON email_notifications(email_type);
CREATE INDEX idx_email_notifications_pending ON email_notifications(status, created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_email_notifications_metadata ON email_notifications USING gin(metadata);

-- Create function to get pending emails for processing
CREATE OR REPLACE FUNCTION get_pending_emails(p_limit INTEGER DEFAULT 10)
RETURNS SETOF email_notifications AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM email_notifications
    WHERE status = 'PENDING'
      AND attempts < max_attempts
      AND (last_attempt_at IS NULL OR last_attempt_at < (CURRENT_TIMESTAMP - INTERVAL '5 minutes'))
    ORDER BY created_at ASC
    LIMIT p_limit
    FOR UPDATE SKIP LOCKED;
END;
$$ LANGUAGE plpgsql;

-- Create function to mark email as sent
CREATE OR REPLACE FUNCTION mark_email_sent(p_email_id BIGINT)
RETURNS VOID AS $$
BEGIN
    UPDATE email_notifications
    SET status = 'SENT',
        sent_at = CURRENT_TIMESTAMP,
        last_attempt_at = CURRENT_TIMESTAMP
    WHERE id = p_email_id;
END;
$$ LANGUAGE plpgsql;

-- Create function to mark email as failed
CREATE OR REPLACE FUNCTION mark_email_failed(
    p_email_id BIGINT,
    p_error_message TEXT
)
RETURNS VOID AS $$
BEGIN
    UPDATE email_notifications
    SET status = CASE
            WHEN attempts + 1 >= max_attempts THEN 'FAILED'::email_status
            ELSE 'PENDING'::email_status
        END,
        attempts = attempts + 1,
        last_attempt_at = CURRENT_TIMESTAMP,
        error_message = p_error_message
    WHERE id = p_email_id;
END;
$$ LANGUAGE plpgsql;

-- Create function to clean up old emails (retention: 90 days)
CREATE OR REPLACE FUNCTION cleanup_old_emails()
RETURNS INTEGER AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    DELETE FROM email_notifications
    WHERE created_at < (CURRENT_TIMESTAMP - INTERVAL '90 days')
      AND status IN ('SENT', 'FAILED', 'CANCELLED');

    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to retry failed emails
CREATE OR REPLACE FUNCTION retry_failed_emails(p_email_ids BIGINT[])
RETURNS INTEGER AS $$
DECLARE
    v_updated_count INTEGER;
BEGIN
    UPDATE email_notifications
    SET status = 'PENDING',
        attempts = 0,
        error_message = NULL,
        last_attempt_at = NULL
    WHERE id = ANY(p_email_ids)
      AND status = 'FAILED';

    GET DIAGNOSTICS v_updated_count = ROW_COUNT;
    RETURN v_updated_count;
END;
$$ LANGUAGE plpgsql;

-- Add comments
COMMENT ON TABLE email_notifications IS 'Email queue and tracking for asynchronous email sending';
COMMENT ON COLUMN email_notifications.id IS 'Primary key - auto-incrementing email ID';
COMMENT ON COLUMN email_notifications.user_id IS 'User ID (NULL for system emails)';
COMMENT ON COLUMN email_notifications.email_type IS 'Type of email notification';
COMMENT ON COLUMN email_notifications.recipient_email IS 'Recipient email address';
COMMENT ON COLUMN email_notifications.subject IS 'Email subject line';
COMMENT ON COLUMN email_notifications.body_text IS 'Plain text email body';
COMMENT ON COLUMN email_notifications.body_html IS 'HTML email body';
COMMENT ON COLUMN email_notifications.status IS 'Email status (PENDING, SENDING, SENT, FAILED, CANCELLED)';
COMMENT ON COLUMN email_notifications.attempts IS 'Number of sending attempts';
COMMENT ON COLUMN email_notifications.max_attempts IS 'Maximum retry attempts';
COMMENT ON COLUMN email_notifications.last_attempt_at IS 'Timestamp of last sending attempt';
COMMENT ON COLUMN email_notifications.sent_at IS 'Timestamp when successfully sent';
COMMENT ON COLUMN email_notifications.error_message IS 'Error message from last failed attempt';
COMMENT ON COLUMN email_notifications.metadata IS 'Additional metadata in JSON format';
COMMENT ON COLUMN email_notifications.created_at IS 'When email was queued';

COMMENT ON FUNCTION get_pending_emails(INTEGER) IS 'Retrieves pending emails for processing with row locking';
COMMENT ON FUNCTION mark_email_sent(BIGINT) IS 'Marks an email as successfully sent';
COMMENT ON FUNCTION mark_email_failed(BIGINT, TEXT) IS 'Records a failed email attempt';
COMMENT ON FUNCTION cleanup_old_emails() IS 'Removes emails older than 90 days';
COMMENT ON FUNCTION retry_failed_emails(BIGINT[]) IS 'Resets failed emails to pending status for retry';
