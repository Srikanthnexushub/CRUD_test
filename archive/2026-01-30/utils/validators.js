/**
 * Validation utility functions for form inputs and security features
 * @module validators
 */

/**
 * Validates a TOTP (Time-based One-Time Password) code
 *
 * @param {string} code - The TOTP code to validate
 * @returns {Object} Validation result
 * @returns {boolean} returns.isValid - Whether the code is valid
 * @returns {string} [returns.error] - Error message if invalid
 *
 * @example
 * const result = validateTOTP('123456');
 * if (!result.isValid) {
 *   console.error(result.error);
 * }
 */
export const validateTOTP = (code) => {
  if (!code || typeof code !== 'string') {
    return {
      isValid: false,
      error: 'TOTP code is required'
    };
  }

  const trimmedCode = code.trim();

  // Must be exactly 6 digits
  if (trimmedCode.length !== 6) {
    return {
      isValid: false,
      error: 'TOTP code must be 6 digits'
    };
  }

  // Must contain only digits
  if (!/^\d{6}$/.test(trimmedCode)) {
    return {
      isValid: false,
      error: 'TOTP code must contain only numbers'
    };
  }

  return {
    isValid: true
  };
};

/**
 * Validates a backup recovery code
 *
 * @param {string} code - The backup code to validate
 * @returns {Object} Validation result
 * @returns {boolean} returns.isValid - Whether the code is valid
 * @returns {string} [returns.error] - Error message if invalid
 *
 * @example
 * const result = validateBackupCode('12345678');
 * if (result.isValid) {
 *   // Submit code
 * }
 */
export const validateBackupCode = (code) => {
  if (!code || typeof code !== 'string') {
    return {
      isValid: false,
      error: 'Backup code is required'
    };
  }

  const trimmedCode = code.trim();

  // Must be exactly 8 digits
  if (trimmedCode.length !== 8) {
    return {
      isValid: false,
      error: 'Backup code must be 8 digits'
    };
  }

  // Must contain only digits
  if (!/^\d{8}$/.test(trimmedCode)) {
    return {
      isValid: false,
      error: 'Backup code must contain only numbers'
    };
  }

  return {
    isValid: true
  };
};

/**
 * Validates an email address
 *
 * @param {string} email - The email address to validate
 * @returns {Object} Validation result
 * @returns {boolean} returns.isValid - Whether the email is valid
 * @returns {string} [returns.error] - Error message if invalid
 *
 * @example
 * const result = validateEmail('user@example.com');
 * if (!result.isValid) {
 *   setError(result.error);
 * }
 */
export const validateEmail = (email) => {
  if (!email || typeof email !== 'string') {
    return {
      isValid: false,
      error: 'Email is required'
    };
  }

  const trimmedEmail = email.trim();

  if (trimmedEmail.length === 0) {
    return {
      isValid: false,
      error: 'Email is required'
    };
  }

  // Basic email regex pattern
  // Supports most common email formats
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

  if (!emailRegex.test(trimmedEmail)) {
    return {
      isValid: false,
      error: 'Please enter a valid email address'
    };
  }

  // Check for common issues
  if (trimmedEmail.includes('..')) {
    return {
      isValid: false,
      error: 'Email cannot contain consecutive dots'
    };
  }

  if (trimmedEmail.startsWith('.') || trimmedEmail.endsWith('.')) {
    return {
      isValid: false,
      error: 'Email cannot start or end with a dot'
    };
  }

  // Check length constraints
  if (trimmedEmail.length > 254) {
    return {
      isValid: false,
      error: 'Email is too long (max 254 characters)'
    };
  }

  return {
    isValid: true
  };
};

/**
 * Validates an IP address (IPv4 or IPv6)
 *
 * @param {string} ip - The IP address to validate
 * @returns {Object} Validation result
 * @returns {boolean} returns.isValid - Whether the IP is valid
 * @returns {string} [returns.error] - Error message if invalid
 * @returns {('ipv4'|'ipv6')} [returns.type] - IP address type if valid
 *
 * @example
 * const result = validateIPAddress('192.168.1.1');
 * if (result.isValid) {
 *   console.log('IP type:', result.type); // 'ipv4'
 * }
 */
export const validateIPAddress = (ip) => {
  if (!ip || typeof ip !== 'string') {
    return {
      isValid: false,
      error: 'IP address is required'
    };
  }

  const trimmedIP = ip.trim();

  // IPv4 validation
  const ipv4Regex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;

  if (ipv4Regex.test(trimmedIP)) {
    return {
      isValid: true,
      type: 'ipv4'
    };
  }

  // IPv6 validation
  const ipv6Regex = /^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::(?:[0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$|^(?:[0-9a-fA-F]{1,4}:){1,7}:$|^(?:[0-9a-fA-F]{1,4}:){1}(?::[0-9a-fA-F]{1,4}){1,6}$|^(?:[0-9a-fA-F]{1,4}:){2}(?::[0-9a-fA-F]{1,4}){1,5}$|^(?:[0-9a-fA-F]{1,4}:){3}(?::[0-9a-fA-F]{1,4}){1,4}$|^(?:[0-9a-fA-F]{1,4}:){4}(?::[0-9a-fA-F]{1,4}){1,3}$|^(?:[0-9a-fA-F]{1,4}:){5}(?::[0-9a-fA-F]{1,4}){1,2}$|^(?:[0-9a-fA-F]{1,4}:){6}(?::[0-9a-fA-F]{1,4}){1}$/;

  if (ipv6Regex.test(trimmedIP)) {
    return {
      isValid: true,
      type: 'ipv6'
    };
  }

  return {
    isValid: false,
    error: 'Please enter a valid IPv4 or IPv6 address'
  };
};

/**
 * Validates a password meets security requirements
 *
 * @param {string} password - The password to validate
 * @param {Object} [options] - Validation options
 * @param {number} [options.minLength=8] - Minimum password length
 * @param {boolean} [options.requireUppercase=true] - Require uppercase letter
 * @param {boolean} [options.requireLowercase=true] - Require lowercase letter
 * @param {boolean} [options.requireNumber=true] - Require number
 * @param {boolean} [options.requireSpecial=false] - Require special character
 * @returns {Object} Validation result
 * @returns {boolean} returns.isValid - Whether the password is valid
 * @returns {string[]} [returns.errors] - Array of error messages if invalid
 *
 * @example
 * const result = validatePassword('MyP@ssw0rd', { requireSpecial: true });
 */
export const validatePassword = (password, options = {}) => {
  const {
    minLength = 8,
    requireUppercase = true,
    requireLowercase = true,
    requireNumber = true,
    requireSpecial = false
  } = options;

  const errors = [];

  if (!password || typeof password !== 'string') {
    return {
      isValid: false,
      errors: ['Password is required']
    };
  }

  if (password.length < minLength) {
    errors.push(`Password must be at least ${minLength} characters long`);
  }

  if (requireUppercase && !/[A-Z]/.test(password)) {
    errors.push('Password must contain at least one uppercase letter');
  }

  if (requireLowercase && !/[a-z]/.test(password)) {
    errors.push('Password must contain at least one lowercase letter');
  }

  if (requireNumber && !/\d/.test(password)) {
    errors.push('Password must contain at least one number');
  }

  if (requireSpecial && !/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
    errors.push('Password must contain at least one special character');
  }

  return {
    isValid: errors.length === 0,
    errors: errors.length > 0 ? errors : undefined
  };
};

/**
 * Validates a username
 *
 * @param {string} username - The username to validate
 * @param {Object} [options] - Validation options
 * @param {number} [options.minLength=3] - Minimum username length
 * @param {number} [options.maxLength=30] - Maximum username length
 * @param {boolean} [options.allowSpecialChars=false] - Allow special characters
 * @returns {Object} Validation result
 * @returns {boolean} returns.isValid - Whether the username is valid
 * @returns {string} [returns.error] - Error message if invalid
 */
export const validateUsername = (username, options = {}) => {
  const {
    minLength = 3,
    maxLength = 30,
    allowSpecialChars = false
  } = options;

  if (!username || typeof username !== 'string') {
    return {
      isValid: false,
      error: 'Username is required'
    };
  }

  const trimmedUsername = username.trim();

  if (trimmedUsername.length < minLength) {
    return {
      isValid: false,
      error: `Username must be at least ${minLength} characters long`
    };
  }

  if (trimmedUsername.length > maxLength) {
    return {
      isValid: false,
      error: `Username must be at most ${maxLength} characters long`
    };
  }

  const validPattern = allowSpecialChars
    ? /^[a-zA-Z0-9_.-]+$/
    : /^[a-zA-Z0-9_]+$/;

  if (!validPattern.test(trimmedUsername)) {
    const allowedChars = allowSpecialChars
      ? 'letters, numbers, underscores, dots, and hyphens'
      : 'letters, numbers, and underscores';
    return {
      isValid: false,
      error: `Username can only contain ${allowedChars}`
    };
  }

  return {
    isValid: true
  };
};
