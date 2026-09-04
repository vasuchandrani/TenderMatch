import React, { useState, useEffect } from 'react';
import { Save, RotateCcw, Bookmark, Sliders } from 'lucide-react';

const CATEGORY_MAP = {
  critical: { label: 'Critical (3.0)', value: 3.0 },
  high: { label: 'High (2.0)', value: 2.0 },
  medium: { label: 'Medium (1.0)', value: 1.0 },
  low: { label: 'Low (0.5)', value: 0.5 },
  informational: { label: 'Informational (0.0)', value: 0.0 },
};

const WeightEditor = ({ weights = [], onSave, onReset, onSaveTemplate, isSaving = false }) => {
  const [localWeights, setLocalWeights] = useState([]);
  const [templateName, setTemplateName] = useState('');
  const [showTemplateModal, setShowTemplateModal] = useState(false);

  useEffect(() => {
    setLocalWeights(weights.map((w) => ({ ...w })));
  }, [weights]);

  const handleCategoryChange = (index, category) => {
    const updated = [...localWeights];
    const defaultVal = CATEGORY_MAP[category]?.value ?? 1.0;
    updated[index] = {
      ...updated[index],
      weightCategory: category,
      weightValue: defaultVal,
    };
    setLocalWeights(updated);
  };

  const handleValueChange = (index, val) => {
    const num = parseFloat(val) || 0.0;
    const updated = [...localWeights];
    updated[index] = {
      ...updated[index],
      weightValue: num,
    };
    setLocalWeights(updated);
  };

  const handleSave = () => {
    if (onSave) onSave(localWeights);
  };

  const handleConfirmTemplate = () => {
    if (!templateName.trim()) return;
    if (onSaveTemplate) onSaveTemplate(templateName.trim(), localWeights);
    setShowTemplateModal(false);
    setTemplateName('');
  };

  if (!localWeights || localWeights.length === 0) {
    return (
      <div className="card" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        No specifications found to configure weights.
      </div>
    );
  }

  return (
    <div className="card" style={{ borderRadius: '12px', border: '1px solid var(--border-color)', padding: '1.5rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Sliders size={18} style={{ color: 'var(--accent-primary)' }} />
            Specification Importance Weights
          </h3>
          <p style={{ margin: '4px 0 0', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Configure the relative weight for each specification to calculate weighted vendor rankings.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          {onReset && (
            <button
              type="button"
              onClick={onReset}
              className="btn btn-secondary"
              style={{ fontSize: '0.8rem', padding: '0.45rem 0.85rem' }}
            >
              <RotateCcw size={14} />
              <span>Reset</span>
            </button>
          )}

          {onSaveTemplate && (
            <button
              type="button"
              onClick={() => setShowTemplateModal(true)}
              className="btn btn-secondary"
              style={{ fontSize: '0.8rem', padding: '0.45rem 0.85rem' }}
            >
              <Bookmark size={14} />
              <span>Save Template</span>
            </button>
          )}

          <button
            type="button"
            disabled={isSaving}
            onClick={handleSave}
            className="btn btn-primary"
            style={{ fontSize: '0.8rem', padding: '0.45rem 1rem' }}
          >
            <Save size={14} />
            <span>{isSaving ? 'Saving...' : 'Save Weights'}</span>
          </button>
        </div>
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-color)', backgroundColor: 'var(--table-header-bg)', color: 'var(--text-secondary)', fontSize: '0.75rem', textTransform: 'uppercase', fontWeight: 700 }}>
              <th style={{ textAlign: 'left', padding: '10px 14px' }}>Specification Name</th>
              <th style={{ textAlign: 'left', padding: '10px 14px', width: '220px' }}>Importance Category</th>
              <th style={{ textAlign: 'left', padding: '10px 14px', width: '120px' }}>Weight Multiplier</th>
            </tr>
          </thead>
          <tbody>
            {localWeights.map((w, idx) => (
              <tr key={w.canonicalSpecName} style={{ borderBottom: '1px solid var(--border-color)' }}>
                <td style={{ padding: '12px 14px', fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.85rem' }}>
                  {w.canonicalSpecName}
                </td>

                <td style={{ padding: '10px 14px' }}>
                  <select
                    value={w.weightCategory || 'medium'}
                    onChange={(e) => handleCategoryChange(idx, e.target.value)}
                    className="input"
                    style={{ padding: '6px 10px', fontSize: '0.85rem', width: '100%' }}
                  >
                    <option value="critical">Critical (3.0x)</option>
                    <option value="high">High (2.0x)</option>
                    <option value="medium">Medium (1.0x)</option>
                    <option value="low">Low (0.5x)</option>
                    <option value="informational">Informational (0.0x)</option>
                  </select>
                </td>

                <td style={{ padding: '10px 14px' }}>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="10"
                    value={w.weightValue}
                    onChange={(e) => handleValueChange(idx, e.target.value)}
                    className="input"
                    style={{ padding: '6px 10px', fontSize: '0.85rem', width: '100%' }}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Save Template Modal */}
      {showTemplateModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.75)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem',
          }}
        >
          <div className="card" style={{ maxWidth: 420, width: '100%', padding: '1.5rem', borderRadius: '12px' }}>
            <h4 style={{ margin: '0 0 10px', color: 'var(--text-primary)' }}>Save Weight Template</h4>
            <p style={{ margin: '0 0 14px', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Save these specification weights as a reusable template for future tender evaluations.
            </p>
            <input
              type="text"
              placeholder="e.g. Government Laptop Standard"
              value={templateName}
              onChange={(e) => setTemplateName(e.target.value)}
              className="input"
              style={{ width: '100%', marginBottom: '1rem' }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                onClick={() => setShowTemplateModal(false)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={!templateName.trim()}
                onClick={handleConfirmTemplate}
                className="btn btn-primary"
              >
                Save Template
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default WeightEditor;
