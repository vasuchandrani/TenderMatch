import React from 'react';
import { Layers, Circle, CheckCircle, XCircle, HelpCircle } from 'lucide-react';

const SpecMiniMap = ({ products = [], currentSpecId, onSelectSpec }) => {
  const getStatusDot = (pair) => {
    if (!pair.isReviewed) {
      return (
        <span
          title="Unreviewed"
          style={{
            width: 8,
            height: 8,
            borderRadius: '50%',
            backgroundColor: '#cbd5e1',
            display: 'inline-block',
            flexShrink: 0,
          }}
        />
      );
    }

    const verdict = pair.humanVerdict || pair.systemVerdict;
    if (verdict === 'Match') {
      return (
        <span
          title="Match"
          style={{
            width: 8,
            height: 8,
            borderRadius: '50%',
            backgroundColor: '#10b981',
            display: 'inline-block',
            flexShrink: 0,
          }}
        />
      );
    }
    if (verdict === 'Unmatch') {
      return (
        <span
          title="Unmatch"
          style={{
            width: 8,
            height: 8,
            borderRadius: '50%',
            backgroundColor: '#ef4444',
            display: 'inline-block',
            flexShrink: 0,
          }}
        />
      );
    }
    return (
      <span
        title="AI Can't Predict"
        style={{
          width: 8,
          height: 8,
          borderRadius: '50%',
          backgroundColor: '#f59e0b',
          display: 'inline-block',
          flexShrink: 0,
        }}
      />
    );
  };

  return (
    <div
      className="review-sidebar-minimap"
      style={{
        width: '260px',
        backgroundColor: 'var(--card-bg)',
        border: '1px solid var(--border-color)',
        borderRadius: '12px',
        padding: '1rem',
        maxHeight: 'calc(100vh - 220px)',
        overflowY: 'auto',
        flexShrink: 0,
      }}
    >
      <div
        style={{
          fontSize: '0.8rem',
          fontWeight: 700,
          textTransform: 'uppercase',
          letterSpacing: '0.05em',
          color: 'var(--text-muted)',
          marginBottom: '0.75rem',
          paddingBottom: '0.5rem',
          borderBottom: '1px solid var(--border-color)',
        }}
      >
        Specification Mini-Map
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {products.map((prod) => (
          <div key={prod.tenderProductId}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                fontSize: '0.8rem',
                fontWeight: 700,
                color: 'var(--text-primary)',
                marginBottom: '6px',
              }}
            >
              <Layers size={13} style={{ color: 'var(--accent-primary)' }} />
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {prod.tenderProductName}
              </span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', paddingLeft: '8px' }}>
              {prod.specPairs.map((pair) => {
                const isActive = pair.resultId === currentSpecId;
                return (
                  <button
                    key={pair.resultId}
                    type="button"
                    onClick={() => onSelectSpec(pair.resultId)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '5px 8px',
                      borderRadius: '6px',
                      border: 'none',
                      backgroundColor: isActive ? 'rgba(37, 99, 235, 0.1)' : 'transparent',
                      color: isActive ? 'var(--accent-primary)' : 'var(--text-secondary)',
                      fontWeight: isActive ? 700 : 500,
                      fontSize: '0.8rem',
                      cursor: 'pointer',
                      textAlign: 'left',
                      transition: 'background-color 0.15s ease',
                      width: '100%',
                    }}
                  >
                    {getStatusDot(pair)}
                    <span
                      style={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        flex: 1,
                      }}
                    >
                      {pair.requiredSpecName}
                    </span>
                    {pair.isOverride && (
                      <span
                        title="Verdict Overridden"
                        style={{
                          fontSize: '0.65rem',
                          padding: '1px 5px',
                          borderRadius: '4px',
                          backgroundColor: 'var(--status-pending-bg)',
                          border: '1px solid var(--status-pending-border)',
                          color: 'var(--status-pending)',
                          fontWeight: 700,
                        }}
                      >
                        OVR
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SpecMiniMap;
