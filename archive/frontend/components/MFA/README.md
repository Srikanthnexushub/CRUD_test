# MFA Components

Multi-Factor Authentication (MFA) components for React application.

## Quick Start

### Import Components

```jsx
import { MFAProvider, useMFA } from '../../contexts/MFAContext';
import {
    MFASetupModal,
    MFAVerificationModal,
    BackupCodesDisplay,
    MFASettings,
    TrustedDevicesList
} from './MFA';
```

### Basic Usage

#### Setup MFA

```jsx
function MyComponent() {
    const [showSetup, setShowSetup] = useState(false);

    return (
        <>
            <button onClick={() => setShowSetup(true)}>
                Enable MFA
            </button>

            <MFASetupModal
                isOpen={showSetup}
                onClose={() => setShowSetup(false)}
                onSuccess={() => {
                    console.log('MFA enabled!');
                    setShowSetup(false);
                }}
            />
        </>
    );
}
```

#### Verify During Login

```jsx
function LoginPage() {
    const [showMFA, setShowMFA] = useState(false);

    const handleMFASuccess = (token, user) => {
        // Store token and redirect
        localStorage.setItem('token', token);
        navigate('/dashboard');
    };

    return (
        <MFAVerificationModal
            isOpen={showMFA}
            onSuccess={handleMFASuccess}
            onCancel={() => setShowMFA(false)}
        />
    );
}
```

#### Display Backup Codes

```jsx
function BackupCodesPage() {
    const codes = [
        '12345678',
        '87654321',
        // ... more codes
    ];

    return <BackupCodesDisplay codes={codes} />;
}
```

#### Full Settings Page

```jsx
function SecurityPage() {
    return (
        <div className="container">
            <h1>Security Settings</h1>
            <MFASettings />
        </div>
    );
}
```

## Component Details

### MFASetupModal

3-step wizard for MFA setup:
1. Display QR code
2. Verify TOTP code
3. Show backup codes

**Features:**
- QR code generation
- Manual secret key with copy button
- TOTP verification
- Backup codes display

### MFAVerificationModal

Login verification modal with:
- 6-digit TOTP input (auto-submit)
- Backup code alternative
- Trust device option
- Countdown timer support

### BackupCodesDisplay

Display and manage backup codes:
- Grid layout (2 columns)
- Copy all functionality
- Download as text file
- Print with formatting

### MFASettings

Complete MFA management:
- Enable/Disable toggle
- Status indicator
- Backup codes management
- Trusted devices list

### TrustedDevicesList

Manage trusted devices:
- Device information cards
- Remove device functionality
- Expiration tracking
- Activity timestamps

## API Integration

All components use the MFA context which connects to these endpoints:

- `POST /api/mfa/setup`
- `POST /api/mfa/verify-setup`
- `POST /api/mfa/verify-login`
- `POST /api/mfa/verify-backup`
- `POST /api/mfa/disable`
- `POST /api/mfa/regenerate-backup-codes`
- `GET /api/mfa/status`
- `GET /api/mfa/trusted-devices`
- `DELETE /api/mfa/trusted-devices/:id`

## Styling

All components use `/src/styles/MFA.css`. The styles are responsive and follow the existing app theme with gradient buttons and consistent spacing.

## Context Hook

Use the `useMFA` hook to access MFA functionality:

```jsx
const {
    mfaEnabled,
    mfaRequired,
    backupCodesRemaining,
    loading,
    setupMFA,
    verifyMFASetup,
    verifyMFACode,
    verifyBackupCode,
    disableMFA,
    regenerateBackupCodes,
    loadMFAStatus
} = useMFA();
```

## Examples

### Check MFA Status

```jsx
function ProfilePage() {
    const { mfaEnabled, backupCodesRemaining } = useMFA();

    return (
        <div>
            <p>MFA Status: {mfaEnabled ? 'Enabled' : 'Disabled'}</p>
            {mfaEnabled && (
                <p>Backup Codes: {backupCodesRemaining} remaining</p>
            )}
        </div>
    );
}
```

### Conditional MFA Prompt

```jsx
function Dashboard() {
    const { mfaEnabled } = useMFA();
    const [showSetup, setShowSetup] = useState(false);

    if (!mfaEnabled) {
        return (
            <div className="security-prompt">
                <h2>Secure your account</h2>
                <p>Enable MFA for additional security</p>
                <button onClick={() => setShowSetup(true)}>
                    Enable Now
                </button>
                <MFASetupModal
                    isOpen={showSetup}
                    onClose={() => setShowSetup(false)}
                    onSuccess={() => setShowSetup(false)}
                />
            </div>
        );
    }

    return <div>Dashboard content...</div>;
}
```

### Regenerate Backup Codes

```jsx
function SecuritySettings() {
    const { regenerateBackupCodes } = useMFA();
    const [codes, setCodes] = useState([]);
    const [showCodes, setShowCodes] = useState(false);

    const handleRegenerate = async () => {
        const result = await regenerateBackupCodes();
        if (result.success) {
            setCodes(result.backupCodes);
            setShowCodes(true);
        }
    };

    return (
        <>
            <button onClick={handleRegenerate}>
                Regenerate Backup Codes
            </button>

            {showCodes && (
                <div>
                    <BackupCodesDisplay codes={codes} />
                    <button onClick={() => setShowCodes(false)}>
                        Done
                    </button>
                </div>
            )}
        </>
    );
}
```

## Responsive Design

All components are fully responsive:
- Mobile: Single column layout
- Tablet: Optimized spacing
- Desktop: Full-width with max constraints

Breakpoints:
- `768px`: Mobile/Tablet
- `480px`: Mobile optimizations

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

## Accessibility

- Keyboard navigation support
- ARIA labels on interactive elements
- Focus indicators
- Screen reader friendly
- High contrast mode compatible
