import React from 'react';
import MatchProgressBar from './MatchProgressBar';
import { Award, ArrowRight } from 'lucide-react';

const MatchSummaryCard = ({ summary, onClick, isMatching = false }) => {
  if (!summary) return null;

  const {
    vendorName,
    fileName,
    matchCount = 0,
    unmatchCount = 0,
    aiErrCount = 0,
    totalSpecs = 0,
    matchPercentage = 0.0,
  } = summary;

  return (
    <div
      style={{
        background: 'var(--card-bg)',
        border: '1px solid var(--border-color)',
        borderRadius: '14px',
        padding: '1.25rem',
        boxShadow: 'var(--shadow-sm)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        transition: 'transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease',
      }}
    >
      <div>
        {/* Header */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            marginBottom: '0.75rem',
          }}
        >
          <div>
            <h4
              style={{
                margin: 0,
                fontSize: '1.05rem',
                fontWeight: 700,
                color: 'var(--text-primary)',
              }}
            >
              {vendorName || 'Vendor Proposal'}
            </h4>
            <span
              style={{
                fontSize: '0.75rem',
                color: 'var(--text-muted)',
                wordBreak: 'break-all',
              }}
            >
              {fileName}
            </span>
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '5px',
              padding: '4px 10px',
              borderRadius: '9999px',
              backgroundColor: 'rgba(37, 99, 235, 0.08)',
              border: '1px solid rgba(37, 99, 235, 0.2)',
              color: 'var(--accent-primary)',
              fontWeight: 800,
              fontSize: '0.875rem',
            }}
          >
            <Award size={15} />
            <span>{matchPercentage}% Match</span>
          </div>
        </div>

        {/* Counts Breakdown */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: '8px',
            marginBottom: '1rem',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              padding: '8px',
              borderRadius: '8px',
              backgroundColor: 'var(--status-match-bg)',
              border: '1px solid var(--status-match-border)',
            }}
          >
            <div style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--status-match)' }}>
              {matchCount}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
              Match
            </div>
          </div>

          <div
            style={{
              padding: '8px',
              borderRadius: '8px',
              backgroundColor: 'var(--status-unmatch-bg)',
              border: '1px solid var(--status-unmatch-border)',
            }}
          >
            <div style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--status-unmatch)' }}>
              {unmatchCount}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
              Unmatch
            </div>
          </div>

          <div
            style={{
              padding: '8px',
              borderRadius: '8px',
              backgroundColor: 'var(--status-pending-bg)',
              border: '1px solid var(--status-pending-border)',
            }}
          >
            <div style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--status-pending)' }}>
              {aiErrCount}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
              AI Err
            </div>
          </div>
        </div>

        {/* Progress Bar */}
        <div style={{ marginBottom: '1rem' }}>
          <MatchProgressBar
            matchCount={matchCount}
            unmatchCount={unmatchCount}
            aiErrCount={aiErrCount}
            total={totalSpecs}
          />
        </div>
      </div>

      {/* Action Button */}
      <button
        type="button"
        onClick={() => onClick && onClick(summary)}
        style={{
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '6px',
          padding: '8px 14px',
          borderRadius: '8px',
          border: '1px solid var(--border-color)',
          backgroundColor: 'var(--tag-bg)',
          color: 'var(--text-primary)',
          fontSize: '0.825rem',
          fontWeight: 600,
          cursor: 'pointer',
          transition: 'background-color 0.2s ease',
        }}
      >
        <span>View Match Breakdown</span>
        <ArrowRight size={14} />
      </button>
    </div>
  );
};

export default MatchSummaryCard;
