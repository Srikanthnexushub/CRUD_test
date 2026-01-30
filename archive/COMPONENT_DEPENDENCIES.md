# Component Dependencies Reference

## Updated Files and Their Dependencies

### 1. LoginForm.jsx
**Requires:**
- `MFAVerificationModal.jsx` - For MFA code entry
- `ThreatAlertModal.jsx` - For account lockout notifications
- `deviceFingerprint.js` - Already exists ✓

**Import statements added:**
```javascript
import MFAVerificationModal from './MFAVerificationModal';
import ThreatAlertModal from './ThreatAlertModal';
```

---

### 2. App.jsx
**Requires:**
- `MFAContext.jsx` - MFA state management
- `RateLimitContext.jsx` - Rate limit monitoring
- `NotificationContext.jsx` - Notification preferences
- `UserSettings.jsx` - Already created ✓
- `react-toastify` - npm package (needs installation)

**Import statements added:**
```javascript
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { MFAProvider } from './contexts/MFAContext';
import { RateLimitProvider } from './contexts/RateLimitContext';
import { NotificationProvider } from './contexts/NotificationContext';
import UserSettings from './components/UserSettings';
```

---

### 3. UserSettings.jsx
**Requires:**
- `MFASettings.jsx` - MFA configuration component
- `NotificationPreferences.jsx` - Email notification settings
- `UserSettings.css` - Already created ✓

**Import statements needed:**
```javascript
import MFASettings from './MFASettings';
import NotificationPreferences from './NotificationPreferences';
import '../styles/UserSettings.css';
```

---

### 4. SOCDashboard.jsx
**Requires:**
- `ThreatIntelligencePanel.jsx` - Threat intelligence dashboard
- `RateLimitDashboard.jsx` - Rate limiting statistics
- `EmailDashboard.jsx` - Email notification logs

**Import statements added:**
```javascript
import ThreatIntelligencePanel from './ThreatIntelligencePanel';
import RateLimitDashboard from './RateLimitDashboard';
import EmailDashboard from './EmailDashboard';
```

---

## Context Providers to Create

### MFAContext.jsx
**Purpose:** Manage MFA state and operations

**Required State:**
- `mfaEnabled` - Boolean
- `mfaSecret` - String (for QR code)
- `backupCodes` - Array
- `trustedDevices` - Array

**Required Methods:**
- `setupMFA()` - Initialize MFA setup
- `verifyMFASetup(code)` - Verify setup with code
- `disableMFA()` - Disable MFA
- `regenerateBackupCodes()` - Generate new backup codes
- `removeTrustedDevice(deviceId)` - Remove trusted device

**API Calls:**
```javascript
api.setupMFA()
api.verifyMFASetup(code)
api.verifyMFALogin(code, trustDevice)
api.disableMFA()
api.regenerateBackupCodes()
api.getTrustedDevices()
api.removeTrustedDevice(deviceId)
```

---

### RateLimitContext.jsx
**Purpose:** Monitor and display rate limit status

**Required State:**
- `rateLimitInfo` - Object { limit, remaining, reset }
- `isLimited` - Boolean
- `retryAfter` - Number (seconds)

**Required Methods:**
- `updateRateLimits(headers)` - Update from API headers
- `handleRateLimitExceeded(data)` - Handle 429 errors

**Event Listeners:**
```javascript
window.addEventListener('ratelimit-update', handler);
window.addEventListener('ratelimit-exceeded', handler);
```

---

### NotificationContext.jsx
**Purpose:** Manage email notification preferences

**Required State:**
- `preferences` - Object with notification settings
- `loading` - Boolean
- `error` - String

**Required Methods:**
- `loadPreferences()` - Load user preferences
- `updatePreferences(settings)` - Update preferences
- `testEmailNotification()` - Send test email

**API Calls:**
```javascript
api.getNotificationPreferences()
api.updateNotificationPreferences(settings)
api.testEmailNotification()
```

---

## Components to Create

### 1. MFAVerificationModal.jsx
**Purpose:** Modal for entering MFA code during login

**Props:**
- `tempToken` (string) - Temporary auth token
- `onVerified` (function) - Callback on success
- `onClose` (function) - Close modal

**Features:**
- 6-digit code input
- "Trust this device" checkbox
- Backup code option
- Error handling
- Loading state

**API Usage:**
```javascript
const response = await api.verifyMFA({
  tempToken,
  mfaCode,
  trustDevice
});
```

---

### 2. ThreatAlertModal.jsx
**Purpose:** Display account lockout notification

**Props:**
- `threatData` (object) - Threat intelligence data
  - `riskScore` (number)
  - `reasons` (array)
  - `blockedUntil` (timestamp)
  - `location` (string)
  - `ipAddress` (string)
- `onClose` (function) - Close modal

**Display Elements:**
- Risk score indicator
- List of threat reasons
- Lockout duration
- Contact support option
- Security tips

---

### 3. MFASettings.jsx
**Purpose:** MFA configuration panel in settings

**Features:**
- Enable/Disable MFA toggle
- QR code display for setup
- Backup codes display and regeneration
- Trusted devices list
- Remove device functionality

**State:**
- `mfaEnabled` - From MFAContext
- `showQRCode` - Boolean
- `showBackupCodes` - Boolean
- `verificationCode` - String

**Flow:**
1. User clicks "Enable MFA"
2. Call `api.setupMFA()` to get secret
3. Display QR code
4. User scans with authenticator app
5. User enters verification code
6. Call `api.verifyMFASetup(code)`
7. Display backup codes
8. MFA enabled

---

### 4. NotificationPreferences.jsx
**Purpose:** Email notification settings

**Settings:**
- Login alerts (enabled/disabled)
- Security alerts (enabled/disabled)
- Account changes (enabled/disabled)
- Rate limit warnings (enabled/disabled)
- Daily digest (enabled/disabled)
- Email address for notifications

**Features:**
- Toggle switches for each notification type
- Email input field
- Save button
- Test email button
- Success/error feedback

---

### 5. ThreatIntelligencePanel.jsx
**Purpose:** Display threat intelligence dashboard

**Sections:**
- Active threats list
- Risk score distribution chart
- Geographic threat map
- Blocked IPs list
- Threat trends over time

**API Calls:**
```javascript
api.getActiveThreats()
api.getThreatStats()
api.getBlockedIPs()
api.getThreatTrends(hours)
```

**Features:**
- Real-time updates via WebSocket
- Filterable threat list
- Export functionality
- Refresh button

---

### 6. RateLimitDashboard.jsx
**Purpose:** Rate limiting statistics and configuration

**Sections:**
- Current rate limit status
- Recent violations list
- Top blocked endpoints
- Whitelist management
- Configuration settings

**API Calls:**
```javascript
api.getRateLimitStats()
api.getRecentViolations(limit)
api.getTopBlockedEndpoints(limit)
api.getRateLimitConfig()
api.updateRateLimitConfig(config)
api.getActiveWhitelists()
api.addWhitelist(data)
api.removeWhitelist(id)
```

**Features:**
- Stats cards (total requests, violations, etc.)
- Violation timeline chart
- Whitelist CRUD operations
- Config editor (admin only)

---

### 7. EmailDashboard.jsx
**Purpose:** Email notification logs and settings

**Sections:**
- Recent emails sent
- Email delivery status
- Failed deliveries
- Email templates
- Sending statistics

**API Calls:**
```javascript
api.getEmailLogs(params)
api.getEmailStats()
api.getEmailTemplates()
api.testEmailDelivery()
```

**Features:**
- Paginated email log
- Status filters (sent, failed, pending)
- Search by recipient
- Resend failed emails
- Template preview

---

## CSS Files Status

| File | Status |
|------|--------|
| UserSettings.css | ✓ Created |
| SOCDashboard.css | ✓ Updated (added tab styles) |
| UserDashboard.css | ✓ Updated (added badge styles) |
| MFAVerificationModal.css | ✗ Needs creation |
| ThreatAlertModal.css | ✗ Needs creation |
| MFASettings.css | ✗ Needs creation |
| NotificationPreferences.css | ✗ Needs creation |
| ThreatIntelligencePanel.css | ✗ Needs creation |
| RateLimitDashboard.css | ✗ Needs creation |
| EmailDashboard.css | ✗ Needs creation |

---

## Installation Requirements

```bash
cd frontend
npm install react-toastify
npm install qrcode.react  # For QR code in MFA setup
npm install recharts       # If using charts in dashboards
```

---

## Backend API Endpoints Required

### MFA Endpoints
- `POST /api/mfa/setup` - Get MFA secret and QR code
- `POST /api/mfa/verify-setup` - Verify setup code
- `POST /api/mfa/verify-login` - Verify login code (with tempToken)
- `POST /api/mfa/disable` - Disable MFA
- `POST /api/mfa/regenerate-backup-codes` - Get new backup codes
- `GET /api/mfa/status` - Get MFA status
- `GET /api/mfa/trusted-devices` - List trusted devices
- `DELETE /api/mfa/trusted-devices/:id` - Remove device

### Rate Limiting Endpoints
- `GET /api/rate-limit/stats` - Get rate limit statistics
- `GET /api/rate-limit/violations/recent` - Recent violations
- `GET /api/rate-limit/violations/top-endpoints` - Most blocked endpoints
- `GET /api/rate-limit/config` - Get configuration
- `PUT /api/rate-limit/config` - Update configuration
- `GET /api/rate-limit/whitelist` - Get whitelisted IPs
- `POST /api/rate-limit/whitelist` - Add to whitelist
- `DELETE /api/rate-limit/whitelist/:id` - Remove from whitelist
- `GET /api/rate-limit/user/info` - User's rate limit info

### Threat Intelligence Endpoints
- `GET /api/threat/active` - Active threats
- `GET /api/threat/stats` - Threat statistics
- `GET /api/threat/blocked-ips` - Blocked IP addresses
- `GET /api/threat/trends` - Threat trends

### Email Notification Endpoints
- `GET /api/notifications/preferences` - Get user preferences
- `PUT /api/notifications/preferences` - Update preferences
- `POST /api/notifications/test` - Send test email
- `GET /api/notifications/logs` - Email logs
- `GET /api/notifications/stats` - Email statistics
- `GET /api/notifications/templates` - Email templates

---

## Component Tree

```
App
├── AuthProvider
│   ├── MFAProvider
│   │   ├── RateLimitProvider
│   │   │   └── NotificationProvider
│   │   │       ├── LoginForm
│   │   │       │   ├── MFAVerificationModal
│   │   │       │   └── ThreatAlertModal
│   │   │       ├── UserDashboard
│   │   │       ├── UserSettings
│   │   │       │   ├── Profile Tab
│   │   │       │   ├── Security Tab (MFASettings)
│   │   │       │   └── Notifications Tab (NotificationPreferences)
│   │   │       └── SOCDashboard
│   │   │           ├── Overview Tab
│   │   │           ├── Threat Intelligence Tab (ThreatIntelligencePanel)
│   │   │           ├── Rate Limiting Tab (RateLimitDashboard)
│   │   │           └── Email Tab (EmailDashboard)
│   │   └── ToastContainer
```

---

## Testing Scenarios

### MFA Flow
1. User without MFA logs in → Direct access
2. User with MFA logs in → MFAVerificationModal appears
3. Enter correct code → Login successful
4. Enter wrong code → Error message
5. Use backup code → Login successful
6. Trust device → No MFA on next login from same device

### Threat Detection Flow
1. Suspicious login detected → ThreatAlertModal appears
2. Display threat reasons and risk score
3. User must wait until unlock time
4. User can contact support

### Rate Limiting Flow
1. User makes many requests → Rate limit warnings
2. Rate limit exceeded → 429 error
3. Automatic retry with backoff
4. User sees rate limit info in UI
5. Admin can whitelist IP addresses

---

## Priority Order for Creation

### High Priority (Core Functionality)
1. MFAContext.jsx
2. MFAVerificationModal.jsx
3. ThreatAlertModal.jsx
4. MFASettings.jsx

### Medium Priority (Enhanced Features)
5. RateLimitContext.jsx
6. NotificationContext.jsx
7. NotificationPreferences.jsx
8. RateLimitDashboard.jsx

### Low Priority (Additional Dashboards)
9. ThreatIntelligencePanel.jsx
10. EmailDashboard.jsx

---

Generated: 2026-01-29
