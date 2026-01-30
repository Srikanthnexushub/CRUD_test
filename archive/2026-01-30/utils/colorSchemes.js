/**
 * Color scheme constants and utilities for consistent theming
 * @module colorSchemes
 */

/**
 * Risk level color schemes
 * @constant
 * @type {Object}
 */
export const RISK_COLORS = {
  low: {
    primary: '#10b981',      // Green
    light: '#d1fae5',
    dark: '#065f46',
    border: '#34d399'
  },
  medium: {
    primary: '#f59e0b',      // Yellow/Amber
    light: '#fef3c7',
    dark: '#92400e',
    border: '#fbbf24'
  },
  high: {
    primary: '#ef4444',      // Red
    light: '#fee2e2',
    dark: '#991b1b',
    border: '#f87171'
  },
  critical: {
    primary: '#dc2626',      // Dark Red
    light: '#fecaca',
    dark: '#7f1d1d',
    border: '#ef4444'
  }
};

/**
 * Status color schemes for various states
 * @constant
 * @type {Object}
 */
export const STATUS_COLORS = {
  success: {
    primary: '#10b981',
    light: '#d1fae5',
    dark: '#065f46',
    border: '#34d399',
    text: '#065f46'
  },
  error: {
    primary: '#ef4444',
    light: '#fee2e2',
    dark: '#991b1b',
    border: '#f87171',
    text: '#991b1b'
  },
  warning: {
    primary: '#f59e0b',
    light: '#fef3c7',
    dark: '#92400e',
    border: '#fbbf24',
    text: '#92400e'
  },
  info: {
    primary: '#3b82f6',
    light: '#dbeafe',
    dark: '#1e40af',
    border: '#60a5fa',
    text: '#1e40af'
  },
  neutral: {
    primary: '#6b7280',
    light: '#f3f4f6',
    dark: '#374151',
    border: '#9ca3af',
    text: '#374151'
  },
  active: {
    primary: '#059669',
    light: '#d1fae5',
    dark: '#065f46',
    border: '#34d399',
    text: '#065f46'
  },
  inactive: {
    primary: '#6b7280',
    light: '#e5e7eb',
    dark: '#1f2937',
    border: '#9ca3af',
    text: '#4b5563'
  },
  pending: {
    primary: '#f59e0b',
    light: '#fef3c7',
    dark: '#92400e',
    border: '#fbbf24',
    text: '#92400e'
  },
  blocked: {
    primary: '#ef4444',
    light: '#fee2e2',
    dark: '#991b1b',
    border: '#f87171',
    text: '#991b1b'
  }
};

/**
 * Application theme colors
 * @constant
 * @type {Object}
 */
export const THEME_COLORS = {
  primary: {
    50: '#eff6ff',
    100: '#dbeafe',
    200: '#bfdbfe',
    300: '#93c5fd',
    400: '#60a5fa',
    500: '#3b82f6',    // Main primary
    600: '#2563eb',
    700: '#1d4ed8',
    800: '#1e40af',
    900: '#1e3a8a'
  },
  secondary: {
    50: '#f5f3ff',
    100: '#ede9fe',
    200: '#ddd6fe',
    300: '#c4b5fd',
    400: '#a78bfa',
    500: '#8b5cf6',    // Main secondary
    600: '#7c3aed',
    700: '#6d28d9',
    800: '#5b21b6',
    900: '#4c1d95'
  },
  gray: {
    50: '#f9fafb',
    100: '#f3f4f6',
    200: '#e5e7eb',
    300: '#d1d5db',
    400: '#9ca3af',
    500: '#6b7280',    // Main gray
    600: '#4b5563',
    700: '#374151',
    800: '#1f2937',
    900: '#111827'
  },
  background: {
    light: '#ffffff',
    lightAlt: '#f9fafb',
    dark: '#111827',
    darkAlt: '#1f2937'
  },
  text: {
    light: {
      primary: '#111827',
      secondary: '#6b7280',
      tertiary: '#9ca3af'
    },
    dark: {
      primary: '#f9fafb',
      secondary: '#d1d5db',
      tertiary: '#9ca3af'
    }
  },
  border: {
    light: '#e5e7eb',
    dark: '#374151'
  }
};

/**
 * Chart colors for data visualization
 * @constant
 * @type {Object}
 */
export const CHART_COLORS = {
  series: [
    '#3b82f6',  // Blue
    '#10b981',  // Green
    '#f59e0b',  // Amber
    '#ef4444',  // Red
    '#8b5cf6',  // Purple
    '#ec4899',  // Pink
    '#06b6d4',  // Cyan
    '#84cc16',  // Lime
    '#f97316',  // Orange
    '#6366f1'   // Indigo
  ],
  gradient: {
    blue: ['#dbeafe', '#3b82f6', '#1e40af'],
    green: ['#d1fae5', '#10b981', '#065f46'],
    red: ['#fee2e2', '#ef4444', '#991b1b'],
    purple: ['#ede9fe', '#8b5cf6', '#5b21b6'],
    amber: ['#fef3c7', '#f59e0b', '#92400e']
  }
};

/**
 * Severity level colors (for logs, alerts, etc.)
 * @constant
 * @type {Object}
 */
export const SEVERITY_COLORS = {
  debug: {
    primary: '#6b7280',
    background: '#f3f4f6',
    text: '#374151'
  },
  info: {
    primary: '#3b82f6',
    background: '#dbeafe',
    text: '#1e40af'
  },
  notice: {
    primary: '#8b5cf6',
    background: '#ede9fe',
    text: '#5b21b6'
  },
  warning: {
    primary: '#f59e0b',
    background: '#fef3c7',
    text: '#92400e'
  },
  error: {
    primary: '#ef4444',
    background: '#fee2e2',
    text: '#991b1b'
  },
  critical: {
    primary: '#dc2626',
    background: '#fecaca',
    text: '#7f1d1d'
  },
  alert: {
    primary: '#dc2626',
    background: '#fecaca',
    text: '#7f1d1d'
  },
  emergency: {
    primary: '#991b1b',
    background: '#fee2e2',
    text: '#7f1d1d'
  }
};

/**
 * Get color by risk level
 *
 * @param {number} riskScore - Risk score (0-100)
 * @returns {Object} Color scheme object
 *
 * @example
 * const colors = getRiskColor(75);
 * console.log(colors.primary); // '#ef4444'
 */
export const getRiskColor = (riskScore) => {
  if (riskScore < 30) {
    return RISK_COLORS.low;
  } else if (riskScore < 70) {
    return RISK_COLORS.medium;
  } else if (riskScore < 90) {
    return RISK_COLORS.high;
  } else {
    return RISK_COLORS.critical;
  }
};

/**
 * Get color by status
 *
 * @param {string} status - Status key
 * @returns {Object} Color scheme object or neutral colors if not found
 *
 * @example
 * const colors = getStatusColor('success');
 * console.log(colors.primary); // '#10b981'
 */
export const getStatusColor = (status) => {
  const normalizedStatus = status?.toLowerCase();
  return STATUS_COLORS[normalizedStatus] || STATUS_COLORS.neutral;
};

/**
 * Get color by severity level
 *
 * @param {string} severity - Severity level
 * @returns {Object} Color scheme object or info colors if not found
 *
 * @example
 * const colors = getSeverityColor('error');
 * console.log(colors.background); // '#fee2e2'
 */
export const getSeverityColor = (severity) => {
  const normalizedSeverity = severity?.toLowerCase();
  return SEVERITY_COLORS[normalizedSeverity] || SEVERITY_COLORS.info;
};

/**
 * Convert hex color to RGB
 *
 * @param {string} hex - Hex color code
 * @returns {Object} RGB values
 * @returns {number} returns.r - Red value (0-255)
 * @returns {number} returns.g - Green value (0-255)
 * @returns {number} returns.b - Blue value (0-255)
 *
 * @example
 * const rgb = hexToRgb('#3b82f6');
 * console.log(rgb); // { r: 59, g: 130, b: 246 }
 */
export const hexToRgb = (hex) => {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : null;
};

/**
 * Convert RGB to hex color
 *
 * @param {number} r - Red value (0-255)
 * @param {number} g - Green value (0-255)
 * @param {number} b - Blue value (0-255)
 * @returns {string} Hex color code
 *
 * @example
 * const hex = rgbToHex(59, 130, 246);
 * console.log(hex); // '#3b82f6'
 */
export const rgbToHex = (r, g, b) => {
  return '#' + [r, g, b].map(x => {
    const hex = x.toString(16);
    return hex.length === 1 ? '0' + hex : hex;
  }).join('');
};

/**
 * Add alpha channel to hex color
 *
 * @param {string} hex - Hex color code
 * @param {number} alpha - Alpha value (0-1)
 * @returns {string} RGBA color string
 *
 * @example
 * const rgba = addAlpha('#3b82f6', 0.5);
 * console.log(rgba); // 'rgba(59, 130, 246, 0.5)'
 */
export const addAlpha = (hex, alpha) => {
  const rgb = hexToRgb(hex);
  if (!rgb) return hex;
  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha})`;
};
