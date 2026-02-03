-- ============================================================================
-- Flyway Migration V5: Password History
-- ============================================================================
-- Description: Creates password_history table to prevent password reuse
-- Author: Enterprise Transformation Team
-- Date: 2026-02-03
-- ============================================================================

-- Create password_history table
CREATE TABLE IF NOT EXISTS password_history (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    password_hash   VARCHAR(60) NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to users table
    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_password_history_user_id ON password_history(user_id);
CREATE INDEX idx_password_history_created_at ON password_history(user_id, created_at DESC);

-- Create function to add password to history
CREATE OR REPLACE FUNCTION add_password_to_history()
RETURNS TRIGGER AS $$
BEGIN
    -- Only add to history if password actually changed
    IF (TG_OP = 'UPDATE' AND OLD.password_hash IS DISTINCT FROM NEW.password_hash) OR TG_OP = 'INSERT' THEN
        INSERT INTO password_history (user_id, password_hash, created_at)
        VALUES (NEW.id, NEW.password_hash, CURRENT_TIMESTAMP);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically track password changes
CREATE TRIGGER track_password_changes
    AFTER INSERT OR UPDATE OF password_hash ON users
    FOR EACH ROW
    EXECUTE FUNCTION add_password_to_history();

-- Create function to check if password was used recently
CREATE OR REPLACE FUNCTION is_password_recently_used(
    p_user_id BIGINT,
    p_password_hash VARCHAR(60),
    p_history_count INTEGER DEFAULT 5
)
RETURNS BOOLEAN AS $$
DECLARE
    v_exists BOOLEAN;
BEGIN
    SELECT EXISTS(
        SELECT 1
        FROM (
            SELECT password_hash
            FROM password_history
            WHERE user_id = p_user_id
            ORDER BY created_at DESC
            LIMIT p_history_count
        ) recent_passwords
        WHERE password_hash = p_password_hash
    ) INTO v_exists;

    RETURN v_exists;
END;
$$ LANGUAGE plpgsql;

-- Create function to clean up old password history (keep only last 10 per user)
CREATE OR REPLACE FUNCTION cleanup_old_password_history()
RETURNS INTEGER AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    DELETE FROM password_history
    WHERE id NOT IN (
        SELECT id
        FROM (
            SELECT id,
                   ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC) as rn
            FROM password_history
        ) ranked
        WHERE rn <= 10
    );

    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Add current passwords to history for existing users
INSERT INTO password_history (user_id, password_hash, created_at)
SELECT id, password_hash, created_at
FROM users
ON CONFLICT DO NOTHING;

-- Add comments
COMMENT ON TABLE password_history IS 'Historical passwords to prevent reuse';
COMMENT ON COLUMN password_history.id IS 'Primary key - auto-incrementing password history ID';
COMMENT ON COLUMN password_history.user_id IS 'User ID';
COMMENT ON COLUMN password_history.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN password_history.created_at IS 'When password was set';

COMMENT ON FUNCTION add_password_to_history() IS 'Trigger function to automatically track password changes';
COMMENT ON FUNCTION is_password_recently_used(BIGINT, VARCHAR, INTEGER) IS 'Checks if password was used in last N changes';
COMMENT ON FUNCTION cleanup_old_password_history() IS 'Keeps only the 10 most recent passwords per user';
