import React from 'react';
import PropTypes from 'prop-types';
import './Badge.css';

/**
 * Reusable badge component for displaying status indicators
 *
 * @component
 * @param {Object} props - Component props
 * @param {('success'|'error'|'warning'|'info')} props.variant - Badge variant style
 * @param {string} props.text - Text to display in the badge
 * @param {('small'|'medium'|'large')} [props.size='medium'] - Badge size
 * @param {string} [props.className] - Additional CSS classes
 * @returns {JSX.Element} Badge component
 *
 * @example
 * <Badge variant="success" text="Active" size="medium" />
 * <Badge variant="error" text="Blocked" size="small" />
 */
const Badge = ({ variant, text, size = 'medium', className = '' }) => {
  return (
    <span
      className={`badge badge--${variant} badge--${size} ${className}`}
      role="status"
      aria-label={text}
    >
      {text}
    </span>
  );
};

Badge.propTypes = {
  variant: PropTypes.oneOf(['success', 'error', 'warning', 'info']).isRequired,
  text: PropTypes.string.isRequired,
  size: PropTypes.oneOf(['small', 'medium', 'large']),
  className: PropTypes.string
};

export default Badge;
