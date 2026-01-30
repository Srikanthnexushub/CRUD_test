# Rate Limiting - Quick Start Guide

## Installation (3 Steps)

### Step 1: Import CSS
In your `App.jsx` or `index.jsx`:
```jsx
import './styles/RateLimit.css';
```

### Step 2: Wrap App with Provider
```jsx
import { RateLimitProvider } from './contexts/RateLimitContext';
import { RateLimitToast } from './components/RateLimit';

function App() {
  return (
    <RateLimitProvider>
      <RateLimitToast />
      {/* Your app content */}
    </RateLimitProvider>
  );
}
```

### Step 3: Use Components Where Needed

**User Dashboard:**
```jsx
import { UsageProgressBar } from './components/RateLimit';

<UsageProgressBar />
```

**Admin Panel:**
```jsx
import { RateLimitDashboard, RateLimitConfigModal } from './components/RateLimit';

const [showConfig, setShowConfig] = useState(false);

<RateLimitDashboard />
<button onClick={() => setShowConfig(true)}>Configure</button>
<RateLimitConfigModal
  isOpen={showConfig}
  onClose={() => setShowConfig(false)}
/>
```

**System Status:**
```jsx
import { RequestRateGauge } from './components/RateLimit';

<RequestRateGauge title="API Usage" />
```

## Components Overview

| Component | Purpose | Auto-Display |
|-----------|---------|--------------|
| **RateLimitToast** | Shows notification on 429 error | Yes |
| **UsageProgressBar** | Shows user's quota usage | No |
| **RequestRateGauge** | Visual gauge for usage | No |
| **RateLimitDashboard** | Admin monitoring panel | No |
| **RateLimitConfigModal** | Whitelist management | No |

## API Requirements

Your backend must:
1. Return these headers on ALL responses:
   - `X-RateLimit-Limit`
   - `X-RateLimit-Remaining`
   - `X-RateLimit-Reset`

2. Return 429 status with these headers when exceeded:
   - `Retry-After`
   - `X-RateLimit-Reset`

3. Implement these endpoints:
   - `GET /api/rate-limit/stats`
   - `GET /api/rate-limit/violations/recent`
   - `GET /api/rate-limit/violations/top-endpoints`
   - `GET /api/rate-limit/config`
   - `GET /api/rate-limit/whitelist`
   - `POST /api/rate-limit/whitelist`
   - `DELETE /api/rate-limit/whitelist/:id`

## How It Works

1. Axios interceptor extracts rate limit headers from responses
2. Dispatches events to `RateLimitContext`
3. Context updates state automatically
4. Components react to state changes
5. Toast appears automatically on 429 errors
6. Auto-retry with exponential backoff (max 3 attempts)

## Customization

### Change Colors
Edit `/frontend/src/styles/RateLimit.css`:
```css
.progress-bar-fill.success {
  background: linear-gradient(90deg, #your-color-1, #your-color-2);
}
```

### Access Rate Limit Data Programmatically
```jsx
import { useRateLimit } from './contexts/RateLimitContext';

function MyComponent() {
  const { limits, exceeded, getUsagePercentage } = useRateLimit();

  return (
    <div>
      <p>Used: {getUsagePercentage()}%</p>
      <p>Remaining: {limits.remaining}</p>
      {exceeded && <p>Rate limit exceeded!</p>}
    </div>
  );
}
```

## Troubleshooting

**Toast not showing?**
- Ensure `RateLimitProvider` wraps your app
- Check backend returns `X-RateLimit-Reset` header on 429

**Progress bar shows 0%?**
- Backend must send headers on ALL responses (not just errors)
- Check CORS allows reading custom headers

**Dashboard shows loading forever?**
- Verify API endpoints exist
- Check JWT token is valid
- Review browser console for errors

## File Locations

```
frontend/src/
├── contexts/RateLimitContext.jsx
├── components/RateLimit/
│   ├── RateLimitToast.jsx
│   ├── UsageProgressBar.jsx
│   ├── RequestRateGauge.jsx
│   ├── RateLimitDashboard.jsx
│   ├── RateLimitConfigModal.jsx
│   ├── index.js
│   └── README.md (full documentation)
├── styles/RateLimit.css
└── services/api.js (updated)
```

## Full Documentation

See `/frontend/src/components/RateLimit/README.md` for:
- Complete API specifications
- Advanced usage examples
- Testing strategies
- Performance optimization tips
- Browser compatibility details

## Support

1. Check component README
2. Review integration examples in `/frontend/src/utils/rateLimitIntegration.example.jsx`
3. Inspect browser console for errors
4. Verify backend logs
