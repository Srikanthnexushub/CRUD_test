import React from 'react';
import './ErrorFallback.css';

interface ErrorFallbackProps {
  error?: Error;
  resetError?: () => void;
  title?: string;
  message?: string;
  showDetails?: boolean;
}

const ErrorFallback: React.FC<ErrorFallbackProps> = ({
  error,
  resetError,
  title = 'Something went wrong',
  message = 'An unexpected error occurred. Please try again.',
  showDetails = false,
}) => {
  return (
    <div className="error-fallback">
      <div className="error-fallback-icon">❌</div>
      <h3 className="error-fallback-title">{title}</h3>
      <p className="error-fallback-message">{message}</p>

      {showDetails && error && (
        <details className="error-fallback-details">
          <summary>Error Details</summary>
          <pre className="error-fallback-stack">
            {error.message}
            {'\n\n'}
            {error.stack}
          </pre>
        </details>
      )}

      {resetError && (
        <button onClick={resetError} className="error-fallback-button">
          Try Again
        </button>
      )}
    </div>
  );
};

export default ErrorFallback;
