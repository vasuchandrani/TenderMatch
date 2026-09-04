import React from 'react';
import { Database, CheckCircle2, AlertTriangle, Flame, Trophy, Layers } from 'lucide-react';

const TrainingDataStats = ({ stats }) => {
  if (!stats) return null;

  return (
    <div
      className="card"
      style={{
        borderRadius: '16px',
        padding: '1.5rem',
        marginBottom: '1.5rem',
        border: '1px solid var(--border-color)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '1.25rem' }}>
        <Database size={20} style={{ color: 'var(--accent-primary)' }} />
        <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)' }}>
          Training Data Readiness & Feedback Loop
        </h3>
      </div>

      {/* Metrics Row */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        <div style={{ padding: '1rem', borderRadius: '12px', backgroundColor: 'var(--tag-bg)', border: '1px solid var(--border-color)', textAlign: 'center' }}>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            {stats.totalCorrectionLogs}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
            Human Corrections
          </div>
        </div>

        <div style={{ padding: '1rem', borderRadius: '12px', backgroundColor: 'rgba(56, 189, 248, 0.1)', border: '1px solid rgba(56, 189, 248, 0.25)', textAlign: 'center' }}>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#38bdf8' }}>
            {stats.totalOverrides}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }}>
            <Flame size={13} style={{ color: '#38bdf8' }} /> Overrides
          </div>
        </div>

        <div style={{ padding: '1rem', borderRadius: '12px', backgroundColor: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.25)', textAlign: 'center' }}>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#10b981' }}>
            {stats.totalAutoConfirmed}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
            Auto Confirmed
          </div>
        </div>

        <div style={{ padding: '1rem', borderRadius: '12px', backgroundColor: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.25)', textAlign: 'center' }}>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#f59e0b' }}>
            {stats.totalTenderOutcomes}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }}>
            <Trophy size={13} style={{ color: '#f59e0b' }} /> Tender Outcomes
          </div>
        </div>
      </div>

      {/* Model Readiness Pills */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '10px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', borderRadius: '10px', backgroundColor: 'var(--tag-bg)', border: '1px solid var(--border-color)' }}>
          <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            Confidence Meta-Model
          </span>
          <span
            style={{
              fontSize: '0.75rem',
              fontWeight: 700,
              padding: '2px 8px',
              borderRadius: '9999px',
              backgroundColor: stats.confidenceModelReady ? 'rgba(16, 185, 129, 0.15)' : 'rgba(245, 158, 11, 0.15)',
              color: stats.confidenceModelReady ? '#10b981' : '#f59e0b',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            {stats.confidenceModelReady ? <CheckCircle2 size={12} /> : <AlertTriangle size={12} />}
            <span>{stats.confidenceModelReady ? 'Ready for Retraining' : 'Collecting Data (need 500)'}</span>
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', borderRadius: '10px', backgroundColor: 'var(--tag-bg)', border: '1px solid var(--border-color)' }}>
          <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            Operator DecisionTree
          </span>
          <span
            style={{
              fontSize: '0.75rem',
              fontWeight: 700,
              padding: '2px 8px',
              borderRadius: '9999px',
              backgroundColor: stats.operatorModelReady ? 'rgba(16, 185, 129, 0.15)' : 'rgba(245, 158, 11, 0.15)',
              color: stats.operatorModelReady ? '#10b981' : '#f59e0b',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            {stats.operatorModelReady ? <CheckCircle2 size={12} /> : <AlertTriangle size={12} />}
            <span>{stats.operatorModelReady ? 'Ready for Retraining' : 'Collecting Data (need 500)'}</span>
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', borderRadius: '10px', backgroundColor: 'var(--tag-bg)', border: '1px solid var(--border-color)' }}>
          <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            Vendor LambdaRanker
          </span>
          <span
            style={{
              fontSize: '0.75rem',
              fontWeight: 700,
              padding: '2px 8px',
              borderRadius: '9999px',
              backgroundColor: stats.rankingModelReady ? 'rgba(16, 185, 129, 0.15)' : 'rgba(245, 158, 11, 0.15)',
              color: stats.rankingModelReady ? '#10b981' : '#f59e0b',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            {stats.rankingModelReady ? <CheckCircle2 size={12} /> : <AlertTriangle size={12} />}
            <span>{stats.rankingModelReady ? 'Ready for Retraining' : 'Collecting Data (need 200)'}</span>
          </span>
        </div>
      </div>
    </div>
  );
};

export default TrainingDataStats;
