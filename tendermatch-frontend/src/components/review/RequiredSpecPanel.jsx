import React from 'react';
import { FileText, Layers, CheckSquare } from 'lucide-react';
import { formatRequirementCriteria } from '../../utils/formatters';

const RequiredSpecPanel = ({ specPair, productName }) => {
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
    requiredSpecName,
    canonicalName,
    requiredValue,
    requiredUnit,
    requiredRawText,
    comparisonOperator,
  } = specPair;

  return (
    <div
      className="card"
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        borderRadius: '12px',
        border: '1px solid var(--border-color)',
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
                background: 'rgba(59, 130, 246, 0.15)',
                color: 'var(--accent-primary)',
                padding: '6px',
                borderRadius: '8px',
                display: 'flex',
              }}
            >
              <CheckSquare size={16} />
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
                Requirement Spec
              </span>
              <h3
                style={{
                  margin: 0,
                  fontSize: '1.15rem',
                  fontWeight: 700,
                  color: 'var(--text-primary)',
                }}
              >
                {requiredSpecName || 'Unnamed Requirement'}
              </h3>
            </div>
          </div>
          {canonicalName && (
            <span
              style={{
                fontSize: '0.75rem',
                padding: '3px 8px',
                borderRadius: '6px',
                backgroundColor: 'var(--tag-bg)',
                border: '1px solid var(--border-color)',
                color: 'var(--text-secondary)',
                fontFamily: 'monospace',
                fontWeight: 600,
              }}
            >
              {canonicalName}
            </span>
          )}
        </div>

        {/* Product Context */}
        {productName && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.8rem',
              color: 'var(--text-muted)',
              marginBottom: '1rem',
            }}
          >
            <Layers size={14} />
            <span>Product: <strong style={{ color: 'var(--text-primary)' }}>{productName}</strong></span>
          </div>
        )}

        {/* Required Value Display */}
        <div
          style={{
            padding: '1rem',
            borderRadius: '8px',
            backgroundColor: 'rgba(59, 130, 246, 0.08)',
            border: '1px solid rgba(59, 130, 246, 0.2)',
            marginBottom: '1.25rem',
          }}
        >
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
            Required Target Criteria
          </div>
          <div style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--accent-primary)' }}>
            {formatRequirementCriteria(comparisonOperator, requiredValue, requiredUnit)}
          </div>
        </div>

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
            <span>Tender PDF Source Evidence</span>
          </div>
          <blockquote
            style={{
              margin: 0,
              padding: '0.75rem 1rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface)',
              borderLeft: '3px solid var(--accent-primary)',
              color: 'var(--text-secondary)',
              fontSize: '0.875rem',
              lineHeight: 1.5,
              fontStyle: 'italic',
              whiteSpace: 'pre-wrap',
            }}
          >
            "{requiredRawText || 'No direct text excerpt available'}"
          </blockquote>
        </div>
      </div>
    </div>
  );
};

export default RequiredSpecPanel;
