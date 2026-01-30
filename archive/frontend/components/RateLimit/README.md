# Rate Limiting Frontend Components

Complete React implementation for Rate Limiting visualization and management in the CRUD Test Application.

## Overview

This package provides a comprehensive set of React components for tracking, displaying, and managing API rate limits with automatic header extraction and 429 error handling.

## Components

### 1. RateLimitContext (Context Provider)

**Location:** `/frontend/src/contexts/RateLimitContext.jsx`

**Purpose:** Global state management for rate limit data

**Features:**
- Tracks rate limit headers (limit, remaining, reset)
- Listens to axios interceptor events
- Auto-reset timer on limit expiration
- Provides hooks for accessing rate limit data

**Usage:**
```jsx
import { RateLimitProvider, useRateLimit } from './contexts/RateLimitContext';

// Wrap your app
<RateLimitProvider>
  <App />
</RateLimitProvider>

// Access in components
const { limits, exceeded, getUsagePercentage } = useRateLimit();
```

**API:**
- `limits` - Object with `limit`, `remaining`, `reset` (Unix timestamp)
- `exceeded` - Boolean indicating if limit is exceeded
- `updateLimits(headers)` - Update limits from response headers
- `handleRateLimitExceeded(resetTime)` - Handle 429 errors
- `getSecondsUntilReset()` - Calculate seconds until reset
- `getUsagePercentage()` - Calculate usage percentage (0-100)

---

### 2. RateLimitToast

**Location:** `/frontend/src/components/RateLimit/RateLimitToast.jsx`

**Purpose:** Auto-triggered toast notification on rate limit exceeded (429)

**Features:**
- Appears automatically on 429 errors
- Countdown timer to reset
- Current limit display
- Dismiss button
- Auto-dismiss when reset time arrives
- Slide-in animation

**Display:**
```
┌─────────────────────────────────┐
│ ⚠️  Rate Limit Exceeded         │
│                                  │
│ You have reached the request    │
│ limit of 100 requests.          │
│                                  │
│ Resets in: 45s              ✕   │
└─────────────────────────────────┘
```

**Props:** None (automatically triggered)

---

### 3. UsageProgressBar

**Location:** `/frontend/src/components/RateLimit/UsageProgressBar.jsx`

**Purpose:** User-facing quota display component

**Features:**
- Color-coded progress bar (green/yellow/red)
- Percentage display
- Remaining requests count
- Status badge (Normal/High/Critical)
- Reset time display

**Visual:**
```
API Usage                      Critical
                              95%
                         95 / 100 requests
[████████████████████░]
5 requests remaining    Resets: 10:45:30 AM
```

**Color Coding:**
- Green (0-70%): Normal usage
- Yellow (70-90%): Warning
- Red (90-100%): Critical

**Props:** None (reads from context)

---

### 4. RequestRateGauge

**Location:** `/frontend/src/components/RateLimit/RequestRateGauge.jsx`

**Purpose:** Visual gauge for current rate limit usage

**Features:**
- Semi-circular gauge display
- Animated needle
- Color-coded based on usage
- Threshold markers at 0%, 70%, 90%
- Center value display
- Legend for status levels

**Visual:**
```
      Request Rate

      ╭─────○─────╮
     ╱     ╱│╲     ╲
    │     ╱ │ ╲     │
    │    ╱  │  ╲    │
    │   ●   │   ●   │
    0      75      100
          of 100

● Normal  ● Warning  ● Critical
```

**Props:**
- `title` (string, optional) - Custom title (default: "Request Rate")

---

### 5. RateLimitDashboard

**Location:** `/frontend/src/components/RateLimit/RateLimitDashboard.jsx`

**Purpose:** Admin dashboard for rate limit monitoring

**Features:**
- Statistics cards (violations 24h, 7d, total requests, active whitelists)
- Recent violations table with IP, user, endpoint
- Top blocked endpoints chart
- Configuration summary
- Auto-refresh every 30 seconds
- Manual refresh button

**Sections:**
1. **Statistics Cards:**
   - Violations (24h)
   - Violations (7d)
   - Total Requests (24h)
   - Active Whitelists

2. **System Load Gauge:**
   - Embedded RequestRateGauge component

3. **Recent Violations Table:**
   - Timestamp
   - IP Address
   - Username
   - Endpoint
   - Limit configuration

4. **Top Blocked Endpoints Chart:**
   - Horizontal bar chart
   - Violation counts
   - Auto-scaled

5. **Configuration Summary:**
   - Status (Enabled/Disabled)
   - Default Limit
   - Time Window

**Props:** None

---

### 6. RateLimitConfigModal

**Location:** `/frontend/src/components/RateLimit/RateLimitConfigModal.jsx`

**Purpose:** Admin modal for whitelist configuration

**Features:**
- Add whitelist entries (IP or User ID)
- Expiration date picker
- Reason textarea
- Active whitelist table
- Remove whitelist entries
- Real-time validation
- Success/error alerts

**Form Fields:**
- Type: IP Address / User ID
- Identifier: IP or user ID value
- Expiration Date: Optional datetime picker
- Reason: Required textarea

**Whitelist Table Columns:**
- Type badge
- Identifier
- Reason
- Created timestamp
- Expires timestamp
- Actions (Remove button)

**Props:**
- `isOpen` (boolean, required) - Modal visibility
- `onClose` (function, required) - Close handler
- `onSuccess` (function, optional) - Success callback

**Usage:**
```jsx
const [showModal, setShowModal] = useState(false);

<RateLimitConfigModal
  isOpen={showModal}
  onClose={() => setShowModal(false)}
  onSuccess={() => {
    console.log('Whitelist updated');
  }}
/>
```

---

## API Integration

### Axios Interceptor

**Location:** `/frontend/src/services/api.js`

**Response Interceptor:**
- Extracts `X-RateLimit-*` headers from all responses
- Dispatches `ratelimit-update` event with headers
- RateLimitContext listens and updates state

**Error Interceptor:**
- Catches 429 (Too Many Requests) errors
- Dispatches `ratelimit-exceeded` event
- Implements exponential backoff retry (max 3 attempts)
- Provides retry-after timing

**Events Dispatched:**
```javascript
// On successful response
window.dispatchEvent(new CustomEvent('ratelimit-update', {
  detail: {
    limit: '100',
    remaining: '75',
    reset: '1706543210'
  }
}));

// On 429 error
window.dispatchEvent(new CustomEvent('ratelimit-exceeded', {
  detail: {
    retryAfter: 60,
    reset: '1706543210',
    message: 'Too many requests'
  }
}));
```

---

## Backend API Endpoints

All endpoints are prefixed with `/api/rate-limit/`

### Statistics & Monitoring

**GET `/api/rate-limit/stats`**
```json
{
  "violations24h": 45,
  "violations7d": 320,
  "totalRequests24h": 12500,
  "activeWhitelists": 3
}
```

**GET `/api/rate-limit/violations/recent?limit=10`**
```json
[
  {
    "timestamp": "2024-01-29T10:30:00Z",
    "ipAddress": "192.168.1.100",
    "username": "john_doe",
    "endpoint": "/api/users",
    "limit": 100,
    "windowSeconds": 60
  }
]
```

**GET `/api/rate-limit/violations/top-endpoints?limit=5`**
```json
[
  {
    "path": "/api/users",
    "count": 45
  },
  {
    "path": "/api/auth/login",
    "count": 32
  }
]
```

### Configuration

**GET `/api/rate-limit/config`**
```json
{
  "defaultLimit": 100,
  "windowSeconds": 60,
  "enabled": true
}
```

**PUT `/api/rate-limit/config`**
```json
{
  "defaultLimit": 150,
  "windowSeconds": 60,
  "enabled": true
}
```

### Whitelist Management

**GET `/api/rate-limit/whitelist`**
```json
[
  {
    "id": 1,
    "type": "IP_ADDRESS",
    "identifier": "192.168.1.100",
    "reason": "Internal monitoring system",
    "createdAt": "2024-01-29T10:00:00Z",
    "expiresAt": "2024-12-31T23:59:59Z"
  }
]
```

**POST `/api/rate-limit/whitelist`**
```json
{
  "type": "IP_ADDRESS",
  "identifier": "192.168.1.100",
  "expiresAt": "2024-12-31T23:59:59Z",
  "reason": "Internal monitoring system"
}
```

**DELETE `/api/rate-limit/whitelist/:id`**

### User Info

**GET `/api/rate-limit/user/info`**
```json
{
  "limit": 100,
  "remaining": 75,
  "reset": 1706543210,
  "windowSeconds": 60
}
```

---

## Response Headers

All API responses include rate limit headers:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 75
X-RateLimit-Reset: 1706543210
```

429 responses include:
```
Retry-After: 60
X-RateLimit-Reset: 1706543210
```

---

## Installation & Setup

### 1. Install Dependencies

All dependencies are already in package.json:
- `react` ^18.2.0
- `react-dom` ^18.2.0
- `axios` ^1.6.0

### 2. Import Styles

Add to your main App.jsx or index.jsx:
```jsx
import './styles/RateLimit.css';
```

### 3. Wrap App with Provider

```jsx
import { RateLimitProvider } from './contexts/RateLimitContext';
import { RateLimitToast } from './components/RateLimit';

function App() {
  return (
    <RateLimitProvider>
      <RateLimitToast />
      {/* Your app components */}
    </RateLimitProvider>
  );
}
```

### 4. Use Components

```jsx
// User Dashboard
import { UsageProgressBar } from './components/RateLimit';

function UserDashboard() {
  return (
    <div>
      <UsageProgressBar />
      {/* ... */}
    </div>
  );
}

// Admin Panel
import { RateLimitDashboard, RateLimitConfigModal } from './components/RateLimit';

function AdminPanel() {
  const [showConfig, setShowConfig] = useState(false);

  return (
    <div>
      <button onClick={() => setShowConfig(true)}>Configure</button>
      <RateLimitDashboard />
      <RateLimitConfigModal
        isOpen={showConfig}
        onClose={() => setShowConfig(false)}
      />
    </div>
  );
}
```

---

## Styling

All components use the unified stylesheet at `/frontend/src/styles/RateLimit.css`

**Key CSS Classes:**
- `.rate-limit-toast` - Toast notification
- `.usage-progress-bar` - Progress bar container
- `.request-rate-gauge` - Gauge component
- `.rate-limit-dashboard` - Dashboard container
- `.rate-limit-config-modal` - Modal dialog

**Customization:**
Override CSS variables or classes in your own stylesheet:
```css
.usage-progress-bar {
  border-radius: 12px;
  padding: 24px;
}

.progress-bar-fill.success {
  background: linear-gradient(90deg, #custom-green-1, #custom-green-2);
}
```

---

## Testing

### Manual Testing

1. **Test Rate Limit Tracking:**
   - Make API requests
   - Watch UsageProgressBar update
   - Verify percentage calculation

2. **Test 429 Handling:**
   - Trigger rate limit (make many requests)
   - Toast should appear
   - Countdown timer should work
   - Auto-dismiss on reset

3. **Test Admin Dashboard:**
   - View statistics
   - Check violations table
   - Verify chart rendering
   - Test refresh button

4. **Test Configuration Modal:**
   - Add IP whitelist
   - Add User ID whitelist
   - Set expiration date
   - Remove entries
   - Verify validation

### Integration Testing

```javascript
// Test context
import { renderHook } from '@testing-library/react';
import { RateLimitProvider, useRateLimit } from './contexts/RateLimitContext';

test('updates limits from headers', () => {
  const { result } = renderHook(() => useRateLimit(), {
    wrapper: RateLimitProvider
  });

  act(() => {
    result.current.updateLimits({
      'x-ratelimit-limit': '100',
      'x-ratelimit-remaining': '75',
      'x-ratelimit-reset': '1706543210'
    });
  });

  expect(result.current.limits.limit).toBe(100);
  expect(result.current.limits.remaining).toBe(75);
});
```

---

## Troubleshooting

### Toast Not Appearing

**Problem:** Toast doesn't show on 429 errors

**Solutions:**
1. Check RateLimitProvider is wrapping your app
2. Verify axios interceptor is configured
3. Check browser console for events
4. Ensure backend returns X-RateLimit-Reset header

### Progress Bar Shows 0%

**Problem:** Progress bar always shows empty

**Solutions:**
1. Backend must send X-RateLimit-* headers on ALL responses
2. Check browser Network tab for headers
3. Verify CORS allows headers to be read
4. Check context is receiving events

### Dashboard Shows No Data

**Problem:** Dashboard displays "Loading..." forever

**Solutions:**
1. Check API endpoints return correct JSON structure
2. Verify JWT token is valid
3. Check backend rate limit service is running
4. Review browser console for API errors

---

## Performance Considerations

- **Context Updates:** Context only updates on header changes
- **Dashboard Refresh:** Auto-refresh rate is 30 seconds (configurable)
- **Event Listeners:** Properly cleaned up in useEffect hooks
- **Chart Rendering:** Uses CSS animations instead of JS for smoothness

---

## Browser Compatibility

- Chrome/Edge: ✅ Full support
- Firefox: ✅ Full support
- Safari: ✅ Full support (iOS 14+)
- IE11: ❌ Not supported (uses modern React features)

---

## Future Enhancements

1. **Real-time Updates:** WebSocket integration for live violations
2. **Historical Charts:** Time-series graphs using Recharts
3. **Export Reports:** CSV/PDF export for violations
4. **User Notifications:** Email alerts on repeated violations
5. **Rate Limit Profiles:** Different limits per user role
6. **Geographic Blocking:** IP-based geo-restrictions

---

## License

Part of CRUD Test Application - Internal Use Only

---

## Support

For issues or questions:
1. Check this README
2. Review `/frontend/src/utils/rateLimitIntegration.example.jsx`
3. Inspect browser console for errors
4. Check backend logs for API issues
