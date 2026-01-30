#!/usr/bin/env python3
"""
Create an admin user for SOC Dashboard access
"""
import psycopg2
import sys

DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'crud_test_db',
    'user': 'postgres',
    'password': 'postgres'
}

def create_admin_user():
    print("=" * 60)
    print("Admin User Setup")
    print("=" * 60)
    print()

    try:
        conn = psycopg2.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # Get list of existing users
        cursor.execute("""
            SELECT id, username, email, role
            FROM users
            ORDER BY created_at DESC
            LIMIT 10
        """)

        users = cursor.fetchall()

        if not users:
            print("No users found. Please register a user first at:")
            print("http://localhost:3001/register")
            print()
            return

        print("Existing users:")
        print("-" * 60)
        print(f"{'ID':<5} {'Username':<20} {'Email':<30} {'Role':<15}")
        print("-" * 60)
        for user in users:
            user_id, username, email, role = user
            print(f"{user_id:<5} {username:<20} {email:<30} {role:<15}")
        print()

        # Prompt for user selection
        print("Which user would you like to promote to admin?")
        user_input = input("Enter user ID or username: ").strip()

        # Find user by ID or username
        try:
            user_id = int(user_input)
            cursor.execute("SELECT id, username, role FROM users WHERE id = %s", (user_id,))
        except ValueError:
            cursor.execute("SELECT id, username, role FROM users WHERE username = %s", (user_input,))

        user = cursor.fetchone()

        if not user:
            print(f"User not found: {user_input}")
            return

        user_id, username, current_role = user

        if current_role == 'ROLE_ADMIN':
            print(f"✓ User '{username}' is already an admin!")
            return

        # Promote to admin
        cursor.execute("""
            UPDATE users
            SET role = 'ROLE_ADMIN'
            WHERE id = %s
        """, (user_id,))

        conn.commit()

        print()
        print("=" * 60)
        print(f"✓ User '{username}' promoted to ADMIN successfully!")
        print("=" * 60)
        print()
        print("Next steps:")
        print("1. Logout from the current session")
        print("2. Login again with the admin account")
        print("3. Access SOC Dashboard at: http://localhost:3001/soc-dashboard")
        print()
        print("Available admin features:")
        print("  • Threat Intelligence Panel")
        print("  • Rate Limiting Dashboard")
        print("  • Email Notification Management")
        print("  • User Management")
        print()

        cursor.close()
        conn.close()

    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    create_admin_user()
