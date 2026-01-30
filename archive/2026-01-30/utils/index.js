/**
 * Utilities export index
 * Provides a single entry point for importing utility functions
 *
 * @module utils
 *
 * @example
 * // Import specific utilities
 * import { validateEmail, formatDate, RISK_COLORS } from '@/utils';
 *
 * // Import by category
 * import * as validators from '@/utils/validators';
 * import * as formatters from '@/utils/formatters';
 * import * as colors from '@/utils/colorSchemes';
 */

// Validators
export {
  validateTOTP,
  validateBackupCode,
  validateEmail,
  validateIPAddress,
  validatePassword,
  validateUsername
} from './validators';

// Formatters
export {
  formatDate,
  formatRelativeTime,
  formatRiskScore,
  formatDuration,
  formatBytes,
  formatNumber,
  formatPercentage,
  truncateText
} from './formatters';

// Color schemes
export {
  RISK_COLORS,
  STATUS_COLORS,
  THEME_COLORS,
  CHART_COLORS,
  SEVERITY_COLORS,
  getRiskColor,
  getStatusColor,
  getSeverityColor,
  hexToRgb,
  rgbToHex,
  addAlpha
} from './colorSchemes';

// Device fingerprinting
export {
  getDeviceFingerprint,
  clearFingerprintCache,
  getBasicDeviceInfo,
  isFingerprintingSupported,
  compareFingerprintIds,
  hashFingerprint,
  initializeFingerprinting,
  generateDeviceFingerprint,
  getSessionInfo
} from './deviceFingerprint';
