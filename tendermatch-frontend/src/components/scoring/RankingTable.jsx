import React, { useState } from 'react';
import {
  ChevronDown,
  ChevronRight,
  Award,
  Layers,
  ExternalLink,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Check,
  Filter,
  Sparkles,
  HelpCircle,
} from 'lucide-react';
import { formatRequirementCriteria, formatAiReasoning } from '../../utils/formatters';

const getSpecTitle = (spec) => {
  if (!spec) return 'Specification';
  const canonical = spec.canonicalSpecName || spec.canonical_spec_name;
  if (canonical) {
    return canonical
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
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

const RankingTable = ({ scores = [], matchResults = [], onSelectVendor }) => {
  const [expandedRows, setExpandedRows] = useState({});
  const [filterByVendor, setFilterByVendor] = useState({});

  const toggleRow = (id) => {
    setExpandedRows((prev) => ({
      ...prev,
      [id]: !prev[id],
    }));
  };

  const handleSetFilter = (vendorId, filter) => {
    setFilterByVendor((prev) => ({
      ...prev,
      [vendorId]: filter,
    }));
  };

  const getMedal = (rank) => {
    if (rank === 1) return '🥇';
    if (rank === 2) return '🥈';
    if (rank === 3) return '🥉';
    return null;
  };

  const getScoreColor = (score) => {
    if (score >= 80) return 'rgb(16, 185, 129)';
    if (score >= 50) return 'rgb(245, 158, 11)';
    return 'rgb(239, 68, 68)';
  };

  if (!scores || scores.length === 0) {
    return (
      <div className="card" style={{ padding: '2.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        No vendor scores calculated yet. Click "Calculate Scores" to evaluate the leaderboard.
      </div>
    );
  }

  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: '0 8px' }}>
        <thead>
          <tr
            style={{
              color: 'var(--text-muted)',
              fontSize: '0.8rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
            }}
          >
            <th style={{ padding: '8px 16px', textAlign: 'left' }}>Rank</th>
            <th style={{ padding: '8px 16px', textAlign: 'left' }}>Vendor Name</th>
            <th style={{ padding: '8px 16px', textAlign: 'center' }}>Overall Score</th>
            <th style={{ padding: '8px 16px', textAlign: 'center' }}>Matched</th>
            <th style={{ padding: '8px 16px', textAlign: 'center' }}>Unmatched</th>
            <th style={{ padding: '8px 16px', textAlign: 'center' }}>Uncertain</th>
            <th style={{ padding: '8px 16px', textAlign: 'right' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {scores.map((s) => {
            const isExpanded = !!expandedRows[s.vendorScoreId || s.vendorSubmissionId];
            const medal = getMedal(s.rank);
            const scoreColor = getScoreColor(s.overallScore);
            const hasProducts = s.productScores && s.productScores.length > 0;

            // Extract matching results for this specific vendor
            const vendorResults = matchResults.filter(
              (r) => r.vendorSubmissionId === s.vendorSubmissionId
            );

            const getNormalizedVerdict = (r) => {
              const raw = (r.humanVerdict || r.systemVerdict || r.llmVerdict || '').trim();
              const upper = raw.toUpperCase();
              if (upper === 'MATCH' || upper === 'MATCHED') return 'MATCH';
              if (upper === 'UNMATCH' || upper === 'NO_MATCH' || upper === 'UNMATCHED') return 'NO_MATCH';
              return 'PARTIAL';
            };

            const metList = vendorResults.filter((r) => getNormalizedVerdict(r) === 'MATCH');
            const unmetList = vendorResults.filter((r) => getNormalizedVerdict(r) === 'NO_MATCH');
            const partialList = vendorResults.filter((r) => getNormalizedVerdict(r) === 'PARTIAL');

            const activeFilter =
              filterByVendor[s.vendorSubmissionId || s.vendorScoreId] || 'ALL';

            const displayedSpecs =
              activeFilter === 'MET'
                ? metList
                : activeFilter === 'UNMET'
                ? unmetList
                : activeFilter === 'PARTIAL'
                ? partialList
                : vendorResults;

            const canExpand = hasProducts || vendorResults.length > 0;

            return (
              <React.Fragment key={s.vendorScoreId || s.vendorSubmissionId}>
                <tr
                  style={{
                    backgroundColor: '#ffffff',
                    borderRadius: '10px',
                    boxShadow: 'var(--shadow-sm)',
                    border: '1px solid var(--border-color)',
                    transition: 'background-color 0.15s ease',
                  }}
                >
                  <td
                    style={{
                      padding: '14px 16px',
                      borderTopLeftRadius: '10px',
                      borderBottomLeftRadius: isExpanded ? 0 : '10px',
                      borderTop: '1px solid var(--border-color)',
                      borderBottom: isExpanded ? 'none' : '1px solid var(--border-color)',
                      borderLeft: '1px solid var(--border-color)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      {canExpand && (
                        <button
                          type="button"
                          data-testid={`expand-${s.vendorScoreId || s.vendorSubmissionId}`}
                          onClick={() => toggleRow(s.vendorScoreId || s.vendorSubmissionId)}
                          style={{
                            background: 'none',
                            border: 'none',
                            color: 'var(--text-muted)',
                            cursor: 'pointer',
                            display: 'flex',
                            padding: 0,
                          }}
                          title={isExpanded ? 'Collapse breakdown' : 'Expand feature breakdown'}
                        >
                          {isExpanded ? <ChevronDown size={18} /> : <ChevronRight size={18} />}
                        </button>
                      )}
                      <span
                        style={{
                          fontWeight: 800,
                          fontSize: '0.95rem',
                          color: 'var(--text-primary)',
                        }}
                      >
                        #{s.rank}
                      </span>
                      {medal && <span style={{ fontSize: '1.2rem' }}>{medal}</span>}
                    </div>
                  </td>

                  <td
                    style={{
                      padding: '14px 16px',
                      borderTop: '1px solid var(--border-color)',
                      borderBottom: isExpanded ? 'none' : '1px solid var(--border-color)',
                      cursor: canExpand ? 'pointer' : 'default',
                    }}
                    onClick={() => canExpand && toggleRow(s.vendorScoreId || s.vendorSubmissionId)}
                  >
                    <div
                      style={{
                        fontWeight: 700,
                        color: 'var(--text-primary)',
                        fontSize: '0.95rem',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                      }}
                    >
                      <span>{s.vendorName}</span>
                      {canExpand && (
                        <span
                          style={{
                            fontSize: '0.72rem',
                            fontWeight: 600,
                            padding: '1px 6px',
                            borderRadius: '4px',
                            backgroundColor: 'var(--bg-surface-hover)',
                            color: 'var(--text-muted)',
                          }}
                        >
                          {isExpanded ? 'Hide Details' : 'View Met/Unmet Details'}
                        </span>
                      )}
                    </div>
                    {s.fileName && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {s.fileName}
                      </div>
                    )}
                  </td>

                  <td
                    style={{
                      padding: '14px 16px',
                      textAlign: 'center',
                      borderTop: '1px solid var(--border-color)',
                      borderBottom: isExpanded ? 'none' : '1px solid var(--border-color)',
                    }}
                  >
                    <span
                      style={{
                        fontWeight: 800,
                        fontSize: '1.1rem',
                        color: scoreColor,
                      }}
                    >
                      {Number.isInteger(s.overallScore)
                        ? `${s.overallScore}%`
                        : `${s.overallScore.toFixed(1)}%`}
                    </span>
                  </td>

                  <td
                    style={{
                      padding: '14px 16px',
                      textAlign: 'center',
                      borderTop: '1px solid var(--border-color)',
                      borderBottom: isExpanded ? 'none' : '1px solid var(--border-color)',
                    }}
                  >
                    <span
                      style={{
                        padding: '3px 9px',
                        borderRadius: '9999px',
                        backgroundColor: 'var(--status-match-bg)',
                        color: 'var(--status-match)',
                        border: '1px solid var(--status-match-border)',
                        fontWeight: 700,
                        fontSize: '0.8rem',
                      }}
                    >
                      {s.matched} / {s.totalSpecs}
                    </span>
                  </td>

                  <td
                    style={{
                      padding: '14px 16px',
                      textAlign: 'center',
                      borderTop: '1px solid var(--border-color)',
                      borderBottom: isExpanded ? 'none' : '1px solid var(--border-color)',
                    }}
                  >
                    <span
                      style={{
                        padding: '3px 9px',
                        borderRadius: '9999px',
                        backgroundColor: 'var(--status-unmatch-bg)',
                        color: 'var(--status-unmatch)',
                        border: '1px solid var(--status-unmatch-border)',
                        fontWeight: 700,
                        fontSize: '0.8rem',
                      }}
                    >
                      {s.unmatched} / {s.totalSpecs}
                    </span>
                  </td>

                  <td
                    style={{
                      padding: '14px 16px',
                      textAlign: 'center',
                      borderTop: '1px solid var(--border-color)',
                      borderBottom: isExpanded ? 'none' : '1px solid var(--border-color)',
                    }}
                  >
                    <span
                      style={{
                        padding: '3px 9px',
                        borderRadius: '9999px',
                        backgroundColor: 'var(--status-pending-bg)',
                        color: 'var(--status-pending)',
                        border: '1px solid var(--status-pending-border)',
                        fontWeight: 700,
                        fontSize: '0.8rem',
                      }}
                    >
                      {s.uncertain} / {s.totalSpecs}
                    </span>
                  </td>

                  <td
                    style={{
                      padding: '14px 16px',
                      textAlign: 'right',
                      borderTopRightRadius: '10px',
                      borderBottomRightRadius: isExpanded ? 0 : '10px',
                      borderTop: '1px solid var(--border-color)',
                      borderBottom: isExpanded ? 'none' : '1px solid var(--border-color)',
                      borderRight: '1px solid var(--border-color)',
                    }}
                  >
                    {onSelectVendor && (
                      <button
                        type="button"
                        onClick={() => onSelectVendor(s.vendorSubmissionId)}
                        className="btn btn-secondary"
                        style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                      >
                        <span>Details</span>
                        <ExternalLink size={13} />
                      </button>
                    )}
                  </td>
                </tr>

                {/* Expanded Details Row: Product Breakdown & Specific Met/Unmet Features */}
                {isExpanded && (
                  <tr>
                    <td
                      colSpan={7}
                      style={{
                        backgroundColor: 'var(--bg-surface-hover)',
                        border: '1px solid var(--border-color)',
                        borderTop: 'none',
                        padding: '18px 24px',
                        borderBottomLeftRadius: '10px',
                        borderBottomRightRadius: '10px',
                      }}
                    >
                      {/* 1. Product Level Breakdown */}
                      {hasProducts && (
                        <div style={{ marginBottom: '1.25rem' }}>
                          <div
                            style={{
                              fontSize: '0.75rem',
                              fontWeight: 700,
                              textTransform: 'uppercase',
                              color: 'var(--text-muted)',
                              marginBottom: '8px',
                              display: 'flex',
                              alignItems: 'center',
                              gap: '6px',
                            }}
                          >
                            <Layers size={14} style={{ color: 'var(--accent-primary)' }} />
                            <span>Product Level Breakdown</span>
                          </div>
                          <div
                            style={{
                              display: 'grid',
                              gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                              gap: '10px',
                            }}
                          >
                            {s.productScores.map((ps) => (
                              <div
                                key={ps.tenderProductId || ps.productName}
                                style={{
                                  padding: '10px 14px',
                                  borderRadius: '8px',
                                  backgroundColor: '#ffffff',
                                  border: '1px solid var(--border-color)',
                                  boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)',
                                }}
                              >
                                <div
                                  style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                    marginBottom: '4px',
                                  }}
                                >
                                  <div
                                    style={{
                                      display: 'flex',
                                      alignItems: 'center',
                                      gap: '6px',
                                      fontWeight: 600,
                                      fontSize: '0.85rem',
                                    }}
                                  >
                                    <span>{ps.productName}</span>
                                  </div>
                                  <span
                                    style={{
                                      fontWeight: 800,
                                      color: getScoreColor(ps.score),
                                      fontSize: '0.85rem',
                                    }}
                                  >
                                    {ps.score}%
                                  </span>
                                </div>
                                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                  Matched: {ps.matched} | Unmatched: {ps.unmatched} | Uncertain:{' '}
                                  {ps.uncertain}
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* 2. Detailed Specification Feature Breakdown (Met vs. Unmet Specifically) */}
                      <div>
                        <div
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            flexWrap: 'wrap',
                            gap: '10px',
                            marginBottom: '10px',
                          }}
                        >
                          <div
                            style={{
                              fontSize: '0.8rem',
                              fontWeight: 700,
                              color: 'var(--text-primary)',
                              display: 'flex',
                              alignItems: 'center',
                              gap: '6px',
                            }}
                          >
                            <Sparkles size={15} style={{ color: 'var(--accent-primary)' }} />
                            <span>Feature Compliance Details (What is Met & What is Not)</span>
                          </div>

                          {/* Filter Tabs for Met / Unmet / All */}
                          <div
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: '6px',
                              backgroundColor: '#ffffff',
                              padding: '3px',
                              borderRadius: '8px',
                              border: '1px solid var(--border-color)',
                            }}
                          >
                            <button
                              type="button"
                              onClick={() =>
                                handleSetFilter(s.vendorSubmissionId || s.vendorScoreId, 'ALL')
                              }
                              style={{
                                padding: '3px 10px',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                borderRadius: '6px',
                                border: 'none',
                                cursor: 'pointer',
                                backgroundColor:
                                  activeFilter === 'ALL'
                                    ? 'var(--accent-primary)'
                                    : 'transparent',
                                color: activeFilter === 'ALL' ? '#ffffff' : 'var(--text-secondary)',
                                transition: 'all 0.15s ease',
                              }}
                            >
                              All ({vendorResults.length})
                            </button>

                            <button
                              type="button"
                              onClick={() =>
                                handleSetFilter(s.vendorSubmissionId || s.vendorScoreId, 'MET')
                              }
                              style={{
                                padding: '3px 10px',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                borderRadius: '6px',
                                border: 'none',
                                cursor: 'pointer',
                                backgroundColor:
                                  activeFilter === 'MET'
                                    ? 'var(--status-match)'
                                    : 'transparent',
                                color: activeFilter === 'MET' ? '#ffffff' : 'var(--status-match)',
                                transition: 'all 0.15s ease',
                              }}
                            >
                              ✓ Met Features ({metList.length})
                            </button>

                            <button
                              type="button"
                              onClick={() =>
                                handleSetFilter(s.vendorSubmissionId || s.vendorScoreId, 'UNMET')
                              }
                              style={{
                                padding: '3px 10px',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                borderRadius: '6px',
                                border: 'none',
                                cursor: 'pointer',
                                backgroundColor:
                                  activeFilter === 'UNMET'
                                    ? 'var(--status-unmatch)'
                                    : 'transparent',
                                color: activeFilter === 'UNMET' ? '#ffffff' : 'var(--status-unmatch)',
                                transition: 'all 0.15s ease',
                              }}
                            >
                              ✕ Unmet Features ({unmetList.length})
                            </button>

                            {partialList.length > 0 && (
                              <button
                                type="button"
                                onClick={() =>
                                  handleSetFilter(s.vendorSubmissionId || s.vendorScoreId, 'PARTIAL')
                                }
                                style={{
                                  padding: '3px 10px',
                                  fontSize: '0.75rem',
                                  fontWeight: 700,
                                  borderRadius: '6px',
                                  border: 'none',
                                  cursor: 'pointer',
                                  backgroundColor:
                                    activeFilter === 'PARTIAL'
                                      ? 'var(--status-pending)'
                                      : 'transparent',
                                  color:
                                    activeFilter === 'PARTIAL'
                                      ? '#ffffff'
                                      : 'var(--status-pending)',
                                  transition: 'all 0.15s ease',
                                }}
                              >
                                ⚠ Partial ({partialList.length})
                              </button>
                            )}
                          </div>
                        </div>

                        {/* Specification List */}
                        {displayedSpecs.length === 0 ? (
                          <div
                            style={{
                              padding: '1.25rem',
                              textAlign: 'center',
                              color: 'var(--text-muted)',
                              fontSize: '0.85rem',
                              backgroundColor: '#ffffff',
                              borderRadius: '8px',
                              border: '1px solid var(--border-color)',
                            }}
                          >
                            No specifications matching the "{activeFilter}" filter.
                          </div>
                        ) : (
                          <div
                            style={{
                              display: 'flex',
                              flexDirection: 'column',
                              gap: '8px',
                            }}
                          >
                            {displayedSpecs.map((item) => {
                              const reqSpec = item.requirementSpec || {};
                              const venSpec = item.vendorSpec || {};
                              const normVerdict = getNormalizedVerdict(item);
                              const isMet = normVerdict === 'MATCH';
                              const isUnmet = normVerdict === 'NO_MATCH';
                              const isPartial = normVerdict === 'PARTIAL';

                              const featureTitle = getSpecTitle(reqSpec);
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
                                      isMet
                                        ? 'var(--status-match)'
                                        : isUnmet
                                        ? 'var(--status-unmatch)'
                                        : 'var(--status-pending)'
                                    }`,
                                    padding: '12px 16px',
                                    boxShadow: '0 1px 3px rgba(0, 0, 0, 0.02)',
                                  }}
                                >
                                  <div
                                    style={{
                                      display: 'flex',
                                      alignItems: 'center',
                                      justifyContent: 'space-between',
                                      flexWrap: 'wrap',
                                      gap: '8px',
                                      marginBottom: '8px',
                                    }}
                                  >
                                    <div
                                      style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '8px',
                                      }}
                                    >
                                      <span
                                        style={{
                                          fontWeight: 700,
                                          fontSize: '0.9rem',
                                          color: 'var(--text-primary)',
                                        }}
                                      >
                                        {featureTitle}
                                      </span>
                                      {item.tenderProductName && (
                                        <span
                                          style={{
                                            fontSize: '0.72rem',
                                            padding: '2px 7px',
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
                                            fontSize: '0.7rem',
                                            padding: '2px 6px',
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

                                    {/* Compliance Badge */}
                                    <div>
                                      {isMet ? (
                                        <span
                                          style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '4px',
                                            padding: '3px 9px',
                                            borderRadius: '9999px',
                                            backgroundColor: 'var(--status-match-bg)',
                                            color: 'var(--status-match)',
                                            border: '1px solid var(--status-match-border)',
                                            fontWeight: 700,
                                            fontSize: '0.78rem',
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
                                            padding: '3px 9px',
                                            borderRadius: '9999px',
                                            backgroundColor: 'var(--status-unmatch-bg)',
                                            color: 'var(--status-unmatch)',
                                            border: '1px solid var(--status-unmatch-border)',
                                            fontWeight: 700,
                                            fontSize: '0.78rem',
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
                                            padding: '3px 9px',
                                            borderRadius: '9999px',
                                            backgroundColor: 'var(--status-pending-bg)',
                                            color: 'var(--status-pending)',
                                            border: '1px solid var(--status-pending-border)',
                                            fontWeight: 700,
                                            fontSize: '0.78rem',
                                          }}
                                        >
                                          <AlertTriangle size={13} /> Partial / Deviated
                                        </span>
                                      )}
                                    </div>
                                  </div>

                                  {/* Side by Side Specification Values */}
                                  <div
                                    style={{
                                      display: 'grid',
                                      gridTemplateColumns:
                                        'repeat(auto-fit, minmax(240px, 1fr))',
                                      gap: '10px',
                                      backgroundColor: 'var(--bg-surface-hover)',
                                      padding: '8px 12px',
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
                                      <span
                                        style={{
                                          fontSize: '0.85rem',
                                          fontWeight: 700,
                                          color: 'var(--text-primary)',
                                        }}
                                      >
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
                                        Offered in Vendor Bid:
                                      </span>
                                      <span
                                        style={{
                                          fontSize: '0.85rem',
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

                                  {/* Detailed AI Reasoning on Met / Unmet verdict */}
                                  {reasoningText && (
                                    <div
                                      style={{
                                        fontSize: '0.8rem',
                                        color: 'var(--text-secondary)',
                                        lineHeight: 1.4,
                                        padding: '6px 10px',
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
                                      <strong>AI Analysis:</strong> {reasoningText}
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                )}
              </React.Fragment>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default RankingTable;
