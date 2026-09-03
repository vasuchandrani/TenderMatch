import React from 'react';
import { CheckCircle2, XCircle, AlertTriangle } from 'lucide-react';

const VerdictBadge = ({ verdict = 'Match', showIcon = true }) => {
  const norm = (verdict || 'Match').trim().toLowerCase();

  const configs = {
    match: {
      testId: 'verdict-match',
      bg: 'var(--status-match-bg)',
      color: 'var(--status-match)',
      border: 'var(--status-match-border)',
      label: 'Match',
      icon: CheckCircle2,
    },
    unmatch: {
      testId: 'verdict-unmatch',
      bg: 'var(--status-unmatch-bg)',
      color: 'var(--status-unmatch)',
      border: 'var(--status-unmatch-border)',
      label: 'Unmatch',
      icon: XCircle,
    },
    'ai err': {
      testId: 'verdict-ai-err',
      bg: 'var(--status-pending-bg)',
      color: 'var(--status-pending)',
      border: 'var(--status-pending-border)',
      label: 'AI Err',
      icon: AlertTriangle,
    },
  };

  const config = configs[norm] || configs['ai err'];
  const Icon = config.icon;

  return (
    <span
      data-testid={config.testId}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '5px',
        padding: '3px 9px',
        borderRadius: '9999px',
        fontSize: '0.75rem',
        fontWeight: 700,
        backgroundColor: config.bg,
        color: config.color,
        border: `1px solid ${config.border}`,
        letterSpacing: '0.025em',
        textTransform: 'uppercase',
      }}
    >
      {showIcon && <Icon size={13} style={{ strokeWidth: 2.5 }} />}
      {config.label}
    </span>
  );
};

export default VerdictBadge;
