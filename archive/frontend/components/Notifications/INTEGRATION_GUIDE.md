# Email Notification Components - Integration Guide

## Quick Start

### Step 1: Wrap Your App with NotificationProvider

Update your main `App.jsx`:

```jsx
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { NotificationProvider } from './contexts/NotificationContext';
import { NotificationHub } from './components/Notifications';

function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <Router>
          <Routes>
            {/* Add notification route */}
            <Route path="/notifications" element={<NotificationHub />} />

            {/* Your other routes */}
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/dashboard" element={<Dashboard />} />
          </Routes>
        </Router>
      </NotificationProvider>
    </AuthProvider>
  );
}

export default App;
```

### Step 2: Add Navigation Link

Add to your navigation menu or sidebar:

```jsx
import { Link } from 'react-router-dom';
import { useNotifications } from './contexts/NotificationContext';

function Navigation() {
  const { unreadCount } = useNotifications();

  return (
    <nav>
      <Link to="/notifications">
        <i className="fas fa-bell"></i>
        Notifications
        {unreadCount > 0 && (
          <span className="badge">{unreadCount}</span>
        )}
      </Link>
    </nav>
  );
}
```

### Step 3: Import Styles

In your main `index.css` or `App.css`:

```css
@import './styles/Notifications.css';
```

Or in your `main.jsx` or `index.jsx`:

```jsx
import './styles/Notifications.css';
```

### Step 4: Ensure Font Awesome is Available

If not already installed:

```bash
npm install @fortawesome/fontawesome-free
```

Then import in your main entry point:

```jsx
// main.jsx or index.jsx
import '@fortawesome/fontawesome-free/css/all.min.css';
```

## Integration Examples

### Example 1: Basic Integration

```jsx
// src/pages/NotificationsPage.jsx
import React from 'react';
import { NotificationHub } from '../components/Notifications';

function NotificationsPage() {
  return (
    <div className="page-container">
      <NotificationHub />
    </div>
  );
}

export default NotificationsPage;
```

### Example 2: Dashboard Widget

```jsx
// src/components/Dashboard.jsx
import React from 'react';
import { EmailDashboard } from './Notifications';
import { useAuth } from '../contexts/AuthContext';

function Dashboard() {
  const { isAdmin } = useAuth();

  return (
    <div className="dashboard">
      <h1>Dashboard</h1>

      {/* Only show email dashboard to admins */}
      {isAdmin() && (
        <section className="email-section">
          <EmailDashboard />
        </section>
      )}

      {/* Other dashboard widgets */}
    </div>
  );
}

export default Dashboard;
```

### Example 3: User Settings Page

```jsx
// src/pages/UserSettings.jsx
import React from 'react';
import { NotificationPreferences } from '../components/Notifications';

function UserSettings() {
  return (
    <div className="settings-page">
      <h1>User Settings</h1>

      <section>
        <h2>Profile Settings</h2>
        {/* Profile form */}
      </section>

      <section>
        <h2>Notification Preferences</h2>
        <NotificationPreferences />
      </section>

      <section>
        <h2>Security Settings</h2>
        {/* Security settings */}
      </section>
    </div>
  );
}

export default UserSettings;
```

### Example 4: Admin Panel

```jsx
// src/pages/AdminPanel.jsx
import React, { useState } from 'react';
import {
  EmailDashboard,
  EmailLogTable,
  EmailTemplateEditor,
  SmtpConfigModal
} from '../components/Notifications';
import { useAuth } from '../contexts/AuthContext';
import { Navigate } from 'react-router-dom';

function AdminPanel() {
  const { isAdmin } = useAuth();
  const [showSmtpModal, setShowSmtpModal] = useState(false);

  if (!isAdmin()) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="admin-panel">
      <header>
        <h1>Admin Panel</h1>
        <button onClick={() => setShowSmtpModal(true)}>
          SMTP Settings
        </button>
      </header>

      <section>
        <h2>Email System Overview</h2>
        <EmailDashboard />
      </section>

      <section>
        <h2>Email Logs</h2>
        <EmailLogTable />
      </section>

      <section>
        <h2>Email Templates</h2>
        <EmailTemplateEditor />
      </section>

      <SmtpConfigModal
        isOpen={showSmtpModal}
        onClose={() => setShowSmtpModal(false)}
        onSave={(config) => {
          console.log('SMTP configured:', config);
          // Show success message
        }}
      />
    </div>
  );
}

export default AdminPanel;
```

### Example 5: Notification Badge Component

```jsx
// src/components/NotificationBadge.jsx
import React, { useEffect } from 'react';
import { useNotifications } from '../contexts/NotificationContext';
import { Link } from 'react-router-dom';

function NotificationBadge() {
  const { unreadCount, loadUnreadCount } = useNotifications();

  useEffect(() => {
    // Load initial count
    loadUnreadCount();

    // Refresh every minute
    const interval = setInterval(loadUnreadCount, 60000);
    return () => clearInterval(interval);
  }, []);

  return (
    <Link to="/notifications" className="notification-badge-link">
      <div className="notification-icon">
        <i className="fas fa-bell"></i>
        {unreadCount > 0 && (
          <span className="notification-count">{unreadCount}</span>
        )}
      </div>
    </Link>
  );
}

export default NotificationBadge;
```

CSS for the badge:

```css
.notification-badge-link {
  position: relative;
  text-decoration: none;
  color: inherit;
}

.notification-icon {
  position: relative;
  display: inline-block;
  font-size: 1.5rem;
}

.notification-count {
  position: absolute;
  top: -8px;
  right: -8px;
  background-color: #f44336;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 0.7rem;
  font-weight: bold;
  min-width: 18px;
  text-align: center;
}
```

### Example 6: Protected Route for Admin Features

```jsx
// src/components/AdminRoute.jsx
import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

function AdminRoute({ children }) {
  const { isAdmin, loading } = useAuth();

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!isAdmin()) {
    return <Navigate to="/" replace />;
  }

  return children;
}

export default AdminRoute;

// Usage in routes:
<Route
  path="/admin/notifications"
  element={
    <AdminRoute>
      <NotificationHub />
    </AdminRoute>
  }
/>
```

## Testing the Integration

### 1. Test User Flow

```javascript
// User opens notification preferences
1. Navigate to /notifications
2. Click on "Preferences" tab
3. Toggle notification settings
4. Click "Send Test Email"
5. Click "Save Preferences"
6. Verify success message appears
```

### 2. Test Admin Flow

```javascript
// Admin monitors email system
1. Navigate to /notifications (as admin)
2. View Dashboard tab - see statistics
3. Click "Logs" tab - see email history
4. Filter by status (Failed)
5. Click retry on a failed email
6. Click "Templates" tab
7. Select a template
8. Edit HTML content
9. Preview changes
10. Save template
11. Click "SMTP Settings"
12. Configure SMTP
13. Click "Test Connection"
14. Save configuration
```

### 3. Test API Integration

```javascript
// Verify API calls work
import api from './services/api';

// Test notification preferences
const prefs = await api.getNotificationPreferences();
console.log(prefs);

// Test email stats
const stats = await api.getEmailStats();
console.log(stats);

// Test template retrieval
const templates = await api.getEmailTemplates();
console.log(templates);
```

## Common Issues and Solutions

### Issue 1: Components not showing

**Solution:** Ensure NotificationProvider wraps your components:

```jsx
// Wrong
<Router>
  <NotificationHub />
</Router>

// Correct
<NotificationProvider>
  <Router>
    <NotificationHub />
  </Router>
</NotificationProvider>
```

### Issue 2: API endpoints returning 404

**Solution:** Verify backend routes match frontend API calls:

```javascript
// Frontend expects: /api/notifications/preferences
// Backend should have: @GetMapping("/api/notifications/preferences")
```

### Issue 3: Styling not applied

**Solution:** Import CSS in the correct order:

```javascript
// main.jsx or App.jsx
import './index.css';
import './styles/Notifications.css';
import '@fortawesome/fontawesome-free/css/all.min.css';
```

### Issue 4: Font Awesome icons not showing

**Solution:** Install and import Font Awesome:

```bash
npm install @fortawesome/fontawesome-free
```

```javascript
import '@fortawesome/fontawesome-free/css/all.min.css';
```

### Issue 5: SMTP test failing

**Solution:** For Gmail, use App Passwords:

1. Enable 2FA on Gmail
2. Generate App Password: https://myaccount.google.com/apppasswords
3. Use App Password in SMTP settings (not regular password)
4. Use port 587 with TLS enabled

## Environment Configuration

Create `.env` file if needed:

```bash
# .env
VITE_API_URL=http://localhost:8080
VITE_ENABLE_NOTIFICATIONS=true
```

Access in code:

```javascript
const apiUrl = import.meta.env.VITE_API_URL;
const notificationsEnabled = import.meta.env.VITE_ENABLE_NOTIFICATIONS === 'true';
```

## Performance Optimization

### 1. Lazy Load Components

```jsx
import { lazy, Suspense } from 'react';

const NotificationHub = lazy(() => import('./components/Notifications/NotificationHub'));

function App() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <NotificationHub />
    </Suspense>
  );
}
```

### 2. Memoize Context Values

Already implemented in NotificationContext using proper dependency arrays.

### 3. Debounce Search Input

```jsx
import { useState, useEffect } from 'react';

function useDebounce(value, delay) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => clearTimeout(handler);
  }, [value, delay]);

  return debouncedValue;
}

// Usage in EmailLogTable
const [searchTerm, setSearchTerm] = useState('');
const debouncedSearch = useDebounce(searchTerm, 500);

useEffect(() => {
  loadEmails();
}, [debouncedSearch]);
```

## Security Considerations

1. **Always validate admin role** before showing admin components
2. **Never expose SMTP passwords** in API responses (mask them)
3. **Sanitize HTML** in email templates to prevent XSS
4. **Rate limit** API calls to prevent abuse
5. **Use HTTPS** in production for SMTP credentials
6. **Implement CSRF protection** for state-changing operations

## Next Steps

1. Update your backend to implement the required API endpoints
2. Test each component individually
3. Test the complete flow
4. Configure SMTP settings
5. Customize email templates
6. Set up notification preferences
7. Monitor email delivery
8. Review logs regularly

## Support Resources

- Component README: `./README.md`
- API Documentation: Check your backend API docs
- React Router: https://reactrouter.com/
- Font Awesome: https://fontawesome.com/icons
- Email Best Practices: https://www.emailonacid.com/blog/

## Checklist

- [ ] NotificationProvider added to App.jsx
- [ ] Routes configured for notification pages
- [ ] Navigation links added
- [ ] CSS imported
- [ ] Font Awesome installed and imported
- [ ] Backend API endpoints implemented
- [ ] SMTP server configured
- [ ] Email templates customized
- [ ] User preferences tested
- [ ] Admin features tested
- [ ] Email sending tested
- [ ] Logs and monitoring verified
- [ ] Production deployment configured
