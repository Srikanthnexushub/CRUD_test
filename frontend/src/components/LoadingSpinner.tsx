import React from 'react';
import './LoadingSpinner.css';

interface LoadingSpinnerProps {
  size?: 'small' | 'medium' | 'large';
  message?: string;
  fullScreen?: boolean;
  overlay?: boolean;
}

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'medium',
  message,
  fullScreen = false,
  overlay = false,
}) => {
  const spinnerClass = `loading-spinner-container ${
    fullScreen ? 'full-screen' : ''
  } ${overlay ? 'overlay' : ''} ${size}`;

  return (
    <div className={spinnerClass}>
      <div className="loading-spinner">
        <div className="spinner"></div>
        {message && <p className="loading-message">{message}</p>}
      </div>
    </div>
  );
};

export default LoadingSpinner;
