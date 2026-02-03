import React, { useState, FormEvent, ChangeEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../stores';
import { ApiError } from '../types';
import { useFocusManagement } from '../hooks/useFocusManagement';
import { useAnnouncer } from '../hooks/useAnnouncer';
import './RegistrationForm.css';

interface RegistrationFormData {
  username: string;
  email: string;
  password: string;
}

interface FormErrors {
  username?: string;
  email?: string;
  password?: string;
}

const RegistrationForm: React.FC = () => {
  const navigate = useNavigate();
  const register = useAuthStore((state) => state.register);
  const { announce } = useAnnouncer();
  const usernameRef = useFocusManagement<HTMLInputElement>({ autoFocus: true });

  const [formData, setFormData] = useState<RegistrationFormData>({
    username: '',
    email: '',
    password: '',
  });

  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState<boolean>(false);
  const [successMessage, setSuccessMessage] = useState<string>('');
  const [apiError, setApiError] = useState<ApiError | null>(null);
  const [showPassword, setShowPassword] = useState<boolean>(false);

  // Client-side validation
  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    // Username validation
    if (!formData.username) {
      newErrors.username = 'Username is required';
    } else if (formData.username.length < 3 || formData.username.length > 50) {
      newErrors.username = 'Username must be between 3 and 50 characters';
    } else if (!/^[a-zA-Z0-9_]+$/.test(formData.username)) {
      newErrors.username = 'Username must contain only letters, numbers, and underscores';
    }

    // Email validation
    if (!formData.email) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }

    // Password validation
    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters long';
    } else if (!/(?=.*[a-z])/.test(formData.password)) {
      newErrors.password = 'Password must contain at least one lowercase letter';
    } else if (!/(?=.*[A-Z])/.test(formData.password)) {
      newErrors.password = 'Password must contain at least one uppercase letter';
    } else if (!/(?=.*\d)/.test(formData.password)) {
      newErrors.password = 'Password must contain at least one digit';
    }

    setErrors(newErrors);

    // Announce validation errors
    if (Object.keys(newErrors).length > 0) {
      const errorMessages = Object.values(newErrors).join('. ');
      announce(`Form validation errors: ${errorMessages}`, 'assertive');
    }

    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e: ChangeEvent<HTMLInputElement>): void => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    // Clear error for this field when user starts typing
    if (errors[name as keyof FormErrors]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
    // Clear success message and API error when user modifies form
    setSuccessMessage('');
    setApiError(null);
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();

    // Client-side validation
    if (!validateForm()) {
      return;
    }

    setLoading(true);
    setApiError(null);
    setSuccessMessage('');

    try {
      // Call API through Zustand store
      const result = await register(formData.username, formData.email, formData.password);

      if (result.success) {
        // Success
        const message = `User ${formData.username} registered successfully! Redirecting to login...`;
        setSuccessMessage(message);
        announce(message, 'polite');

        // Clear form
        setFormData({
          username: '',
          email: '',
          password: '',
        });
        setErrors({});

        // Redirect to login after 2 seconds
        setTimeout(() => {
          navigate('/login');
        }, 2000);
      } else {
        const errorMsg = result.error || 'Registration failed';
        setApiError({ message: errorMsg });
        announce(errorMsg, 'assertive');
      }
    } catch (error: any) {
      // Error from backend
      const apiErr = error as ApiError;
      setApiError(apiErr);
      announce(apiErr.message, 'assertive');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="registration-page">
      <header className="auth-header" role="banner">
        <h1 className="brand-name">AI NEXUS HUB</h1>
      </header>
      <main id="main-content" className="registration-form-container" role="main">
        <h2>Create Account</h2>
        <form onSubmit={handleSubmit} className="registration-form" noValidate aria-busy={loading}>
          {/* Username Field */}
          <div className="form-group">
            <label htmlFor="username">
              Username <span className="required" aria-label="required">*</span>
            </label>
            <input
              ref={usernameRef}
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              className={errors.username ? 'error' : ''}
              placeholder="Enter username (3-50 chars, alphanumeric + underscore)"
              disabled={loading}
              autoComplete="username"
              aria-required="true"
              aria-invalid={!!errors.username}
              aria-describedby="username-hint username-error"
            />
            {errors.username && (
              <span id="username-error" className="error-message" role="alert">❌ {errors.username}</span>
            )}
            <span id="username-hint" className="hint">3-50 characters, letters, numbers, and underscores only</span>
          </div>

          {/* Email Field */}
          <div className="form-group">
            <label htmlFor="email">
              Email <span className="required" aria-label="required">*</span>
            </label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className={errors.email ? 'error' : ''}
              placeholder="Enter email address"
              disabled={loading}
              autoComplete="email"
              aria-required="true"
              aria-invalid={!!errors.email}
              aria-describedby="email-hint email-error"
            />
            {errors.email && (
              <span id="email-error" className="error-message" role="alert">❌ {errors.email}</span>
            )}
            <span id="email-hint" className="hint">Valid email format required</span>
          </div>

          {/* Password Field */}
          <div className="form-group">
            <label htmlFor="password">
              Password <span className="required" aria-label="required">*</span>
            </label>
            <div className="password-input-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                id="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                className={errors.password ? 'error' : ''}
                placeholder="Enter password"
                disabled={loading}
                autoComplete="new-password"
                aria-required="true"
                aria-invalid={!!errors.password}
                aria-describedby="password-hint password-error"
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword(!showPassword)}
                disabled={loading}
                aria-label={showPassword ? "Hide password" : "Show password"}
                aria-pressed={showPassword}
              >
                {showPassword ? '👁️' : '👁️‍🗨️'}
              </button>
            </div>
            {errors.password && (
              <span id="password-error" className="error-message" role="alert">❌ {errors.password}</span>
            )}
            <span id="password-hint" className="hint">Min 8 characters, including uppercase, lowercase, and digit</span>
          </div>

          {/* API Error Display */}
          {apiError && (
            <div className="api-error" role="alert" aria-live="assertive">
              <strong>❌ Error {apiError.status ? `(${apiError.status})` : ''}:</strong> {apiError.message}
              {apiError.details && apiError.details.length > 0 && (
                <ul className="error-details">
                  {apiError.details.map((detail, index) => (
                    <li key={index}>{detail}</li>
                  ))}
                </ul>
              )}
            </div>
          )}

          {/* Success Message */}
          {successMessage && (
            <div className="success-message" role="status" aria-live="polite">
              ✅ {successMessage}
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            className="submit-button"
            disabled={loading}
            aria-label="Submit registration form"
          >
            {loading ? (
              <>
                <span className="spinner" aria-hidden="true"></span>
                Registering...
              </>
            ) : (
              'Register User'
            )}
          </button>

          <div className="login-link">
            Already have an account? <Link to="/login">Login here</Link>
          </div>
        </form>
      </main>
      <footer className="auth-footer" role="contentinfo">
        <p>All rights reserved | 2026-27</p>
      </footer>
    </div>
  );
};

export default RegistrationForm;
