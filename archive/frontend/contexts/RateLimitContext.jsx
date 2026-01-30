import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';

const RateLimitContext = createContext(null);

export const RateLimitProvider = ({ children }) => {
    const [limits, setLimits] = useState({
        limit: null,
        remaining: null,
        reset: null,
    });
    const [exceeded, setExceeded] = useState(false);
    const [resetTimer, setResetTimer] = useState(null);

    // Update rate limit info from headers
    const updateLimits = useCallback((headers) => {
        const limit = parseInt(headers['x-ratelimit-limit']);
        const remaining = parseInt(headers['x-ratelimit-remaining']);
        const reset = parseInt(headers['x-ratelimit-reset']);

        setLimits({
            limit: isNaN(limit) ? null : limit,
            remaining: isNaN(remaining) ? null : remaining,
            reset: isNaN(reset) ? null : reset,
        });

        setExceeded(remaining === 0);
    }, []);

    // Handle rate limit exceeded
    const handleRateLimitExceeded = useCallback((resetTime) => {
        setExceeded(true);
        setLimits(prev => ({ ...prev, remaining: 0, reset: resetTime }));
    }, []);

    // Reset exceeded state
    const resetExceeded = useCallback(() => {
        setExceeded(false);
    }, []);

    // Listen to rate limit events from axios interceptor
    useEffect(() => {
        const handleRateLimitUpdate = (event) => {
            updateLimits(event.detail);
        };

        const handleRateLimitExceeded = (event) => {
            const resetTime = parseInt(event.detail.reset);
            handleRateLimitExceeded(resetTime);
        };

        window.addEventListener('ratelimit-update', handleRateLimitUpdate);
        window.addEventListener('ratelimit-exceeded', handleRateLimitExceeded);

        return () => {
            window.removeEventListener('ratelimit-update', handleRateLimitUpdate);
            window.removeEventListener('ratelimit-exceeded', handleRateLimitExceeded);
        };
    }, [updateLimits, handleRateLimitExceeded]);

    // Auto-reset timer when limit is exceeded
    useEffect(() => {
        if (exceeded && limits.reset) {
            const now = Date.now();
            const resetTime = limits.reset * 1000; // Convert to milliseconds
            const timeUntilReset = resetTime - now;

            if (timeUntilReset > 0) {
                const timer = setTimeout(() => {
                    setExceeded(false);
                    // Reset remaining to a reasonable value
                    setLimits(prev => ({ ...prev, remaining: prev.limit }));
                }, timeUntilReset);

                setResetTimer(timer);

                return () => {
                    if (timer) clearTimeout(timer);
                };
            } else {
                // Reset time has already passed
                setExceeded(false);
            }
        }
    }, [exceeded, limits.reset, limits.limit]);

    // Calculate seconds until reset
    const getSecondsUntilReset = useCallback(() => {
        if (!limits.reset) return 0;
        const now = Math.floor(Date.now() / 1000);
        const secondsRemaining = limits.reset - now;
        return Math.max(0, secondsRemaining);
    }, [limits.reset]);

    // Calculate usage percentage
    const getUsagePercentage = useCallback(() => {
        if (limits.limit === null || limits.remaining === null) return 0;
        const used = limits.limit - limits.remaining;
        return Math.round((used / limits.limit) * 100);
    }, [limits.limit, limits.remaining]);

    const value = {
        limits,
        exceeded,
        updateLimits,
        handleRateLimitExceeded,
        resetExceeded,
        getSecondsUntilReset,
        getUsagePercentage,
    };

    return (
        <RateLimitContext.Provider value={value}>
            {children}
        </RateLimitContext.Provider>
    );
};

export const useRateLimit = () => {
    const context = useContext(RateLimitContext);
    if (!context) {
        throw new Error('useRateLimit must be used within a RateLimitProvider');
    }
    return context;
};
