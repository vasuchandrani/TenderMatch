import React from 'react';
import { ChevronLeft, ChevronRight, Layers, Building2, HelpCircle } from 'lucide-react';

const SpecNavigator = ({
  currentIndex = 0,
  totalSpecs = 0,
  currentProduct = '',
  vendorName = '',
  onPrev,
  onNext,
  canPrev = false,
  canNext = false,
  onOpenShortcuts,
}) => {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '1rem',
        padding: '0.75rem 1.25rem',
        backgroundColor: 'var(--card-bg)',
        border: '1px solid var(--border-color)',
        borderRadius: '12px',
        marginBottom: '1rem',
      }}
    >
      {/* Hierarchical Context */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
        {vendorName && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem' }}>
            <Building2 size={15} style={{ color: 'var(--accent-cyan)' }} />
            <span style={{ color: 'var(--text-muted)' }}>Vendor:</span>
            <strong style={{ color: 'var(--text-primary)' }}>{vendorName}</strong>
          </div>
        )}

        {currentProduct && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem' }}>
            <Layers size={15} style={{ color: 'var(--accent-primary)' }} />
            <span style={{ color: 'var(--text-muted)' }}>Product:</span>
            <strong style={{ color: 'var(--text-primary)' }}>{currentProduct}</strong>
          </div>
        )}

        <div
          style={{
            padding: '4px 12px',
            borderRadius: '20px',
            backgroundColor: 'rgba(37, 99, 235, 0.08)',
            border: '1px solid rgba(37, 99, 235, 0.2)',
            fontSize: '0.8rem',
            fontWeight: 700,
            color: 'var(--accent-primary)',
          }}
        >
          Spec {totalSpecs > 0 ? currentIndex + 1 : 0} of {totalSpecs}
        </div>
      </div>

      {/* Nav Controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        {onOpenShortcuts && (
          <button
            type="button"
            onClick={onOpenShortcuts}
            title="View Keyboard Shortcuts (?)"
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.65rem', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.8rem' }}
          >
            <HelpCircle size={15} />
            <span>Shortcuts</span>
          </button>
        )}

        <button
          type="button"
          disabled={!canPrev}
          onClick={onPrev}
          className="btn btn-secondary"
          style={{ padding: '0.45rem 0.85rem', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.85rem' }}
        >
          <ChevronLeft size={16} />
          <span>Prev [P]</span>
        </button>

        <button
          type="button"
          disabled={!canNext}
          onClick={onNext}
          className="btn btn-secondary"
          style={{ padding: '0.45rem 0.85rem', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.85rem' }}
        >
          <span>Next [N]</span>
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  );
};

export default SpecNavigator;
