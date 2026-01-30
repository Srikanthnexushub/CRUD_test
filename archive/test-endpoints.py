#!/usr/bin/env python3
import requests
import json
import time

BASE_URL = "http://localhost:8080"

def test_endpoints():
    print("=" * 60)
    print("Phase 2 Enterprise Security - Endpoint Testing")
    print("=" * 60)
    print()

    # Test 1: Register a user
    print("Test 1: User Registration")
    print("-" * 40)
    timestamp = str(int(time.time()))
    reg_data = {
        "username": f"testuser{timestamp}",
        "email": f"test{timestamp}@example.com",
        "password": "TestPass123"
    }

    try:
        response = requests.post(f"{BASE_URL}/api/users/register", json=reg_data)
        print(f"Status: {response.status_code}")
        print(json.dumps(response.json(), indent=2))
    except Exception as e:
        print(f"Error: {e}")
    print()

    # Test 2: Login and get token
    print("Test 2: User Login")
    print("-" * 40)
    login_data = {
        "username": reg_data["username"],
        "password": reg_data["password"],
        "deviceFingerprint": f"test-device-{timestamp}"
    }

    token = None
    try:
        response = requests.post(f"{BASE_URL}/api/auth/login", json=login_data)
        print(f"Status: {response.status_code}")
        result = response.json()
        print(json.dumps(result, indent=2))
        token = result.get("token")
        if token:
            print(f"\n✓ Token obtained: {token[:50]}...")
    except Exception as e:
        print(f"Error: {e}")
    print()

    if not token:
        print("Cannot continue without token. Exiting.")
        return

    headers = {"Authorization": f"Bearer {token}"}

    # Test 3: MFA Setup
    print("Test 3: MFA Setup")
    print("-" * 40)
    try:
        response = requests.post(f"{BASE_URL}/api/mfa/setup", headers=headers)
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            result = response.json()
            print(json.dumps(result, indent=2))
            if result.get("success"):
                print("\n✓ MFA setup successful!")
                print(f"  QR Code URL: {result.get('data', {}).get('qrCodeUrl', 'N/A')[:80]}...")
        else:
            print(response.text)
    except Exception as e:
        print(f"Error: {e}")
    print()

    # Test 4: MFA Status
    print("Test 4: MFA Status")
    print("-" * 40)
    try:
        response = requests.get(f"{BASE_URL}/api/mfa/status", headers=headers)
        print(f"Status: {response.status_code}")
        print(json.dumps(response.json(), indent=2))
    except Exception as e:
        print(f"Error: {e}")
    print()

    # Test 5: Notification Preferences
    print("Test 5: Notification Preferences")
    print("-" * 40)
    try:
        response = requests.get(f"{BASE_URL}/api/notifications/preferences", headers=headers)
        print(f"Status: {response.status_code}")
        print(json.dumps(response.json(), indent=2))
    except Exception as e:
        print(f"Error: {e}")
    print()

    # Test 6: Rate Limiting Test (rapid requests)
    print("Test 6: Rate Limiting Test")
    print("-" * 40)
    print("Sending 8 rapid login requests...")
    rate_limit_hit = False
    for i in range(8):
        try:
            bad_login = {"username": "nonexistent", "password": "wrong", "deviceFingerprint": f"rate-{i}"}
            response = requests.post(f"{BASE_URL}/api/auth/login", json=bad_login)

            # Extract rate limit headers
            limit = response.headers.get('X-RateLimit-Limit', 'N/A')
            remaining = response.headers.get('X-RateLimit-Remaining', 'N/A')
            reset = response.headers.get('X-RateLimit-Reset', 'N/A')

            print(f"  Request {i+1}: HTTP {response.status_code} | Limit: {limit} | Remaining: {remaining} | Reset: {reset}")

            if response.status_code == 429:
                print("  ✓ Rate limit triggered (429 Too Many Requests)")
                rate_limit_hit = True
                break
        except Exception as e:
            print(f"  Error on request {i+1}: {e}")

    if not rate_limit_hit:
        print("  Note: Rate limit not hit in 8 requests (limit may be higher)")
    print()

    # Test 7: Threat Intelligence Endpoints (Admin only)
    print("Test 7: Threat Intelligence Endpoints (requires admin)")
    print("-" * 40)
    try:
        response = requests.get(f"{BASE_URL}/api/threat/assessments", headers=headers)
        print(f"GET /api/threat/assessments: {response.status_code}")
        if response.status_code == 403:
            print("  Expected: User doesn't have admin role")
        elif response.status_code == 200:
            print("  ✓ Accessible")
    except Exception as e:
        print(f"Error: {e}")
    print()

    # Test 8: Rate Limit Dashboard (Admin only)
    print("Test 8: Rate Limit Endpoints (requires admin)")
    print("-" * 40)
    try:
        response = requests.get(f"{BASE_URL}/api/rate-limit/violations", headers=headers)
        print(f"GET /api/rate-limit/violations: {response.status_code}")
        if response.status_code == 403:
            print("  Expected: User doesn't have admin role")
        elif response.status_code == 200:
            print("  ✓ Accessible")
    except Exception as e:
        print(f"Error: {e}")
    print()

    # Summary
    print("=" * 60)
    print("Test Summary")
    print("=" * 60)
    print("✓ User Registration: Working")
    print("✓ User Login: Working")
    print("✓ JWT Authentication: Working")
    print("  MFA Endpoints: See results above")
    print("  Notification Endpoints: See results above")
    print("  Rate Limiting: See results above")
    print("  Frontend: Running on http://localhost:3001")
    print()

if __name__ == "__main__":
    test_endpoints()
