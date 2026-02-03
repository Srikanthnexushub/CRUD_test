import axios, { AxiosInstance, AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import {
  ApiResponse,
  PaginatedResponse,
  ApiError,
  RateLimitHeaders,
  RateLimitExceededEvent,
  AuditLogParams,
  AuditLog,
  MFASetupResponse,
  MFAVerifyLoginRequest,
  MFAStatus,
  TrustedDevice,
  RateLimitStats,
  RateLimitViolation,
  WhitelistEntry,
  ThreatAssessment,
  ThreatStatistics,
  NotificationPreferences,
  EmailStats,
  EmailLog,
  EmailTemplate,
  SmtpConfig,
  DashboardStats,
  LoginResponse,
  RegisterData,
  LoginCredentials,
  User,
} from '../types';

// Extend AxiosRequestConfig to include retryCount
interface RetryAxiosRequestConfig extends InternalAxiosRequestConfig {
  retryCount?: number;
}

// Base API URL - uses proxy in development, direct in production
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Create axios instance with default config
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds
});

// Request interceptor for JWT token and logging
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, config.data);
    return config;
  },
  (error: AxiosError): Promise<AxiosError> => {
    console.error('[API Request Error]', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling and rate limiting
apiClient.interceptors.response.use(
  (response: AxiosResponse): AxiosResponse => {
    console.log(`[API Response] ${response.status}`, response.data);

    // Extract and dispatch rate limit headers
    const rateLimitHeaders: RateLimitHeaders = {
      limit: response.headers['x-ratelimit-limit'] as string | undefined,
      remaining: response.headers['x-ratelimit-remaining'] as string | undefined,
      reset: response.headers['x-ratelimit-reset'] as string | undefined,
    };

    if (rateLimitHeaders.limit) {
      // Dispatch custom event for rate limit updates
      window.dispatchEvent(new CustomEvent('ratelimit-update', {
        detail: rateLimitHeaders,
      }));
    }

    return response;
  },
  async (error: AxiosError): Promise<never> => {
    console.error('[API Response Error]', error.response || error);

    // Handle 429 Too Many Requests - Rate Limiting
    if (error.response?.status === 429) {
      const retryAfter = error.response.headers['retry-after'] || '60';
      const rateLimitReset = error.response.headers['x-ratelimit-reset'] as string | undefined;

      // Dispatch rate limit exceeded event
      const rateLimitEvent: RateLimitExceededEvent = {
        retryAfter: parseInt(retryAfter),
        reset: rateLimitReset,
        message: (error.response?.data as any)?.message || 'Too many requests',
      };

      window.dispatchEvent(new CustomEvent('ratelimit-exceeded', {
        detail: rateLimitEvent,
      }));

      // Implement exponential backoff retry (max 3 attempts)
      const config = error.config as RetryAxiosRequestConfig;
      if (config) {
        config.retryCount = config.retryCount || 0;

        if (config.retryCount < 3) {
          config.retryCount++;
          const delay = Math.min(1000 * Math.pow(2, config.retryCount - 1), 10000); // Exponential backoff, max 10s

          console.log(`[API Retry] Attempt ${config.retryCount}/3 after ${delay}ms`);

          await new Promise(resolve => setTimeout(resolve, delay));
          return apiClient(config);
        }
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
    const errorData = error.response?.data as any;
    const errorMessage = errorData?.message ||
                        errorData?.error ||
                        error.message ||
                        'An unexpected error occurred';

    const formattedError: ApiError = {
      message: errorMessage,
      status: error.response?.status,
      details: errorData?.details || [],
      timestamp: errorData?.timestamp,
    };

    return Promise.reject(formattedError);
  }
);

// API Service
const api = {
  // User registration
  register: async (userData: RegisterData): Promise<AxiosResponse<ApiResponse<void>>> => {
    return await apiClient.post('/api/auth/register', userData);
  },

  // User login
  login: async (credentials: LoginCredentials): Promise<AxiosResponse<LoginResponse>> => {
    return await apiClient.post('/api/auth/login', credentials);
  },

  // Get all users
  getUsers: async (): Promise<AxiosResponse<User[]>> => {
    return await apiClient.get('/api/users');
  },

  // Get single user by ID
  getUser: async (id: number): Promise<AxiosResponse<User>> => {
    return await apiClient.get(`/api/users/${id}`);
  },

  // Update user
  updateUser: async (id: number, data: Partial<User>): Promise<AxiosResponse<User>> => {
    return await apiClient.put(`/api/users/${id}`, data);
  },

  // Delete user
  deleteUser: async (id: number): Promise<AxiosResponse<void>> => {
    return await apiClient.delete(`/api/users/${id}`);
  },

  // Health check
  checkHealth: async (): Promise<{ success: boolean; data?: any; error?: any }> => {
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
  getAuditLogs: async (params: AuditLogParams = {}): Promise<AxiosResponse<PaginatedResponse<AuditLog>>> => {
    const queryParams = new URLSearchParams();
    if (params.userId) queryParams.append('userId', params.userId.toString());
    if (params.eventType) queryParams.append('eventType', params.eventType);
    if (params.status) queryParams.append('status', params.status);
    if (params.startDate) queryParams.append('startDate', params.startDate);
    if (params.endDate) queryParams.append('endDate', params.endDate);
    if (params.searchTerm) queryParams.append('searchTerm', params.searchTerm);
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.size !== undefined) queryParams.append('size', params.size.toString());
    if (params.sortBy) queryParams.append('sortBy', params.sortBy);
    if (params.sortDirection) queryParams.append('sortDirection', params.sortDirection);

    return await apiClient.get(`/api/audit-logs?${queryParams.toString()}`);
  },

  getAuditLogsByUser: async (userId: number, page = 0, size = 20): Promise<AxiosResponse<PaginatedResponse<AuditLog>>> => {
    return await apiClient.get(`/api/audit-logs/user/${userId}?page=${page}&size=${size}`);
  },

  getSecurityEvents: async (hours = 24): Promise<AxiosResponse<AuditLog[]>> => {
    return await apiClient.get(`/api/audit-logs/security-events?hours=${hours}`);
  },

  getDashboardStats: async (): Promise<AxiosResponse<DashboardStats>> => {
    return await apiClient.get('/api/audit-logs/dashboard-stats');
  },

  // MFA (Multi-Factor Authentication)
  setupMFA: async (): Promise<AxiosResponse<MFASetupResponse>> => {
    return await apiClient.post('/api/mfa/setup');
  },

  verifyMFASetup: async (code: string): Promise<AxiosResponse<ApiResponse<{ backupCodes: string[] }>>> => {
    return await apiClient.post('/api/mfa/verify-setup', { code });
  },

  verifyMFA: async (data: MFAVerifyLoginRequest): Promise<AxiosResponse<LoginResponse>> => {
    return await apiClient.post('/api/mfa/verify-login', {
      tempToken: data.tempToken,
      code: data.code,
      trustDevice: data.trustDevice
    });
  },

  verifyMFALogin: async (code: string, trustDevice = false): Promise<AxiosResponse<LoginResponse>> => {
    return await apiClient.post('/api/mfa/verify-login', { code, trustDevice });
  },

  verifyBackupCode: async (code: string): Promise<AxiosResponse<LoginResponse>> => {
    return await apiClient.post('/api/mfa/verify-backup', { code });
  },

  disableMFA: async (): Promise<AxiosResponse<ApiResponse<void>>> => {
    return await apiClient.post('/api/mfa/disable');
  },

  regenerateBackupCodes: async (): Promise<AxiosResponse<ApiResponse<{ backupCodes: string[] }>>> => {
    return await apiClient.post('/api/mfa/regenerate-backup-codes');
  },

  getMFAStatus: async (): Promise<AxiosResponse<MFAStatus>> => {
    return await apiClient.get('/api/mfa/status');
  },

  getTrustedDevices: async (): Promise<AxiosResponse<TrustedDevice[]>> => {
    return await apiClient.get('/api/mfa/trusted-devices');
  },

  removeTrustedDevice: async (deviceId: number): Promise<AxiosResponse<void>> => {
    return await apiClient.delete(`/api/mfa/trusted-devices/${deviceId}`);
  },

  // Rate Limiting
  getRateLimitStats: async (): Promise<AxiosResponse<RateLimitStats>> => {
    return await apiClient.get('/api/rate-limit/stats');
  },

  getRecentViolations: async (limit = 10): Promise<AxiosResponse<RateLimitViolation[]>> => {
    return await apiClient.get(`/api/rate-limit/violations?limit=${limit}`);
  },

  getTopBlockedEndpoints: async (_limit = 5): Promise<AxiosResponse<ApiResponse>> => {
    // Backend doesn't have this endpoint yet, return mock data
    return Promise.resolve({ data: { success: true, endpoints: [] } } as unknown as AxiosResponse<ApiResponse>);
  },

  getRateLimitConfig: async (): Promise<AxiosResponse<ApiResponse>> => {
    // Backend doesn't have this endpoint yet, return mock data
    return Promise.resolve({ data: { success: true, config: {} } } as unknown as AxiosResponse<ApiResponse>);
  },

  updateRateLimitConfig: async (_config: any): Promise<AxiosResponse<ApiResponse>> => {
    // Backend doesn't have this endpoint yet, return success
    return Promise.resolve({ data: { success: true, message: 'Config updated' } } as unknown as AxiosResponse<ApiResponse>);
  },

  getActiveWhitelists: async (): Promise<AxiosResponse<WhitelistEntry[]>> => {
    return await apiClient.get('/api/rate-limit/whitelist');
  },

  addWhitelist: async (whitelistData: Partial<WhitelistEntry>): Promise<AxiosResponse<WhitelistEntry>> => {
    return await apiClient.post('/api/rate-limit/whitelist', whitelistData);
  },

  removeWhitelist: async (id: number): Promise<AxiosResponse<void>> => {
    return await apiClient.delete(`/api/rate-limit/whitelist/${id}`);
  },

  getUserRateLimitInfo: async (): Promise<AxiosResponse<any>> => {
    return await apiClient.get('/api/rate-limit/user/info');
  },

  // Threat Intelligence
  getThreatAssessments: async (params: { page?: number; size?: number; highRiskOnly?: boolean } = {}): Promise<AxiosResponse<PaginatedResponse<ThreatAssessment>>> => {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.size !== undefined) queryParams.append('size', (params.size || 20).toString());
    if (params.highRiskOnly) queryParams.append('highRiskOnly', params.highRiskOnly.toString());

    return await apiClient.get(`/api/threat/assessments?${queryParams.toString()}`);
  },

  getThreatAssessmentById: async (id: number): Promise<AxiosResponse<ThreatAssessment>> => {
    return await apiClient.get(`/api/threat/assessments/${id}`);
  },

  getThreatStatistics: async (): Promise<AxiosResponse<ThreatStatistics>> => {
    return await apiClient.get('/api/threat/statistics');
  },

  getUserThreatHistory: async (userId: number): Promise<AxiosResponse<ThreatAssessment[]>> => {
    return await apiClient.get(`/api/threat/user/${userId}/history`);
  },

  lockUserAccount: async (userId: number, reason: string): Promise<AxiosResponse<void>> => {
    return await apiClient.post(`/api/threat/account/${userId}/lock`, { reason });
  },

  unlockUserAccount: async (userId: number): Promise<AxiosResponse<void>> => {
    return await apiClient.post(`/api/threat/account/${userId}/unlock`);
  },

  getAccountLockStatus: async (userId: number): Promise<AxiosResponse<any>> => {
    return await apiClient.get(`/api/threat/account/${userId}/lock-status`);
  },

  getThreatAssessmentForCurrentSession: async (): Promise<AxiosResponse<ThreatAssessment>> => {
    return await apiClient.get('/api/threat/current-session');
  },

  // Notification Preferences
  getNotificationPreferences: async (): Promise<AxiosResponse<NotificationPreferences>> => {
    return await apiClient.get('/api/notifications/preferences');
  },

  updateNotificationPreferences: async (preferences: NotificationPreferences): Promise<AxiosResponse<NotificationPreferences>> => {
    return await apiClient.put('/api/notifications/preferences', preferences);
  },

  sendTestEmail: async (): Promise<AxiosResponse<ApiResponse<void>>> => {
    return await apiClient.post('/api/notifications/test-email');
  },

  getUnreadNotificationCount: async (): Promise<AxiosResponse<{ count: number }>> => {
    return await apiClient.get('/api/notifications/unread-count');
  },

  markNotificationAsRead: async (notificationId: number): Promise<AxiosResponse<void>> => {
    return await apiClient.put(`/api/notifications/${notificationId}/read`);
  },

  // Email Dashboard & Logs
  getEmailStats: async (): Promise<AxiosResponse<EmailStats>> => {
    return await apiClient.get('/api/notifications/admin/stats');
  },

  getRecentEmails: async (limit = 10): Promise<AxiosResponse<EmailLog[]>> => {
    return await apiClient.get(`/api/notifications/admin/queue?status=all&limit=${limit}`);
  },

  getEmailLogs: async (params: {
    status?: string;
    search?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  } = {}): Promise<AxiosResponse<PaginatedResponse<EmailLog>>> => {
    const queryParams = new URLSearchParams();
    if (params.status) queryParams.append('status', params.status);
    if (params.search) queryParams.append('search', params.search);
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.size !== undefined) queryParams.append('size', params.size.toString());
    if (params.sortBy) queryParams.append('sortBy', params.sortBy);
    if (params.sortDirection) queryParams.append('sortDirection', params.sortDirection);

    return await apiClient.get(`/api/notifications/history?${queryParams.toString()}`);
  },

  retryEmail: async (emailId: number): Promise<AxiosResponse<void>> => {
    return await apiClient.post(`/api/notifications/admin/retry/${emailId}`);
  },

  retryFailedEmails: async (): Promise<AxiosResponse<ApiResponse>> => {
    // Backend doesn't have batch retry endpoint yet, return success
    return Promise.resolve({ data: { success: true, message: 'Retry queued' } } as unknown as AxiosResponse<ApiResponse>);
  },

  // Email Templates
  getEmailTemplates: async (): Promise<AxiosResponse<EmailTemplate[]>> => {
    return await apiClient.get('/api/notifications/templates');
  },

  getEmailTemplate: async (templateId: string): Promise<AxiosResponse<EmailTemplate>> => {
    return await apiClient.get(`/api/notifications/templates/${templateId}`);
  },

  updateEmailTemplate: async (templateId: string, data: Partial<EmailTemplate>): Promise<AxiosResponse<EmailTemplate>> => {
    return await apiClient.put(`/api/notifications/templates/${templateId}`, data);
  },

  // SMTP Configuration
  getSmtpConfig: async (): Promise<AxiosResponse<SmtpConfig>> => {
    return await apiClient.get('/api/notifications/smtp/config');
  },

  updateSmtpConfig: async (config: SmtpConfig): Promise<AxiosResponse<SmtpConfig>> => {
    return await apiClient.put('/api/notifications/smtp/config', config);
  },

  testSmtpConnection: async (config: SmtpConfig): Promise<AxiosResponse<ApiResponse<void>>> => {
    return await apiClient.post('/api/notifications/smtp/test', config);
  },
};

export default api;
