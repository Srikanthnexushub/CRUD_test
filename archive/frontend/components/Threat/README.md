# Threat Intelligence Components

This directory contains all React components for the Threat Intelligence system, providing real-time threat detection, risk assessment, and geographic visualization capabilities.

## Components Overview

### 1. RiskScoreBadge
A color-coded badge component that displays risk scores with appropriate visual indicators.

**Props:**
- `score` (number, required): Risk score from 0-100
- `size` (string, optional): 'small', 'medium', or 'large' (default: 'medium')
- `showLabel` (boolean, optional): Show risk level label (default: true)

**Risk Levels:**
- 0-39: GREEN (LOW)
- 40-59: YELLOW (MEDIUM)
- 60-79: ORANGE (HIGH)
- 80-100: RED (CRITICAL)

**Usage:**
```jsx
import { RiskScoreBadge } from './components/Threat';

<RiskScoreBadge score={75} size="large" />
```

### 2. ThreatAlertModal
Modal dialog shown when a user's account is locked due to suspicious activity.

**Props:**
- `threatAssessment` (object, required): Threat assessment data
- `onClose` (function, required): Callback when modal is closed
- `onContactSupport` (function, required): Callback for support button

**Features:**
- Displays risk score and factors
- Real-time countdown to unlock
- Contact support button
- Auto-closes when account unlocks

**Usage:**
```jsx
import { ThreatAlertModal } from './components/Threat';

<ThreatAlertModal
  threatAssessment={assessment}
  onClose={() => setShowModal(false)}
  onContactSupport={() => navigate('/support')}
/>
```

### 3. SessionDetailsCard
Card component displaying enhanced session information with threat indicators.

**Props:**
- `session` (object, required): Session data including device, IP, location
- `onViewDetails` (function, optional): Callback for viewing full details

**Features:**
- Device type detection with icons
- IP address and geolocation
- Risk score badge
- Threat factors list
- View details button

**Usage:**
```jsx
import { SessionDetailsCard } from './components/Threat';

<SessionDetailsCard
  session={sessionData}
  onViewDetails={(session) => handleViewDetails(session)}
/>
```

### 4. ThreatIntelligencePanel
Comprehensive admin dashboard panel for threat monitoring and management.

**Props:**
- `apiBaseUrl` (string, optional): Base URL for API calls (default: '/api/threat')

**Features:**
- Recent threat assessments table
- High-risk filter toggle
- Time range filtering (24h, 7d, 30d, all)
- Statistics cards (24h assessments, high-risk count, blocked logins, avg score)
- Account lock/unlock actions
- Auto-refresh every 30 seconds
- Detailed threat view modal

**Usage:**
```jsx
import { ThreatIntelligencePanel } from './components/Threat';

<ThreatIntelligencePanel apiBaseUrl="/api/threat" />
```

### 5. GeographicHeatmap
Interactive Leaflet map displaying threat locations with clustering.

**Props:**
- `apiBaseUrl` (string, optional): Base URL for API calls (default: '/api/threat')

**Features:**
- Color-coded markers by risk level
- Marker clustering for same locations
- Country filter dropdown
- Popup with threat details
- Map statistics (total, high-risk, blocked)
- Legend for risk levels
- Auto-refresh every minute

**Usage:**
```jsx
import { GeographicHeatmap } from './components/Threat';

<GeographicHeatmap apiBaseUrl="/api/threat" />
```

### 6. ThreatDetailsModal
Modal displaying full threat assessment details.

**Props:**
- `assessment` (object, required): Full threat assessment data
- `onClose` (function, required): Callback when modal is closed

**Features:**
- Overall risk overview
- Risk factors list
- User information
- Network information (IP reputation, ISP, proxy/VPN/Tor detection)
- Geolocation details
- Device fingerprint
- Behavioral analysis flags
- Timeline of events
- Additional metadata

**Usage:**
```jsx
import { ThreatDetailsModal } from './components/Threat';

<ThreatDetailsModal
  assessment={fullAssessment}
  onClose={() => setShowModal(false)}
/>
```

## API Integration

All components use the threat intelligence API endpoints at `/api/threat/*`. Ensure your API service includes:

```javascript
// In src/services/api.js
const api = {
  // ... other methods

  // Threat Intelligence
  getThreatAssessments: async (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.limit) queryParams.append('limit', params.limit);
    if (params.highRiskOnly) queryParams.append('highRiskOnly', params.highRiskOnly);
    if (params.hours) queryParams.append('hours', params.hours);
    return await apiClient.get(`/api/threat/assessments/recent?${queryParams.toString()}`);
  },

  getThreatStatistics: async () => {
    return await apiClient.get('/api/threat/statistics');
  },

  lockUserAccount: async (userId, reason) => {
    return await apiClient.post(`/api/threat/account/${userId}/lock`, { reason });
  },

  unlockUserAccount: async (userId) => {
    return await apiClient.post(`/api/threat/account/${userId}/unlock`);
  },
};
```

## Styling

All components use the centralized `Threat.css` stylesheet located at `src/styles/Threat.css`. The stylesheet includes:

- Risk badge colors and sizes
- Modal overlays and animations
- Card layouts and hover effects
- Table styling
- Map container styling
- Responsive breakpoints

## Dependencies

Required packages (already installed):
- `react` (^18.2.0)
- `react-dom` (^18.2.0)
- `leaflet` (^1.9.4)
- `react-leaflet` (^4.2.1)
- `react-leaflet-cluster` (^4.0.0)
- `recharts` (^2.10.0)

## Integration Examples

### 1. Admin Dashboard Integration

```jsx
import React from 'react';
import { ThreatIntelligencePanel, GeographicHeatmap } from './components/Threat';

const AdminDashboard = () => {
  return (
    <div className="admin-dashboard">
      <h1>Security Operations Center</h1>

      {/* Threat Intelligence Panel */}
      <ThreatIntelligencePanel />

      {/* Geographic Map */}
      <GeographicHeatmap />
    </div>
  );
};
```

### 2. User Login Flow Integration

```jsx
import React, { useState } from 'react';
import { ThreatAlertModal } from './components/Threat';
import api from './services/api';

const LoginForm = () => {
  const [showThreatAlert, setShowThreatAlert] = useState(false);
  const [threatAssessment, setThreatAssessment] = useState(null);

  const handleLogin = async (credentials) => {
    try {
      const response = await api.login(credentials);

      // Check if account is locked due to threat
      if (response.data.accountLocked && response.data.threatAssessment) {
        setThreatAssessment(response.data.threatAssessment);
        setShowThreatAlert(true);
      } else {
        // Proceed with normal login
        handleSuccessfulLogin(response.data);
      }
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  return (
    <div>
      {/* Login form */}

      {/* Threat Alert Modal */}
      {showThreatAlert && (
        <ThreatAlertModal
          threatAssessment={threatAssessment}
          onClose={() => setShowThreatAlert(false)}
          onContactSupport={() => window.location.href = '/support'}
        />
      )}
    </div>
  );
};
```

### 3. User Profile - Active Sessions

```jsx
import React, { useState, useEffect } from 'react';
import { SessionDetailsCard } from './components/Threat';
import api from './services/api';

const UserSessions = () => {
  const [sessions, setSessions] = useState([]);

  useEffect(() => {
    loadSessions();
  }, []);

  const loadSessions = async () => {
    const response = await api.getUserSessions();
    setSessions(response.data);
  };

  return (
    <div className="sessions-grid">
      {sessions.map(session => (
        <SessionDetailsCard
          key={session.id}
          session={session}
          onViewDetails={(s) => console.log('View details:', s)}
        />
      ))}
    </div>
  );
};
```

## Backend Requirements

The backend must provide the following endpoints:

### GET /api/threat/assessments/recent
Returns recent threat assessments with optional filters.

**Query Parameters:**
- `limit` (number): Max results to return
- `highRiskOnly` (boolean): Filter high-risk only
- `hours` (number): Time range in hours

**Response:**
```json
[
  {
    "id": 1,
    "userId": 123,
    "username": "john.doe",
    "ipAddress": "192.168.1.1",
    "riskScore": 75,
    "riskFactors": ["VPN detected", "Unusual login time"],
    "allowed": false,
    "accountLocked": true,
    "lockExpiresAt": "2026-01-29T12:00:00Z",
    "timestamp": "2026-01-29T11:30:00Z",
    "geolocation": {
      "country": "United States",
      "city": "New York",
      "latitude": 40.7128,
      "longitude": -74.0060
    }
  }
]
```

### GET /api/threat/statistics
Returns threat statistics.

**Response:**
```json
{
  "totalAssessments24h": 1250,
  "highRiskCount24h": 45,
  "blockedLogins24h": 23,
  "averageRiskScore": 32.5
}
```

### POST /api/threat/account/{userId}/lock
Locks a user account.

**Request Body:**
```json
{
  "reason": "Multiple failed login attempts"
}
```

### POST /api/threat/account/{userId}/unlock
Unlocks a user account.

## Performance Considerations

1. **Auto-refresh**: ThreatIntelligencePanel refreshes every 30 seconds, GeographicHeatmap every 60 seconds
2. **Data limits**: Default limits are set to prevent excessive data loading
3. **Clustering**: Map uses clustering to handle many markers efficiently
4. **Memoization**: Consider using React.memo for frequently re-rendered components

## Security Notes

1. All API calls require authentication (Bearer token)
2. Admin-only components should be protected with route guards
3. Sensitive data (IP addresses, locations) should only be shown to authorized users
4. Account lock/unlock actions are admin-only operations

## Troubleshooting

### Map not displaying
- Ensure Leaflet CSS is imported: `import 'leaflet/dist/leaflet.css';`
- Check that geolocation data has valid latitude/longitude values
- Verify map container has a defined height in CSS

### API errors
- Check that backend endpoints are implemented
- Verify authentication token is valid
- Check CORS configuration if running on different domains

### Styling issues
- Ensure `Threat.css` is imported in components
- Check for CSS conflicts with existing styles
- Verify responsive breakpoints match your design system

## Future Enhancements

- Real-time WebSocket updates for threat events
- Advanced filtering and search capabilities
- Export threat reports (PDF/CSV)
- Threat trend analysis charts
- Integration with external threat intelligence feeds
- Automated response workflows
- Machine learning risk prediction

## License

Part of the CRUD Test Application security suite.
