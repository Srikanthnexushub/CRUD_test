# Shared Components

This directory contains reusable UI components used throughout the application.

## Components

### Badge

A flexible badge component for displaying status indicators.

**Props:**
- `variant` (required): 'success' | 'error' | 'warning' | 'info'
- `text` (required): string - Text to display
- `size`: 'small' | 'medium' | 'large' (default: 'medium')
- `className`: string - Additional CSS classes

**Example:**
```jsx
import { Badge } from '@/components/shared';

<Badge variant="success" text="Active" size="medium" />
<Badge variant="error" text="Blocked" />
```

---

### ProgressBar

A color-coded progress bar that transitions between colors based on thresholds.

**Props:**
- `value` (required): number - Current progress value
- `max`: number (default: 100)
- `colorScheme`: object - Custom color configuration
  - `low`: number (default: 33) - Threshold for green
  - `medium`: number (default: 66) - Threshold for yellow
  - `lowColor`: string - Custom green color
  - `mediumColor`: string - Custom yellow color
  - `highColor`: string - Custom red color
- `showLabel`: boolean (default: true)
- `size`: 'small' | 'medium' | 'large' (default: 'medium')
- `className`: string

**Example:**
```jsx
import { ProgressBar } from '@/components/shared';

<ProgressBar value={75} max={100} />
<ProgressBar
  value={50}
  colorScheme={{ low: 40, medium: 70 }}
  showLabel={false}
/>
```

---

### CountdownTimer

A countdown timer with auto-updating display and completion callback.

**Props:**
- `targetTime` (required): Date | number - Target time or duration
- `mode`: 'timestamp' | 'duration' (default: 'timestamp')
- `onComplete`: function - Callback when countdown reaches zero
- `showHours`: boolean (default: false)
- `warningThreshold`: number (default: 10) - Seconds for warning style
- `className`: string

**Example:**
```jsx
import { CountdownTimer } from '@/components/shared';

// Countdown to specific time
<CountdownTimer
  targetTime={new Date('2026-01-30T12:00:00')}
  mode="timestamp"
  onComplete={() => alert('Time is up!')}
/>

// Countdown duration (5 minutes)
<CountdownTimer
  targetTime={300}
  mode="duration"
  onComplete={() => console.log('Timer expired')}
/>
```

---

### Toast

Toast notification system integrated with react-toastify.

**Setup:**
Add the Toast component to your main App component:
```jsx
import Toast from '@/components/shared/Toast';

function App() {
  return (
    <>
      <Toast />
      {/* rest of app */}
    </>
  );
}
```

**Usage:**
```jsx
import { showToast } from '@/components/shared';

// Success notification
showToast.success('Operation successful!');

// Error notification
showToast.error('Something went wrong');

// Warning notification
showToast.warning('Please be careful');

// Info notification
showToast.info('Here is some information');

// Loading notification
const loadingId = showToast.loading('Processing...');
// Later update it
showToast.update(loadingId, {
  render: 'Completed!',
  type: 'success',
  isLoading: false
});

// Dismiss notifications
showToast.dismiss(toastId); // Dismiss specific
showToast.dismiss(); // Dismiss all
```

## Installation

Make sure you have the required dependencies:

```bash
npm install react-toastify prop-types
```

## Importing

You can import components individually or all at once:

```jsx
// Individual imports
import { Badge, ProgressBar, CountdownTimer } from '@/components/shared';

// Import all
import * as SharedComponents from '@/components/shared';
```
