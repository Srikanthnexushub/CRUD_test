#!/usr/bin/env python3
"""
Test script to verify the deviceFingerprint JSON parsing fix
"""
import requests
import json
import time

BASE_URL = "http://localhost:8080"

def test_login_with_fingerprint():
    print("=" * 60)
    print("Testing Login with Device Fingerprint Fix")
    print("=" * 60)
    print()

    # Test 1: Register a new user
    print("1. Registering test user...")
    timestamp = str(int(time.time()))
    username = f"testuser{timestamp}"
    email = f"test{timestamp}@example.com"
    password = "TestPass123"

    reg_response = requests.post(
        f"{BASE_URL}/api/users/register",
        json={"username": username, "email": email, "password": password}
    )

    if reg_response.status_code == 201:
        print(f"✓ User registered: {username}")
    else:
        print(f"✗ Registration failed: {reg_response.status_code}")
        print(reg_response.text)
        return

    print()

    # Test 2: Login WITHOUT device fingerprint (should work)
    print("2. Testing login WITHOUT deviceFingerprint...")
    login_response = requests.post(
        f"{BASE_URL}/api/auth/login",
        json={"username": username, "password": password}
    )

    print(f"   Status: {login_response.status_code}")
    if login_response.status_code == 200:
        print("   ✓ Login successful without fingerprint")
        data = login_response.json()
        print(f"   Token: {data.get('token', 'N/A')[:50]}...")
    else:
        print(f"   ✗ Login failed: {login_response.text}")

    print()

    # Test 3: Login WITH device fingerprint as string (should work)
    print("3. Testing login WITH deviceFingerprint (string)...")
    login_response = requests.post(
        f"{BASE_URL}/api/auth/login",
        json={
            "username": username,
            "password": password,
            "deviceFingerprint": "test-fingerprint-12345"
        }
    )

    print(f"   Status: {login_response.status_code}")
    if login_response.status_code == 200:
        print("   ✓ Login successful with fingerprint (string)")
        data = login_response.json()
        print(f"   Token: {data.get('token', 'N/A')[:50]}...")
        print(f"   MFA Required: {data.get('mfaRequired', False)}")
        print(f"   Account Locked: {data.get('accountLocked', False)}")
    else:
        print(f"   ✗ Login failed: {login_response.text}")

    print()

    # Test 4: Login WITH device fingerprint as object (should FAIL - this was the bug)
    print("4. Testing login WITH deviceFingerprint (object - should fail)...")
    login_response = requests.post(
        f"{BASE_URL}/api/auth/login",
        json={
            "username": username,
            "password": password,
            "deviceFingerprint": {
                "visitorId": "abc123",
                "confidence": 0.95
            }
        }
    )

    print(f"   Status: {login_response.status_code}")
    if login_response.status_code == 200:
        print("   ✗ Unexpected: Login succeeded with object (should fail)")
    elif login_response.status_code == 500:
        response_data = login_response.json()
        if "Cannot deserialize" in response_data.get('message', ''):
            print("   ✓ Expected error: Cannot deserialize object to String")
            print(f"   Message: {response_data.get('message', 'N/A')[:80]}...")
        else:
            print(f"   ? Different error: {response_data.get('message', 'N/A')}")
    else:
        print(f"   ? Unexpected status: {login_response.status_code}")
        print(f"   Response: {login_response.text[:200]}")

    print()
    print("=" * 60)
    print("Summary")
    print("=" * 60)
    print("The frontend fix (adding 'await') ensures deviceFingerprint")
    print("is sent as a STRING (visitor ID) instead of an object (Promise).")
    print()
    print("Before fix: generateDeviceFingerprint() → Promise object → Error")
    print("After fix:  await generateDeviceFingerprint() → String → Success")
    print()

if __name__ == "__main__":
    test_login_with_fingerprint()
