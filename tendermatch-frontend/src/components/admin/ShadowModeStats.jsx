import React from 'react';
import { ShieldAlert, CheckCircle2, XCircle, X } from 'lucide-react';

const ShadowModeStats = ({ stats, onClose }) => {
  if (!stats) return null;

  const percent = stats.agreementRate != null ? (stats.agreementRate * 100).toFixed(1) + '%' : '100%';
  const isHealthy = stats.agreementRate >= 0.90;

  return (
    <div
      className="card"
      style={{
        borderRadius: '16px',
        padding: '1.5rem',
        marginBottom: '1.5rem',
        border: '1px solid var(--border-color)',
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <ShieldAlert size={20} style={{ color: isHealthy ? '#10b981' : '#f59e0b' }} />
          <div>
            <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Shadow Mode Agreement Evaluation: {stats.modelName}
            </h3>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Dual execution monitoring: Rule Engine vs Machine Learning model predictions.
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

      {/* Stats Summary Bar */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: '#ffffff', border: '1px solid var(--border-color)', boxShadow: 'var(--shadow-sm)', textAlign: 'center' }}>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: isHealthy ? 'var(--status-match)' : 'var(--status-pending)' }}>
            {percent}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
            Agreement Rate
          </div>
        </div>

        <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: '#ffffff', border: '1px solid var(--border-color)', boxShadow: 'var(--shadow-sm)', textAlign: 'center' }}>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            {stats.totalPredictions}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
            Total Inferences
          </div>
        </div>

        <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: 'var(--status-match-bg)', border: '1px solid var(--status-match-border)', textAlign: 'center' }}>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--status-match)' }}>
            {stats.agreementCount}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
            Agreed
          </div>
        </div>

        <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: 'var(--status-unmatch-bg)', border: '1px solid var(--status-unmatch-border)', textAlign: 'center' }}>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--status-unmatch)' }}>
            {stats.disagreementCount}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
            Disagreed
          </div>
        </div>
      </div>

      {/* Disagreements Table */}
      {stats.disagreements && stats.disagreements.length > 0 ? (
        <div>
          <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '8px' }}>
            Recent Shadow Disagreements (Sampled for Audit)
          </div>
          <div style={{ overflowX: 'auto', borderRadius: '10px', border: '1px solid var(--border-color)', boxShadow: 'var(--shadow-sm)' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem', backgroundColor: '#ffffff' }}>
              <thead>
                <tr style={{ backgroundColor: 'var(--table-header-bg)', color: 'var(--text-secondary)', borderBottom: '1px solid var(--border-color)', fontWeight: 700 }}>
                  <th style={{ padding: '8px 12px', textAlign: 'left' }}>Input Hash</th>
                  <th style={{ padding: '8px 12px', textAlign: 'left' }}>Rule Result</th>
                  <th style={{ padding: '8px 12px', textAlign: 'left' }}>ML Result</th>
                  <th style={{ padding: '8px 12px', textAlign: 'left' }}>Sample Context</th>
                </tr>
              </thead>
              <tbody>
                {stats.disagreements.map((d, i) => (
                  <tr key={d.inputHash + i} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '8px 12px', fontFamily: 'monospace', color: 'var(--text-muted)' }}>
                      {d.inputHash}
                    </td>
                    <td style={{ padding: '8px 12px', fontWeight: 700, color: 'var(--status-pending)' }}>
                      {d.ruleResult}
                    </td>
                    <td style={{ padding: '8px 12px', fontWeight: 700, color: 'var(--accent-primary)' }}>
                      {d.mlResult}
                    </td>
                    <td style={{ padding: '8px 12px', color: 'var(--text-secondary)' }}>
                      {d.inputSample || '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', textAlign: 'center', padding: '1rem' }}>
          No shadow disagreements recorded. Rule engine and ML model are in complete agreement.
        </div>
      )}
    </div>
  );
};

export default ShadowModeStats;
