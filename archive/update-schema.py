#!/usr/bin/env python3
import psycopg2
from psycopg2 import sql

# Database connection parameters
DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'crud_test_db',
    'user': 'postgres',
    'password': 'postgres'
}

def update_schema():
    try:
        # Connect to database
        print("Connecting to database...")
        conn = psycopg2.connect(**DB_CONFIG)
        cursor = conn.cursor()

        print("Adding missing columns to users table...")

        # Add mfa_enabled column
        try:
            cursor.execute("""
                ALTER TABLE users
                ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE
            """)
            print("✓ Added mfa_enabled column")
        except Exception as e:
            print(f"  mfa_enabled: {e}")

        # Add is_account_locked column
        try:
            cursor.execute("""
                ALTER TABLE users
                ADD COLUMN IF NOT EXISTS is_account_locked BOOLEAN NOT NULL DEFAULT FALSE
            """)
            print("✓ Added is_account_locked column")
        except Exception as e:
            print(f"  is_account_locked: {e}")

        # Add account_locked_until column
        try:
            cursor.execute("""
                ALTER TABLE users
                ADD COLUMN IF NOT EXISTS account_locked_until TIMESTAMP
            """)
            print("✓ Added account_locked_until column")
        except Exception as e:
            print(f"  account_locked_until: {e}")

        # Add lock_reason column
        try:
            cursor.execute("""
                ALTER TABLE users
                ADD COLUMN IF NOT EXISTS lock_reason VARCHAR(500)
            """)
            print("✓ Added lock_reason column")
        except Exception as e:
            print(f"  lock_reason: {e}")

        # Commit changes
        conn.commit()
        print("\nCommitted changes successfully!")

        # Verify columns
        cursor.execute("""
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'users'
              AND column_name IN ('mfa_enabled', 'is_account_locked', 'account_locked_until', 'lock_reason')
            ORDER BY column_name
        """)

        print("\nVerification - Users table columns:")
        print("-" * 60)
        for row in cursor.fetchall():
            print(f"  {row[0]:<25} {row[1]:<20} nullable: {row[2]}")

        cursor.close()
        conn.close()
        print("\n✓ Schema update completed successfully!")

    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    update_schema()
