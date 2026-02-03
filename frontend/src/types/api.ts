export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
  details?: string[];
  timestamp?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export interface ApiError {
  message: string;
  status?: number;
  details?: string[];
  timestamp?: string;
}

export interface RateLimitHeaders {
  limit?: string;
  remaining?: string;
  reset?: string;
}

export interface RateLimitExceededEvent {
  retryAfter: number;
  reset?: string;
  message: string;
}

// Audit Log Types
export interface AuditLog {
  id: number;
  userId: number;
  username: string;
  eventType: string;
  action: string;
  details: string;
  ipAddress: string;
  userAgent: string;
  status: 'SUCCESS' | 'FAILURE' | 'BLOCKED';
  timestamp: string;
}

export interface AuditLogParams {
  userId?: number;
  eventType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  searchTerm?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

// MFA Types
export interface MFASetupResponse {
  qrCodeUrl: string;
  secret: string;
  backupCodes: string[];
}

export interface MFAVerifyRequest {
  code: string;
  trustDevice?: boolean;
}

export interface MFAVerifyLoginRequest {
  tempToken?: string;
  code: string;
  trustDevice?: boolean;
}

export interface MFAStatus {
  enabled: boolean;
  backupCodesRemaining?: number;
}

export interface TrustedDevice {
  id: number;
  deviceName: string;
  deviceType: string;
  lastUsed: string;
  createdAt: string;
}

// Rate Limit Types
export interface RateLimitStats {
  totalRequests: number;
  blockedRequests: number;
  allowedRequests: number;
  topEndpoints: Array<{
    endpoint: string;
    count: number;
  }>;
}

export interface RateLimitViolation {
  id: number;
  ipAddress: string;
  endpoint: string;
  timestamp: string;
  requestCount: number;
}

export interface WhitelistEntry {
  id: number;
  ipAddress: string;
  reason: string;
  createdBy: string;
  createdAt: string;
}

// Threat Intelligence Types
export interface ThreatAssessment {
  id: number;
  ipAddress: string;
  riskScore: number;
  riskLevel: string;
  isBlacklisted: boolean;
  isTor: boolean;
  isProxy: boolean;
  isVpn: boolean;
  isDatacenter: boolean;
  failedLoginCount: number;
  suspiciousActivityCount: number;
  lastSeenAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface ThreatStatistics {
  totalAssessments: number;
  highRiskIps: number;
  blacklistedIps: number;
  totalFailedLogins: number;
  averageRiskScore: number;
}

// Notification Types
export interface NotificationPreferences {
  emailEnabled: boolean;
  loginAlerts: boolean;
  mfaAlerts: boolean;
  accountLockAlerts: boolean;
  suspiciousActivityAlerts: boolean;
}

export interface EmailStats {
  totalSent: number;
  totalFailed: number;
  successRate: number;
  recentFailures: number;
}

export interface EmailLog {
  id: number;
  recipient: string;
  subject: string;
  status: 'PENDING' | 'SENT' | 'FAILED';
  sentAt?: string;
  error?: string;
  retryCount: number;
}

export interface EmailTemplate {
  id: string;
  name: string;
  subject: string;
  content: string;
}

export interface SmtpConfig {
  host: string;
  port: number;
  username: string;
  password?: string;
  fromAddress: string;
  fromName: string;
  useTls: boolean;
}

// Dashboard Types
export interface DashboardStats {
  totalUsers: number;
  activeUsers: number;
  totalLogins: number;
  failedLogins: number;
  auditLogCount: number;
  securityEvents: number;
}
