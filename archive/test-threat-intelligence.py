#!/usr/bin/env python3
"""
Threat Intelligence Testing Script
Generates test data by simulating various login scenarios
"""
import requests
import time
import random

BASE_URL = "http://localhost:8080"

def test_threat_intelligence():
    print("=" * 70)
    print("Threat Intelligence Testing")
    print("=" * 70)
    print()

    # Test Scenario 1: Normal Logins (LOW RISK)
    print("Scenario 1: Generating Normal Login Attempts (LOW RISK)")
    print("-" * 70)

    for i in range(3):
        timestamp = int(time.time())
        username = f"normaluser{i}_{timestamp}"

        # Register
        reg_response = requests.post(f"{BASE_URL}/api/users/register", json={
            "username": username,
            "email": f"{username}@example.com",
            "password": "Test123"
        })

        if reg_response.status_code == 201:
            print(f"✓ Registered: {username}")

            # Login (triggers threat assessment)
            login_response = requests.post(f"{BASE_URL}/api/auth/login", json={
                "username": username,
                "password": "Test123",
                "deviceFingerprint": f"normal-device-{i}"
            })

            if login_response.status_code == 200:
                data = login_response.json()
                print(f"  ✓ Login successful - Risk should be LOW")
                print(f"  Account Locked: {data.get('accountLocked', False)}")
            else:
                print(f"  ✗ Login failed: {login_response.status_code}")

        time.sleep(0.5)

    print()

    # Test Scenario 2: New Device Logins (MEDIUM RISK)
    print("Scenario 2: Login from New Devices (MEDIUM RISK)")
    print("-" * 70)

    # Register one user
    timestamp = int(time.time())
    username = f"devicetest_{timestamp}"

    reg_response = requests.post(f"{BASE_URL}/api/users/register", json={
        "username": username,
        "email": f"{username}@example.com",
        "password": "Test123"
    })

    if reg_response.status_code == 201:
        print(f"✓ Registered: {username}")

        # Login from 3 different devices
        for i in range(3):
            device_fp = f"device-{random.randint(1000, 9999)}"
            login_response = requests.post(f"{BASE_URL}/api/auth/login", json={
                "username": username,
                "password": "Test123",
                "deviceFingerprint": device_fp
            })

            if login_response.status_code == 200:
                print(f"  ✓ Login from new device: {device_fp}")

            time.sleep(0.5)

    print()

    # Test Scenario 3: Failed Login Attempts (HIGH RISK)
    print("Scenario 3: Failed Login Attempts → Success (HIGH RISK)")
    print("-" * 70)

    timestamp = int(time.time())
    username = f"brutetest_{timestamp}"

    reg_response = requests.post(f"{BASE_URL}/api/users/register", json={
        "username": username,
        "email": f"{username}@example.com",
        "password": "Test123"
    })

    if reg_response.status_code == 201:
        print(f"✓ Registered: {username}")

        # Simulate 4 failed attempts
        print("  Simulating failed login attempts...")
        for i in range(4):
            requests.post(f"{BASE_URL}/api/auth/login", json={
                "username": username,
                "password": f"WrongPassword{i}",
                "deviceFingerprint": "suspicious-device"
            })
            print(f"  ✗ Failed attempt {i+1}")
            time.sleep(0.3)

        # Now login with correct password (should be HIGH risk)
        login_response = requests.post(f"{BASE_URL}/api/auth/login", json={
            "username": username,
            "password": "Test123",
            "deviceFingerprint": "suspicious-device"
        })

        if login_response.status_code == 200:
            data = login_response.json()
            print(f"  ✓ Login successful after failed attempts")
            print(f"  Account Locked: {data.get('accountLocked', False)}")
            print(f"  Risk should be HIGH due to recent failed logins")
        else:
            print(f"  ✗ Login failed: {login_response.status_code}")

    print()

    # Test Scenario 4: Rapid Logins (Potential Bot)
    print("Scenario 4: Rapid Login Attempts (Bot Detection)")
    print("-" * 70)

    timestamp = int(time.time())
    username = f"rapiduser_{timestamp}"

    reg_response = requests.post(f"{BASE_URL}/api/users/register", json={
        "username": username,
        "email": f"{username}@example.com",
        "password": "Test123"
    })

    if reg_response.status_code == 201:
        print(f"✓ Registered: {username}")

        # Rapid login attempts (5 in quick succession)
        print("  Simulating rapid login attempts...")
        for i in range(5):
            login_response = requests.post(f"{BASE_URL}/api/auth/login", json={
                "username": username,
                "password": "Test123",
                "deviceFingerprint": f"bot-device-{i}"
            })

            if login_response.status_code == 200:
                print(f"  ✓ Rapid login {i+1}")
            elif login_response.status_code == 429:
                print(f"  ⚠ Rate limited on attempt {i+1}")
                break

            time.sleep(0.1)  # Very short delay

    print()

    # Summary
    print("=" * 70)
    print("Test Data Generation Complete!")
    print("=" * 70)
    print()
    print("Next Steps:")
    print("1. Login as admin at: http://localhost:3001/login")
    print("2. Navigate to: SOC Dashboard → Threat Intelligence tab")
    print("3. You should see multiple threat assessments with varying risk scores")
    print()
    print("Database Check:")
    print("  Run: python3 -c \"import psycopg2; conn=psycopg2.connect(host='localhost',")
    print("       port=5432, database='crud_test_db', user='postgres', password='postgres');")
    print("       cursor=conn.cursor(); cursor.execute('SELECT COUNT(*) FROM threat_assessments');")
    print("       print('Total assessments:', cursor.fetchone()[0])\"")
    print()
    print("Expected Results:")
    print("  • 3 normal logins (LOW risk: 0-39)")
    print("  • 3 new device logins (MEDIUM risk: 40-59)")
    print("  • 1 post-brute-force login (HIGH risk: 60-79)")
    print("  • 5 rapid logins (MEDIUM risk, possible rate limit)")
    print()

if __name__ == "__main__":
    test_threat_intelligence()
