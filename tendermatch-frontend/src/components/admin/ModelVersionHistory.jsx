import React from 'react';
import { History, ArrowUpRight, RotateCcw, X, CheckCircle2 } from 'lucide-react';

const ModelVersionHistory = ({
  modelName,
  versions = [],
  onPromote,
  onRollback,
  onClose,
  isProcessing = false,
}) => {
  if (!versions) return null;

  return (
    <div
      className="card"
      style={{
        borderRadius: '16px',
        padding: '1.5rem',
        marginBottom: '1.5rem',
        border: '1px solid var(--border-color)',
        backgroundColor: 'rgba(15, 23, 42, 0.95)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <History size={20} style={{ color: 'var(--accent-primary)' }} />
          <div>
            <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Version History: {modelName}
            </h3>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Inspect historical checkpoints, evaluation metrics, and perform zero-downtime rollouts or rollbacks.
            </div>
          </div>
        </div>

        {onClose && (
          <button
            type="button"
            onClick={onClose}
            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
          >
            <X size={18} />
          </button>
        )}
      </div>

      <div style={{ overflowX: 'auto', borderRadius: '10px', border: '1px solid var(--border-color)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
          <thead>
            <tr style={{ backgroundColor: 'var(--table-header-bg)', color: 'var(--text-muted)', borderBottom: '1px solid var(--border-color)' }}>
              <th style={{ padding: '10px 14px', textAlign: 'left' }}>Version</th>
              <th style={{ padding: '10px 14px', textAlign: 'left' }}>Status</th>
              <th style={{ padding: '10px 14px', textAlign: 'left' }}>Primary Metric</th>
              <th style={{ padding: '10px 14px', textAlign: 'left' }}>Training Samples</th>
              <th style={{ padding: '10px 14px', textAlign: 'left' }}>Trained At</th>
              <th style={{ padding: '10px 14px', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {versions.map((v) => {
              const isPromoted = v.status?.toLowerCase() === 'promoted';
              const metricStr = v.primaryMetricValue != null ? (v.primaryMetricValue * 100).toFixed(1) + '%' : '-';

              return (
                <tr key={v.version} style={{ borderBottom: '1px solid var(--border-color)' }}>
                  <td style={{ padding: '10px 14px', fontWeight: 700, color: 'var(--text-primary)' }}>
                    v{v.version}
                  </td>
                  <td style={{ padding: '10px 14px' }}>
                    <span
                      style={{
                        padding: '2px 8px',
                        borderRadius: '6px',
                        fontSize: '0.75rem',
                        fontWeight: 700,
                        backgroundColor: isPromoted ? 'rgba(16, 185, 129, 0.15)' : 'var(--tag-bg)',
                        color: isPromoted ? '#10b981' : 'var(--text-muted)',
                        border: '1px solid var(--border-color)',
                      }}
                    >
                      {v.status?.toUpperCase()}
                    </span>
                  </td>
                  <td style={{ padding: '10px 14px', color: 'var(--text-primary)' }}>
                    {metricStr} ({v.primaryMetric || 'Metric'})
                  </td>
                  <td style={{ padding: '10px 14px', color: 'var(--text-secondary)' }}>
                    {v.trainingDataCount ?? '-'}
                  </td>
                  <td style={{ padding: '10px 14px', color: 'var(--text-muted)' }}>
                    {v.trainedAt ? new Date(v.trainedAt).toLocaleDateString() : '-'}
                  </td>
                  <td style={{ padding: '10px 14px', textAlign: 'right' }}>
                    {isPromoted ? (
                      <button
                        type="button"
                        disabled={isProcessing}
                        onClick={() => onRollback && onRollback(modelName, v.version)}
                        className="btn btn-secondary"
                        style={{ fontSize: '0.75rem', padding: '3px 8px', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                      >
                        <RotateCcw size={13} />
                        <span>Rollback to Shadow</span>
                      </button>
                    ) : (
                      <button
                        type="button"
                        disabled={isProcessing}
                        onClick={() => onPromote && onPromote(modelName, v.version)}
                        className="btn btn-primary"
                        style={{ fontSize: '0.75rem', padding: '3px 8px', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                      >
                        <ArrowUpRight size={13} />
                        <span>Promote to Production</span>
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ModelVersionHistory;
