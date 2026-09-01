import React from 'react';

const SpecConfidenceBadge = ({ level = 'high', label }) => {
  const normalizedLevel = (level || 'high').toLowerCase();

  const colors = {
    high: {
      bg: 'rgba(16, 185, 129, 0.15)',
      color: '#10b981',
      border: 'rgba(16, 185, 129, 0.3)',
      text: label || 'High Confidence',
    },
    medium: {
      bg: 'rgba(245, 158, 11, 0.15)',
      color: '#f59e0b',
      border: 'rgba(245, 158, 11, 0.3)',
      text: label || 'Review Needed',
    },
    low: {
      bg: 'rgba(239, 68, 68, 0.15)',
      color: '#ef4444',
      border: 'rgba(239, 68, 68, 0.3)',
      text: label || 'Low Confidence',
    },
  };

  const current = colors[normalizedLevel] || colors.high;

  return (
    <span
      data-testid={`confidence-${normalizedLevel}`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        padding: '3px 8px',
        borderRadius: '9999px',
        fontSize: '0.75rem',
        fontWeight: 600,
        backgroundColor: current.bg,
        color: current.color,
        border: `1px solid ${current.border}`,
        letterSpacing: '0.025em',
        textTransform: 'uppercase',
      }}
    >
      <span
        style={{
          width: '6px',
          height: '6px',
          borderRadius: '50%',
          backgroundColor: current.color,
        }}
      />
      {current.text}
    </span>
  );
};

export default SpecConfidenceBadge;
