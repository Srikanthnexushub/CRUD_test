import React, { useState, FormEvent, ChangeEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../stores';
import { useFocusManagement } from '../hooks/useFocusManagement';
import { useAnnouncer } from '../hooks/useAnnouncer';
import '../styles/LoginForm.css';

interface LoginFormData {
  username: string;
  password: string;
}

const LoginForm: React.FC = () => {
  const [formData, setFormData] = useState<LoginFormData>({
    username: '',
    password: ''
  });
  const [error, setError] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [showPassword, setShowPassword] = useState<boolean>(false);

  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  const { announce } = useAnnouncer();
  const usernameRef = useFocusManagement<HTMLInputElement>({ autoFocus: true });

  const handleChange = (e: ChangeEvent<HTMLInputElement>): void => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError('');
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    const result = await login(formData.username, formData.password);

    if (result.success) {
      announce('Login successful, redirecting to dashboard', 'polite');
      navigate('/dashboard');
    } else {
      const errorMessage = result.error || 'Login failed. Please try again.';
      setError(errorMessage);
      announce(errorMessage, 'assertive');
    }

    setIsLoading(false);
  };

  return (
    <div className="login-container">
      <header className="auth-header" role="banner">
        <h1 className="brand-name">AI NEXUS HUB</h1>
      </header>
      <main id="main-content" className="login-card" role="main">
        <h2>Login</h2>
        <form onSubmit={handleSubmit} aria-busy={isLoading} noValidate>
          {error && (
            <div className="error-message" role="alert" aria-live="assertive">
              {error}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              ref={usernameRef}
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              required
              disabled={isLoading}
              aria-required="true"
              aria-invalid={!!error}
              autoComplete="username"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <div className="password-input-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                id="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                required
                disabled={isLoading}
                aria-required="true"
                aria-invalid={!!error}
                autoComplete="current-password"
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword(!showPassword)}
                disabled={isLoading}
                aria-label={showPassword ? "Hide password" : "Show password"}
                aria-pressed={showPassword}
              >
                {showPassword ? '👁️' : '👁️‍🗨️'}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className="login-button"
            disabled={isLoading}
            aria-label="Submit login form"
          >
            {isLoading ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <div className="register-link">
          Don't have an account? <Link to="/register">Register here</Link>
        </div>
      </main>

      <footer className="auth-footer" role="contentinfo">
        <p>All rights reserved | 2026-27</p>
      </footer>
    </div>
  );
};

export default LoginForm;
