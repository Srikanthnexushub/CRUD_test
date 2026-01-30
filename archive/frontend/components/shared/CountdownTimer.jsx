import React, { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import './CountdownTimer.css';

/**
 * Countdown timer component with auto-updating display
 *
 * @component
 * @param {Object} props - Component props
 * @param {(Date|number)} props.targetTime - Target time (Date object or timestamp in ms) or duration in seconds
 * @param {('timestamp'|'duration')} [props.mode='timestamp'] - Timer mode
 * @param {Function} [props.onComplete] - Callback function when countdown reaches zero
 * @param {boolean} [props.showHours=false] - Always show hours even if zero
 * @param {string} [props.className] - Additional CSS classes
 * @param {boolean} [props.warningThreshold=10] - Seconds remaining to trigger warning style
 * @returns {JSX.Element} CountdownTimer component
 *
 * @example
 * // Countdown to specific time
 * <CountdownTimer
 *   targetTime={new Date('2026-01-30T12:00:00')}
 *   mode="timestamp"
 *   onComplete={() => alert('Time is up!')}
 * />
 *
 * // Countdown duration (5 minutes)
 * <CountdownTimer
 *   targetTime={300}
 *   mode="duration"
 *   onComplete={() => console.log('Timer expired')}
 * />
 */
const CountdownTimer = ({
  targetTime,
  mode = 'timestamp',
  onComplete,
  showHours = false,
  className = '',
  warningThreshold = 10
}) => {
  const [timeRemaining, setTimeRemaining] = useState(0);
  const [isComplete, setIsComplete] = useState(false);
  const intervalRef = useRef(null);
  const onCompleteRef = useRef(onComplete);
  const startTimeRef = useRef(Date.now());

  // Update the onComplete ref when it changes
  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  /**
   * Calculate remaining time based on mode
   */
  const calculateTimeRemaining = useCallback(() => {
    if (mode === 'timestamp') {
      const target = targetTime instanceof Date ? targetTime.getTime() : targetTime;
      return Math.max(0, target - Date.now());
    } else {
      // duration mode
      const elapsed = Date.now() - startTimeRef.current;
      const durationMs = targetTime * 1000;
      return Math.max(0, durationMs - elapsed);
    }
  }, [targetTime, mode]);

  /**
   * Format time as hh:mm:ss or mm:ss
   */
  const formatTime = useCallback((milliseconds) => {
    const totalSeconds = Math.floor(milliseconds / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    const pad = (num) => String(num).padStart(2, '0');

    if (hours > 0 || showHours) {
      return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
    }
    return `${pad(minutes)}:${pad(seconds)}`;
  }, [showHours]);

  useEffect(() => {
    // Initialize start time for duration mode
    if (mode === 'duration') {
      startTimeRef.current = Date.now();
    }

    // Initial calculation
    const initialTime = calculateTimeRemaining();
    setTimeRemaining(initialTime);

    if (initialTime <= 0) {
      setIsComplete(true);
      if (onCompleteRef.current) {
        onCompleteRef.current();
      }
      return;
    }

    // Set up interval to update every second
    intervalRef.current = setInterval(() => {
      const remaining = calculateTimeRemaining();
      setTimeRemaining(remaining);

      if (remaining <= 0 && !isComplete) {
        setIsComplete(true);
        if (onCompleteRef.current) {
          onCompleteRef.current();
        }
        clearInterval(intervalRef.current);
      }
    }, 1000);

    // Cleanup interval on unmount
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [targetTime, mode, calculateTimeRemaining, isComplete]);

  const formattedTime = formatTime(timeRemaining);
  const secondsRemaining = Math.floor(timeRemaining / 1000);
  const isWarning = secondsRemaining <= warningThreshold && secondsRemaining > 0;

  return (
    <div
      className={`countdown-timer ${isComplete ? 'countdown-timer--complete' : ''} ${isWarning ? 'countdown-timer--warning' : ''} ${className}`}
      role="timer"
      aria-live="polite"
      aria-atomic="true"
      aria-label={`Time remaining: ${formattedTime}`}
    >
      <span className="countdown-timer__time">
        {formattedTime}
      </span>
    </div>
  );
};

CountdownTimer.propTypes = {
  targetTime: PropTypes.oneOfType([
    PropTypes.instanceOf(Date),
    PropTypes.number
  ]).isRequired,
  mode: PropTypes.oneOf(['timestamp', 'duration']),
  onComplete: PropTypes.func,
  showHours: PropTypes.bool,
  className: PropTypes.string,
  warningThreshold: PropTypes.number
};

export default CountdownTimer;
