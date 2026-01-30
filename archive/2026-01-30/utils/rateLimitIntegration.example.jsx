/**
 * Rate Limit Integration Example
 *
 * This file demonstrates how to integrate the Rate Limit components
 * into your React application.
 */

import React from 'react';
import { RateLimitProvider } from '../contexts/RateLimitContext';
import {
  RateLimitToast,
  UsageProgressBar,
  RateLimitDashboard,
  RequestRateGauge,
  RateLimitConfigModal
} from '../components/RateLimit';

/**
 * Step 1: Wrap your app with RateLimitProvider
 *
 * In your main App.jsx or index.jsx:
 */
export function AppWithRateLimit({ children }) {
  return (
    <RateLimitProvider>
      {/* Global toast for rate limit notifications */}
      <RateLimitToast />

      {/* Your app content */}
      {children}
    </RateLimitProvider>
  );
}

/**
 * Step 2: Use UsageProgressBar in user-facing pages
 *
 * Example: In a Dashboard or Profile page:
 */
export function UserDashboardExample() {
  return (
    <div className="user-dashboard">
      <h1>My Dashboard</h1>

      {/* Show usage progress bar */}
      <UsageProgressBar />

      {/* Rest of your dashboard content */}
      <div className="dashboard-content">
        {/* ... */}
      </div>
    </div>
  );
}

/**
 * Step 3: Use RateLimitDashboard for Admin pages
 *
 * Example: In an Admin panel:
 */
export function AdminPanelExample() {
  const [showConfigModal, setShowConfigModal] = React.useState(false);

  return (
    <div className="admin-panel">
      <div className="admin-header">
        <h1>Rate Limit Management</h1>
        <button onClick={() => setShowConfigModal(true)}>
          Configure Whitelist
        </button>
      </div>

      {/* Full rate limit dashboard */}
      <RateLimitDashboard />

      {/* Configuration modal */}
      <RateLimitConfigModal
        isOpen={showConfigModal}
        onClose={() => setShowConfigModal(false)}
        onSuccess={() => {
          // Refresh data or show success message
          console.log('Whitelist updated');
        }}
      />
    </div>
  );
}

/**
 * Step 4: Use RequestRateGauge in monitoring pages
 *
 * Example: In a system status page:
 */
export function SystemStatusExample() {
  return (
    <div className="system-status">
      <h1>System Status</h1>

      <div className="metrics-grid">
        {/* Rate limit gauge */}
        <RequestRateGauge title="API Usage" />

        {/* Other metrics */}
        {/* ... */}
      </div>
    </div>
  );
}

/**
 * Step 5: Access rate limit data programmatically
 *
 * Use the useRateLimit hook in any component:
 */
import { useRateLimit } from '../contexts/RateLimitContext';

export function CustomRateLimitComponent() {
  const {
    limits,
    exceeded,
    getSecondsUntilReset,
    getUsagePercentage
  } = useRateLimit();

  if (!limits.limit) {
    return null; // No rate limit data available
  }

  return (
    <div className="rate-limit-info">
      <p>Used: {limits.limit - limits.remaining} / {limits.limit}</p>
      <p>Usage: {getUsagePercentage()}%</p>
      {exceeded && (
        <p>Rate limit exceeded. Resets in {getSecondsUntilReset()}s</p>
      )}
    </div>
  );
}

/**
 * Complete App Structure Example:
 */
export function CompleteAppExample() {
  return (
    <RateLimitProvider>
      {/* Global rate limit toast */}
      <RateLimitToast />

      <div className="app">
        <nav>
          {/* Navigation */}
        </nav>

        <main>
          {/* Your routes */}
          <Routes>
            <Route path="/dashboard" element={<UserDashboardExample />} />
            <Route path="/admin/rate-limits" element={<AdminPanelExample />} />
            <Route path="/status" element={<SystemStatusExample />} />
          </Routes>
        </main>
      </div>
    </RateLimitProvider>
  );
}

/**
 * API Integration Notes:
 *
 * The axios interceptor in src/services/api.js automatically:
 * 1. Extracts X-RateLimit-* headers from responses
 * 2. Dispatches 'ratelimit-update' events
 * 3. Handles 429 errors with auto-retry
 * 4. Dispatches 'ratelimit-exceeded' events
 *
 * The RateLimitContext listens to these events and updates state automatically.
 */

/**
 * Backend API Endpoints Expected:
 *
 * GET /api/rate-limit/stats - Get rate limit statistics
 * GET /api/rate-limit/violations/recent?limit=10 - Get recent violations
 * GET /api/rate-limit/violations/top-endpoints?limit=5 - Get top blocked endpoints
 * GET /api/rate-limit/config - Get rate limit configuration
 * PUT /api/rate-limit/config - Update rate limit configuration
 * GET /api/rate-limit/whitelist - Get active whitelists
 * POST /api/rate-limit/whitelist - Add whitelist entry
 * DELETE /api/rate-limit/whitelist/:id - Remove whitelist entry
 * GET /api/rate-limit/user/info - Get current user's rate limit info
 *
 * Response Headers:
 * X-RateLimit-Limit: Maximum requests allowed
 * X-RateLimit-Remaining: Remaining requests
 * X-RateLimit-Reset: Unix timestamp when limit resets
 */
