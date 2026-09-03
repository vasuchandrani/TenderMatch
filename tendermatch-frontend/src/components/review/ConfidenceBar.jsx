import React from 'react';
import { ShieldCheck, ShieldAlert, Cpu } from 'lucide-react';

const ConfidenceBar = ({ score = 0, method = 'ai_comparison' }) => {
  const percentage = Math.round(score * 100);

  let barColor = '#10b981'; // Green
  let label = 'High Confidence';
  let Icon = ShieldCheck;

  if (percentage < 50) {
    barColor = '#ef4444'; // Red
    label = 'Low Confidence / Review Recommended';
    Icon = ShieldAlert;
  } else if (percentage < 80) {
    barColor = '#f59e0b'; // Amber
    label = 'Medium Confidence';
    Icon = Cpu;
  }

  return (
    <div style={{ marginBottom: '1.25rem' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '6px',
          fontSize: '0.8rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: barColor, fontWeight: 600 }}>
          <Icon size={14} />
          <span>{label}</span>
        </div>
        <div style={{ fontWeight: 800, color: 'var(--text-primary)' }}>
          {percentage}%
        </div>
      </div>

      <div
        style={{
          width: '100%',
          height: '6px',
          backgroundColor: '#e2e8f0',
          borderRadius: '3px',
          overflow: 'hidden',
        }}
      >
        <div
          data-testid="confidence-bar-fill"
          style={{
            width: `${percentage}%`,
            height: '100%',
            backgroundColor: barColor,
            transition: 'width 0.4s ease',
          }}
        />
      </div>

      {method && (
        <div
          style={{
            marginTop: '4px',
            fontSize: '0.725rem',
            color: 'var(--text-muted)',
            display: 'flex',
            justifyContent: 'space-between',
          }}
        >
          <span>Method: <code style={{ color: 'var(--accent-cyan)' }}>{method}</code></span>
        </div>
      )}
    </div>
  );
};

export default ConfidenceBar;
