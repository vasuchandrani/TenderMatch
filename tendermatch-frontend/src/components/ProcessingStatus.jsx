import React, { useEffect } from 'react';
import { Clock, RefreshCw, CheckCircle2, AlertCircle, FileText, Building2, Sparkles, Cpu, Layers } from 'lucide-react';

export const ProcessingStatus = ({ statusData, onRefresh, isPolling = false }) => {
  if (!statusData) return null;

  const vendorDocs = statusData.vendorDocuments || [];
  const hasVendors = vendorDocs.length > 0;

  // Pipeline completes ONLY when all vendors have reached 'MATCHED' status
  const allVendorsMatched = hasVendors
    ? vendorDocs.every((v) => v.status === 'MATCHED')
    : (statusData.tenderStatus === 'COMPLETED' || statusData.tenderStatus === 'SCORED');

  const isCompleted =
    (statusData.tenderStatus === 'COMPLETED' || statusData.tenderStatus === 'SCORED') &&
    allVendorsMatched;

  const isFailed = statusData.tenderStatus === 'FAILED';

  const isExtracting =
    statusData.tenderStatus === 'PROCESSING' ||
    statusData.requirementStatus === 'PROCESSING' ||
    vendorDocs.some((v) => v.status === 'PROCESSING');

  const isMatching =
    statusData.tenderStatus === 'MATCHING' ||
    statusData.tenderStage === 'MATCHING_SPECS' ||
    vendorDocs.some((v) => v.processingStage === 'MATCHING_SPECS' || v.processingStage === 'MATCHING');

  const isExtracted = statusData.tenderStatus === 'EXTRACTED' && !isMatching && !isExtracting;

  const isProcessing = isPolling || isExtracting || isMatching;

  useEffect(() => {
    if (!isProcessing || !onRefresh) return;

    const timer = setInterval(() => {
      onRefresh();
    }, 3000);

    return () => clearInterval(timer);
  }, [isProcessing, onRefresh]);

  const total = statusData.totalDocuments || 1;
  const completed = statusData.completedDocuments || 0;
  const matchedCount = vendorDocs.filter((v) => v.status === 'MATCHED').length;
  const remainingDocs = Math.max(0, total - completed);

  // Granular Progress Calculation: 0-50% for extraction, 50-95% for vendor matching, 100% when all matched
  let progressPercent;
  if (isCompleted) {
    progressPercent = 100;
  } else if (isFailed) {
    progressPercent = Math.min(90, Math.round((completed / total) * 100));
  } else if (isExtracted) {
    progressPercent = 50;
  } else {
    const extractionProgress = total > 0 ? (completed / total) * 50 : 0;
    const matchingProgress = hasVendors ? (matchedCount / vendorDocs.length) * 45 : 0;
    progressPercent = Math.min(95, Math.max(8, Math.round(extractionProgress + matchingProgress)));
  }

  // Estimated time calculation
  const getEstimatedTimeText = () => {
    if (isCompleted) {
      return 'All specifications matched & ranked!';
    }
    if (isFailed) {
      return 'Pipeline stopped due to error.';
    }
    if (isMatching || matchedCount > 0) {
      return `Matching & scoring vendors (${matchedCount}/${vendorDocs.length} completed) ~10–20 seconds...`;
    }
    if (isExtracted) {
      return 'Extraction complete. Click "Run Match Evaluation" above to compare specifications.';
    }
    if (remainingDocs === 0) {
      return 'Finalizing extraction...';
    }
    if (remainingDocs === 1) {
      return '~20–40 seconds';
    }
    return `~${Math.max(1, Math.ceil(remainingDocs * 0.4))}–${Math.ceil(remainingDocs * 0.8)} minutes`;
  };

  const getStageBadge = (stage, status, isRequirement = false, itemsCount = null, errorMsg = null) => {
    const rawStage = (stage || '').toUpperCase();
    const rawStatus = (status || 'PENDING').toUpperCase();

    if (rawStatus === 'FAILED' || rawStage === 'FAILED') {
      return (
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '5px',
            padding: '4px 10px',
            borderRadius: '9999px',
            fontSize: '0.74rem',
            fontWeight: 700,
            backgroundColor: 'rgba(239, 68, 68, 0.12)',
            color: '#dc2626',
            border: '1px solid rgba(239, 68, 68, 0.28)',
          }}
          title={errorMsg || 'Failed'}
        >
          <AlertCircle size={13} />
          Failed: {errorMsg ? errorMsg.substring(0, 30) + '...' : 'Error during processing'}
        </span>
      );
    }

    if (rawStage === 'OCR_SCANNING' || rawStage === 'OCR_REQUIREMENT') {
      return (
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            padding: '4px 10px',
            borderRadius: '9999px',
            fontSize: '0.74rem',
            fontWeight: 700,
            backgroundColor: 'rgba(14, 165, 233, 0.12)',
            color: '#0284c7',
            border: '1px solid rgba(14, 165, 233, 0.3)',
          }}
        >
          <span
            style={{
              width: '7px',
              height: '7px',
              borderRadius: '50%',
              backgroundColor: '#0284c7',
              animation: 'pulse 1.2s infinite',
            }}
          />
          OCR Text Scanning...
        </span>
      );
    }

    if (rawStage === 'SPEC_EXTRACTION' || rawStage === 'EXTRACTING_SPECS') {
      return (
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            padding: '4px 10px',
            borderRadius: '9999px',
            fontSize: '0.74rem',
            fontWeight: 700,
            backgroundColor: 'rgba(168, 85, 247, 0.12)',
            color: '#7e22ce',
            border: '1px solid rgba(168, 85, 247, 0.3)',
          }}
        >
          <span
            style={{
              width: '7px',
              height: '7px',
              borderRadius: '50%',
              backgroundColor: '#7e22ce',
              animation: 'pulse 1.2s infinite',
            }}
          />
          Spec & Feature AI Extraction...
        </span>
      );
    }

    if (rawStage === 'MATCHING_SPECS' || rawStatus === 'MATCHING') {
      return (
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            padding: '4px 10px',
            borderRadius: '9999px',
            fontSize: '0.74rem',
            fontWeight: 700,
            backgroundColor: 'rgba(99, 102, 241, 0.12)',
            color: '#4f46e5',
            border: '1px solid rgba(99, 102, 241, 0.3)',
          }}
        >
          <span
            style={{
              width: '7px',
              height: '7px',
              borderRadius: '50%',
              backgroundColor: '#4f46e5',
              animation: 'pulse 1.2s infinite',
            }}
          />
          AI Matching & Scoring...
        </span>
      );
    }

    if (rawStatus === 'MATCHED') {
      return (
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '5px',
            padding: '4px 10px',
            borderRadius: '9999px',
            fontSize: '0.74rem',
            fontWeight: 700,
            backgroundColor: 'rgba(99, 102, 241, 0.12)',
            color: '#4f46e5',
            border: '1px solid rgba(99, 102, 241, 0.28)',
          }}
        >
          <Sparkles size={13} />
          Matched & Scored
        </span>
      );
    }

    if (rawStatus === 'DONE' || rawStatus === 'EXTRACTED' || rawStage === 'EXTRACTED' || rawStage === 'REQUIREMENT_DONE') {
      return (
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '5px',
            padding: '4px 10px',
            borderRadius: '9999px',
            fontSize: '0.74rem',
            fontWeight: 700,
            backgroundColor: 'rgba(16, 185, 129, 0.12)',
            color: '#059669',
            border: '1px solid rgba(16, 185, 129, 0.28)',
          }}
        >
          <CheckCircle2 size={13} />
          {itemsCount ? `Extracted (${itemsCount} specs)` : 'Extraction Done'}
        </span>
      );
    }

    return (
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '5px',
          padding: '4px 10px',
          borderRadius: '9999px',
          fontSize: '0.74rem',
          fontWeight: 600,
          backgroundColor: 'var(--bg-surface)',
          color: 'var(--text-muted)',
          border: '1px solid var(--border-color)',
        }}
      >
        <Clock size={12} />
        Queued (Next in Pipeline)
      </span>
    );
  };

  return (
    <div
      style={{
        background: 'var(--card-bg)',
        border: '1px solid var(--border-color)',
        borderRadius: '16px',
        padding: '1.5rem',
        boxShadow: 'var(--shadow-md)',
        marginBottom: '1.5rem',
      }}
    >
      {/* Header Info */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '1.25rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              width: 44,
              height: 44,
              borderRadius: '12px',
              backgroundColor: isCompleted ? 'rgba(16, 185, 129, 0.12)' : isExtracted ? 'rgba(6, 182, 212, 0.12)' : 'rgba(59, 130, 246, 0.12)',
              color: isCompleted ? 'var(--status-match)' : isExtracted ? '#0891b2' : 'var(--accent-primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {isCompleted ? (
              <CheckCircle2 size={24} />
            ) : isExtracted ? (
              <Sparkles size={22} />
            ) : (
              <RefreshCw size={22} className="animate-spin" data-testid="processing-spinner" />
            )}
          </div>

          <div>
            <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              {isCompleted
                ? 'Document Pipeline & AI Matching Completed'
                : isMatching || matchedCount > 0
                ? 'AI Matching & Specification Evaluation in Progress...'
                : isExtracted
                ? 'Document Specifications Extracted (Ready for Matching)'
                : 'Processing Documents & Extracting Specifications'}
            </h3>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '3px', fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Clock size={13} />
                <span>Estimated Time: <strong style={{ color: 'var(--text-primary)' }}>{getEstimatedTimeText()}</strong></span>
              </span>
              <span>•</span>
              <span>
                Progress:{' '}
                <strong style={{ color: isCompleted ? 'var(--status-match)' : isExtracted ? '#0891b2' : 'var(--accent-primary)' }}>
                  {isCompleted
                    ? `${total}/${total} Documents (${vendorDocs.length} Matched & Scored)`
                    : isMatching
                    ? `${matchedCount}/${vendorDocs.length} Vendors Matched`
                    : `${completed}/${total} Documents Extracted`}
                </strong>
              </span>
            </div>
          </div>
        </div>

        {isProcessing && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 12px',
              backgroundColor: 'rgba(59, 130, 246, 0.08)',
              border: '1px solid rgba(59, 130, 246, 0.2)',
              borderRadius: '8px',
              fontSize: '0.78rem',
              color: 'var(--accent-primary)',
              fontWeight: 600,
            }}
          >
            <span>Background execution active. You can safely leave or browse.</span>
          </div>
        )}
      </div>

      {/* Progress Bar */}
      <div style={{ marginBottom: '1.5rem' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '0.78rem',
            color: 'var(--text-muted)',
            marginBottom: '6px',
          }}
        >
          <span>Pipeline Completion</span>
          <span style={{ fontWeight: 800, color: isCompleted ? 'var(--status-match)' : isExtracted ? '#0891b2' : 'var(--text-primary)' }}>
            {progressPercent}%
          </span>
        </div>

        <div
          style={{
            height: '8px',
            borderRadius: '9999px',
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            overflow: 'hidden',
          }}
        >
          <div
            style={{
              height: '100%',
              width: `${progressPercent}%`,
              borderRadius: '9999px',
              background: isCompleted
                ? 'linear-gradient(90deg, #10b981 0%, #059669 100%)'
                : isExtracted
                ? 'linear-gradient(90deg, #06b6d4 0%, #3b82f6 100%)'
                : 'linear-gradient(90deg, #3b82f6 0%, #8b5cf6 50%, #3b82f6 100%)',
              backgroundSize: isCompleted || isExtracted ? '100% 100%' : '200% 100%',
              animation: isCompleted || isExtracted ? 'none' : 'shimmer 2s linear infinite',
              transition: 'width 0.4s ease',
            }}
          />
        </div>
      </div>

      {/* Per-Document Stage Breakdown Cards */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {/* 1. Requirement Tender Document */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0.75rem 1rem',
            borderRadius: '10px',
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            gap: '1rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
            <FileText size={18} style={{ color: 'var(--accent-primary)', flexShrink: 0 }} />
            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: '0.86rem', fontWeight: 700, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                Tender Requirement: <span>{statusData.requirementFileName || 'requirement.pdf'}</span>
              </div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                Target Criteria Source Document
              </div>
            </div>
          </div>

          <div style={{ flexShrink: 0 }}>
            {getStageBadge(
              statusData.tenderStage,
              statusData.requirementStatus,
              true,
              null,
              statusData.tenderErrorMessage
            )}
          </div>
        </div>

        {/* 2. Vendor Bid Documents */}
        {(statusData.vendorDocuments || []).map((vDoc, idx) => (
          <div
            key={vDoc.submissionId || idx}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '0.75rem 1rem',
              borderRadius: '10px',
              backgroundColor: 'var(--bg-surface)',
              border: '1px solid var(--border-color)',
              gap: '1rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
              <Building2 size={18} style={{ color: 'var(--accent-cyan)', flexShrink: 0 }} />
              <div style={{ minWidth: 0 }}>
                <div style={{ fontSize: '0.86rem', fontWeight: 700, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  <span>{vDoc.vendorName || 'Vendor Proposal'}</span>
                  {vDoc.fileName && <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem', fontWeight: 500, marginLeft: '6px' }}>({vDoc.fileName})</span>}
                </div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                  Vendor Proposal Document #{idx + 1}
                </div>
              </div>
            </div>

            <div style={{ flexShrink: 0 }}>
              {getStageBadge(
                vDoc.processingStage,
                vDoc.status,
                false,
                vDoc.productCount,
                vDoc.errorMessage
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ProcessingStatus;
