import React from 'react';
import SpecConfidenceBadge from './SpecConfidenceBadge';

const SpecTable = ({ specifications = [], productName = 'Product' }) => {
  if (!specifications || specifications.length === 0) {
    return (
      <div
        style={{
          padding: '2rem',
          textAlign: 'center',
          background: 'var(--bg-surface-hover)',
          borderRadius: '12px',
          border: '1px dashed var(--border-color)',
          color: 'var(--text-muted)',
          fontSize: '0.9rem',
        }}
      >
        No specifications extracted yet for {productName}.
      </div>
    );
  }

  return (
    <div
      style={{
        background: '#ffffff',
        borderRadius: '12px',
        border: '1px solid var(--border-color)',
        overflow: 'hidden',
        boxShadow: 'var(--shadow-sm)',
      }}
    >
      <div
        style={{
          padding: '1rem 1.25rem',
          borderBottom: '1px solid var(--border-color)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          backgroundColor: 'var(--table-header-bg)',
        }}
      >
        <h4 style={{ margin: 0, fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          {productName} — Extracted Specifications ({specifications.length})
        </h4>
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table
          style={{
            width: '100%',
            borderCollapse: 'collapse',
            fontSize: '0.875rem',
            textAlign: 'left',
          }}
        >
          <thead>
            <tr
              style={{
                borderBottom: '1px solid var(--border-color)',
                backgroundColor: 'var(--table-header-bg)',
                color: 'var(--text-secondary)',
              }}
            >
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Raw Spec Name</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Canonical Key</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Value</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Unit</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Extraction</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Normalization</th>
            </tr>
          </thead>
          <tbody>
            {specifications.map((spec, index) => (
              <tr
                key={spec.id || index}
                style={{
                  borderBottom:
                    index < specifications.length - 1
                      ? '1px solid var(--border-color)'
                      : 'none',
                  transition: 'background-color 0.15s ease',
                }}
              >
                <td style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                  {spec.raw_spec_name}
                </td>
                <td style={{ padding: '12px 16px', fontFamily: 'monospace', color: 'var(--accent-primary)', fontWeight: 600 }}>
                  {spec.canonical_spec_name || '—'}
                </td>
                <td style={{ padding: '12px 16px', fontWeight: 500, color: 'var(--text-primary)' }}>
                  {spec.spec_value}
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-muted)' }}>
                  {spec.unit || '—'}
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <SpecConfidenceBadge level={spec.extraction_confidence || 'high'} />
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <SpecConfidenceBadge level={spec.normalization_confidence || 'high'} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default SpecTable;
