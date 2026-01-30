import React, { useMemo } from 'react';
import PropTypes from 'prop-types';
import './ProgressBar.css';

/**
 * Color-coded progress bar that transitions between colors based on thresholds
 *
 * @component
 * @param {Object} props - Component props
 * @param {number} props.value - Current progress value
 * @param {number} [props.max=100] - Maximum value
 * @param {Object} [props.colorScheme] - Custom color scheme configuration
 * @param {number} [props.colorScheme.low=33] - Threshold for low (green to yellow)
 * @param {number} [props.colorScheme.medium=66] - Threshold for medium (yellow to red)
 * @param {string} [props.colorScheme.lowColor='#10b981'] - Color for low values
 * @param {string} [props.colorScheme.mediumColor='#f59e0b'] - Color for medium values
 * @param {string} [props.colorScheme.highColor='#ef4444'] - Color for high values
 * @param {boolean} [props.showLabel=true] - Show percentage label
 * @param {string} [props.className] - Additional CSS classes
 * @param {('small'|'medium'|'large')} [props.size='medium'] - Progress bar size
 * @returns {JSX.Element} ProgressBar component
 *
 * @example
 * <ProgressBar value={75} max={100} />
 * <ProgressBar
 *   value={50}
 *   colorScheme={{ low: 40, medium: 70, lowColor: '#22c55e' }}
 * />
 */
const ProgressBar = ({
  value,
  max = 100,
  colorScheme = {},
  showLabel = true,
  className = '',
  size = 'medium'
}) => {
  const defaultColorScheme = {
    low: 33,
    medium: 66,
    lowColor: '#10b981', // green
    mediumColor: '#f59e0b', // yellow
    highColor: '#ef4444', // red
    ...colorScheme
  };

  /**
   * Calculate progress percentage and color
   */
  const { percentage, color } = useMemo(() => {
    const pct = Math.min(Math.max((value / max) * 100, 0), 100);

    let progressColor;
    if (pct <= defaultColorScheme.low) {
      progressColor = defaultColorScheme.lowColor;
    } else if (pct <= defaultColorScheme.medium) {
      progressColor = defaultColorScheme.mediumColor;
    } else {
      progressColor = defaultColorScheme.highColor;
    }

    return { percentage: pct, color: progressColor };
  }, [value, max, defaultColorScheme]);

  return (
    <div className={`progress-bar-container progress-bar--${size} ${className}`}>
      <div
        className="progress-bar-track"
        role="progressbar"
        aria-valuenow={value}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-label={`Progress: ${percentage.toFixed(0)}%`}
      >
        <div
          className="progress-bar-fill"
          style={{
            width: `${percentage}%`,
            backgroundColor: color,
            transition: 'width 0.3s ease, background-color 0.3s ease'
          }}
        />
      </div>
      {showLabel && (
        <span className="progress-bar-label">
          {percentage.toFixed(0)}%
        </span>
      )}
    </div>
  );
};

ProgressBar.propTypes = {
  value: PropTypes.number.isRequired,
  max: PropTypes.number,
  colorScheme: PropTypes.shape({
    low: PropTypes.number,
    medium: PropTypes.number,
    lowColor: PropTypes.string,
    mediumColor: PropTypes.string,
    highColor: PropTypes.string
  }),
  showLabel: PropTypes.bool,
  className: PropTypes.string,
  size: PropTypes.oneOf(['small', 'medium', 'large'])
};

export default ProgressBar;
