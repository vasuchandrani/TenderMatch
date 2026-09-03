import React, { useState, useEffect } from 'react';
import VerdictBadge from '../matching/VerdictBadge';
import ConfidenceBar from './ConfidenceBar';
import { CheckCircle2, XCircle, HelpCircle, Save, SkipForward, Lock, MessageSquare } from 'lucide-react';

const VerdictPanel = ({
  specPair,
  onSaveVerdict,
  onSkip,
  isSaving = false,
  isLocked = false,
}) => {
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
        Select a specification to evaluate verdict
      </div>
    );
  }

  const {
    systemVerdict = 'Match',
    confidenceScore = 0.9,
    comparisonMethod = 'exact',
    comparisonDetails,
    humanVerdict,
    userComment = '',
  } = specPair;

  // Initialize selected verdict: user's prior verdict, or default to system verdict
  const getInitialVerdict = () => {
    if (humanVerdict) return humanVerdict;
    if (systemVerdict === 'AI Err') return "AI Can't Predict";
    return systemVerdict;
  };

  const [selectedVerdict, setSelectedVerdict] = useState(getInitialVerdict());
  const [comment, setComment] = useState(userComment || '');

  useEffect(() => {
    setSelectedVerdict(getInitialVerdict());
    setComment(userComment || '');
  }, [specPair.resultId]);

  const handleSave = () => {
    if (isLocked) return;
    onSaveVerdict(selectedVerdict, comment);
  };

  const isOverride =
    (systemVerdict === 'AI Err' && selectedVerdict !== "AI Can't Predict") ||
    (systemVerdict !== 'AI Err' && selectedVerdict !== systemVerdict);

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
              Evaluation & Decision
            </span>
            <h3
              style={{
                margin: 0,
                fontSize: '1.15rem',
                fontWeight: 700,
                color: 'var(--text-primary)',
              }}
            >
              Verdict Assessment
            </h3>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>System:</span>
            <VerdictBadge verdict={systemVerdict} />
          </div>
        </div>

        {/* Confidence & Method */}
        <ConfidenceBar score={confidenceScore} method={comparisonMethod} />

        {/* Comparison Details / Explanation */}
        {comparisonDetails && (
          <div
            style={{
              padding: '0.65rem 0.85rem',
              borderRadius: '8px',
              backgroundColor: 'var(--bg-surface)',
              border: '1px solid var(--border-color)',
              fontSize: '0.8rem',
              color: 'var(--text-secondary)',
              marginBottom: '1.25rem',
            }}
          >
            <strong style={{ color: 'var(--text-primary)' }}>Rule / LLM Details: </strong>
            <code>{comparisonDetails}</code>
          </div>
        )}

        {/* User Verdict Radio Selection */}
        <div style={{ marginBottom: '1.25rem' }}>
          <div
            style={{
              fontSize: '0.8rem',
              fontWeight: 700,
              color: 'var(--text-primary)',
              marginBottom: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <span>Your Human Verdict:</span>
            {isOverride && (
              <span
                style={{
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  color: 'var(--status-pending)',
                  backgroundColor: 'var(--status-pending-bg)',
                  border: '1px solid var(--status-pending-border)',
                  padding: '2px 8px',
                  borderRadius: '9999px',
                }}
              >
                ⚡ Override Applied
              </span>
            )}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px' }}>
            {/* Match Option */}
            <button
              type="button"
              disabled={isLocked}
              onClick={() => setSelectedVerdict('Match')}
              style={{
                padding: '10px 8px',
                borderRadius: '8px',
                border: selectedVerdict === 'Match' ? '2px solid var(--status-match)' : '1px solid var(--border-color)',
                backgroundColor: selectedVerdict === 'Match' ? 'var(--status-match-bg)' : '#ffffff',
                color: selectedVerdict === 'Match' ? '#10b981' : 'var(--text-secondary)',
                fontWeight: 700,
                fontSize: '0.85rem',
                cursor: isLocked ? 'not-allowed' : 'pointer',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '4px',
                transition: 'all 0.15s ease',
                boxShadow: selectedVerdict === 'Match' ? '0 2px 6px rgba(21, 128, 61, 0.12)' : '0 1px 2px rgba(0, 0, 0, 0.02)',
              }}
            >
              <CheckCircle2 size={18} />
              <span>Match</span>
              <span style={{ fontSize: '0.65rem', opacity: 0.6 }}>[1]</span>
            </button>

            {/* Unmatch Option */}
            <button
              type="button"
              disabled={isLocked}
              onClick={() => setSelectedVerdict('Unmatch')}
              style={{
                padding: '10px 8px',
                borderRadius: '8px',
                border: selectedVerdict === 'Unmatch' ? '2px solid var(--status-unmatch)' : '1px solid var(--border-color)',
                backgroundColor: selectedVerdict === 'Unmatch' ? 'var(--status-unmatch-bg)' : '#ffffff',
                color: selectedVerdict === 'Unmatch' ? 'var(--status-unmatch)' : 'var(--text-secondary)',
                fontWeight: 700,
                fontSize: '0.85rem',
                cursor: isLocked ? 'not-allowed' : 'pointer',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '4px',
                transition: 'all 0.15s ease',
                boxShadow: selectedVerdict === 'Unmatch' ? '0 2px 6px rgba(185, 28, 28, 0.12)' : '0 1px 2px rgba(0, 0, 0, 0.02)',
              }}
            >
              <XCircle size={18} />
              <span>Unmatch</span>
              <span style={{ fontSize: '0.65rem', opacity: 0.6 }}>[2]</span>
            </button>

            {/* AI Can't Predict Option */}
            <button
              type="button"
              disabled={isLocked}
              onClick={() => setSelectedVerdict("AI Can't Predict")}
              style={{
                padding: '10px 8px',
                borderRadius: '8px',
                border: selectedVerdict === "AI Can't Predict" ? '2px solid var(--status-pending)' : '1px solid var(--border-color)',
                backgroundColor: selectedVerdict === "AI Can't Predict" ? 'var(--status-pending-bg)' : '#ffffff',
                color: selectedVerdict === "AI Can't Predict" ? 'var(--status-pending)' : 'var(--text-secondary)',
                fontWeight: 700,
                fontSize: '0.85rem',
                cursor: isLocked ? 'not-allowed' : 'pointer',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '4px',
                transition: 'all 0.15s ease',
                boxShadow: selectedVerdict === "AI Can't Predict" ? '0 2px 6px rgba(180, 83, 9, 0.12)' : '0 1px 2px rgba(0, 0, 0, 0.02)',
              }}
            >
              <HelpCircle size={18} />
              <span>AI Err</span>
              <span style={{ fontSize: '0.65rem', opacity: 0.6 }}>[3]</span>
            </button>
          </div>
        </div>

        {/* Comment Field */}
        <div style={{ marginBottom: '1rem' }}>
          <label
            htmlFor="reviewer-comment"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.75rem',
              fontWeight: 600,
              color: 'var(--text-muted)',
              marginBottom: '6px',
            }}
          >
            <MessageSquare size={13} />
            <span>Reviewer Comment (Optional):</span>
          </label>
          <textarea
            id="reviewer-comment"
            disabled={isLocked}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Add explanation for your verdict or override..."
            rows={2}
            style={{
              width: '100%',
              padding: '8px 10px',
              borderRadius: '8px',
              backgroundColor: '#ffffff',
              border: '1px solid #cbd5e1',
              color: 'var(--text-primary)',
              fontSize: '0.85rem',
              resize: 'none',
              outline: 'none',
              fontFamily: 'inherit',
              boxSizing: 'border-box',
            }}
          />
        </div>
      </div>

      {/* Action Footer */}
      <div>
        {isLocked ? (
          <div
            style={{
              padding: '0.75rem',
              borderRadius: '8px',
              backgroundColor: 'var(--tag-bg)',
              border: '1px solid var(--border-color)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              color: 'var(--text-muted)',
              fontSize: '0.85rem',
              fontWeight: 600,
            }}
          >
            <Lock size={16} />
            <span>Review is finalized and locked</span>
          </div>
        ) : (
          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              type="button"
              disabled={isSaving}
              onClick={handleSave}
              className="btn btn-primary"
              style={{
                flex: 2,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                padding: '0.65rem 1rem',
                fontSize: '0.9rem',
                fontWeight: 700,
              }}
            >
              <Save size={16} />
              <span>{isSaving ? 'Saving...' : 'Save & Next'}</span>
              <span style={{ fontSize: '0.7rem', opacity: 0.7 }}>[↵]</span>
            </button>

            <button
              type="button"
              disabled={isSaving}
              onClick={onSkip}
              className="btn btn-secondary"
              style={{
                flex: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                padding: '0.65rem 0.85rem',
                fontSize: '0.85rem',
              }}
            >
              <SkipForward size={15} />
              <span>Skip</span>
              <span style={{ fontSize: '0.7rem', opacity: 0.6 }}>[S]</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default VerdictPanel;
