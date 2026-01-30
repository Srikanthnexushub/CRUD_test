/**
 * Formatting utility functions for dates, numbers, and other data
 * @module formatters
 */

/**
 * Format a date with various format options
 *
 * @param {Date|string|number} date - Date to format (Date object, ISO string, or timestamp)
 * @param {string} [format='default'] - Format type: 'default', 'short', 'long', 'time', 'datetime', 'relative', 'iso'
 * @param {Object} [options] - Additional formatting options
 * @param {string} [options.locale='en-US'] - Locale for formatting
 * @param {string} [options.timezone] - Timezone for formatting
 * @returns {string} Formatted date string
 *
 * @example
 * formatDate(new Date(), 'short') // '1/29/26'
 * formatDate(new Date(), 'long') // 'January 29, 2026'
 * formatDate(new Date(), 'datetime') // '1/29/2026, 3:45 PM'
 * formatDate(new Date(), 'relative') // '2 hours ago'
 * formatDate(new Date(), 'time') // '3:45 PM'
 */
export const formatDate = (date, format = 'default', options = {}) => {
  const { locale = 'en-US', timezone } = options;

  // Convert to Date object if needed
  let dateObj;
  if (date instanceof Date) {
    dateObj = date;
  } else if (typeof date === 'string' || typeof date === 'number') {
    dateObj = new Date(date);
  } else {
    return 'Invalid date';
  }

  // Check if date is valid
  if (isNaN(dateObj.getTime())) {
    return 'Invalid date';
  }

  const formatOptions = timezone ? { timeZone: timezone } : {};

  switch (format) {
    case 'short':
      return dateObj.toLocaleDateString(locale, {
        ...formatOptions,
        month: 'numeric',
        day: 'numeric',
        year: '2-digit'
      });

    case 'long':
      return dateObj.toLocaleDateString(locale, {
        ...formatOptions,
        month: 'long',
        day: 'numeric',
        year: 'numeric'
      });

    case 'time':
      return dateObj.toLocaleTimeString(locale, {
        ...formatOptions,
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });

    case 'datetime':
      return dateObj.toLocaleString(locale, {
        ...formatOptions,
        month: 'numeric',
        day: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });

    case 'relative':
      return formatRelativeTime(dateObj);

    case 'iso':
      return dateObj.toISOString();

    case 'default':
    default:
      return dateObj.toLocaleDateString(locale, {
        ...formatOptions,
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });
  }
};

/**
 * Format a date as relative time (e.g., "2 hours ago")
 *
 * @param {Date|string|number} date - Date to format
 * @returns {string} Relative time string
 *
 * @example
 * formatRelativeTime(Date.now() - 1000 * 60 * 5) // '5 minutes ago'
 */
export const formatRelativeTime = (date) => {
  const dateObj = date instanceof Date ? date : new Date(date);
  const now = new Date();
  const diffMs = now - dateObj;
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);
  const diffWeek = Math.floor(diffDay / 7);
  const diffMonth = Math.floor(diffDay / 30);
  const diffYear = Math.floor(diffDay / 365);

  if (diffSec < 10) {
    return 'just now';
  } else if (diffSec < 60) {
    return `${diffSec} seconds ago`;
  } else if (diffMin === 1) {
    return '1 minute ago';
  } else if (diffMin < 60) {
    return `${diffMin} minutes ago`;
  } else if (diffHour === 1) {
    return '1 hour ago';
  } else if (diffHour < 24) {
    return `${diffHour} hours ago`;
  } else if (diffDay === 1) {
    return 'yesterday';
  } else if (diffDay < 7) {
    return `${diffDay} days ago`;
  } else if (diffWeek === 1) {
    return '1 week ago';
  } else if (diffWeek < 4) {
    return `${diffWeek} weeks ago`;
  } else if (diffMonth === 1) {
    return '1 month ago';
  } else if (diffMonth < 12) {
    return `${diffMonth} months ago`;
  } else if (diffYear === 1) {
    return '1 year ago';
  } else {
    return `${diffYear} years ago`;
  }
};

/**
 * Format a risk score with emoji indicator
 *
 * @param {number} score - Risk score (0-100)
 * @param {Object} [options] - Formatting options
 * @param {boolean} [options.includeEmoji=true] - Include emoji indicator
 * @param {boolean} [options.includeLabel=true] - Include text label
 * @returns {string} Formatted risk score
 *
 * @example
 * formatRiskScore(15) // '🟢 Low (15)'
 * formatRiskScore(55) // '🟡 Medium (55)'
 * formatRiskScore(85) // '🔴 High (85)'
 */
export const formatRiskScore = (score, options = {}) => {
  const { includeEmoji = true, includeLabel = true } = options;

  if (typeof score !== 'number' || isNaN(score)) {
    return 'N/A';
  }

  const normalizedScore = Math.max(0, Math.min(100, score));

  let emoji = '';
  let label = '';

  if (normalizedScore < 30) {
    emoji = '🟢';
    label = 'Low';
  } else if (normalizedScore < 70) {
    emoji = '🟡';
    label = 'Medium';
  } else {
    emoji = '🔴';
    label = 'High';
  }

  const parts = [];
  if (includeEmoji) parts.push(emoji);
  if (includeLabel) parts.push(label);
  parts.push(`(${Math.round(normalizedScore)})`);

  return parts.join(' ');
};

/**
 * Format a duration in milliseconds to human-readable string
 *
 * @param {number} ms - Duration in milliseconds
 * @param {Object} [options] - Formatting options
 * @param {boolean} [options.short=false] - Use short format (1h 30m instead of 1 hour 30 minutes)
 * @param {number} [options.precision=2] - Number of units to show
 * @returns {string} Formatted duration
 *
 * @example
 * formatDuration(3661000) // '1 hour 1 minute'
 * formatDuration(3661000, { short: true }) // '1h 1m'
 * formatDuration(90000, { precision: 1 }) // '1 minute'
 */
export const formatDuration = (ms, options = {}) => {
  const { short = false, precision = 2 } = options;

  if (typeof ms !== 'number' || isNaN(ms) || ms < 0) {
    return '0s';
  }

  const units = [
    { name: 'day', short: 'd', value: 24 * 60 * 60 * 1000 },
    { name: 'hour', short: 'h', value: 60 * 60 * 1000 },
    { name: 'minute', short: 'm', value: 60 * 1000 },
    { name: 'second', short: 's', value: 1000 },
    { name: 'millisecond', short: 'ms', value: 1 }
  ];

  const parts = [];
  let remaining = ms;

  for (const unit of units) {
    const count = Math.floor(remaining / unit.value);
    if (count > 0) {
      const unitName = short
        ? unit.short
        : count === 1 ? unit.name : `${unit.name}s`;
      parts.push(`${count}${short ? '' : ' '}${unitName}`);
      remaining -= count * unit.value;
    }
    if (parts.length >= precision) break;
  }

  return parts.length > 0 ? parts.join(' ') : '0s';
};

/**
 * Format bytes to human-readable file size
 *
 * @param {number} bytes - Number of bytes
 * @param {Object} [options] - Formatting options
 * @param {number} [options.decimals=2] - Number of decimal places
 * @param {boolean} [options.binary=false] - Use binary (1024) instead of decimal (1000) units
 * @returns {string} Formatted file size
 *
 * @example
 * formatBytes(1024) // '1.02 KB'
 * formatBytes(1024, { binary: true }) // '1.00 KiB'
 * formatBytes(1234567) // '1.23 MB'
 */
export const formatBytes = (bytes, options = {}) => {
  const { decimals = 2, binary = false } = options;

  if (typeof bytes !== 'number' || isNaN(bytes)) {
    return '0 B';
  }

  if (bytes === 0) return '0 B';

  const k = binary ? 1024 : 1000;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = binary
    ? ['B', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB']
    : ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];

  const i = Math.floor(Math.log(bytes) / Math.log(k));
  const value = bytes / Math.pow(k, i);

  return `${value.toFixed(dm)} ${sizes[i]}`;
};

/**
 * Format a number with thousands separators
 *
 * @param {number} number - Number to format
 * @param {Object} [options] - Formatting options
 * @param {string} [options.locale='en-US'] - Locale for formatting
 * @param {number} [options.decimals] - Fixed number of decimal places
 * @returns {string} Formatted number
 *
 * @example
 * formatNumber(1234567) // '1,234,567'
 * formatNumber(1234.5678, { decimals: 2 }) // '1,234.57'
 */
export const formatNumber = (number, options = {}) => {
  const { locale = 'en-US', decimals } = options;

  if (typeof number !== 'number' || isNaN(number)) {
    return '0';
  }

  const formatOptions = {};
  if (decimals !== undefined) {
    formatOptions.minimumFractionDigits = decimals;
    formatOptions.maximumFractionDigits = decimals;
  }

  return number.toLocaleString(locale, formatOptions);
};

/**
 * Format a percentage
 *
 * @param {number} value - Value to format as percentage (0-1 or 0-100)
 * @param {Object} [options] - Formatting options
 * @param {boolean} [options.isDecimal=true] - Whether value is decimal (0-1) or whole number (0-100)
 * @param {number} [options.decimals=1] - Number of decimal places
 * @returns {string} Formatted percentage
 *
 * @example
 * formatPercentage(0.75) // '75.0%'
 * formatPercentage(75, { isDecimal: false }) // '75.0%'
 * formatPercentage(0.7534, { decimals: 2 }) // '75.34%'
 */
export const formatPercentage = (value, options = {}) => {
  const { isDecimal = true, decimals = 1 } = options;

  if (typeof value !== 'number' || isNaN(value)) {
    return '0%';
  }

  const percentage = isDecimal ? value * 100 : value;
  return `${percentage.toFixed(decimals)}%`;
};

/**
 * Truncate text with ellipsis
 *
 * @param {string} text - Text to truncate
 * @param {number} maxLength - Maximum length
 * @param {Object} [options] - Truncation options
 * @param {string} [options.ellipsis='...'] - Ellipsis string
 * @param {boolean} [options.breakWords=false] - Break in middle of words
 * @returns {string} Truncated text
 *
 * @example
 * truncateText('Hello World', 8) // 'Hello...'
 * truncateText('Hello World', 8, { ellipsis: '…' }) // 'Hello W…'
 */
export const truncateText = (text, maxLength, options = {}) => {
  const { ellipsis = '...', breakWords = false } = options;

  if (typeof text !== 'string') {
    return '';
  }

  if (text.length <= maxLength) {
    return text;
  }

  const truncateAt = maxLength - ellipsis.length;

  if (breakWords) {
    return text.substring(0, truncateAt) + ellipsis;
  }

  // Find last space before truncation point
  const lastSpace = text.lastIndexOf(' ', truncateAt);
  if (lastSpace > 0) {
    return text.substring(0, lastSpace) + ellipsis;
  }

  return text.substring(0, truncateAt) + ellipsis;
};
