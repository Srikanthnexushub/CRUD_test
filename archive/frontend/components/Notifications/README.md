# Email Notification Components

This directory contains all frontend components for the Email Notification system of the CRUD Test application.

## Table of Contents

- [Overview](#overview)
- [Components](#components)
- [Context](#context)
- [Installation](#installation)
- [Usage](#usage)
- [API Integration](#api-integration)
- [Styling](#styling)

## Overview

The Email Notification system provides a comprehensive solution for managing email notifications, including:

- User notification preferences
- Email dashboard with statistics
- Email history and logs
- Email template editor
- SMTP configuration
- Real-time notification updates

## Components

### 1. NotificationHub

**Location:** `NotificationHub.jsx`

Main container component that provides a tabbed interface for all notification features.

**Features:**
- Tab navigation between different notification views
- Role-based access control (admin-only tabs)
- SMTP configuration modal trigger
- Responsive layout

**Usage:**
```jsx
import { NotificationHub } from './components/Notifications';

function App() {
  return <NotificationHub />;
}
```

### 2. NotificationPreferences

**Location:** `NotificationPreferences.jsx`

User preferences management for email notifications.

**Features:**
- Toggle switches for each notification type
- Daily digest configuration
- Time picker for digest delivery
- Test email functionality
- Auto-save with feedback

**Usage:**
```jsx
import { NotificationPreferences } from './components/Notifications';
import { NotificationProvider } from './contexts/NotificationContext';

function PreferencesPage() {
  return (
    <NotificationProvider>
      <NotificationPreferences />
    </NotificationProvider>
  );
}
```

### 3. EmailDashboard

**Location:** `EmailDashboard.jsx`

Admin dashboard for email system monitoring.

**Features:**
- Real-time statistics (total, pending, sent, failed)
- Success rate calculation
- Recent emails table
- Retry all failed emails
- Auto-refresh (30 seconds)
- Queue size monitoring

**Usage:**
```jsx
import { EmailDashboard } from './components/Notifications';

function AdminDashboard() {
  return <EmailDashboard />;
}
```

### 4. EmailLogTable

**Location:** `EmailLogTable.jsx`

Comprehensive email history and log viewer.

**Features:**
- Filterable by status (pending, sent, failed)
- Full-text search
- Sortable columns
- Pagination
- View email template/content
- Retry individual failed emails
- Error message tooltips

**Usage:**
```jsx
import { EmailLogTable } from './components/Notifications';

function EmailLogsPage() {
  return <EmailLogTable />;
}
```

### 5. EmailTemplateEditor

**Location:** `EmailTemplateEditor.jsx`

HTML email template editor with live preview.

**Features:**
- Template selector (USER_CREATED, USER_UPDATED, etc.)
- HTML code editor with syntax highlighting
- Live preview iframe
- Variable insertion sidebar
- Available variables by template type
- Reset to saved version
- Tips and best practices

**Usage:**
```jsx
import { EmailTemplateEditor } from './components/Notifications';

function TemplateEditor() {
  return <EmailTemplateEditor />;
}
```

**Available Variables by Template Type:**

- **USER_CREATED**: `${username}`, `${email}`, `${role}`, `${createdAt}`
- **USER_UPDATED**: `${username}`, `${email}`, `${role}`, `${updatedBy}`, `${changes}`
- **USER_DELETED**: `${username}`, `${email}`, `${deletedBy}`, `${deletedAt}`
- **SECURITY_ALERT**: `${eventType}`, `${username}`, `${ipAddress}`, `${timestamp}`, `${details}`
- **DAILY_DIGEST**: `${date}`, `${totalUsers}`, `${newUsers}`, `${updatedUsers}`, `${deletedUsers}`, `${securityEvents}`, `${activities}`

### 6. SmtpConfigModal

**Location:** `SmtpConfigModal.jsx`

Modal dialog for SMTP server configuration.

**Features:**
- SMTP host and port configuration
- TLS/SSL toggle
- Authentication settings
- Password visibility toggle
- Test connection functionality
- Connection status feedback
- From email and name settings
- Field validation

**Usage:**
```jsx
import { SmtpConfigModal } from './components/Notifications';

function AdminPanel() {
  const [showModal, setShowModal] = useState(false);

  return (
    <>
      <button onClick={() => setShowModal(true)}>
        Configure SMTP
      </button>
      <SmtpConfigModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        onSave={(config) => console.log('Saved:', config)}
      />
    </>
  );
}
```

## Context

### NotificationContext

**Location:** `src/contexts/NotificationContext.jsx`

Provides notification state management across the application.

**State:**
- `preferences`: User notification preferences
- `unreadCount`: Count of unread notifications
- `loading`: Loading state
- `error`: Error messages

**Methods:**
- `updatePreferences(preferences)`: Update user preferences
- `sendTestEmail()`: Send test email to current user
- `loadPreferences()`: Reload preferences from server
- `loadUnreadCount()`: Get unread notification count
- `markAsRead(notificationId)`: Mark notification as read

**Usage:**
```jsx
import { NotificationProvider, useNotifications } from './contexts/NotificationContext';

// Wrap your app with the provider
function App() {
  return (
    <NotificationProvider>
      <YourComponents />
    </NotificationProvider>
  );
}

// Use in components
function MyComponent() {
  const { preferences, updatePreferences, sendTestEmail } = useNotifications();

  const handleSave = async () => {
    const result = await updatePreferences(newPreferences);
    if (result.success) {
      alert('Saved!');
    }
  };

  return <button onClick={handleSave}>Save</button>;
}
```

## Installation

1. Ensure all components are in the correct directory:
   ```
   frontend/src/components/Notifications/
   ├── NotificationHub.jsx
   ├── NotificationPreferences.jsx
   ├── EmailDashboard.jsx
   ├── EmailLogTable.jsx
   ├── EmailTemplateEditor.jsx
   ├── SmtpConfigModal.jsx
   ├── index.js
   └── README.md
   ```

2. Create the notification context:
   ```
   frontend/src/contexts/NotificationContext.jsx
   ```

3. Add notification API endpoints to your API service:
   ```
   frontend/src/services/api.js
   ```

4. Add the CSS styles:
   ```
   frontend/src/styles/Notifications.css
   ```

5. Install Font Awesome for icons (if not already installed):
   ```bash
   npm install --save @fortawesome/fontawesome-free
   ```

   Then import in your main CSS or index.js:
   ```js
   import '@fortawesome/fontawesome-free/css/all.min.css';
   ```

## Usage

### Basic Integration

1. **Add the NotificationProvider to your app:**

```jsx
// src/App.jsx
import { NotificationProvider } from './contexts/NotificationContext';
import { NotificationHub } from './components/Notifications';

function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <Router>
          <Routes>
            <Route path="/notifications" element={<NotificationHub />} />
            {/* Other routes */}
          </Routes>
        </Router>
      </NotificationProvider>
    </AuthProvider>
  );
}
```

2. **Add navigation link:**

```jsx
<nav>
  <Link to="/notifications">
    <i className="fas fa-bell"></i>
    Notifications
  </Link>
</nav>
```

3. **Display unread count (optional):**

```jsx
import { useNotifications } from './contexts/NotificationContext';

function NotificationBadge() {
  const { unreadCount, loadUnreadCount } = useNotifications();

  useEffect(() => {
    loadUnreadCount();
  }, []);

  return (
    <div className="notification-badge">
      <i className="fas fa-bell"></i>
      {unreadCount > 0 && <span className="badge">{unreadCount}</span>}
    </div>
  );
}
```

### Individual Component Usage

Each component can be used independently:

```jsx
// Only preferences
import { NotificationPreferences } from './components/Notifications';

// Only dashboard
import { EmailDashboard } from './components/Notifications';

// Custom layout
function CustomNotificationPage() {
  return (
    <div>
      <h1>My Notifications</h1>
      <NotificationPreferences />
      <hr />
      <EmailDashboard />
    </div>
  );
}
```

## API Integration

The components expect the following API endpoints to be implemented:

### Notification Preferences
- `GET /api/notifications/preferences` - Get user preferences
- `PUT /api/notifications/preferences` - Update preferences
- `POST /api/notifications/test-email` - Send test email
- `GET /api/notifications/unread-count` - Get unread count
- `PUT /api/notifications/{id}/read` - Mark as read

### Email Dashboard & Logs
- `GET /api/notifications/emails/stats` - Get email statistics
- `GET /api/notifications/emails/recent?limit=10` - Get recent emails
- `GET /api/notifications/emails?status=&search=&page=&size=` - Get email logs
- `POST /api/notifications/emails/{id}/retry` - Retry single email
- `POST /api/notifications/emails/retry-failed` - Retry all failed

### Email Templates
- `GET /api/notifications/templates` - Get all templates
- `GET /api/notifications/templates/{id}` - Get single template
- `PUT /api/notifications/templates/{id}` - Update template

### SMTP Configuration
- `GET /api/notifications/smtp/config` - Get SMTP config
- `PUT /api/notifications/smtp/config` - Update SMTP config
- `POST /api/notifications/smtp/test` - Test SMTP connection

## Styling

All styles are contained in `/src/styles/Notifications.css`.

### Customization

You can customize the appearance by overriding CSS variables or classes:

```css
/* Custom colors */
.notification-hub {
  --primary-color: #667eea;
  --success-color: #4CAF50;
  --error-color: #f44336;
  --warning-color: #FF9800;
}

/* Custom button styles */
.btn-save {
  background-color: your-color;
}

/* Custom card styles */
.stat-card {
  border-radius: your-radius;
}
```

### Responsive Design

The components are fully responsive and support:
- Desktop (1200px+)
- Tablet (768px - 1199px)
- Mobile (< 768px)

## Features Overview

### User Features
- Configure notification preferences
- Enable/disable specific notification types
- Set up daily digest with custom time
- Send test emails
- View personal notification history

### Admin Features
- Monitor email system health
- View email statistics and success rates
- Access complete email logs
- Retry failed emails
- Edit email templates
- Configure SMTP settings
- Test SMTP connections

## Best Practices

1. **Always wrap components with NotificationProvider**
2. **Use role-based access control for admin features**
3. **Implement proper error handling**
4. **Show loading states during API calls**
5. **Provide user feedback for all actions**
6. **Test email templates before deploying**
7. **Keep SMTP credentials secure**
8. **Monitor email queue regularly**
9. **Set up proper rate limiting**
10. **Log all email activities**

## Troubleshooting

### Common Issues

1. **Components not rendering:**
   - Check if NotificationProvider is wrapping the components
   - Verify API endpoints are accessible
   - Check browser console for errors

2. **API errors:**
   - Verify backend is running
   - Check authentication token
   - Confirm API endpoints match

3. **Styling issues:**
   - Ensure Notifications.css is imported
   - Check for CSS conflicts
   - Verify Font Awesome is loaded

4. **SMTP configuration:**
   - Test connection before saving
   - Use correct port (587 for TLS, 465 for SSL)
   - For Gmail, use App Passwords, not regular passwords

## Support

For issues or questions:
1. Check this README
2. Review component source code
3. Check browser console for errors
4. Verify API responses
5. Test with different user roles

## License

Part of the CRUD Test application.
