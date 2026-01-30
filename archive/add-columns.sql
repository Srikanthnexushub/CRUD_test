-- Phase 2: Add missing columns to users table

-- Check if columns exist before adding them
DO $$
BEGIN
    -- Add mfa_enabled column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='users' AND column_name='mfa_enabled') THEN
        ALTER TABLE users ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;

    -- Add is_account_locked column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='users' AND column_name='is_account_locked') THEN
        ALTER TABLE users ADD COLUMN is_account_locked BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;

    -- Add account_locked_until column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='users' AND column_name='account_locked_until') THEN
        ALTER TABLE users ADD COLUMN account_locked_until TIMESTAMP;
    END IF;

    -- Add lock_reason column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='users' AND column_name='lock_reason') THEN
        ALTER TABLE users ADD COLUMN lock_reason VARCHAR(500);
    END IF;
END $$;

-- Verify the columns were added
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'users'
  AND column_name IN ('mfa_enabled', 'is_account_locked', 'account_locked_until', 'lock_reason')
ORDER BY column_name;
