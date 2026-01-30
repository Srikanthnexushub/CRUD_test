import React, { useMemo } from 'react';
import { useRateLimit } from '../../contexts/RateLimitContext';
import '../../styles/RateLimit.css';

const RequestRateGauge = ({ title = 'Request Rate' }) => {
    const { limits } = useRateLimit();

    const gaugeData = useMemo(() => {
        if (limits.limit === null || limits.remaining === null) {
            return { percentage: 0, used: 0, limit: 0 };
        }

        const used = limits.limit - limits.remaining;
        const percentage = (used / limits.limit) * 100;

        return { percentage, used, limit: limits.limit };
    }, [limits]);

    // Calculate rotation angle for needle (180 degrees range for semi-circle)
    const needleRotation = (gaugeData.percentage / 100) * 180 - 90;

    // Determine color based on usage
    const getColor = () => {
        if (gaugeData.percentage >= 90) return '#ef4444'; // red
        if (gaugeData.percentage >= 70) return '#f59e0b'; // yellow
        return '#10b981'; // green
    };

    const color = getColor();

    // Create SVG arc path
    const createArc = (percentage, radius, strokeWidth) => {
        const angle = (percentage / 100) * 180;
        const startAngle = -90;
        const endAngle = startAngle + angle;

        const start = polarToCartesian(50, 50, radius, endAngle);
        const end = polarToCartesian(50, 50, radius, startAngle);

        const largeArcFlag = angle <= 180 ? '0' : '1';

        return [
            'M', start.x, start.y,
            'A', radius, radius, 0, largeArcFlag, 0, end.x, end.y
        ].join(' ');
    };

    const polarToCartesian = (centerX, centerY, radius, angleInDegrees) => {
        const angleInRadians = (angleInDegrees * Math.PI) / 180.0;
        return {
            x: centerX + (radius * Math.cos(angleInRadians)),
            y: centerY + (radius * Math.sin(angleInRadians))
        };
    };

    return (
        <div className="request-rate-gauge">
            <h3 className="gauge-title">{title}</h3>
            <div className="gauge-container">
                <svg viewBox="0 0 100 60" className="gauge-svg">
                    {/* Background arc */}
                    <path
                        d={createArc(100, 35, 8)}
                        fill="none"
                        stroke="#e5e7eb"
                        strokeWidth="8"
                        strokeLinecap="round"
                    />

                    {/* Progress arc */}
                    <path
                        d={createArc(gaugeData.percentage, 35, 8)}
                        fill="none"
                        stroke={color}
                        strokeWidth="8"
                        strokeLinecap="round"
                        className="gauge-progress"
                    />

                    {/* Threshold markers */}
                    <circle cx="15" cy="50" r="2" fill="#10b981" />
                    <circle cx="50" cy="15" r="2" fill="#f59e0b" />
                    <circle cx="85" cy="50" r="2" fill="#ef4444" />

                    {/* Needle */}
                    <g transform={`rotate(${needleRotation} 50 50)`}>
                        <line
                            x1="50"
                            y1="50"
                            x2="50"
                            y2="20"
                            stroke={color}
                            strokeWidth="2"
                            strokeLinecap="round"
                        />
                        <circle cx="50" cy="50" r="3" fill={color} />
                    </g>
                </svg>

                <div className="gauge-labels">
                    <span className="gauge-label-left">0</span>
                    <span className="gauge-label-center">
                        <div className="gauge-value">{gaugeData.used}</div>
                        <div className="gauge-subtitle">of {gaugeData.limit}</div>
                    </span>
                    <span className="gauge-label-right">{gaugeData.limit}</span>
                </div>
            </div>

            <div className="gauge-legend">
                <div className="gauge-legend-item">
                    <span className="gauge-legend-dot success"></span>
                    <span>Normal (0-70%)</span>
                </div>
                <div className="gauge-legend-item">
                    <span className="gauge-legend-dot warning"></span>
                    <span>Warning (70-90%)</span>
                </div>
                <div className="gauge-legend-item">
                    <span className="gauge-legend-dot danger"></span>
                    <span>Critical (90-100%)</span>
                </div>
            </div>
        </div>
    );
};

export default RequestRateGauge;
