# Threat Intelligence Testing Guide

## Overview

The Threat Intelligence system automatically analyzes every login attempt and assigns a **risk score (0-100)** based on multiple factors. This guide shows you how to test it.

---

## 🎯 Quick Test (5 minutes)

### Step 1: Generate Threat Assessments

Simply **login and logout** a few times to generate threat assessment data:

```bash
# Quick test script
cd /Users/ainexusstudio/Documents/GitHub/CRUD_test
python3 << 'EOF'
import requests
import time

BASE_URL = "http://localhost:8080"

# Register and login 5 times with different users
for i in range(5):
    username = f"testuser{int(time.time())}_{i}"

    # Register
    requests.post(f"{BASE_URL}/api/users/register", json={
        "username": username,
        "email": f"{username}@test.com",
        "password": "Test123"
    })

    # Login (this triggers threat assessment)
    response = requests.post(f"{BASE_URL}/api/auth/login", json={
        "username": username,
        "password": "Test123",
        "deviceFingerprint": f"test-device-{i}"
    })

    print(f"✓ Login {i+1}: User {username} - Status {response.status_code}")
    time.sleep(1)

print("\n✓ Generated 5 threat assessments!")
print("Now check the SOC Dashboard → Threat Intelligence tab")
EOF
```

### Step 2: View Results

1. **Login as admin** at http://localhost:3001/login
2. Navigate to **SOC Dashboard** → **Threat Intelligence** tab
3. You should now see **5 threat assessments**

---

## 📊 What Gets Analyzed

Every login triggers an **async threat assessment** that checks:

### 1. IP Reputation (0-40 points)
- Queries **AbuseIPDB** API (if configured)
- Checks if IP is known for malicious activity
- Higher abuse score = higher risk

### 2. VPN/Proxy/Tor Detection (15-30 points)
- VPN detected: +15 points
- Proxy detected: +15 points
- Tor exit node: +30 points

### 3. Location Anomaly (20 points)
- Compares current login location with previous logins
- Different country/city = suspicious
- Requires **IP-API.com** geolocation

### 4. Recent Failed Logins (10 points)
- More than 3 failed attempts recently = +10 points

### 5. New Device (10 points)
- Device fingerprint not seen before = +10 points

### 6. Unusual Time (5 points)
- Login at 2 AM when user normally logs in at 9 AM = +5 points

**Total Risk Score: 0-100 (capped)**

---

## 🧪 Testing Different Risk Levels

### Test 1: LOW Risk (0-39 points) ✅

**Scenario:** Normal login from home

```python
import requests

# Login normally (should be LOW risk)
response = requests.post("http://localhost:8080/api/auth/login", json={
    "username": "testuser",
    "password": "Test123",
    "deviceFingerprint": "normal-device-123"
})

print(f"Response: {response.json()}")
# Expected: accountLocked = false, normal login
```

**Expected Result:**
- Risk Score: 0-39 (LOW)
- Color: Green badge in dashboard
- Action: None (login allowed)

---

### Test 2: MEDIUM Risk (40-59 points) ⚠️

**Scenario:** Login from new device in different location

**How to trigger:**
1. Use VPN or different network
2. Change device fingerprint
3. Login at unusual time (e.g., 3 AM if you normally login at 9 AM)

```python
import requests

# Login with new device fingerprint
response = requests.post("http://localhost:8080/api/auth/login", json={
    "username": "testuser",
    "password": "Test123",
    "deviceFingerprint": "new-device-456"  # Different from before
})

print(f"Response: {response.json()}")
# Expected: Email alert sent (if SMTP configured), login allowed
```

**Expected Result:**
- Risk Score: 40-59 (MEDIUM)
- Color: Yellow badge
- Action: Email alert sent (if SMTP configured)
- Login: Allowed

---

### Test 3: HIGH Risk (60-79 points) 🔶

**Scenario:** Login from VPN + new device

**How to trigger:**
1. Connect to VPN
2. Use new device fingerprint
3. Login from different location

**Expected Result:**
- Risk Score: 60-79 (HIGH)
- Color: Orange badge
- Action: Email alert sent
- Login: Allowed but flagged

---

### Test 4: CRITICAL Risk (80-100 points) 🔴

**Scenario:** Multiple suspicious factors

**How to trigger:**
1. **5 failed login attempts** first
2. Then login from **VPN + Tor** (if possible)
3. Use **new device**
4. Login at **unusual time**

```python
import requests

BASE_URL = "http://localhost:8080"

# Step 1: Trigger 5 failed logins
for i in range(5):
    requests.post(f"{BASE_URL}/api/auth/login", json={
        "username": "testuser",
        "password": "WrongPassword",
        "deviceFingerprint": "attacker-device"
    })
    print(f"Failed attempt {i+1}")

# Step 2: Login with correct password (high risk)
response = requests.post(f"{BASE_URL}/api/auth/login", json={
    "username": "testuser",
    "password": "Test123",  # Correct password
    "deviceFingerprint": "attacker-device"
})

print(f"\nResponse: {response.json()}")
# Expected: accountLocked = true, lockDetails provided
```

**Expected Result:**
- Risk Score: 80-100 (CRITICAL)
- Color: Red badge
- **Action: ACCOUNT LOCKED** (30 minutes default)
- Login: **BLOCKED**
- Email: Security alert sent
- Response includes `lockDetails` with unlock time

---

## 🔍 Viewing Threat Assessments in Dashboard

### Admin Dashboard View

Navigate to **SOC Dashboard** → **Threat Intelligence** tab

**What you'll see:**

1. **Statistics Cards:**
   - Total Assessments (24h)
   - High Risk Assessments
   - Locked Accounts
   - Average Risk Score

2. **Assessment Table:**
   - User
   - IP Address
   - Risk Score (color-coded badge)
   - Location
   - Timestamp
   - Actions (View Details, Unlock)

3. **Geographic Heatmap:**
   - Interactive map showing login locations
   - Color-coded markers by risk level
   - Cluster view for multiple logins from same area

4. **Filters:**
   - High Risk Only toggle
   - Time Range (24h, 7d, 30d, All)

---

## 🔓 Unlocking Locked Accounts

### Via Admin Dashboard:

1. Go to **Threat Intelligence** tab
2. Find the locked account (red badge)
3. Click **Unlock** button
4. Account immediately unlocked

### Via API:

```bash
curl -X POST http://localhost:8080/api/threat/unlock-account/14 \
  -H "Authorization: Bearer <admin-token>"
```

### Via Database:

```sql
UPDATE users
SET is_account_locked = false,
    account_locked_until = NULL,
    lock_reason = NULL
WHERE id = 14;
```

---

## 📧 Email Notifications (if SMTP configured)

The system sends emails for:

1. **High Risk Login (60+):**
   - Subject: "High Risk Login Detected"
   - Includes: IP, location, risk score, factors

2. **Account Locked (80+):**
   - Subject: "Account Locked Due to Suspicious Activity"
   - Includes: Lock reason, unlock time, contact info

3. **New Device Login:**
   - Subject: "New Device Login Detected"
   - Includes: Device info, location, trust device link

---

## 🎛️ Advanced Testing

### Test with AbuseIPDB API (Optional)

If you configure an AbuseIPDB API key, the system will check IP reputation:

```bash
# Set API key
export ABUSEIPDB_API_KEY="your-api-key"

# Restart backend
cd /Users/ainexusstudio/Documents/GitHub/CRUD_test
kill -9 $(lsof -ti:8080)
java -jar target/CRUD_test-1.0-SNAPSHOT.jar

# Now login from known malicious IP (for testing, use a VPN)
```

**Get API Key:** https://www.abuseipdb.com/account/api

---

## 📊 Database Queries for Testing

### View All Threat Assessments:

```python
import psycopg2

conn = psycopg2.connect(
    host='localhost',
    port=5432,
    database='crud_test_db',
    user='postgres',
    password='postgres'
)

cursor = conn.cursor()

# Get all assessments with user info
cursor.execute("""
    SELECT
        ta.id,
        u.username,
        ta.ip_address,
        ta.risk_score,
        ta.location_country,
        ta.location_city,
        ta.is_vpn,
        ta.is_proxy,
        ta.assessed_at
    FROM threat_assessments ta
    JOIN users u ON ta.user_id = u.id
    ORDER BY ta.assessed_at DESC
    LIMIT 20
""")

print("Recent Threat Assessments:")
print("-" * 100)
for row in cursor.fetchall():
    print(f"ID: {row[0]} | User: {row[1]} | IP: {row[2]} | Risk: {row[3]} | "
          f"Location: {row[4]}, {row[5]} | VPN: {row[6]} | Proxy: {row[7]} | "
          f"Time: {row[8]}")

cursor.close()
conn.close()
```

### View Locked Accounts:

```sql
SELECT
    id,
    username,
    email,
    lock_reason,
    account_locked_until,
    created_at
FROM users
WHERE is_account_locked = true
ORDER BY account_locked_until DESC;
```

---

## 🐛 Troubleshooting

### "No assessments showing"

**Cause:** No login activity yet
**Solution:** Run the Quick Test script above

### "All scores are 0"

**Cause:** IP-API and AbuseIPDB not configured
**Solution:** This is normal - system can still track logins, just without external data

### "Account locked but can't unlock"

**Cause:** Locked accounts auto-unlock after timeout
**Solution:** Wait for `account_locked_until` time or unlock via admin panel

### "Geographic map not showing"

**Cause:** No geolocation data (IP-API not responding)
**Solution:** Check network, IP-API rate limits (45 req/min free tier)

---

## 🎯 Real-World Testing Scenarios

### Scenario 1: Stolen Credentials

```python
# Simulate attacker trying stolen credentials

# 1. Legitimate user logs in (LOW risk)
login("testuser", "Test123", "user-device-abc")

# 2. Attacker tries same credentials from different location/device (HIGH risk)
login("testuser", "Test123", "attacker-device-xyz")

# Expected: Second login flagged as HIGH risk, email sent
```

### Scenario 2: Brute Force Attack

```python
# Simulate brute force

# Try 5 wrong passwords
for i in range(5):
    login("testuser", f"WrongPass{i}", "attacker-bot")

# Try correct password (CRITICAL risk)
response = login("testuser", "Test123", "attacker-bot")

# Expected: Account locked, risk score 80+
```

### Scenario 3: Account Takeover Prevention

```python
# User normally logs in from New York

# 1. Normal login from home (LOW risk)
login("testuser", "Test123", "home-device", location="New York")

# 2. Suspicious login from Russia 5 minutes later (HIGH risk)
login("testuser", "Test123", "new-device", location="Moscow")

# Expected: HIGH risk due to location anomaly
```

---

## 📈 Success Criteria

Your threat intelligence system is working correctly if:

✅ Every login creates a threat assessment record
✅ Risk scores are calculated (0-100 range)
✅ High risk logins (80+) trigger account locks
✅ Admin can view assessments in dashboard
✅ Geographic map shows login locations
✅ Locked accounts can be unlocked by admin
✅ Email alerts sent (if SMTP configured)
✅ Audit logs record all security events

---

## 🚀 Quick Start Commands

```bash
# 1. Generate test data
cd /Users/ainexusstudio/Documents/GitHub/CRUD_test
python3 test-threat-intelligence.py

# 2. View in dashboard
# Login as admin → http://localhost:3001/soc-dashboard
# Click "Threat Intelligence" tab

# 3. Check database
python3 -c "
import psycopg2
conn = psycopg2.connect(host='localhost', port=5432, database='crud_test_db', user='postgres', password='postgres')
cursor = conn.cursor()
cursor.execute('SELECT COUNT(*) FROM threat_assessments')
print(f'Total threat assessments: {cursor.fetchone()[0]}')
"

# 4. View locked accounts
python3 -c "
import psycopg2
conn = psycopg2.connect(host='localhost', port=5432, database='crud_test_db', user='postgres', password='postgres')
cursor = conn.cursor()
cursor.execute('SELECT username, lock_reason FROM users WHERE is_account_locked = true')
print('Locked accounts:', cursor.fetchall())
"
```

---

## 📝 Summary

The Threat Intelligence system is **fully automated** and requires no configuration to work. It:

1. ✅ Analyzes every login attempt
2. ✅ Calculates risk scores based on multiple factors
3. ✅ Locks accounts automatically when risk is critical (80+)
4. ✅ Sends email alerts (if SMTP configured)
5. ✅ Provides admin dashboard for monitoring
6. ✅ Stores all assessments in database
7. ✅ Works with or without external APIs (AbuseIPDB, IP-API)

**Start testing by simply logging in a few times - the system does the rest!**

---

**Next:** Try the Quick Test script above to generate data, then explore the admin dashboard!
