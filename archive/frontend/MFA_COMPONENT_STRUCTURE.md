# MFA Component Structure

## File Tree

```
frontend/
├── src/
│   ├── components/
│   │   └── MFA/
│   │       ├── MFASetupModal.jsx           (237 lines) - Setup wizard
│   │       ├── MFAVerificationModal.jsx    (210 lines) - Login verification
│   │       ├── BackupCodesDisplay.jsx      (135 lines) - Backup codes UI
│   │       ├── MFASettings.jsx             (234 lines) - Settings panel
│   │       ├── TrustedDevicesList.jsx      (207 lines) - Device management
│   │       ├── index.js                    (6 lines)   - Barrel exports
│   │       └── README.md                   - Component docs
│   │
│   ├── contexts/
│   │   └── MFAContext.jsx                  (214 lines) - State management
│   │
│   ├── styles/
│   │   └── MFA.css                         (840 lines) - Complete styling
│   │
│   └── services/
│       └── api.js                          (Modified)  - Added MFA endpoints
│
└── MFA_INTEGRATION_GUIDE.md               - Integration instructions

Total: ~2,077 lines of code
```

## Component Hierarchy

```
MFAProvider (Context)
├── MFASetupModal
│   ├── Step 1: QR Code Display
│   ├── Step 2: Verification Input
│   └── Step 3: BackupCodesDisplay
│
├── MFAVerificationModal
│   ├── TOTP Input (6 digits)
│   ├── Backup Code Input (8 digits)
│   └── Trust Device Checkbox
│
└── MFASettings
    ├── Enable/Disable Controls
    ├── Backup Codes Section
    │   └── BackupCodesDisplay
    └── Trusted Devices Section
        └── TrustedDevicesList
```

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                        MFAContext                           │
│  • State: mfaEnabled, mfaRequired, backupCodesRemaining    │
│  • Methods: setupMFA, verifyMFACode, regenerateBackupCodes │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────────┐    ┌──────────────┐
│ MFASetupModal│    │MFAVerificationModal│   │ MFASettings  │
└──────────────┘    └──────────────────┘    └──────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
  ┌──────────┐         ┌──────────┐         ┌─────────────┐
  │ API POST │         │ API POST │         │  API GET    │
  │ /setup   │         │ /verify  │         │  /status    │
  └──────────┘         └──────────┘         └─────────────┘
```

## Component Relationships

### MFASetupModal
**Used by:** User Dashboard, Settings Page
**Uses:** BackupCodesDisplay
**Triggers:** MFA enable flow

### MFAVerificationModal
**Used by:** LoginForm
**Uses:** MFAContext hooks
**Triggers:** Post-login verification

### BackupCodesDisplay
**Used by:** MFASetupModal, MFASettings
**Uses:** None (pure component)
**Triggers:** Download, Print, Copy

### MFASettings
**Used by:** Settings Page
**Uses:** BackupCodesDisplay, TrustedDevicesList
**Triggers:** Enable/Disable, Regenerate

### TrustedDevicesList
**Used by:** MFASettings
**Uses:** API service
**Triggers:** Device removal

## User Flows

### 1. Initial Setup Flow

```
User clicks "Enable MFA"
    ↓
MFASetupModal opens (Step 1)
    ↓
Display QR Code + Secret Key
    ↓
User scans with authenticator app
    ↓
Click "Next" → Step 2
    ↓
Enter 6-digit TOTP code
    ↓
Backend verifies code
    ↓
Success → Step 3
    ↓
Display 10 Backup Codes
    ↓
User saves codes (copy/download/print)
    ↓
Click "Done" → Modal closes
    ↓
MFA is now enabled
```

### 2. Login with MFA Flow

```
User enters username/password
    ↓
Backend validates credentials
    ↓
Returns mfaRequired: true
    ↓
MFAVerificationModal opens
    ↓
User enters 6-digit TOTP code
    ↓
Auto-submit on 6th digit
    ↓
Backend verifies code
    ↓
Success → Receive final JWT token
    ↓
Store token, redirect to dashboard
```

### 3. Backup Code Usage Flow

```
Login screen → MFA required
    ↓
MFAVerificationModal opens
    ↓
User clicks "Use backup code"
    ↓
Input changes to 8-digit field
    ↓
User enters backup code
    ↓
Backend verifies and invalidates code
    ↓
Success → Login complete
    ↓
backupCodesRemaining decremented
```

### 4. Trusted Device Flow

```
MFAVerificationModal shown
    ↓
User checks "Trust this device"
    ↓
Enters TOTP code
    ↓
Backend creates trust token
    ↓
Token stored in localStorage
    ↓
Device added to trusted list
    ↓
Valid for 30 days
    ↓
No MFA required on this device
```

## API Endpoints

| Method | Endpoint | Component | Purpose |
|--------|----------|-----------|---------|
| POST | `/api/mfa/setup` | MFASetupModal | Initialize MFA setup |
| POST | `/api/mfa/verify-setup` | MFASetupModal | Verify setup code |
| POST | `/api/mfa/verify-login` | MFAVerificationModal | Verify login code |
| POST | `/api/mfa/verify-backup` | MFAVerificationModal | Verify backup code |
| POST | `/api/mfa/disable` | MFASettings | Disable MFA |
| POST | `/api/mfa/regenerate-backup-codes` | MFASettings | New backup codes |
| GET | `/api/mfa/status` | MFAContext | Get MFA status |
| GET | `/api/mfa/trusted-devices` | TrustedDevicesList | List devices |
| DELETE | `/api/mfa/trusted-devices/:id` | TrustedDevicesList | Remove device |

## State Management

### MFAContext State

```javascript
{
    mfaRequired: boolean,           // MFA verification needed
    mfaEnabled: boolean,            // User has MFA enabled
    backupCodesRemaining: number,   // Count of unused codes
    trustDeviceToken: string,       // Device trust token
    loading: boolean,               // API call in progress
    pendingCredentials: object      // Temporary login data
}
```

### Component Local State Examples

**MFASetupModal:**
```javascript
{
    step: 1|2|3,                    // Wizard step
    qrCodeUrl: string,              // Base64 QR image
    secret: string,                 // TOTP secret key
    backupCodes: string[],          // Generated codes
    verificationCode: string,       // User input
    error: string                   // Error message
}
```

**MFAVerificationModal:**
```javascript
{
    verificationCode: string,       // User input
    useBackupCode: boolean,         // Toggle mode
    trustDevice: boolean,           // Trust checkbox
    error: string                   // Error message
}
```

## Styling Classes

### Key CSS Classes

- `.modal-overlay` - Full-screen modal backdrop
- `.modal-content` - Modal container
- `.mfa-setup-modal` - Setup modal specific
- `.mfa-verification-modal` - Verification modal specific
- `.verification-input` - 6/8-digit code input
- `.qr-code-container` - QR code display
- `.backup-codes-grid` - 2-column code layout
- `.device-card` - Trusted device card
- `.btn-primary` - Primary action button
- `.btn-secondary` - Secondary action button
- `.btn-danger` - Destructive action button

## Security Features

1. **TOTP Verification**: Time-based one-time passwords
2. **Backup Codes**: 10 single-use recovery codes
3. **Trusted Devices**: 30-day device trust
4. **Auto-submit**: Reduces user friction
5. **Code Invalidation**: Backup codes expire after use
6. **Device Tracking**: IP, browser, location metadata
7. **Expiration Warnings**: Alert before device trust expires

## Responsive Breakpoints

- **Desktop**: > 768px (full width, 2-column codes)
- **Tablet**: 481px - 768px (optimized spacing)
- **Mobile**: ≤ 480px (single column, larger inputs)

## Dependencies

### Required
- `react` (18.2.0+)
- `react-router-dom` (6.20.0+)
- `axios` (1.6.0+)
- `qrcode.react` (Need to install)

### Included
All other functionality uses standard React hooks and browser APIs.

## Testing Checklist

- [ ] QR code displays correctly
- [ ] Secret key copy works
- [ ] TOTP verification succeeds
- [ ] Backup codes display
- [ ] Backup codes download
- [ ] Backup codes print
- [ ] Login MFA prompt appears
- [ ] Auto-submit on 6 digits
- [ ] Backup code login works
- [ ] Trust device saves token
- [ ] Trusted devices list loads
- [ ] Device removal works
- [ ] MFA enable/disable toggles
- [ ] Backup code regeneration
- [ ] Low codes warning shows
- [ ] Expiring device warning
- [ ] Responsive on mobile
- [ ] Keyboard navigation
- [ ] Error handling

## Performance Notes

- **Lazy Loading**: Consider code-splitting MFA components
- **Caching**: MFA status cached in context
- **Optimistic Updates**: UI updates before API confirmation
- **Debouncing**: Input validation debounced
- **Memoization**: Device list memoized

## Future Enhancements

1. SMS/Email backup delivery
2. Biometric verification
3. Push notifications
4. WebAuthn/FIDO2 support
5. Risk-based authentication
6. Geo-fencing rules
7. Time-based access policies
8. Recovery email workflow
