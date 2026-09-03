import React from 'react';
import { FileText, Building2, AlertTriangle, CheckCircle2 } from 'lucide-react';

const OfferedSpecPanel = ({ specPair, vendorName }) => {
  if (!specPair) {
    return (
      <div
        className="card"
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--text-muted)',
          minHeight: 280,
        }}
      >
        Select a specification to review
      </div>
    );
  }

  const {
    offeredSpecName,
    offeredValue,
    offeredUnit,
    offeredRawText,
    vendorSpecId,
  } = specPair;

  const isMissing = !vendorSpecId || offeredSpecName === 'Not Offered';

  return (
    <div
      className="card"
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        borderRadius: '12px',
        border: isMissing
          ? '1px solid rgba(239, 68, 68, 0.3)'
          : '1px solid var(--border-color)',
        backgroundColor: 'var(--card-bg)',
        padding: '1.25rem',
      }}
    >
      <div>
        {/* Panel Header */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid var(--border-color)',
            paddingBottom: '0.75rem',
            marginBottom: '1rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div
              style={{
                background: isMissing ? 'rgba(239, 68, 68, 0.15)' : 'rgba(6, 182, 212, 0.15)',
                color: isMissing ? '#ef4444' : 'var(--accent-cyan)',
                padding: '6px',
                borderRadius: '8px',
                display: 'flex',
              }}
            >
              <Building2 size={16} />
            </div>
            <div>
              <span
                style={{
                  fontSize: '0.7rem',
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                  color: 'var(--text-muted)',
                  fontWeight: 600,
                }}
              >
                Vendor Offering
              </span>
              <h3
                style={{
                  margin: 0,
                  fontSize: '1.15rem',
                  fontWeight: 700,
                  color: isMissing ? '#ef4444' : 'var(--text-primary)',
                }}
              >
                {isMissing ? 'Not Offered by Vendor' : offeredSpecName}
              </h3>
            </div>
          </div>
          {vendorName && (
            <span
              style={{
                fontSize: '0.75rem',
                padding: '3px 8px',
                borderRadius: '6px',
                backgroundColor: 'var(--tag-bg)',
                border: '1px solid var(--border-color)',
                color: 'var(--text-secondary)',
                fontWeight: 600,
              }}
            >
              {vendorName}
            </span>
          )}
        </div>

        {/* Value Box */}
        {isMissing ? (
          <div
            style={{
              padding: '1rem',
              borderRadius: '8px',
              backgroundColor: 'rgba(239, 68, 68, 0.08)',
              border: '1px solid rgba(239, 68, 68, 0.25)',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              marginBottom: '1.25rem',
            }}
          >
            <AlertTriangle size={22} style={{ color: '#ef4444', flexShrink: 0 }} />
            <div>
              <div style={{ fontWeight: 700, color: '#ef4444', fontSize: '0.95rem' }}>
                Specification Missing
              </div>
              <div style={{ fontSize: '0.775rem', color: 'var(--text-muted)' }}>
                The vendor proposal document did not address this required specification.
              </div>
            </div>
          </div>
        ) : (
          <div
            style={{
              padding: '1rem',
              borderRadius: '8px',
              backgroundColor: 'rgba(6, 182, 212, 0.08)',
              border: '1px solid rgba(6, 182, 212, 0.2)',
              marginBottom: '1.25rem',
            }}
          >
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
              Offered Value
            </div>
            <div style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--accent-cyan)' }}>
              {offeredValue || 'N/A'}{' '}
              <span style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                {offeredUnit || ''}
              </span>
            </div>
          </div>
        )}

        {/* Source Text / Context */}
        <div>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.75rem',
              color: 'var(--text-muted)',
              marginBottom: '6px',
              fontWeight: 600,
              textTransform: 'uppercase',
            }}
          >
            <FileText size={13} />
            <span>Vendor Proposal Source Excerpt</span>
          </div>
          <blockquote
            style={{
              margin: 0,
              padding: '0.75rem 1rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface)',
              borderLeft: isMissing ? '3px solid #ef4444' : '3px solid var(--accent-cyan)',
              color: 'var(--text-secondary)',
              fontSize: '0.875rem',
              lineHeight: 1.5,
              fontStyle: 'italic',
              whiteSpace: 'pre-wrap',
            }}
          >
            "{offeredRawText || (isMissing ? 'No matching statement in vendor PDF' : 'No text excerpt')}"
          </blockquote>
        </div>
      </div>
    </div>
  );
};

export default OfferedSpecPanel;
