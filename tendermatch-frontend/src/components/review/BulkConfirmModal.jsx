import React from 'react';
import { AlertTriangle, CheckCheck, X } from 'lucide-react';

const BulkConfirmModal = ({
  isOpen,
  onClose,
  onConfirm,
  unreviewedCount = 0,
  isConfirming = false,
}) => {
  if (!isOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.6)',
        backdropFilter: 'blur(4px)',
        WebkitBackdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '1rem',
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '480px',
          padding: '1.75rem',
          borderRadius: '16px',
          backgroundColor: '#ffffff',
          border: '1px solid var(--border-color)',
          boxShadow: 'var(--shadow-lg)',
          position: 'relative',
        }}
      >
        <button
          type="button"
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '1rem',
            right: '1rem',
            background: 'transparent',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer',
          }}
        >
          <X size={20} />
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '1rem' }}>
          <div
            style={{
              width: 44,
              height: 44,
              borderRadius: '12px',
              backgroundColor: 'var(--status-match-bg)',
              color: 'var(--status-match)',
              border: '1px solid var(--status-match-border)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <CheckCheck size={24} />
          </div>
          <div>
            <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              Confirm Remaining Specs?
            </h3>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Bulk Human Confirmation
            </span>
          </div>
        </div>

        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.6, marginBottom: '1.25rem' }}>
          This action will accept the AI system verdict for all{' '}
          <strong style={{ color: 'var(--text-primary)' }}>{unreviewedCount} unreviewed specifications</strong>{' '}
          in this proposal. Each verdict will be logged with an <code>is_auto_confirmed</code> audit flag.
        </p>

        <div
          style={{
            padding: '0.75rem 1rem',
            borderRadius: '8px',
            backgroundColor: 'var(--status-pending-bg)',
            border: '1px solid var(--status-pending-border)',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            marginBottom: '1.5rem',
            fontSize: '0.825rem',
            color: 'var(--status-pending)',
            fontWeight: 600,
          }}
        >
          <AlertTriangle size={18} style={{ flexShrink: 0 }} />
          <span>You can still review individual specifications afterwards before finalizing.</span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
          <button
            type="button"
            disabled={isConfirming}
            onClick={onClose}
            className="btn btn-secondary"
            style={{ padding: '0.55rem 1.15rem' }}
          >
            Cancel
          </button>

          <button
            type="button"
            disabled={isConfirming}
            onClick={onConfirm}
            className="btn btn-primary"
            style={{
              padding: '0.55rem 1.25rem',
              backgroundColor: '#10b981',
              borderColor: '#10b981',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <CheckCheck size={16} />
            <span>{isConfirming ? 'Confirming...' : `Confirm All ${unreviewedCount}`}</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default BulkConfirmModal;
