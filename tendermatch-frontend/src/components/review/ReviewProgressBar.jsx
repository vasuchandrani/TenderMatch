import React from 'react';
import { CheckCheck, Flame, CheckCircle2 } from 'lucide-react';

const ReviewProgressBar = ({
  reviewedCount = 0,
  totalCount = 0,
  percentage = 0,
  overrideCount = 0,
  onBulkConfirm,
  isLocked = false,
}) => {
  const unreviewedCount = Math.max(0, totalCount - reviewedCount);

  return (
    <div
      style={{
        padding: '1rem 1.25rem',
        backgroundColor: 'var(--card-bg)',
        border: '1px solid var(--border-color)',
        borderRadius: '12px',
        marginBottom: '1.25rem',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '8px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            Review Progress:{' '}
            <span style={{ color: 'var(--accent-primary)' }}>
              {reviewedCount} of {totalCount} specs reviewed
            </span>
          </div>

          <div
            style={{
              padding: '2px 8px',
              borderRadius: '12px',
              backgroundColor: 'var(--status-info-bg)',
              color: 'var(--status-info)',
              border: '1px solid var(--status-info-border)',
              fontWeight: 800,
              fontSize: '0.8rem',
            }}
          >
            {percentage}%
          </div>

          {overrideCount > 0 && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                padding: '2px 8px',
                borderRadius: '12px',
                backgroundColor: 'var(--status-pending-bg)',
                color: 'var(--status-pending)',
                border: '1px solid var(--status-pending-border)',
                fontWeight: 700,
                fontSize: '0.8rem',
              }}
            >
              <Flame size={13} />
              <span>{overrideCount} Override{overrideCount > 1 ? 's' : ''}</span>
            </div>
          )}
        </div>

        {/* Bulk Confirm Button */}
        {!isLocked && unreviewedCount > 0 && onBulkConfirm && (
          <button
            type="button"
            onClick={onBulkConfirm}
            className="btn btn-secondary"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '0.4rem 0.85rem',
              fontSize: '0.8rem',
              borderColor: 'var(--status-match-border)',
              color: 'var(--status-match)',
              backgroundColor: 'var(--status-match-bg)',
            }}
          >
            <CheckCheck size={15} />
            <span>Confirm Remaining ({unreviewedCount}) as System</span>
          </button>
        )}
      </div>

      {/* Progress Bar */}
      <div
        style={{
          width: '100%',
          height: '8px',
          backgroundColor: '#e2e8f0',
          borderRadius: '4px',
          overflow: 'hidden',
        }}
      >
        <div
          data-testid="review-progress-fill"
          style={{
            width: `${percentage}%`,
            height: '100%',
            backgroundColor: 'var(--status-match)',
            borderRadius: '4px',
            transition: 'width 0.4s ease',
          }}
        />
      </div>
    </div>
  );
};

export default ReviewProgressBar;
