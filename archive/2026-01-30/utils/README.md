# Utility Functions

This directory contains reusable utility functions for validation, formatting, colors, and device fingerprinting.

## Validators

Functions for validating user input and data.

### Available Validators

- `validateTOTP(code)` - Validates 6-digit TOTP codes
- `validateBackupCode(code)` - Validates 8-digit backup codes
- `validateEmail(email)` - Validates email addresses
- `validateIPAddress(ip)` - Validates IPv4 and IPv6 addresses
- `validatePassword(password, options)` - Validates password strength
- `validateUsername(username, options)` - Validates usernames

**Example:**
```javascript
import { validateEmail, validateTOTP, validatePassword } from '@/utils';

const emailResult = validateEmail('user@example.com');
if (!emailResult.isValid) {
  console.error(emailResult.error);
}

const totpResult = validateTOTP('123456');
if (totpResult.isValid) {
  // Submit TOTP code
}

const passwordResult = validatePassword('MyP@ssw0rd', {
  minLength: 8,
  requireSpecial: true
});
if (!passwordResult.isValid) {
  console.error(passwordResult.errors);
}
```

---

## Formatters

Functions for formatting dates, numbers, and other data types.

### Available Formatters

- `formatDate(date, format, options)` - Format dates with multiple styles
- `formatRelativeTime(date)` - Format as relative time (e.g., "2 hours ago")
- `formatRiskScore(score, options)` - Format risk scores with emoji
- `formatDuration(ms, options)` - Format duration to human-readable
- `formatBytes(bytes, options)` - Format file sizes
- `formatNumber(number, options)` - Format numbers with separators
- `formatPercentage(value, options)` - Format percentages
- `truncateText(text, maxLength, options)` - Truncate text with ellipsis

**Example:**
```javascript
import { formatDate, formatDuration, formatRiskScore } from '@/utils';

formatDate(new Date(), 'short'); // '1/29/26'
formatDate(new Date(), 'long'); // 'January 29, 2026'
formatDate(new Date(), 'relative'); // '2 hours ago'

formatDuration(90000); // '1 minute 30 seconds'
formatDuration(3661000, { short: true }); // '1h 1m 1s'

formatRiskScore(75); // '🔴 High (75)'
formatRiskScore(25, { includeEmoji: false }); // 'Low (25)'
```

---

## Color Schemes

Constants and utilities for consistent theming and color management.

### Available Color Schemes

- `RISK_COLORS` - Colors for risk levels (low, medium, high, critical)
- `STATUS_COLORS` - Colors for various statuses
- `THEME_COLORS` - Application theme colors
- `CHART_COLORS` - Colors for data visualization
- `SEVERITY_COLORS` - Colors for log severity levels

### Helper Functions

- `getRiskColor(riskScore)` - Get color scheme by risk score
- `getStatusColor(status)` - Get color scheme by status
- `getSeverityColor(severity)` - Get color scheme by severity
- `hexToRgb(hex)` - Convert hex to RGB
- `rgbToHex(r, g, b)` - Convert RGB to hex
- `addAlpha(hex, alpha)` - Add alpha channel to hex color

**Example:**
```javascript
import { RISK_COLORS, getRiskColor, addAlpha } from '@/utils';

// Use predefined colors
const lowRiskColor = RISK_COLORS.low.primary; // '#10b981'

// Get color by risk score
const colors = getRiskColor(75); // Returns high risk colors
console.log(colors.primary); // '#ef4444'

// Add transparency
const transparentColor = addAlpha('#3b82f6', 0.5); // 'rgba(59, 130, 246, 0.5)'
```

---

## Device Fingerprinting

Utilities for generating and managing device fingerprints using FingerprintJS.

### Main Functions

- `getDeviceFingerprint(options)` - Generate unique device fingerprint
- `clearFingerprintCache()` - Clear cached fingerprint
- `getBasicDeviceInfo()` - Get basic device info without fingerprinting
- `isFingerprintingSupported()` - Check if supported in browser
- `compareFingerprintIds(fp1, fp2)` - Compare two fingerprints
- `hashFingerprint(visitorId)` - Hash fingerprint for storage
- `initializeFingerprinting(options)` - Initialize on app load

### Legacy Functions (Backward Compatible)

- `generateDeviceFingerprint()` - Legacy fingerprint generation
- `getSessionInfo()` - Get session info with fingerprint

**Example:**
```javascript
import {
  getDeviceFingerprint,
  initializeFingerprinting,
  getSessionInfo
} from '@/utils';

// Initialize in App.jsx
useEffect(() => {
  initializeFingerprinting({ background: true });
}, []);

// Get fingerprint when needed
const fingerprint = await getDeviceFingerprint();
console.log(fingerprint.visitorId);

// Use in login/registration
const sessionInfo = await getSessionInfo();
// Send to backend with auth request
```

## Installation

Install required dependencies:

```bash
npm install @fingerprintjs/fingerprintjs
```

## Importing

Import utilities by category or individually:

```javascript
// Category imports
import * as validators from '@/utils/validators';
import * as formatters from '@/utils/formatters';
import * as colors from '@/utils/colorSchemes';

// Individual imports
import { validateEmail, formatDate, RISK_COLORS } from '@/utils';

// Direct imports
import { validateTOTP } from '@/utils/validators';
import { formatBytes } from '@/utils/formatters';
```

## TypeScript Support

All utilities include comprehensive JSDoc comments with type information for IDE autocomplete and IntelliSense support.
