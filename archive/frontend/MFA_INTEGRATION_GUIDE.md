# MFA Integration Guide

This guide explains how to integrate the Multi-Factor Authentication (MFA) components into your React application.

## Files Created

### Context
- **`/src/contexts/MFAContext.jsx`** - MFA state management and API interaction

### Components
- **`/src/components/MFA/MFASetupModal.jsx`** - Setup wizard with QR code and verification
- **`/src/components/MFA/MFAVerificationModal.jsx`** - Login verification modal
- **`/src/components/MFA/BackupCodesDisplay.jsx`** - Display and manage backup codes
- **`/src/components/MFA/MFASettings.jsx`** - User settings panel
- **`/src/components/MFA/TrustedDevicesList.jsx`** - Manage trusted devices
- **`/src/components/MFA/index.js`** - Barrel export for easy imports

### Styles
- **`/src/styles/MFA.css`** - Comprehensive styling for all MFA components

### API
- **`/src/services/api.js`** - Updated with MFA endpoints

## Integration Steps

### 1. Install QR Code Library

```bash
npm install qrcode.react
```

### 2. Wrap Your App with MFAProvider

Update your main `App.jsx` or `index.jsx`:

```jsx
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { MFAProvider } from './contexts/MFAContext';
import App from './App';

function Root() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <MFAProvider>
                    <App />
                </MFAProvider>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default Root;
```

### 3. Update Login Flow

Modify your `LoginForm.jsx` to handle MFA requirement:

```jsx
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useMFA } from '../contexts/MFAContext';
import { MFAVerificationModal } from '../components/MFA';
import { generateDeviceFingerprint } from '../utils/deviceFingerprint';
import '../styles/LoginForm.css';

function LoginForm() {
    const [formData, setFormData] = useState({
        username: '',
        password: ''
    });
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [showMFAModal, setShowMFAModal] = useState(false);
    const [tempToken, setTempToken] = useState(null);

    const navigate = useNavigate();
    const { login } = useAuth();
    const { requireMFA } = useMFA();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        const deviceFingerprint = generateDeviceFingerprint();
        const result = await login(formData.username, formData.password, deviceFingerprint);

        if (result.success) {
            // Check if MFA is required
            if (result.mfaRequired) {
                setTempToken(result.tempToken);
                setShowMFAModal(true);
            } else {
                navigate('/dashboard');
            }
        } else {
            setError(result.error);
        }

        setIsLoading(false);
    };

    const handleMFASuccess = (token, user) => {
        // Update auth context with the final token
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(user));
        setShowMFAModal(false);
        navigate('/dashboard');
    };

    const handleMFACancel = () => {
        setShowMFAModal(false);
        setTempToken(null);
    };

    return (
        <>
            <div className="login-container">
                {/* ... existing login form ... */}
            </div>

            <MFAVerificationModal
                isOpen={showMFAModal}
                onSuccess={handleMFASuccess}
                onCancel={handleMFACancel}
            />
        </>
    );
}

export default LoginForm;
```

### 4. Add MFA Settings to User Dashboard

Add a link to MFA settings in your user dashboard:

```jsx
import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

function UserDashboard() {
    const { user } = useAuth();

    return (
        <div className="dashboard-container">
            <nav className="dashboard-nav">
                <Link to="/dashboard/settings/mfa">
                    Security & MFA Settings
                </Link>
            </nav>
            {/* ... rest of dashboard ... */}
        </div>
    );
}
```

### 5. Create MFA Settings Route

Create a new route component:

```jsx
// src/components/MFASettingsPage.jsx
import React from 'react';
import { useNavigate } from 'react-router-dom';
import MFASettings from './MFA/MFASettings';
import '../styles/UserDashboard.css';

function MFASettingsPage() {
    const navigate = useNavigate();

    return (
        <div className="dashboard-container">
            <header className="dashboard-header">
                <div className="header-content">
                    <h1>Security Settings</h1>
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="btn-back"
                    >
                        Back to Dashboard
                    </button>
                </div>
            </header>

            <main className="dashboard-main">
                <MFASettings />
            </main>
        </div>
    );
}

export default MFASettingsPage;
```

### 6. Update App Routes

Add the MFA settings route to your router:

```jsx
import { Routes, Route } from 'react-router-dom';
import LoginForm from './components/LoginForm';
import UserDashboard from './components/UserDashboard';
import MFASettingsPage from './components/MFASettingsPage';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
    return (
        <Routes>
            <Route path="/login" element={<LoginForm />} />
            <Route
                path="/dashboard"
                element={
                    <ProtectedRoute>
                        <UserDashboard />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/dashboard/settings/mfa"
                element={
                    <ProtectedRoute>
                        <MFASettingsPage />
                    </ProtectedRoute>
                }
            />
        </Routes>
    );
}

export default App;
```

## API Endpoints Used

The components expect the following backend endpoints:

- `POST /api/mfa/setup` - Initialize MFA setup
- `POST /api/mfa/verify-setup` - Verify initial MFA code
- `POST /api/mfa/verify-login` - Verify MFA code during login
- `POST /api/mfa/verify-backup` - Verify backup code
- `POST /api/mfa/disable` - Disable MFA
- `POST /api/mfa/regenerate-backup-codes` - Generate new backup codes
- `GET /api/mfa/status` - Get user's MFA status
- `GET /api/mfa/trusted-devices` - Get list of trusted devices
- `DELETE /api/mfa/trusted-devices/:id` - Remove a trusted device

## Component Props Reference

### MFASetupModal

```jsx
<MFASetupModal
    isOpen={boolean}          // Show/hide modal
    onClose={function}        // Called when modal is closed
    onSuccess={function}      // Called after successful setup
/>
```

### MFAVerificationModal

```jsx
<MFAVerificationModal
    isOpen={boolean}          // Show/hide modal
    onSuccess={function}      // Called with (token, user) on success
    onCancel={function}       // Called when user cancels
/>
```

### BackupCodesDisplay

```jsx
<BackupCodesDisplay
    codes={string[]}          // Array of backup codes
/>
```

### MFASettings

```jsx
<MFASettings />               // No props needed - standalone component
```

### TrustedDevicesList

```jsx
<TrustedDevicesList />        // No props needed - standalone component
```

## Features

### MFASetupModal
- Three-step setup wizard
- QR code generation and display
- Manual secret key entry with copy button
- 6-digit TOTP verification
- Backup codes display with download/print options

### MFAVerificationModal
- 6-digit TOTP input
- Auto-submit on 6 digits entered
- Switch between TOTP and backup code
- Trust device option (30 days)
- Shows remaining backup codes

### BackupCodesDisplay
- 2-column grid layout
- Copy all codes button
- Download as text file
- Print functionality
- Automatic date stamping

### MFASettings
- Enable/Disable MFA toggle
- MFA status indicator
- Backup codes management
- Regenerate backup codes
- Low backup codes warning
- Trusted devices list integration

### TrustedDevicesList
- Device cards with metadata
- Device type icons (mobile/desktop)
- IP address and location display
- Last used timestamp
- Expiration date tracking
- Expiring soon warnings
- Remove device functionality

## Styling Customization

The MFA components use the existing color scheme from your app. To customize:

1. **Primary Color**: Update the gradient in `.btn-primary` and `.step-number.active`
2. **Status Colors**: Modify `.status-enabled`, `.status-disabled`
3. **Modal Size**: Adjust `max-width` in `.modal-content`
4. **Spacing**: Update padding values in `.settings-section`

## Security Best Practices

1. **Backup Codes**: Always ensure users save backup codes during setup
2. **Trust Device**: Limit to 30 days and require re-verification
3. **Device Removal**: Allow users to remove trusted devices at any time
4. **Low Codes Warning**: Alert users when backup codes are running low
5. **QR Code**: Display only during setup, never store client-side

## Troubleshooting

### QR Code Not Displaying
- Verify backend returns `qrCodeUrl` in base64 format
- Check browser console for CORS issues
- Ensure `setupMFA()` API call is successful

### Auto-Submit Not Working
- Check that input value length is exactly 6 digits
- Verify `useEffect` dependency array includes `verificationCode`
- Ensure numeric keyboard is enabled on mobile

### Trusted Devices Not Loading
- Verify backend endpoint `/api/mfa/trusted-devices` is accessible
- Check authentication token in headers
- Verify user has MFA enabled

## Next Steps

1. Install required dependencies: `npm install qrcode.react`
2. Wrap app with MFAProvider
3. Update login flow to handle MFA
4. Add MFA settings route
5. Test the complete flow
6. Configure backend MFA endpoints

## Support

For backend integration, ensure your Spring Boot application has the corresponding MFA endpoints implemented and properly secured with JWT authentication.
