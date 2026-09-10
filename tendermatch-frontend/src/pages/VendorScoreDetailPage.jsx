import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { tendersApi } from '../services/api';
import ScoreGauge from '../components/scoring/ScoreGauge';
import { formatRequirementCriteria, formatAiReasoning } from '../utils/formatters';
import {
  ArrowLeft,
  Award,
  Layers,
  CheckCircle2,
  XCircle,
  HelpCircle,
  FileText,
  Lock,
  ExternalLink,
  AlertCircle,
  RefreshCw,
  AlertTriangle,
  Sparkles,
  Check,
} from 'lucide-react';

export const VendorScoreDetailPage = () => {
  const { id: tenderId, vendorId } = useParams();

  const [scoreData, setScoreData] = useState(null);
  const [results, setResults] = useState([]);
  const [activeFilter, setActiveFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchScore = async () => {
    try {
      setLoading(true);
      setError(null);
      const [scoreRes, resultsRes] = await Promise.allSettled([
        tendersApi.getVendorScore(tenderId, vendorId),
        tendersApi.getVendorResults(tenderId, vendorId),
      ]);

      if (scoreRes.status === 'fulfilled' && scoreRes.value?.success && scoreRes.value?.data) {
        setScoreData(scoreRes.value.data);
      } else {
        setError('Error fetching vendor score.');
      }

      if (resultsRes.status === 'fulfilled' && resultsRes.value?.success && resultsRes.value?.data) {
        setResults(resultsRes.value.data);
      } else {
        setResults([]);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error fetching vendor score.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchScore();
  }, [tenderId, vendorId]);

  if (loading) {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '6rem 2rem', color: 'var(--text-secondary)' }}>
        <RefreshCw size={36} className="animate-spin" style={{ margin: '0 auto 1.25rem', color: 'var(--accent-primary)' }} />
        <p>Loading vendor score evaluation...</p>
      </div>
    );
  }

  if (error || !scoreData) {
    return (
      <div className="main-content">
        <div className="card" style={{ textAlign: 'center', padding: '3rem', borderColor: 'var(--status-unmatch-border)' }}>
          <AlertCircle size={40} style={{ color: 'var(--status-unmatch)', margin: '0 auto 1rem' }} />
          <h2 style={{ fontSize: '1.25rem', color: 'var(--text-primary)' }}>{error || 'Score not found'}</h2>
          <Link to={`/tenders/${tenderId}/scores`} className="btn btn-secondary" style={{ marginTop: '1.5rem' }}>
            <ArrowLeft size={16} /> Back to Leaderboard
          </Link>
        </div>
      </div>
    );
  }

  const getMedal = (rank) => {
    if (rank === 1) return '🥇';
    if (rank === 2) return '🥈';
    if (rank === 3) return '🥉';
    return null;
  };

  const medal = getMedal(scoreData.rank);

  return (
    <div className="main-content" style={{ maxWidth: '1100px' }}>
      {/* Navigation Header */}
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
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Link
            to={`/tenders/${tenderId}/scores`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <ArrowLeft size={16} /> Back to Leaderboard
          </Link>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Link
            to={`/tenders/${tenderId}/vendors/${vendorId}/review-summary`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <FileText size={15} />
            <span>Audit & Review Summary</span>
          </Link>

          <Link
            to={`/tenders/${tenderId}/vendors/${vendorId}/review`}
            className="btn btn-primary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <ExternalLink size={15} />
            <span>Open 3-Panel Review</span>
          </Link>
        </div>
      </div>

      {/* Main Vendor Score Card */}
      <div
        className="card"
        style={{
          padding: '2rem',
          borderRadius: '16px',
          marginBottom: '1.5rem',
          background: 'linear-gradient(135deg, #f0fdf4 0%, #ffffff 60%, #f8fafc 100%)',
          border: '1px solid rgba(16, 185, 129, 0.25)',
          boxShadow: '0 4px 14px rgba(16, 185, 129, 0.06), 0 1px 3px rgba(0, 0, 0, 0.03)',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1.5rem',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
              <span
                style={{
                  padding: '4px 12px',
                  borderRadius: '9999px',
                  backgroundColor: 'rgba(59, 130, 246, 0.15)',
                  color: 'var(--accent-primary)',
                  fontWeight: 800,
                  fontSize: '0.85rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                }}
              >
                <span>Rank #{scoreData.rank}</span>
                {medal && <span>{medal}</span>}
              </span>

              {scoreData.isReviewLocked && (
                <span
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    padding: '4px 10px',
                    borderRadius: '9999px',
                    backgroundColor: 'rgba(16, 185, 129, 0.15)',
                    color: '#10b981',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                  }}
                >
                  <Lock size={13} /> Finalized
                </span>
              )}
            </div>

            <h1 style={{ margin: 0, fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              {scoreData.vendorName}
            </h1>
            {scoreData.fileName && (
              <p style={{ margin: '6px 0 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                Proposal Source: {scoreData.fileName}
              </p>
            )}
            <p style={{ margin: '4px 0 0', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Scoring Formula: {scoreData.scoringMethod || 'WEIGHTED_AVERAGE'} • Scored at{' '}
              {new Date(scoreData.scoredAt).toLocaleString()}
            </p>
          </div>

          <div>
            <ScoreGauge score={scoreData.overallScore} size={150} strokeWidth={12} label="Overall Score" />
          </div>
        </div>

        {/* Metrics Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
            gap: '1rem',
            marginTop: '2rem',
          }}
        >
          <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: '#ffffff', border: '1px solid var(--border-color)', boxShadow: 'var(--shadow-sm)', textAlign: 'center' }}>
            <div style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-primary)' }}>{scoreData.totalSpecs}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>Total Specs</div>
          </div>

          <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: 'var(--status-match-bg)', border: '1px solid var(--status-match-border)', textAlign: 'center' }}>
            <div style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--status-match)' }}>{scoreData.matched}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }}>
              <CheckCircle2 size={13} style={{ color: 'var(--status-match)' }} /> Matches
            </div>
          </div>

          <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: 'var(--status-unmatch-bg)', border: '1px solid var(--status-unmatch-border)', textAlign: 'center' }}>
            <div style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--status-unmatch)' }}>{scoreData.unmatched}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }}>
              <XCircle size={13} style={{ color: 'var(--status-unmatch)' }} /> Unmatches
            </div>
          </div>

          <div style={{ padding: '1rem', borderRadius: '10px', backgroundColor: 'var(--status-pending-bg)', border: '1px solid var(--status-pending-border)', textAlign: 'center' }}>
            <div style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--status-pending)' }}>{scoreData.uncertain}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }}>
              <HelpCircle size={13} style={{ color: 'var(--status-pending)' }} /> Uncertain
            </div>
          </div>
        </div>
      </div>

      {/* Product Level Scores */}
      {scoreData.productScores && scoreData.productScores.length > 0 && (
        <div className="card" style={{ padding: '1.5rem', borderRadius: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '1rem' }}>
            <Layers size={18} style={{ color: 'var(--accent-primary)' }} />
            <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Product Breakdown Scores
            </h3>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
            {scoreData.productScores.map((ps) => (
              <div
                key={ps.tenderProductId || ps.productName}
                style={{
                  padding: '1.25rem',
                  borderRadius: '12px',
                  backgroundColor: '#ffffff',
                  border: '1px solid var(--border-color)',
                  boxShadow: 'var(--shadow-sm)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-primary)', marginBottom: '4px' }}>
                    {ps.productName}
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    {ps.matched} Matched • {ps.unmatched} Unmatched • {ps.uncertain} Uncertain
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                    Total Specs: {ps.totalSpecs}
                  </div>
                </div>

                <div style={{ textAlign: 'center' }}>
                  <span
                    style={{
                      fontSize: '1.35rem',
                      fontWeight: 800,
                      color: ps.score >= 80 ? '#10b981' : ps.score >= 50 ? '#f59e0b' : '#ef4444',
                    }}
                  >
                    {ps.score}%
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Feature Compliance Breakdown Section (Met vs. Unmet Specifically) */}
      {results.length > 0 && (() => {
        const getNormalizedVerdict = (r) => {
          const raw = (r.humanVerdict || r.systemVerdict || r.llmVerdict || '').trim();
          const upper = raw.toUpperCase();
          if (upper === 'MATCH' || upper === 'MATCHED') return 'MATCH';
          if (upper === 'UNMATCH' || upper === 'NO_MATCH' || upper === 'UNMATCHED') return 'NO_MATCH';
          return 'PARTIAL';
        };

        const getSpecTitle = (spec) => {
          if (!spec) return 'Specification';
          const canonical = spec.canonicalSpecName || spec.canonical_spec_name;
          if (canonical) {
            return canonical
              .split('_')
              .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
              .join(' ');
          }
          return spec.rawSpecName || spec.raw_spec_name || 'Specification';
        };

        const getSpecValue = (spec) => {
          if (!spec) return '';
          return spec.specValue ?? spec.spec_value ?? '';
        };

        const getSpecUnit = (spec) => {
          if (!spec) return '';
          return spec.unit ?? '';
        };

        const metList = results.filter((r) => getNormalizedVerdict(r) === 'MATCH');
        const unmetList = results.filter((r) => getNormalizedVerdict(r) === 'NO_MATCH');
        const partialList = results.filter((r) => getNormalizedVerdict(r) === 'PARTIAL');

        const displayedResults =
          activeFilter === 'MET'
            ? metList
            : activeFilter === 'UNMET'
            ? unmetList
            : activeFilter === 'PARTIAL'
            ? partialList
            : results;

        return (
          <div className="card" style={{ padding: '1.5rem', borderRadius: '16px', marginTop: '1.5rem' }}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '12px',
                marginBottom: '1.25rem',
              }}
            >
              <div>
                <h3
                  style={{
                    margin: 0,
                    fontSize: '1.2rem',
                    fontWeight: 700,
                    color: 'var(--text-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                  }}
                >
                  <Sparkles size={18} style={{ color: 'var(--accent-primary)' }} />
                  <span>Specification Feature Details (Met vs. Unmet)</span>
                </h3>
                <p style={{ margin: '4px 0 0', fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                  Detailed breakdown of every required specification evaluated against this vendor proposal.
                </p>
              </div>

              {/* Filter Tabs */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  backgroundColor: 'var(--bg-surface-hover)',
                  padding: '3px',
                  borderRadius: '8px',
                  border: '1px solid var(--border-color)',
                }}
              >
                <button
                  type="button"
                  onClick={() => setActiveFilter('ALL')}
                  style={{
                    padding: '4px 10px',
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    borderRadius: '6px',
                    border: 'none',
                    cursor: 'pointer',
                    backgroundColor: activeFilter === 'ALL' ? 'var(--accent-primary)' : 'transparent',
                    color: activeFilter === 'ALL' ? '#ffffff' : 'var(--text-secondary)',
                    transition: 'all 0.15s ease',
                  }}
                >
                  All ({results.length})
                </button>

                <button
                  type="button"
                  onClick={() => setActiveFilter('MET')}
                  style={{
                    padding: '4px 10px',
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    borderRadius: '6px',
                    border: 'none',
                    cursor: 'pointer',
                    backgroundColor: activeFilter === 'MET' ? 'var(--status-match)' : 'transparent',
                    color: activeFilter === 'MET' ? '#ffffff' : 'var(--status-match)',
                    transition: 'all 0.15s ease',
                  }}
                >
                  ✓ Met ({metList.length})
                </button>

                <button
                  type="button"
                  onClick={() => setActiveFilter('UNMET')}
                  style={{
                    padding: '4px 10px',
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    borderRadius: '6px',
                    border: 'none',
                    cursor: 'pointer',
                    backgroundColor: activeFilter === 'UNMET' ? 'var(--status-unmatch)' : 'transparent',
                    color: activeFilter === 'UNMET' ? '#ffffff' : 'var(--status-unmatch)',
                    transition: 'all 0.15s ease',
                  }}
                >
                  ✕ Unmet ({unmetList.length})
                </button>

                {partialList.length > 0 && (
                  <button
                    type="button"
                    onClick={() => setActiveFilter('PARTIAL')}
                    style={{
                      padding: '4px 10px',
                      fontSize: '0.78rem',
                      fontWeight: 700,
                      borderRadius: '6px',
                      border: 'none',
                      cursor: 'pointer',
                      backgroundColor: activeFilter === 'PARTIAL' ? 'var(--status-pending)' : 'transparent',
                      color: activeFilter === 'PARTIAL' ? '#ffffff' : 'var(--status-pending)',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    ⚠ Partial ({partialList.length})
                  </button>
                )}
              </div>
            </div>

            {/* List of Feature Items */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {displayedResults.map((item) => {
                const reqSpec = item.requirementSpec || {};
                const venSpec = item.vendorSpec || {};
                const normVerdict = getNormalizedVerdict(item);
                const isMet = normVerdict === 'MATCH';
                const isUnmet = normVerdict === 'NO_MATCH';
                const isPartial = normVerdict === 'PARTIAL';

                const title = getSpecTitle(reqSpec);
                const reqVal = getSpecValue(reqSpec);
                const reqUnit = getSpecUnit(reqSpec);
                const venVal = getSpecValue(venSpec);
                const venUnit = getSpecUnit(venSpec);

                const requiredText = formatRequirementCriteria(item.comparisonOperator, reqVal, reqUnit);
                const offeredText = venVal
                  ? `${venVal}${venUnit ? ` ${venUnit.trim()}` : ''}`
                  : (isMet ? (reqVal ? `${reqVal}${reqUnit ? ` ${reqUnit.trim()}` : ''}` : 'Offered & Compliant') : 'Not Offered / Missing');

                const reasoningText = formatAiReasoning(item.llmReasoning || item.details);

                return (
                  <div
                    key={item.id}
                    style={{
                      backgroundColor: '#ffffff',
                      borderRadius: '10px',
                      border: '1px solid var(--border-color)',
                      borderLeft: `4px solid ${
                        isMet ? 'var(--status-match)' : isUnmet ? 'var(--status-unmatch)' : 'var(--status-pending)'
                      }`,
                      padding: '14px 18px',
                      boxShadow: 'var(--shadow-sm)',
                    }}
                  >
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        flexWrap: 'wrap',
                        gap: '8px',
                        marginBottom: '10px',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-primary)' }}>
                          {title}
                        </span>
                        {item.tenderProductName && (
                          <span
                            style={{
                              fontSize: '0.75rem',
                              padding: '2px 8px',
                              borderRadius: '4px',
                              backgroundColor: 'var(--bg-surface-hover)',
                              color: 'var(--text-muted)',
                              fontWeight: 600,
                            }}
                          >
                            {item.tenderProductName}
                          </span>
                        )}
                        {item.humanVerdict && (
                          <span
                            style={{
                              fontSize: '0.72rem',
                              padding: '2px 7px',
                              borderRadius: '4px',
                              backgroundColor: 'rgba(59, 130, 246, 0.1)',
                              color: 'var(--accent-primary)',
                              fontWeight: 700,
                              display: 'flex',
                              alignItems: 'center',
                              gap: '3px',
                            }}
                          >
                            <Check size={11} /> Reviewer Verified
                          </span>
                        )}
                      </div>

                      <div>
                        {isMet ? (
                          <span
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: '4px',
                              padding: '3px 10px',
                              borderRadius: '9999px',
                              backgroundColor: 'var(--status-match-bg)',
                              color: 'var(--status-match)',
                              border: '1px solid var(--status-match-border)',
                              fontWeight: 700,
                              fontSize: '0.8rem',
                            }}
                          >
                            <CheckCircle2 size={13} /> Feature Met
                          </span>
                        ) : isUnmet ? (
                          <span
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: '4px',
                              padding: '3px 10px',
                              borderRadius: '9999px',
                              backgroundColor: 'var(--status-unmatch-bg)',
                              color: 'var(--status-unmatch)',
                              border: '1px solid var(--status-unmatch-border)',
                              fontWeight: 700,
                              fontSize: '0.8rem',
                            }}
                          >
                            <XCircle size={13} /> Feature Not Met
                          </span>
                        ) : (
                          <span
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: '4px',
                              padding: '3px 10px',
                              borderRadius: '9999px',
                              backgroundColor: 'var(--status-pending-bg)',
                              color: 'var(--status-pending)',
                              border: '1px solid var(--status-pending-border)',
                              fontWeight: 700,
                              fontSize: '0.8rem',
                            }}
                          >
                            <AlertTriangle size={13} /> Partial / Deviated
                          </span>
                        )}
                      </div>
                    </div>

                    <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                        gap: '12px',
                        backgroundColor: 'var(--bg-surface-hover)',
                        padding: '10px 14px',
                        borderRadius: '8px',
                        marginBottom: '8px',
                      }}
                    >
                      <div>
                        <span
                          style={{
                            fontSize: '0.72rem',
                            color: 'var(--text-muted)',
                            textTransform: 'uppercase',
                            fontWeight: 700,
                            display: 'block',
                          }}
                        >
                          Required in Tender:
                        </span>
                        <span style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                          {requiredText}
                        </span>
                      </div>

                      <div>
                        <span
                          style={{
                            fontSize: '0.72rem',
                            color: 'var(--text-muted)',
                            textTransform: 'uppercase',
                            fontWeight: 700,
                            display: 'block',
                          }}
                        >
                          Offered in Vendor Proposal:
                        </span>
                        <span
                          style={{
                            fontSize: '0.9rem',
                            fontWeight: 700,
                            color: isMet
                              ? 'var(--status-match)'
                              : isUnmet
                              ? 'var(--status-unmatch)'
                              : 'var(--text-primary)',
                          }}
                        >
                          {offeredText}
                        </span>
                      </div>
                    </div>

                    {(reasoningText) && (
                      <div
                        style={{
                          fontSize: '0.82rem',
                          color: 'var(--text-secondary)',
                          lineHeight: 1.4,
                          padding: '8px 12px',
                          backgroundColor: '#fafbfc',
                          borderLeft: `2px solid ${
                            isMet
                              ? 'var(--status-match-border)'
                              : isUnmet
                              ? 'var(--status-unmatch-border)'
                              : 'var(--status-pending-border)'
                          }`,
                          borderRadius: '0 6px 6px 0',
                        }}
                      >
                        <strong>AI Evaluation Reasoning:</strong> {reasoningText}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        );
      })()}
    </div>
  );
};

export default VendorScoreDetailPage;
