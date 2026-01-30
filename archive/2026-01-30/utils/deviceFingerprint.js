/**
 * Device fingerprinting utilities using FingerprintJS
 * @module deviceFingerprint
 */

import FingerprintJS from '@fingerprintjs/fingerprintjs';

/**
 * Storage key for cached fingerprint
 * @constant
 * @type {string}
 */
const FINGERPRINT_STORAGE_KEY = 'device_fingerprint';

/**
 * Cache duration in milliseconds (24 hours)
 * @constant
 * @type {number}
 */
const CACHE_DURATION = 24 * 60 * 60 * 1000;

/**
 * Cached fingerprint agent instance
 * @type {Promise|null}
 */
let fpAgentPromise = null;

/**
 * Get the FingerprintJS agent instance (singleton pattern)
 *
 * @returns {Promise<Object>} FingerprintJS agent
 * @private
 */
const getFpAgent = async () => {
  if (!fpAgentPromise) {
    fpAgentPromise = FingerprintJS.load();
  }
  return fpAgentPromise;
};

/**
 * Get cached fingerprint from localStorage
 *
 * @returns {Object|null} Cached fingerprint data or null if expired/missing
 * @private
 */
const getCachedFingerprint = () => {
  try {
    const cached = localStorage.getItem(FINGERPRINT_STORAGE_KEY);
    if (!cached) return null;

    const data = JSON.parse(cached);
    const now = Date.now();

    // Check if cache is still valid
    if (data.timestamp && (now - data.timestamp) < CACHE_DURATION) {
      return data;
    }

    // Cache expired, remove it
    localStorage.removeItem(FINGERPRINT_STORAGE_KEY);
    return null;
  } catch (error) {
    console.error('Error reading cached fingerprint:', error);
    return null;
  }
};

/**
 * Save fingerprint to localStorage cache
 *
 * @param {Object} fingerprintData - Fingerprint data to cache
 * @private
 */
const cacheFingerprint = (fingerprintData) => {
  try {
    const dataToCache = {
      ...fingerprintData,
      timestamp: Date.now()
    };
    localStorage.setItem(FINGERPRINT_STORAGE_KEY, JSON.stringify(dataToCache));
  } catch (error) {
    console.error('Error caching fingerprint:', error);
  }
};

/**
 * Generate a unique device fingerprint
 *
 * @param {Object} [options] - Fingerprint options
 * @param {boolean} [options.useCache=true] - Use cached fingerprint if available
 * @param {boolean} [options.includeMetadata=true] - Include additional metadata
 * @returns {Promise<Object>} Fingerprint data
 * @returns {string} returns.visitorId - Unique visitor ID
 * @returns {number} returns.confidence - Confidence score
 * @returns {Object} [returns.components] - Fingerprint components (if includeMetadata is true)
 * @returns {Object} [returns.metadata] - Additional metadata (if includeMetadata is true)
 *
 * @example
 * const fingerprint = await getDeviceFingerprint();
 * console.log(fingerprint.visitorId);
 *
 * // Force fresh fingerprint without cache
 * const freshFingerprint = await getDeviceFingerprint({ useCache: false });
 */
export const getDeviceFingerprint = async (options = {}) => {
  const { useCache = true, includeMetadata = true } = options;

  // Check cache first
  if (useCache) {
    const cached = getCachedFingerprint();
    if (cached) {
      return cached;
    }
  }

  try {
    // Get the fingerprint agent
    const fp = await getFpAgent();

    // Get the visitor identifier
    const result = await fp.get();

    // Prepare fingerprint data
    const fingerprintData = {
      visitorId: result.visitorId,
      confidence: result.confidence?.score || 0
    };

    // Include additional metadata if requested
    if (includeMetadata) {
      fingerprintData.components = result.components;
      fingerprintData.metadata = {
        userAgent: navigator.userAgent,
        platform: navigator.platform,
        language: navigator.language,
        screenResolution: `${window.screen.width}x${window.screen.height}`,
        colorDepth: window.screen.colorDepth,
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        touchSupport: 'ontouchstart' in window || navigator.maxTouchPoints > 0,
        hardwareConcurrency: navigator.hardwareConcurrency || 'unknown',
        deviceMemory: navigator.deviceMemory || 'unknown'
      };
    }

    // Cache the result
    if (useCache) {
      cacheFingerprint(fingerprintData);
    }

    return fingerprintData;
  } catch (error) {
    console.error('Error generating fingerprint:', error);
    throw new Error('Failed to generate device fingerprint');
  }
};

/**
 * Clear cached fingerprint from localStorage
 *
 * @example
 * clearFingerprintCache();
 */
export const clearFingerprintCache = () => {
  try {
    localStorage.removeItem(FINGERPRINT_STORAGE_KEY);
  } catch (error) {
    console.error('Error clearing fingerprint cache:', error);
  }
};

/**
 * Get basic device information without full fingerprinting
 *
 * @returns {Object} Basic device information
 * @returns {string} returns.userAgent - Browser user agent
 * @returns {string} returns.platform - Operating system platform
 * @returns {string} returns.language - Browser language
 * @returns {string} returns.screenResolution - Screen resolution
 * @returns {number} returns.colorDepth - Screen color depth
 * @returns {string} returns.timezone - User timezone
 * @returns {boolean} returns.touchSupport - Touch support availability
 *
 * @example
 * const deviceInfo = getBasicDeviceInfo();
 * console.log(deviceInfo.platform); // 'MacIntel'
 */
export const getBasicDeviceInfo = () => {
  return {
    userAgent: navigator.userAgent,
    platform: navigator.platform,
    language: navigator.language,
    screenResolution: `${window.screen.width}x${window.screen.height}`,
    colorDepth: window.screen.colorDepth,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    touchSupport: 'ontouchstart' in window || navigator.maxTouchPoints > 0,
    hardwareConcurrency: navigator.hardwareConcurrency || 'unknown',
    deviceMemory: navigator.deviceMemory || 'unknown',
    connectionType: navigator.connection?.effectiveType || 'unknown'
  };
};

/**
 * Check if fingerprinting is supported in current browser
 *
 * @returns {boolean} True if fingerprinting is supported
 *
 * @example
 * if (isFingerprintingSupported()) {
 *   const fp = await getDeviceFingerprint();
 * }
 */
export const isFingerprintingSupported = () => {
  try {
    // Check for localStorage support
    const testKey = '__fp_test__';
    localStorage.setItem(testKey, 'test');
    localStorage.removeItem(testKey);

    // Check for required browser APIs
    return !!(
      window.navigator &&
      window.screen &&
      window.localStorage
    );
  } catch (error) {
    return false;
  }
};

/**
 * Compare two fingerprints to check if they match
 *
 * @param {string} fingerprint1 - First fingerprint visitor ID
 * @param {string} fingerprint2 - Second fingerprint visitor ID
 * @returns {boolean} True if fingerprints match
 *
 * @example
 * const match = compareFingerprintIds(fp1.visitorId, fp2.visitorId);
 * if (match) {
 *   console.log('Same device detected');
 * }
 */
export const compareFingerprintIds = (fingerprint1, fingerprint2) => {
  if (!fingerprint1 || !fingerprint2) {
    return false;
  }
  return fingerprint1 === fingerprint2;
};

/**
 * Get a hash of the device fingerprint for storage/comparison
 * Useful when you don't want to store the full fingerprint
 *
 * @param {string} visitorId - Visitor ID from fingerprint
 * @returns {Promise<string>} Hashed fingerprint
 *
 * @example
 * const hash = await hashFingerprint(fingerprint.visitorId);
 * // Store hash in backend instead of full visitor ID
 */
export const hashFingerprint = async (visitorId) => {
  if (!visitorId) {
    throw new Error('Visitor ID is required');
  }

  // Use Web Crypto API to hash the fingerprint
  const encoder = new TextEncoder();
  const data = encoder.encode(visitorId);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

  return hashHex;
};

/**
 * Initialize fingerprinting on page load
 * Call this early in your app initialization to warm up the cache
 *
 * @param {Object} [options] - Initialization options
 * @param {boolean} [options.background=true] - Load in background without blocking
 * @returns {Promise<Object|void>} Fingerprint data if not in background mode
 *
 * @example
 * // In your main App.jsx useEffect
 * useEffect(() => {
 *   initializeFingerprinting({ background: true });
 * }, []);
 */
export const initializeFingerprinting = async (options = {}) => {
  const { background = true } = options;

  if (!isFingerprintingSupported()) {
    console.warn('Device fingerprinting is not supported in this browser');
    return null;
  }

  if (background) {
    // Non-blocking initialization
    getDeviceFingerprint().catch(error => {
      console.error('Background fingerprint initialization failed:', error);
    });
    return null;
  } else {
    // Blocking initialization
    return await getDeviceFingerprint();
  }
};

// Legacy functions for backward compatibility
// These maintain the existing API for the session info functionality

/**
 * @deprecated Use getDeviceFingerprint() instead
 * Legacy function for backward compatibility
 */
export const generateDeviceFingerprint = async () => {
  try {
    const fingerprint = await getDeviceFingerprint();
    return fingerprint.visitorId;
  } catch (error) {
    console.error('Error generating device fingerprint:', error);
    // Fallback to simple hash
    return hashCode(JSON.stringify(getBasicDeviceInfo())).toString();
  }
};

/**
 * Get session information including device fingerprint
 *
 * @returns {Promise<Object>} Session information
 * @returns {string} returns.deviceFingerprint - Device fingerprint ID
 * @returns {string} returns.screenResolution - Screen resolution
 * @returns {string} returns.timezone - User timezone
 * @returns {string} returns.language - Browser language
 * @returns {number} returns.colorDepth - Screen color depth
 * @returns {string} returns.platform - Operating system platform
 *
 * @example
 * const sessionInfo = await getSessionInfo();
 * console.log(sessionInfo.deviceFingerprint);
 */
export const getSessionInfo = async () => {
  try {
    const fingerprint = await getDeviceFingerprint();
    return {
      deviceFingerprint: fingerprint.visitorId,
      screenResolution: `${screen.width}x${screen.height}`,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      language: navigator.language,
      colorDepth: screen.colorDepth,
      platform: navigator.platform
    };
  } catch (error) {
    console.error('Error getting session info:', error);
    // Fallback to basic info
    const basicInfo = getBasicDeviceInfo();
    return {
      deviceFingerprint: hashCode(JSON.stringify(basicInfo)).toString(),
      screenResolution: basicInfo.screenResolution,
      timezone: basicInfo.timezone,
      language: basicInfo.language,
      colorDepth: basicInfo.colorDepth,
      platform: basicInfo.platform
    };
  }
};

/**
 * Simple hash function for fallback scenarios
 * @private
 */
const hashCode = (str) => {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32-bit integer
  }
  return Math.abs(hash);
};
