import React from 'react';

const ScoreGauge = ({ score = 0, size = 160, strokeWidth = 12, label = 'Match Score' }) => {
  const normalizedScore = Math.min(100, Math.max(0, score));
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (normalizedScore / 100) * circumference;

  const getColor = (s) => {
    if (s >= 80) return '#10b981';
    if (s >= 50) return '#f59e0b';
    return '#ef4444';
  };

  const strokeColor = getColor(normalizedScore);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ position: 'relative', width: size, height: size }}>
        <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
          {/* Background circle track */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke="#e2e8f0"
            strokeWidth={strokeWidth}
            fill="transparent"
          />
          {/* Value circle progress */}
          <circle
            data-testid="gauge-progress"
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={strokeColor}
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            strokeLinecap="round"
            fill="transparent"
            style={{ transition: 'stroke-dashoffset 0.8s ease, stroke 0.3s ease' }}
          />
        </svg>

        {/* Center label */}
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: size,
            height: size,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <span style={{ fontSize: `${size * 0.22}px`, fontWeight: 800, color: 'var(--text-primary)', lineHeight: 1 }}>
            {score}%
          </span>
          {label && (
            <span style={{ fontSize: `${size * 0.085}px`, fontWeight: 600, color: 'var(--text-muted)', marginTop: '4px' }}>
              {label}
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

export default ScoreGauge;
