import React from 'react';
import { Cpu, ShieldCheck, History, Eye, RotateCw } from 'lucide-react';

const MODEL_TITLES = {
  confidence_model: 'Confidence Meta-Model',
  operator_model: 'Operator Inference Model',
  doctype_model: 'Document Type Classifier',
  normalization_model: 'Specification Normalizer',
  ranking_model: 'Vendor LambdaRanker',
};

const ModelCard = ({
  model,
  onSelectVersions,
  onSelectShadow,
  onRetrain,
  isRetraining = false,
}) => {
  if (!model) return null;

  const title = MODEL_TITLES[model.modelName] || model.modelName;

  const getStatusStyle = (status) => {
    switch (status?.toLowerCase()) {
      case 'promoted':
        return {
          bg: 'rgba(16, 185, 129, 0.15)',
          color: '#10b981',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          label: 'PROMOTED',
        };
      case 'shadow':
        return {
          bg: 'rgba(245, 158, 11, 0.15)',
          color: '#f59e0b',
          border: '1px solid rgba(245, 158, 11, 0.3)',
          label: 'SHADOW',
        };
      case 'trained':
        return {
          bg: 'rgba(59, 130, 246, 0.15)',
          color: '#3b82f6',
          border: '1px solid rgba(59, 130, 246, 0.3)',
          label: 'TRAINED',
        };
      default:
        return {
          bg: 'var(--tag-bg)',
          color: 'var(--text-muted)',
          border: '1px solid var(--border-color)',
          label: status?.toUpperCase() || 'UNKNOWN',
        };
    }
  };

  const statusStyle = getStatusStyle(model.status);
  const metricPercent = model.primaryMetricValue != null ? (model.primaryMetricValue * 100).toFixed(1) + '%' : 'N/A';

  return (
    <div
      className="card"
      style={{
        borderRadius: '14px',
        padding: '1.5rem',
        border: '1px solid var(--border-color)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        transition: 'transform 0.15s ease, box-shadow 0.15s ease',
      }}
    >
      <div>
        {/* Header: Title and Status Badge */}
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '0.75rem', gap: '8px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Cpu size={20} style={{ color: 'var(--accent-primary)' }} />
            <div>
              <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                {title}
              </h3>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                {model.modelType || 'Machine Learning Model'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span
              style={{
                fontSize: '0.75rem',
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: '6px',
                backgroundColor: 'var(--tag-bg)',
                border: '1px solid var(--border-color)',
                color: 'var(--text-secondary)',
              }}
            >
              v{model.version}
            </span>
            <span
              style={{
                fontSize: '0.7rem',
                fontWeight: 800,
                padding: '2px 8px',
                borderRadius: '6px',
                backgroundColor: statusStyle.bg,
                color: statusStyle.color,
                border: statusStyle.border,
              }}
            >
              {statusStyle.label}
            </span>
          </div>
        </div>

        {/* Primary Metric & Training Volume */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '10px',
            margin: '1rem 0',
            padding: '10px 12px',
            borderRadius: '10px',
            backgroundColor: 'var(--bg-surface-hover)',
            border: '1px solid var(--border-color)',
          }}
        >
          <div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
              {model.primaryMetric?.replace(/_/g, ' ') || 'Metric'}
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              {metricPercent}
            </div>
          </div>

          <div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
              Training Samples
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-secondary)' }}>
              {model.trainingDataCount ?? '-'}
            </div>
          </div>
        </div>

        {model.trainedAt && (
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
            Last trained: {new Date(model.trainedAt).toLocaleDateString()}
          </div>
        )}
      </div>

      {/* Action Buttons */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', borderTop: '1px solid var(--border-color)', paddingTop: '1rem' }}>
        {onSelectVersions && (
          <button
            type="button"
            onClick={() => onSelectVersions(model.modelName)}
            className="btn btn-secondary"
            style={{ fontSize: '0.8rem', padding: '0.4rem 0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}
          >
            <History size={14} />
            <span>History</span>
          </button>
        )}

        {onSelectShadow && (
          <button
            type="button"
            onClick={() => onSelectShadow(model.modelName)}
            className="btn btn-secondary"
            style={{ fontSize: '0.8rem', padding: '0.4rem 0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}
          >
            <Eye size={14} />
            <span>Shadow Stats</span>
          </button>
        )}

        {onRetrain && (
          <button
            type="button"
            disabled={isRetraining}
            onClick={() => onRetrain(model.modelName)}
            className="btn btn-primary"
            style={{ fontSize: '0.8rem', padding: '0.4rem 0.85rem', display: 'flex', alignItems: 'center', gap: '4px', marginLeft: 'auto' }}
          >
            <RotateCw size={14} className={isRetraining ? 'animate-spin' : ''} />
            <span>{isRetraining ? 'Retraining...' : 'Retrain'}</span>
          </button>
        )}
      </div>
    </div>
  );
};

export default ModelCard;
