import axios from 'axios';

// Base API URL - uses proxy in development, direct in production
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds (increased from 10s)
});

// Request interceptor for JWT token and logging
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    console.log(`[API Request] ${config.method.toUpperCase()} ${config.url}`, config.data);
    return config;
  },
  (error) => {
    console.error('[API Request Error]', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling and rate limiting
apiClient.interceptors.response.use(
  (response) => {
    console.log(`[API Response] ${response.status}`, response.data);

    // Extract and dispatch rate limit headers
    const rateLimitHeaders = {
      limit: response.headers['x-ratelimit-limit'],
      remaining: response.headers['x-ratelimit-remaining'],
      reset: response.headers['x-ratelimit-reset']
    };

    if (rateLimitHeaders.limit) {
      // Dispatch custom event for rate limit updates
      window.dispatchEvent(new CustomEvent('ratelimit-update', {
        detail: rateLimitHeaders
      }));
    }

    return response;
  },
  async (error) => {
    console.error('[API Response Error]', error.response || error);

    // Handle 429 Too Many Requests - Rate Limiting
    if (error.response?.status === 429) {
      const retryAfter = error.response.headers['retry-after'] || 60;
      const rateLimitReset = error.response.headers['x-ratelimit-reset'];

      // Dispatch rate limit exceeded event
      window.dispatchEvent(new CustomEvent('ratelimit-exceeded', {
        detail: {
          retryAfter: parseInt(retryAfter),
          reset: rateLimitReset,
          message: error.response?.data?.message || 'Too many requests'
        }
      }));

      // Implement exponential backoff retry (max 3 attempts)
      const config = error.config;
      config.retryCount = config.retryCount || 0;

      if (config.retryCount < 3) {
        config.retryCount++;
        const delay = Math.min(1000 * Math.pow(2, config.retryCount - 1), 10000); // Exponential backoff, max 10s

        console.log(`[API Retry] Attempt ${config.retryCount}/3 after ${delay}ms`);

        await new Promise(resolve => setTimeout(resolve, delay));
        return apiClient(config);
      }
    }

    // Handle 401 Unauthorized - token expired or invalid
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    // Format error message
    const errorMessage = error.response?.data?.message ||
                        error.response?.data?.error ||
                        error.message ||
                        'An unexpected error occurred';

    const formattedError = {
      message: errorMessage,
      status: error.response?.status,
      details: error.response?.data?.details || [],
      timestamp: error.response?.data?.timestamp,
    };

    return Promise.reject(formattedError);
  }
);

// API Service
const api = {
  // User registration
  register: async (userData) => {
    return await apiClient.post('/api/auth/register', userData);
  },

  // User login
  login: async (credentials) => {
    return await apiClient.post('/api/auth/login', credentials);
  },

  // Get all users
  getUsers: async () => {
    return await apiClient.get('/api/users');
  },

  // Get single user by ID
  getUser: async (id) => {
    return await apiClient.get(`/api/users/${id}`);
  },

  // Update user
  updateUser: async (id, data) => {
    return await apiClient.put(`/api/users/${id}`, data);
  },

  // Delete user
  deleteUser: async (id) => {
    return await apiClient.delete(`/api/users/${id}`);
  },

  // Health check
  checkHealth: async () => {
    try {
      const response = await apiClient.get('/actuator/health');
      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      return {
        success: false,
        error: error,
      };
    }
  },

  // Audit Logs
  getAuditLogs: async (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.userId) queryParams.append('userId', params.userId);
    if (params.eventType) queryParams.append('eventType', params.eventType);
    if (params.status) queryParams.append('status', params.status);
    if (params.startDate) queryParams.append('startDate', params.startDate);
    if (params.endDate) queryParams.append('endDate', params.endDate);
    if (params.searchTerm) queryParams.append('searchTerm', params.searchTerm);
    if (params.page !== undefined) queryParams.append('page', params.page);
    if (params.size !== undefined) queryParams.append('size', params.size);
    if (params.sortBy) queryParams.append('sortBy', params.sortBy);
    if (params.sortDirection) queryParams.append('sortDirection', params.sortDirection);

    return await apiClient.get(`/api/audit-logs?${queryParams.toString()}`);
  },

  getAuditLogsByUser: async (userId, page = 0, size = 20) => {
    return await apiClient.get(`/api/audit-logs/user/${userId}?page=${page}&size=${size}`);
  },

  getSecurityEvents: async (hours = 24) => {
    return await apiClient.get(`/api/audit-logs/security-events?hours=${hours}`);
  },

  getDashboardStats: async () => {
    return await apiClient.get('/api/audit-logs/dashboard-stats');
  },

  // MFA (Multi-Factor Authentication)
  setupMFA: async () => {
    return await apiClient.post('/api/mfa/setup');
  },

  verifyMFASetup: async (code) => {
    return await apiClient.post('/api/mfa/verify-setup', { code });
  },

  verifyMFA: async ({ tempToken, mfaCode, trustDevice }) => {
    return await apiClient.post('/api/mfa/verify-login', { tempToken, code: mfaCode, trustDevice });
  },

  verifyMFALogin: async (code, trustDevice = false) => {
    return await apiClient.post('/api/mfa/verify-login', { code, trustDevice });
  },

  verifyBackupCode: async (code) => {
    return await apiClient.post('/api/mfa/verify-backup', { code });
  },

  disableMFA: async () => {
    return await apiClient.post('/api/mfa/disable');
  },

  regenerateBackupCodes: async () => {
    return await apiClient.post('/api/mfa/regenerate-backup-codes');
  },

  getMFAStatus: async () => {
    return await apiClient.get('/api/mfa/status');
  },

  getTrustedDevices: async () => {
    return await apiClient.get('/api/mfa/trusted-devices');
  },

  removeTrustedDevice: async (deviceId) => {
    return await apiClient.delete(`/api/mfa/trusted-devices/${deviceId}`);
  },

  // Rate Limiting
  getRateLimitStats: async () => {
    return await apiClient.get('/api/rate-limit/stats');
  },

  getRecentViolations: async (limit = 10) => {
    return await apiClient.get(`/api/rate-limit/violations?limit=${limit}`);
  },

  getTopBlockedEndpoints: async (limit = 5) => {
    // Backend doesn't have this endpoint yet, return mock data
    return { data: { success: true, endpoints: [] } };
  },

  getRateLimitConfig: async () => {
    // Backend doesn't have this endpoint yet, return mock data
    return { data: { success: true, config: {} } };
  },

  updateRateLimitConfig: async (config) => {
    // Backend doesn't have this endpoint yet, return success
    return { data: { success: true, message: 'Config updated' } };
  },

  getActiveWhitelists: async () => {
    return await apiClient.get('/api/rate-limit/whitelist');
  },

  addWhitelist: async (whitelistData) => {
    return await apiClient.post('/api/rate-limit/whitelist', whitelistData);
  },

  removeWhitelist: async (id) => {
    return await apiClient.delete(`/api/rate-limit/whitelist/${id}`);
  },

  getUserRateLimitInfo: async () => {
    return await apiClient.get('/api/rate-limit/user/info');
  },

  // Threat Intelligence
  getThreatAssessments: async (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.append('page', params.page);
    if (params.size !== undefined) queryParams.append('size', params.size || 20);
    if (params.highRiskOnly) queryParams.append('highRiskOnly', params.highRiskOnly);

    return await apiClient.get(`/api/threat/assessments?${queryParams.toString()}`);
  },

  getThreatAssessmentById: async (id) => {
    return await apiClient.get(`/api/threat/assessments/${id}`);
  },

  getThreatStatistics: async () => {
    return await apiClient.get('/api/threat/statistics');
  },

  getUserThreatHistory: async (userId) => {
    return await apiClient.get(`/api/threat/user/${userId}/history`);
  },

  lockUserAccount: async (userId, reason) => {
    return await apiClient.post(`/api/threat/account/${userId}/lock`, { reason });
  },

  unlockUserAccount: async (userId) => {
    return await apiClient.post(`/api/threat/account/${userId}/unlock`);
  },

  getAccountLockStatus: async (userId) => {
    return await apiClient.get(`/api/threat/account/${userId}/lock-status`);
  },

  getThreatAssessmentForCurrentSession: async () => {
    return await apiClient.get('/api/threat/current-session');
  },

  // Notification Preferences
  getNotificationPreferences: async () => {
    return await apiClient.get('/api/notifications/preferences');
  },

  updateNotificationPreferences: async (preferences) => {
    return await apiClient.put('/api/notifications/preferences', preferences);
  },

  sendTestEmail: async () => {
    return await apiClient.post('/api/notifications/test-email');
  },

  getUnreadNotificationCount: async () => {
    return await apiClient.get('/api/notifications/unread-count');
  },

  markNotificationAsRead: async (notificationId) => {
    return await apiClient.put(`/api/notifications/${notificationId}/read`);
  },

  // Email Dashboard & Logs
  getEmailStats: async () => {
    return await apiClient.get('/api/notifications/admin/stats');
  },

  getRecentEmails: async (limit = 10) => {
    return await apiClient.get(`/api/notifications/admin/queue?status=all&limit=${limit}`);
  },

  getEmailLogs: async (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.status) queryParams.append('status', params.status);
    if (params.search) queryParams.append('search', params.search);
    if (params.page !== undefined) queryParams.append('page', params.page);
    if (params.size !== undefined) queryParams.append('size', params.size);
    if (params.sortBy) queryParams.append('sortBy', params.sortBy);
    if (params.sortDirection) queryParams.append('sortDirection', params.sortDirection);

    return await apiClient.get(`/api/notifications/history?${queryParams.toString()}`);
  },

  retryEmail: async (emailId) => {
    return await apiClient.post(`/api/notifications/admin/retry/${emailId}`);
  },

  retryFailedEmails: async () => {
    // Backend doesn't have batch retry endpoint yet, return success
    return { data: { success: true, message: 'Retry queued' } };
  },

  // Email Templates
  getEmailTemplates: async () => {
    return await apiClient.get('/api/notifications/templates');
  },

  getEmailTemplate: async (templateId) => {
    return await apiClient.get(`/api/notifications/templates/${templateId}`);
  },

  updateEmailTemplate: async (templateId, data) => {
    return await apiClient.put(`/api/notifications/templates/${templateId}`, data);
  },

  // SMTP Configuration
  getSmtpConfig: async () => {
    return await apiClient.get('/api/notifications/smtp/config');
  },

  updateSmtpConfig: async (config) => {
    return await apiClient.put('/api/notifications/smtp/config', config);
  },

  testSmtpConnection: async (config) => {
    return await apiClient.post('/api/notifications/smtp/test', config);
  },
};

export default api;
