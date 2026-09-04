import React, { useState, useEffect } from 'react';
import {
  CheckCircle2,
  XCircle,
  HelpCircle,
  User,
  ChevronLeft,
  ChevronRight,
  Sparkles,
  TrendingUp,
  Percent,
} from 'lucide-react';
import { formatRequirementCriteria } from '../../utils/formatters';

const ComparisonMatrix = ({
  matrix,
  viewMode = 'grid',
  selectedVendorIndex = 0,
  onSelectVendorIndex,
}) => {
  if (!matrix || !matrix.rows || matrix.rows.length === 0) {
    return (
      <div className="card" style={{ padding: '2.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        No comparison data available for this tender.
      </div>
    );
  }

  const vendorNames = matrix.vendorNames || [];
  const activeIdx = Math.min(Math.max(0, selectedVendorIndex), Math.max(0, vendorNames.length - 1));

  const getVerdictStyle = (verdict) => {
    if (verdict === 'Match') {
      return {
        bg: 'var(--status-match-bg)',
        border: '1px solid var(--status-match-border)',
        color: 'var(--status-match)',
        icon: <CheckCircle2 size={13} />,
      };
    }
    if (verdict === 'Unmatch') {
      return {
        bg: 'var(--status-unmatch-bg)',
        border: '1px solid var(--status-unmatch-border)',
        color: 'var(--status-unmatch)',
        icon: <XCircle size={13} />,
      };
    }
    return {
      bg: 'var(--status-pending-bg)',
      border: '1px solid var(--status-pending-border)',
      color: 'var(--status-pending)',
      icon: <HelpCircle size={13} />,
    };
  };

  // Compute quick summary stats for the active single vendor
  const currentVendorName = vendorNames[activeIdx] || '';
  const vendorStats = React.useMemo(() => {
    let matchCount = 0;
    let unmatchCount = 0;
    let uncertainCount = 0;
    let total = 0;

    matrix.rows.forEach((row) => {
      const cell = row.vendorCells?.[activeIdx];
      if (cell) {
        total++;
        const eff = cell.humanVerdict || cell.systemVerdict || 'Unmatch';
        if (eff.toLowerCase() === 'match') matchCount++;
        else if (eff.toLowerCase() === 'unmatch') unmatchCount++;
        else uncertainCount++;
      }
    });

    const matchRate = total > 0 ? Math.round((matchCount / total) * 100) : 0;
    return { matchCount, unmatchCount, uncertainCount, total, matchRate };
  }, [matrix, activeIdx]);

  /* -------------------------------------------------------------
     MODE 1: SINGLE VENDOR (1-on-1) FOCUSED COMPARISON VIEW
  ------------------------------------------------------------- */
  if (viewMode === 'single') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {/* Vendor Selector & Navigation Bar */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            backgroundColor: 'var(--bg-secondary)',
            padding: '0.85rem 1.25rem',
            borderRadius: '12px',
            border: '1px solid var(--border-color)',
            flexWrap: 'wrap',
            gap: '0.75rem',
          }}
        >
          {/* Vendor Tab Pills */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', flex: 1 }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Select Vendor:
            </span>
            {vendorNames.map((name, idx) => {
              const isSelected = idx === activeIdx;
              return (
                <button
                  key={name}
                  type="button"
                  onClick={() => onSelectVendorIndex && onSelectVendorIndex(idx)}
                  style={{
                    padding: '6px 14px',
                    borderRadius: '8px',
                    fontSize: '0.85rem',
                    fontWeight: isSelected ? 700 : 500,
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                    border: isSelected ? '1px solid var(--accent-primary)' : '1px solid var(--border-color)',
                    backgroundColor: isSelected ? 'rgba(59, 130, 246, 0.10)' : '#ffffff',
                    color: isSelected ? 'var(--accent-primary)' : 'var(--text-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    boxShadow: isSelected ? 'var(--shadow-sm)' : 'none',
                  }}
                >
                  <User size={14} style={{ opacity: isSelected ? 1 : 0.6 }} />
                  <span>{name}</span>
                </button>
              );
            })}
          </div>

          {/* Prev / Next controls */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={activeIdx === 0}
              onClick={() => onSelectVendorIndex && onSelectVendorIndex(activeIdx - 1)}
              style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem', opacity: activeIdx === 0 ? 0.4 : 1 }}
            >
              <ChevronLeft size={16} /> Prev
            </button>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', minWidth: '48px', textAlign: 'center' }}>
              {activeIdx + 1} / {vendorNames.length}
            </span>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={activeIdx >= vendorNames.length - 1}
              onClick={() => onSelectVendorIndex && onSelectVendorIndex(activeIdx + 1)}
              style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem', opacity: activeIdx >= vendorNames.length - 1 ? 0.4 : 1 }}
            >
              Next <ChevronRight size={16} />
            </button>
          </div>
        </div>

        {/* Selected Vendor Quick Performance Banner */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            backgroundColor: '#ffffff',
            padding: '1rem 1.25rem',
            borderRadius: '12px',
            border: '1px solid var(--border-color)',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Focused 1-on-1 Vendor Evaluation
            </div>
            <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              {currentVendorName}
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ display: 'inline-block', width: 8, height: 8, borderRadius: '50%', backgroundColor: 'var(--status-match)' }} />
              <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--status-match)' }}>
                {vendorStats.matchCount} Matches
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ display: 'inline-block', width: 8, height: 8, borderRadius: '50%', backgroundColor: 'var(--status-unmatch)' }} />
              <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--status-unmatch)' }}>
                {vendorStats.unmatchCount} Unmatches
              </span>
            </div>
            {vendorStats.uncertainCount > 0 && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span style={{ display: 'inline-block', width: 8, height: 8, borderRadius: '50%', backgroundColor: 'var(--status-pending)' }} />
                <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--status-pending)' }}>
                  {vendorStats.uncertainCount} Uncertain
                </span>
              </div>
            )}
            <div
              style={{
                padding: '4px 12px',
                borderRadius: '8px',
                backgroundColor: 'rgba(59, 130, 246, 0.10)',
                color: 'var(--accent-primary)',
                fontWeight: 800,
                fontSize: '0.9rem',
              }}
            >
              {vendorStats.matchRate}% Match Rate
            </div>
          </div>
        </div>

        {/* 1-on-1 Split Comparison Table */}
        <div style={{ overflowX: 'auto', borderRadius: '12px', border: '1px solid var(--border-color)', boxShadow: 'var(--shadow-sm)' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', backgroundColor: '#ffffff' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--table-header-bg)', borderBottom: '1px solid var(--border-color)' }}>
                <th style={{ padding: '12px 16px', textAlign: 'left', width: '25%', color: 'var(--text-primary)', fontSize: '0.85rem', fontWeight: 700 }}>
                  Specification Name
                </th>
                <th style={{ padding: '12px 16px', textAlign: 'left', width: '30%', color: 'var(--text-primary)', fontSize: '0.85rem', fontWeight: 700 }}>
                  Tender Requirement & Criteria
                </th>
                <th style={{ padding: '12px 16px', textAlign: 'center', width: '10%', color: 'var(--text-primary)', fontSize: '0.85rem', fontWeight: 700 }}>
                  Weight
                </th>
                <th style={{ padding: '12px 16px', textAlign: 'left', width: '35%', color: 'var(--text-primary)', fontSize: '0.85rem', fontWeight: 700, borderLeft: '1px solid var(--border-color)' }}>
                  Offered by {currentVendorName}
                </th>
              </tr>
            </thead>
            <tbody>
              {matrix.rows.map((row) => {
                const cell = row.vendorCells?.[activeIdx];
                const effectiveVerdict = cell ? (cell.humanVerdict || cell.systemVerdict || 'Unmatch') : 'Unmatch';
                const style = getVerdictStyle(effectiveVerdict);
                const valText = cell && cell.offeredValue !== '-' ? `${cell.offeredValue} ${cell.offeredUnit || ''}`.trim() : 'Not Offered';

                return (
                  <tr
                    key={row.tenderSpecId}
                    style={{ borderBottom: '1px solid var(--border-color)', transition: 'background-color 0.15s ease' }}
                  >
                    {/* Column 1: Spec Name */}
                    <td style={{ padding: '14px 16px', verticalAlign: 'top' }}>
                      <div style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '0.9rem' }}>
                        {row.requiredSpecName}
                      </div>
                      {row.canonicalSpecName && (
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontFamily: 'monospace', marginTop: '2px' }}>
                          {row.canonicalSpecName}
                        </div>
                      )}
                    </td>

                    {/* Column 2: Requirement */}
                    <td style={{ padding: '14px 16px', verticalAlign: 'top' }}>
                      <div style={{ fontWeight: 700, color: 'var(--accent-primary)', fontSize: '0.9rem' }}>
                        {formatRequirementCriteria(row.comparisonOperator, row.requiredValue, row.requiredUnit)}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                        Comparison: <code>{row.comparisonOperator || '>='}</code>
                      </div>
                    </td>

                    {/* Column 3: Weight */}
                    <td style={{ padding: '14px 16px', textAlign: 'center', verticalAlign: 'top' }}>
                      <span
                        style={{
                          padding: '3px 8px',
                          borderRadius: '6px',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          backgroundColor: 'var(--tag-bg)',
                          border: '1px solid var(--border-color)',
                          color: 'var(--text-secondary)',
                        }}
                      >
                        {row.weight}x
                      </span>
                    </td>

                    {/* Column 4: Single Vendor Card */}
                    <td style={{ padding: '12px 16px', verticalAlign: 'top', borderLeft: '1px solid var(--border-color)' }}>
                      <div
                        style={{
                          padding: '10px 14px',
                          borderRadius: '10px',
                          backgroundColor: style.bg,
                          border: style.border,
                          display: 'flex',
                          flexDirection: 'column',
                          gap: '6px',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
                          <span style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                            {valText}
                          </span>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.78rem', color: style.color, fontWeight: 700 }}>
                            {style.icon}
                            <span>{effectiveVerdict}</span>
                          </div>
                        </div>

                        {cell?.confidenceScore !== undefined && (
                          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span>AI Confidence:</span>
                            <span style={{ fontWeight: 700, color: 'var(--text-secondary)' }}>
                              {Math.round((cell.confidenceScore || 0.90) * 100)}%
                            </span>
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  /* -------------------------------------------------------------
     MODE 2: ALL VENDORS MULTI-COLUMN GRID MATRIX VIEW
  ------------------------------------------------------------- */
  return (
    <div style={{ overflowX: 'auto', borderRadius: '12px', border: '1px solid var(--border-color)', boxShadow: 'var(--shadow-sm)' }}>
      <table className="sticky-first-col" style={{ width: '100%', borderCollapse: 'collapse', backgroundColor: '#ffffff' }}>
        <thead className="sticky-header">
          <tr style={{ backgroundColor: 'var(--table-header-bg)', borderBottom: '1px solid var(--border-color)' }}>
            <th style={{ padding: '12px 16px', textAlign: 'left', minWidth: '180px', color: 'var(--text-primary)', fontSize: '0.85rem', fontWeight: 700 }}>
              Specification
            </th>
            <th style={{ padding: '12px 16px', textAlign: 'left', minWidth: '140px', color: 'var(--text-primary)', fontSize: '0.85rem', fontWeight: 700 }}>
              Requirement
            </th>
            <th style={{ padding: '12px 16px', textAlign: 'center', minWidth: '80px', color: 'var(--text-primary)', fontSize: '0.85rem', fontWeight: 700 }}>
              Weight
            </th>
            {vendorNames.map((name) => (
              <th
                key={name}
                style={{
                  padding: '12px 16px',
                  textAlign: 'center',
                  minWidth: '180px',
                  color: 'var(--text-primary)',
                  fontSize: '0.85rem',
                  fontWeight: 700,
                  borderLeft: '1px solid var(--border-color)',
                }}
              >
                {name}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {matrix.rows.map((row) => (
            <tr
              key={row.tenderSpecId}
              style={{ borderBottom: '1px solid var(--border-color)', transition: 'background-color 0.15s ease' }}
            >
              {/* Spec info */}
              <td style={{ padding: '12px 16px' }}>
                <div style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '0.85rem' }}>
                  {row.requiredSpecName}
                </div>
                {row.canonicalSpecName && (
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                    {row.canonicalSpecName}
                  </div>
                )}
              </td>

              {/* Required value */}
              <td style={{ padding: '12px 16px' }}>
                <span style={{ fontWeight: 700, color: 'var(--accent-primary)', fontSize: '0.85rem' }}>
                  {formatRequirementCriteria(row.comparisonOperator, row.requiredValue, row.requiredUnit)}
                </span>
              </td>

              {/* Weight */}
              <td style={{ padding: '12px 16px', textAlign: 'center' }}>
                <span
                  style={{
                    padding: '3px 8px',
                    borderRadius: '6px',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    backgroundColor: 'var(--tag-bg)',
                    border: '1px solid var(--border-color)',
                    color: 'var(--text-secondary)',
                  }}
                >
                  {row.weight}x
                </span>
              </td>

              {/* Vendor Cells */}
              {row.vendorCells.map((cell) => {
                const effectiveVerdict = cell.humanVerdict || cell.systemVerdict || 'Unmatch';
                const style = getVerdictStyle(effectiveVerdict);
                const valText = cell.offeredValue !== '-' ? `${cell.offeredValue} ${cell.offeredUnit || ''}`.trim() : '-';

                return (
                  <td
                    key={cell.vendorSubmissionId}
                    style={{
                      padding: '12px 16px',
                      textAlign: 'center',
                      borderLeft: '1px solid var(--border-color)',
                      backgroundColor: 'transparent',
                    }}
                  >
                    <div
                      style={{
                        padding: '8px 10px',
                        borderRadius: '8px',
                        backgroundColor: style.bg,
                        border: style.border,
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        gap: '4px',
                      }}
                    >
                      <span style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--text-primary)' }}>
                        {valText}
                      </span>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.75rem', color: style.color, fontWeight: 700 }}>
                        {style.icon}
                        <span>{effectiveVerdict}</span>
                      </div>
                    </div>
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default ComparisonMatrix;

