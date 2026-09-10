import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { tendersApi } from '../services/api';
import VerdictBadge from '../components/matching/VerdictBadge';
import {
  ArrowLeft,
  CheckCircle2,
  Lock,
  Flame,
  Layers,
  AlertCircle,
  Building2,
  CheckSquare,
  Award,
  Sparkles,
} from 'lucide-react';

export const ReviewSummaryPage = () => {
  const { id: tenderId, vendorId } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [summary, setSummary] = useState(null);
  const [isFinalizing, setIsFinalizing] = useState(false);

  const fetchSummary = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await tendersApi.getReviewSummaryReport(tenderId, vendorId);
      if (res?.success && res?.data) {
        setSummary(res.data);
      } else {
        setError('Failed to load review summary.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error fetching review summary.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSummary();
  }, [tenderId, vendorId]);

  const handleReverifyReflection = async () => {
    setIsFinalizing(true);
    try {
      const res = await tendersApi.reverifyReflection(tenderId, vendorId);
      if (res?.success && res?.data) {
        setSummary(res.data);
      }
    } catch (err) {
      alert('AI Reflection Reverification failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setIsFinalizing(false);
    }
  };

  const handleFinalize = async () => {
    if (!window.confirm('Finalizing will lock all verdicts for this vendor proposal. Proceed?')) {
      return;
    }

    setIsFinalizing(true);
    try {
      const res = await tendersApi.finalizeReview(tenderId, vendorId);
      if (res?.success && res?.data) {
        setSummary(res.data);
      }
    } catch (err) {
      alert('Finalization failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setIsFinalizing(false);
    }
  };


  if (loading) {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '6rem 2rem', color: 'var(--text-secondary)' }}>
        <div
          style={{
            width: 40,
            height: 40,
            border: '3px solid rgba(59, 130, 246, 0.2)',
            borderTopColor: 'var(--accent-primary)',
            borderRadius: '50%',
            animation: 'spin 0.8s linear infinite',
            margin: '0 auto 1.5rem',
          }}
        />
        <p>Loading review summary...</p>
      </div>
    );
  }

  if (error || !summary) {
    return (
      <div className="main-content">
        <div className="card" style={{ textAlign: 'center', padding: '3rem', borderColor: 'var(--status-unmatch-border)' }}>
          <AlertCircle size={40} style={{ color: 'var(--status-unmatch)', margin: '0 auto 1rem' }} />
          <h2 style={{ fontSize: '1.25rem', color: 'var(--text-primary)' }}>{error || 'Summary not found'}</h2>
          <Link to={`/tenders/${tenderId}`} className="btn btn-secondary" style={{ marginTop: '1.5rem' }}>
            <ArrowLeft size={16} /> Back to Tender
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="main-content" style={{ maxWidth: '1200px' }}>
      {/* Top Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '1.5rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Link
            to={`/tenders/${tenderId}/vendors/${vendorId}/review`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <ArrowLeft size={16} /> Back to 3-Panel Review
          </Link>

          <Link
            to={`/tenders/${tenderId}/scores`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Award size={16} /> Leaderboard & Scores
          </Link>
        </div>

        <div>
          {summary.isLocked ? (
            <span
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '6px 12px',
                borderRadius: '8px',
                backgroundColor: 'rgba(16, 185, 129, 0.15)',
                color: '#10b981',
                fontWeight: 700,
                fontSize: '0.85rem',
              }}
            >
              <Lock size={15} />
              <span>Review Finalized & Locked</span>
            </span>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <button
                type="button"
                disabled={isFinalizing}
                onClick={handleReverifyReflection}
                className="btn btn-primary"
                style={{
                  background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)',
                  borderColor: '#818cf8',
                  padding: '0.55rem 1.25rem',
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  boxShadow: '0 4px 14px rgba(99, 102, 241, 0.3)',
                }}
              >
                <Sparkles size={16} />
                <span>{isFinalizing ? 'Auditing with AI...' : 'Submit Review & Re-Verify with AI'}</span>
              </button>

              <button
                type="button"
                disabled={isFinalizing}
                onClick={handleFinalize}
                className="btn btn-secondary"
                style={{
                  padding: '0.55rem 1rem',
                  fontWeight: 600,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                }}
              >
                <CheckCircle2 size={16} />
                <span>Quick Lock (Skip AI)</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Main Vendor Header Card */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '0.5rem' }}>
          <Building2 size={24} style={{ color: 'var(--accent-cyan)' }} />
          <h1 style={{ margin: 0, fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            Review Summary: {summary.vendorName}
          </h1>
        </div>
        <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Detailed audit breakdown of human validations, system verdicts, and override decisions.
        </p>

        {/* Metrics Bar */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
            gap: '1rem',
            marginTop: '1.5rem',
          }}
        >
          <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.25)', textAlign: 'center' }}>
            <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#10b981' }}>{summary.matchCount}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>Matches</div>
          </div>

          <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.25)', textAlign: 'center' }}>
            <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#ef4444' }}>{summary.unmatchCount}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>Unmatches</div>
          </div>

          <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.25)', textAlign: 'center' }}>
            <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#f59e0b' }}>{summary.aiErrCount}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>AI Err</div>
          </div>

          <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: 'rgba(56, 189, 248, 0.1)', border: '1px solid rgba(56, 189, 248, 0.25)', textAlign: 'center' }}>
            <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#38bdf8' }}>{summary.overrideCount}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>Overrides Applied</div>
          </div>
        </div>
      </div>

      {/* Product Specification Breakdown */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {summary.productSummaries.map((prod, idx) => (
          <div key={idx} className="card" style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Layers size={18} style={{ color: 'var(--accent-primary)' }} />
                <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {prod.productName}
                </h3>
              </div>

              <div style={{ display: 'flex', gap: '10px', fontSize: '0.8rem' }}>
                <span style={{ color: '#10b981', fontWeight: 600 }}>{prod.matchCount} Match</span>
                <span style={{ color: 'var(--text-muted)' }}>•</span>
                <span style={{ color: '#ef4444', fontWeight: 600 }}>{prod.unmatchCount} Unmatch</span>
                {prod.overrideCount > 0 && (
                  <>
                    <span style={{ color: 'var(--text-muted)' }}>•</span>
                    <span style={{ color: '#f59e0b', fontWeight: 700 }}>{prod.overrideCount} Overridden</span>
                  </>
                )}
              </div>
            </div>

            {/* Table of Specifications */}
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border-color)', backgroundColor: 'var(--table-header-bg)', color: 'var(--text-secondary)', fontWeight: 700 }}>
                    <th style={{ padding: '8px 12px' }}>Specification</th>
                    <th style={{ padding: '8px 12px' }}>System Verdict</th>
                    <th style={{ padding: '8px 12px' }}>Final Verdict</th>
                    <th style={{ padding: '8px 12px' }}>Status</th>
                    <th style={{ padding: '8px 12px' }}>Notes / Explanation</th>
                  </tr>
                </thead>
                <tbody>
                  {prod.specs.map((s, sIdx) => (
                    <tr
                      key={sIdx}
                      style={{
                        borderBottom: '1px solid var(--border-color)',
                        backgroundColor: s.isOverride ? 'var(--status-pending-bg)' : 'transparent',
                      }}
                    >
                      <td style={{ padding: '10px 12px', fontWeight: 600, color: 'var(--text-primary)' }}>
                        {s.specName}
                      </td>
                      <td style={{ padding: '10px 12px' }}>
                        <VerdictBadge verdict={s.systemVerdict} />
                      </td>
                      <td style={{ padding: '10px 12px' }}>
                        <VerdictBadge verdict={s.humanVerdict || s.systemVerdict} />
                      </td>
                      <td style={{ padding: '10px 12px' }}>
                        {s.isOverride ? (
                          <span
                            style={{
                              fontSize: '0.725rem',
                              padding: '2px 6px',
                              borderRadius: '4px',
                              backgroundColor: 'rgba(245, 158, 11, 0.15)',
                              color: '#f59e0b',
                              fontWeight: 700,
                            }}
                          >
                            Overridden
                          </span>
                        ) : (
                          <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
                            Confirmed
                          </span>
                        )}
                      </td>
                      <td style={{ padding: '10px 12px', color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                        {s.comment || '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ReviewSummaryPage;
