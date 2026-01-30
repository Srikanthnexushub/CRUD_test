# Threat Intelligence Components - Quick Start Guide

## Component Hierarchy

```
Threat Intelligence System
│
├── 📊 ThreatIntelligencePanel (Admin Dashboard)
│   ├── Uses: RiskScoreBadge
│   └── Opens: ThreatDetailsModal
│
├── 🗺️ GeographicHeatmap (Map Visualization)
│   ├── Uses: Leaflet, react-leaflet
│   └── Displays: Risk markers on map
│
├── ⚠️ ThreatAlertModal (Account Lock Alert)
│   └── Uses: RiskScoreBadge
│
├── 📱 SessionDetailsCard (Session Info)
│   └── Uses: RiskScoreBadge
│
├── 🔍 ThreatDetailsModal (Full Details)
│   └── Uses: RiskScoreBadge
│
└── 🎯 RiskScoreBadge (Core Component)
    └── Used by: All other components
```

## Quick Integration Guide

### 1. Import Components

```jsx
// Import single component
import { RiskScoreBadge } from './components/Threat';

// Import multiple components
import {
  RiskScoreBadge,
  ThreatAlertModal,
  SessionDetailsCard,
  ThreatIntelligencePanel,
  GeographicHeatmap,
  ThreatDetailsModal
} from './components/Threat';
```

### 2. Import Styles

```jsx
// In your main App.jsx or component
import './styles/Threat.css';
```

### 3. Basic Usage Examples

#### Show Risk Score Badge
```jsx
<RiskScoreBadge score={75} size="large" />
```

#### Admin Dashboard Panel
```jsx
<ThreatIntelligencePanel apiBaseUrl="/api/threat" />
```

#### Geographic Map
```jsx
<GeographicHeatmap apiBaseUrl="/api/threat" />
```

#### Account Lock Alert
```jsx
{accountLocked && (
  <ThreatAlertModal
    threatAssessment={threatData}
    onClose={() => setAccountLocked(false)}
    onContactSupport={() => navigate('/support')}
  />
)}
```

#### Session Card
```jsx
<SessionDetailsCard
  session={sessionData}
  onViewDetails={(session) => console.log(session)}
/>
```

#### Details Modal
```jsx
{showDetails && (
  <ThreatDetailsModal
    assessment={selectedAssessment}
    onClose={() => setShowDetails(false)}
  />
)}
```

## Component Props Reference

### RiskScoreBadge
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| score | number | required | Risk score 0-100 |
| size | string | 'medium' | 'small', 'medium', 'large' |
| showLabel | boolean | true | Show risk level text |

### ThreatAlertModal
| Prop | Type | Required | Description |
|------|------|----------|-------------|
| threatAssessment | object | ✓ | Threat data with riskScore, factors, etc. |
| onClose | function | ✓ | Callback when closed |
| onContactSupport | function | ✓ | Callback for support button |

### SessionDetailsCard
| Prop | Type | Required | Description |
|------|------|----------|-------------|
| session | object | ✓ | Session with device, IP, location, threatAssessment |
| onViewDetails | function | | Callback to view full details |

### ThreatIntelligencePanel
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| apiBaseUrl | string | '/api/threat' | Base URL for API calls |

### GeographicHeatmap
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| apiBaseUrl | string | '/api/threat' | Base URL for API calls |

### ThreatDetailsModal
| Prop | Type | Required | Description |
|------|------|----------|-------------|
| assessment | object | ✓ | Full threat assessment data |
| onClose | function | ✓ | Callback when closed |

## Data Structure Examples

### Threat Assessment Object
```javascript
{
  id: 1,
  userId: 123,
  username: "john.doe",
  ipAddress: "192.168.1.1",
  riskScore: 75,
  riskFactors: [
    "VPN detected",
    "Unusual login time",
    "New device"
  ],
  allowed: false,
  accountLocked: true,
  lockExpiresAt: "2026-01-29T12:00:00Z",
  timestamp: "2026-01-29T11:30:00Z",
  geolocation: {
    country: "United States",
    city: "New York",
    latitude: 40.7128,
    longitude: -74.0060
  },
  ipReputation: {
    reputation: "SUSPICIOUS",
    isp: "Example ISP",
    isVpn: true
  }
}
```

### Session Object
```javascript
{
  id: 1,
  userId: 123,
  ipAddress: "192.168.1.1",
  device: "Chrome 96 on Windows",
  loginTime: "2026-01-29T11:30:00Z",
  location: {
    city: "New York",
    country: "United States"
  },
  threatAssessment: {
    riskScore: 35,
    riskFactors: []
  }
}
```

## Common Use Cases

### 1. Admin Security Dashboard
```jsx
function SecurityDashboard() {
  return (
    <div className="security-dashboard">
      <h1>Threat Intelligence</h1>

      {/* Main Panel */}
      <ThreatIntelligencePanel />

      {/* Geographic View */}
      <GeographicHeatmap />
    </div>
  );
}
```

### 2. User Login with Threat Detection
```jsx
function LoginPage() {
  const [showAlert, setShowAlert] = useState(false);
  const [threat, setThreat] = useState(null);

  const handleLogin = async (credentials) => {
    const response = await api.login(credentials);

    if (response.data.accountLocked) {
      setThreat(response.data.threatAssessment);
      setShowAlert(true);
    } else {
      // Success
      navigate('/dashboard');
    }
  };

  return (
    <div>
      <LoginForm onSubmit={handleLogin} />

      {showAlert && (
        <ThreatAlertModal
          threatAssessment={threat}
          onClose={() => setShowAlert(false)}
          onContactSupport={() => navigate('/support')}
        />
      )}
    </div>
  );
}
```

### 3. User Profile with Sessions
```jsx
function UserProfile() {
  const [sessions, setSessions] = useState([]);
  const [selectedSession, setSelectedSession] = useState(null);

  return (
    <div>
      <h2>Active Sessions</h2>

      <div className="sessions-grid">
        {sessions.map(session => (
          <SessionDetailsCard
            key={session.id}
            session={session}
            onViewDetails={setSelectedSession}
          />
        ))}
      </div>

      {selectedSession && (
        <ThreatDetailsModal
          assessment={selectedSession.threatAssessment}
          onClose={() => setSelectedSession(null)}
        />
      )}
    </div>
  );
}
```

### 4. Dashboard Widget
```jsx
function ThreatWidget() {
  const [currentRisk, setCurrentRisk] = useState(0);

  useEffect(() => {
    const loadRisk = async () => {
      const res = await api.getThreatAssessmentForCurrentSession();
      setCurrentRisk(res.data.riskScore);
    };
    loadRisk();
  }, []);

  return (
    <div className="widget">
      <h3>Your Security Status</h3>
      <RiskScoreBadge score={currentRisk} size="large" />
    </div>
  );
}
```

## Styling Customization

### Override Risk Colors
```css
/* In your custom CSS file */
.risk-score-badge.risk-green {
  background: linear-gradient(135deg, #your-green-1, #your-green-2);
}

.risk-score-badge.risk-red {
  background: linear-gradient(135deg, #your-red-1, #your-red-2);
}
```

### Customize Modal
```css
.threat-alert-modal {
  max-width: 700px; /* Make wider */
  border-radius: 16px; /* More rounded */
}

.threat-alert-modal .modal-header {
  background: linear-gradient(135deg, #your-color-1, #your-color-2);
}
```

### Customize Card
```css
.session-details-card {
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.session-details-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
}
```

## API Integration Checklist

- [ ] Backend endpoints implemented
- [ ] JWT authentication configured
- [ ] CORS settings correct
- [ ] Response format matches expected structure
- [ ] Rate limiting configured
- [ ] Error handling implemented
- [ ] Database schema in place

## Testing Checklist

- [ ] All components render without errors
- [ ] Risk score colors display correctly (4 levels)
- [ ] Modals open and close properly
- [ ] API calls return data
- [ ] Map displays markers
- [ ] Filters work correctly
- [ ] Auto-refresh functions
- [ ] Responsive on mobile
- [ ] Account lock/unlock works
- [ ] Countdown timer accurate

## Performance Tips

1. **Use React.memo** for components that don't change often
```jsx
import React, { memo } from 'react';
const RiskScoreBadge = memo(({ score, size, showLabel }) => {
  // component code
});
```

2. **Debounce API calls** when filtering
```jsx
import { debounce } from 'lodash';
const debouncedFilter = debounce(loadData, 500);
```

3. **Limit data** to prevent large payloads
```jsx
api.getThreatAssessments({ limit: 50, highRiskOnly: true });
```

4. **Use pagination** for large datasets
```jsx
const [page, setPage] = useState(0);
api.getThreatAssessments({ page, size: 20 });
```

## Common Issues & Solutions

### Issue: Map not displaying
**Solution**: Ensure Leaflet CSS is imported and container has height
```jsx
import 'leaflet/dist/leaflet.css';
```
```css
.map-container { height: 600px; }
```

### Issue: API returns 401
**Solution**: Check token is valid
```javascript
const token = localStorage.getItem('token');
if (!token) navigate('/login');
```

### Issue: Risk colors not showing
**Solution**: Import Threat.css
```jsx
import '../../styles/Threat.css';
```

### Issue: Modal not closing
**Solution**: Ensure onClose is called
```jsx
<ThreatAlertModal
  onClose={() => {
    setShowModal(false);
    setThreatData(null);
  }}
/>
```

## Best Practices

1. **Always handle loading states**
```jsx
{loading ? <Spinner /> : <ThreatIntelligencePanel />}
```

2. **Handle errors gracefully**
```jsx
try {
  const data = await api.getThreatAssessments();
} catch (error) {
  console.error('Failed to load:', error);
  setError(error.message);
}
```

3. **Use environment variables for API URLs**
```javascript
const API_URL = import.meta.env.VITE_API_URL;
```

4. **Implement proper authentication checks**
```jsx
if (user.role !== 'ADMIN') {
  return <Navigate to="/dashboard" />;
}
```

5. **Add loading indicators**
```jsx
{loading && <div className="loading">Loading threats...</div>}
```

## Resources

- **Component Documentation**: `/components/Threat/README.md`
- **Usage Examples**: `/components/Threat/ExampleUsage.jsx`
- **Setup Guide**: `/THREAT_INTELLIGENCE_SETUP.md`
- **API Service**: `/services/api.js`
- **Styles**: `/styles/Threat.css`

## Support

For questions or issues:
1. Check this guide and README.md
2. Review ExampleUsage.jsx for implementation examples
3. Check browser console for errors
4. Verify API endpoints are working
5. Review backend logs

## Version Info

- **Version**: 1.0.0
- **Created**: 2026-01-29
- **Components**: 6 core + 1 utility
- **Total Lines**: ~2,800
- **Dependencies**: leaflet, react-leaflet, react-leaflet-cluster, recharts

---

**Happy Coding!** 🚀
