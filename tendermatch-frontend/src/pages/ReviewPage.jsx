import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { tendersApi } from '../services/api';
import { formatRequirementCriteria, formatAiReasoning } from '../utils/formatters';
import VerdictBadge from '../components/matching/VerdictBadge';
import ConfidenceBar from '../components/review/ConfidenceBar';
import BulkConfirmModal from '../components/review/BulkConfirmModal';
import KeyboardShortcutHelp from '../components/review/KeyboardShortcutHelp';
import {
  ArrowLeft,
  CheckCircle2,
  XCircle,
  AlertCircle,
  HelpCircle,
  ChevronRight,
  ChevronLeft,
  RefreshCw,
  Sparkles,
  Building2,
  Trophy,
  BrainCircuit,
  Check,
  CheckCheck,
  Flame,
  FileText,
  Layers,
  Save,
  MessageSquare,
  Scale,
  Package,
  ArrowRight,
  SlidersHorizontal,
  Info,
} from 'lucide-react';

export const ReviewPage = () => {
  const { id: tenderId, vendorId: initialVendorId } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [allVendors, setAllVendors] = useState([]);
  const [currentVendorIndex, setCurrentVendorIndex] = useState(0);
  const [selectedProductIndex, setSelectedProductIndex] = useState(0);
  const [selectedSpecResultId, setSelectedSpecResultId] = useState(null);
  const [selectedVerdictInInspector, setSelectedVerdictInInspector] = useState('Match');
  const [inspectorComment, setInspectorComment] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isBulkConfirmOpen, setIsBulkConfirmOpen] = useState(false);
  const [isConfirmingBulk, setIsConfirmingBulk] = useState(false);
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isReverifyingAll, setIsReverifyingAll] = useState(false);
  const [reflectionStatus, setReflectionStatus] = useState('');
  const [tenderStatus, setTenderStatus] = useState('');

  // Fetch all vendor reviews for the tender
  const fetchReviewData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Safe check for tender details if getById is available
      let currentTenderStatus = '';
      if (tendersApi.getById) {
        try {
          const tRes = await tendersApi.getById(tenderId);
          if (tRes?.success && tRes?.data) {
            currentTenderStatus = tRes.data.status;
            setTenderStatus(tRes.data.status);
          }
        } catch (_) {}
      }

      // Try getTenderReview first, fall back to getVendorReview
      let tenderReviewRes = null;
      if (tendersApi.getTenderReview) {
        try {
          tenderReviewRes = await tendersApi.getTenderReview(tenderId);
        } catch (_) {}
      }

      if (
        tenderReviewRes?.success &&
        Array.isArray(tenderReviewRes?.data) &&
        tenderReviewRes.data.length > 0
      ) {
        setAllVendors(tenderReviewRes.data);
        if (initialVendorId) {
          const idx = tenderReviewRes.data.findIndex((v) => v.vendorId === initialVendorId);
          if (idx !== -1) setCurrentVendorIndex(idx);
        }
      } else if (initialVendorId && tendersApi.getVendorReview) {
        const singleRes = await tendersApi.getVendorReview(tenderId, initialVendorId);
        if (singleRes?.success && singleRes?.data) {
          setAllVendors([singleRes.data]);
          setCurrentVendorIndex(0);
        } else if (currentTenderStatus === 'PROCESSING' || currentTenderStatus === 'MATCHING' || currentTenderStatus === 'EXTRACTED') {
          setAllVendors([]);
        } else {
          setError('No reviewable specification data found for this tender.');
        }
      } else if (currentTenderStatus === 'PROCESSING' || currentTenderStatus === 'MATCHING' || currentTenderStatus === 'EXTRACTED') {
        setAllVendors([]);
      } else {
        setError('No reviewable specification data found for this tender.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load review workspace.');
    } finally {
      setLoading(false);
    }
  }, [tenderId, initialVendorId]);

  useEffect(() => {
    fetchReviewData();
  }, [fetchReviewData]);

  // Live auto-polling while matching is in progress
  useEffect(() => {
    if (allVendors.length > 0 || error) return;
    const interval = setInterval(() => {
      fetchReviewData();
    }, 2000);
    return () => clearInterval(interval);
  }, [allVendors.length, error, fetchReviewData]);

  const vendorReview = allVendors[currentVendorIndex] || null;
  const products = useMemo(() => vendorReview?.products || [], [vendorReview]);
  const activeProduct = products[selectedProductIndex] || products[0] || null;
  const activeSpecs = useMemo(() => activeProduct?.specPairs || [], [activeProduct]);

  // Set initial selected spec when active product changes
  useEffect(() => {
    if (activeSpecs.length > 0) {
      const exists = activeSpecs.some((sp) => sp.resultId === selectedSpecResultId);
      if (!exists) {
        setSelectedSpecResultId(activeSpecs[0].resultId);
      }
    } else {
      setSelectedSpecResultId(null);
    }
  }, [activeSpecs, selectedSpecResultId]);

  // Active spec pair for Section 3 inspector
  const currentSpecPair = useMemo(() => {
    if (!activeSpecs.length) return null;
    const found = activeSpecs.find((sp) => sp.resultId === selectedSpecResultId);
    return found || activeSpecs[0];
  }, [activeSpecs, selectedSpecResultId]);

  const activeSpecIndexInProduct = useMemo(() => {
    if (!activeSpecs.length || !currentSpecPair) return 0;
    const idx = activeSpecs.findIndex((sp) => sp.resultId === currentSpecPair.resultId);
    return idx >= 0 ? idx : 0;
  }, [activeSpecs, currentSpecPair]);

  // Synchronize inspector verdict & comment when selected spec changes
  useEffect(() => {
    if (currentSpecPair) {
      const initVerdict =
        currentSpecPair.humanVerdict ||
        (currentSpecPair.systemVerdict === 'AI Err' ? "AI Can't Predict" : currentSpecPair.systemVerdict) ||
        'Match';
      setSelectedVerdictInInspector(initVerdict);
      setInspectorComment(currentSpecPair.userComment || '');
    }
  }, [currentSpecPair?.resultId]);

  // Calculations for active product
  const productStats = useMemo(() => {
    let matched = 0;
    let unmatched = 0;
    let overrides = 0;
    activeSpecs.forEach((sp) => {
      const v = sp.humanVerdict || sp.systemVerdict || 'Match';
      if (v === 'Match') matched += 1;
      else unmatched += 1;
      if (sp.isOverride) overrides += 1;
    });
    const total = activeSpecs.length;
    const percentage = total > 0 ? Math.round((matched / total) * 100) : 0;
    return { matched, unmatched, overrides, total, percentage };
  }, [activeSpecs]);

  // Submit verdict to API and update in-memory state
  const handleSaveVerdict = async (resultId, verdict, comment = '') => {
    if (!vendorReview || !resultId) return;
    setIsSaving(true);
    const currentVendorId = vendorReview.vendorId;

    try {
      const res = await tendersApi.submitVerdict(tenderId, resultId, verdict, comment);
      if (res?.success && res?.data) {
        setAllVendors((prevVendors) => {
          return prevVendors.map((vendor) => {
            if (vendor.vendorId !== currentVendorId) return vendor;

            const updatedProducts = (vendor.products || []).map((prod) => ({
              ...prod,
              specPairs: (prod.specPairs || []).map((sp) => {
                if (sp.resultId === resultId) {
                  return {
                    ...sp,
                    humanVerdict: verdict,
                    isReviewed: true,
                    isOverride: res.data.isOverride ?? (verdict !== sp.systemVerdict),
                    userComment: comment,
                  };
                }
                return sp;
              }),
            }));

            return {
              ...vendor,
              products: updatedProducts,
              progress: res.data.reviewProgress || vendor.progress,
            };
          });
        });
      }
    } catch (err) {
      console.error('Failed to save verdict:', err);
      alert('Failed to save verdict: ' + (err.response?.data?.message || err.message));
    } finally {
      setIsSaving(false);
    }
  };

  // Selection handler: Selects row for inspection without modifying or toggling verdict
  const handleSelectSpec = (pair, e) => {
    if (e) e.stopPropagation();
    setSelectedSpecResultId(pair.resultId);
    const currentVerdict =
      pair.humanVerdict ||
      (pair.systemVerdict === 'AI Err' ? "AI Can't Predict" : pair.systemVerdict) ||
      'Match';
    setSelectedVerdictInInspector(currentVerdict);
    setInspectorComment(pair.userComment || '');
  };

  // Section 3 "Save & Next" handler: commits verdict and justification
  const handleSaveAndNext = async () => {
    if (!currentSpecPair || vendorReview?.isReviewLocked) return;
    await handleSaveVerdict(currentSpecPair.resultId, selectedVerdictInInspector, inspectorComment);

    // Advance to next spec if exists
    if (activeSpecIndexInProduct < activeSpecs.length - 1) {
      const nextPair = activeSpecs[activeSpecIndexInProduct + 1];
      setSelectedSpecResultId(nextPair.resultId);
      const nextVerdict =
        nextPair.humanVerdict ||
        (nextPair.systemVerdict === 'AI Err' ? "AI Can't Predict" : nextPair.systemVerdict) ||
        'Match';
      setSelectedVerdictInInspector(nextVerdict);
      setInspectorComment(nextPair.userComment || '');
    }
  };

  // Product tab selection
  const handleSelectProduct = (index) => {
    if (index >= 0 && index < products.length) {
      setSelectedProductIndex(index);
      if (products[index]?.specPairs?.length > 0) {
        setSelectedSpecResultId(products[index].specPairs[0].resultId);
      }
    }
  };

  // Vendor navigation
  const handleSelectVendor = (index) => {
    if (index >= 0 && index < allVendors.length) {
      setCurrentVendorIndex(index);
      setSelectedProductIndex(0);
      const nextVendorProds = allVendors[index]?.products || [];
      if (nextVendorProds[0]?.specPairs?.length > 0) {
        setSelectedSpecResultId(nextVendorProds[0].specPairs[0].resultId);
      }
    }
  };

  const handlePrevVendor = () => {
    if (currentVendorIndex > 0) {
      handleSelectVendor(currentVendorIndex - 1);
    }
  };

  const handleNextVendor = () => {
    if (currentVendorIndex < allVendors.length - 1) {
      handleSelectVendor(currentVendorIndex + 1);
    }
  };

  // Bulk confirm remaining specs for current vendor
  const handleConfirmBulk = async () => {
    if (!vendorReview) return;
    setIsConfirmingBulk(true);
    try {
      const res = await tendersApi.bulkConfirmReview(tenderId, vendorReview.vendorId);
      if (res?.success) {
        await fetchReviewData();
        setIsBulkConfirmOpen(false);
      }
    } catch (err) {
      alert('Bulk confirmation failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setIsConfirmingBulk(false);
    }
  };

  // One-click Pass 2 Reflection across all reviewed vendors + navigate to leaderboard
  const handleGetAiScoresAndLeaderboard = async () => {
    setIsReverifyingAll(true);
    setReflectionStatus('Running Pass 2 Cognitive Reflection (llama3.1)... Re-evaluating human feedback & dynamic weights (RAM/CPU prioritized over Color)...');
    try {
      if (tendersApi.reverifyAllReflection) {
        await tendersApi.reverifyAllReflection(tenderId);
      } else if (tendersApi.calculateScores) {
        await tendersApi.calculateScores(tenderId);
      }
      navigate(`/tenders/${tenderId}/scores`);
    } catch (err) {
      console.warn('Reverify all error, falling back to direct score calculation:', err);
      try {
        if (tendersApi.calculateScores) {
          await tendersApi.calculateScores(tenderId);
        }
        navigate(`/tenders/${tenderId}/scores`);
      } catch (calcErr) {
        alert('Failed to calculate AI scores: ' + (err.response?.data?.message || err.message));
        setIsReverifyingAll(false);
      }
    }
  };

  // Keyboard shortcut listener
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (['INPUT', 'TEXTAREA'].includes(e.target.tagName)) return;

      if (e.key === '?' || (e.shiftKey && e.key === '/')) {
        setIsHelpOpen((prev) => !prev);
      } else if (e.key === '1' && currentSpecPair) {
        setSelectedVerdictInInspector('Match');
      } else if (e.key === '2' && currentSpecPair) {
        setSelectedVerdictInInspector('Unmatch');
      } else if (e.key === '3' && currentSpecPair) {
        setSelectedVerdictInInspector("AI Can't Predict");
      } else if (e.key === 'Enter' && currentSpecPair && !vendorReview?.isReviewLocked) {
        e.preventDefault();
        handleSaveAndNext();
      } else if (e.key === 'ArrowDown' || e.key === 'j') {
        if (activeSpecIndexInProduct < activeSpecs.length - 1) {
          const nextPair = activeSpecs[activeSpecIndexInProduct + 1];
          setSelectedSpecResultId(nextPair.resultId);
        }
      } else if (e.key === 'ArrowUp' || e.key === 'k') {
        if (activeSpecIndexInProduct > 0) {
          const prevPair = activeSpecs[activeSpecIndexInProduct - 1];
          setSelectedSpecResultId(prevPair.resultId);
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentSpecPair, activeSpecIndexInProduct, activeSpecs, selectedVerdictInInspector, inspectorComment, vendorReview]);

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
        <p style={{ fontSize: '1.05rem', fontWeight: 500 }}>Loading review workspace...</p>
      </div>
    );
  }

  if (!vendorReview) {
    if (!error) {
      return (
        <div className="main-content">
          <div className="card" style={{ textAlign: 'center', padding: '3.5rem 2rem', maxWidth: 620, margin: '2rem auto' }}>
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: '16px',
                backgroundColor: 'rgba(99, 102, 241, 0.12)',
                color: 'var(--accent-primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1.25rem',
              }}
            >
              <RefreshCw size={26} className="animate-spin" />
            </div>
            <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
              AI Specification Matching in Progress...
            </h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.75rem', lineHeight: 1.5 }}>
              The AI matching engine is currently evaluating vendor specifications against the RFP. This review workspace will open automatically as soon as matching is completed.
            </p>
            <Link to={`/tenders/${tenderId}`} className="btn btn-secondary">
              <ArrowLeft size={16} /> Back to Session Details
            </Link>
          </div>
        </div>
      );
    }

    return (
      <div className="main-content">
        <div className="card" style={{ textAlign: 'center', padding: '3rem', borderColor: 'var(--status-unmatch-border)' }}>
          <AlertCircle size={40} style={{ color: 'var(--status-unmatch)', margin: '0 auto 1rem' }} />
          <h2 style={{ fontSize: '1.25rem', color: 'var(--text-primary)' }}>{error || 'Review data not found'}</h2>
          <Link to={`/tenders/${tenderId}`} className="btn btn-secondary" style={{ marginTop: '1.5rem' }}>
            <ArrowLeft size={16} /> Back to Tender Workspace
          </Link>
        </div>
      </div>
    );
  }

  const progress = vendorReview.progress || { reviewedCount: 0, totalCount: activeSpecs.length, percentage: 0 };
  const allCurrentReviewed = progress.totalCount > 0 && progress.reviewedCount === progress.totalCount;

  // Global review statistics across all vendors
  const totalVendors = allVendors.length;
  const totalSpecsAcrossAll = allVendors.reduce((acc, v) => acc + (v.progress?.totalCount || 0), 0);
  const totalReviewedAcrossAll = allVendors.reduce((acc, v) => acc + (v.progress?.reviewedCount || 0), 0);
  const totalOverridesAcrossAll = allVendors.reduce((acc, v) => acc + (v.progress?.overrideCount || 0), 0);

  const isOverrideActive =
    currentSpecPair &&
    ((currentSpecPair.systemVerdict === 'AI Err' && selectedVerdictInInspector !== "AI Can't Predict") ||
      (currentSpecPair.systemVerdict !== 'AI Err' && selectedVerdictInInspector !== currentSpecPair.systemVerdict));

  return (
    <div className="main-content" style={{ maxWidth: '1680px', margin: '0 auto', padding: '1rem 1.5rem' }}>
      {/* Top Header & Global Actions */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '1rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Link
            to={`/tenders/${tenderId}`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <ArrowLeft size={16} /> Back to Tender
          </Link>

          <span
            style={{
              fontSize: '0.85rem',
              color: 'var(--text-muted)',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <span>Vendors: <strong style={{ color: 'var(--text-primary)' }}>{totalVendors}</strong></span>
            <span>•</span>
            <span>Total Reviewed: <strong style={{ color: 'var(--accent-primary)' }}>{totalReviewedAcrossAll}/{totalSpecsAcrossAll}</strong></span>
            {totalOverridesAcrossAll > 0 && (
              <>
                <span>•</span>
                <span style={{ color: '#f59e0b', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '3px' }}>
                  <Flame size={13} /> {totalOverridesAcrossAll} Overrides
                </span>
              </>
            )}
          </span>
        </div>

        {/* Top Right Action Group */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          {(() => {
            const isPipelineRunning = tenderStatus === 'PROCESSING' || tenderStatus === 'MATCHING';
            const isBtnDisabled = isReverifyingAll || isPipelineRunning;

            return (
              <button
                type="button"
                onClick={handleGetAiScoresAndLeaderboard}
                disabled={isBtnDisabled}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '0.55rem 1.25rem',
                  fontSize: '0.88rem',
                  fontWeight: 700,
                  color: '#fff',
                  background: isBtnDisabled
                    ? 'linear-gradient(135deg, #9ca3af 0%, #6b7280 100%)'
                    : 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #2563eb 100%)',
                  border: '1px solid rgba(255, 255, 255, 0.2)',
                  borderRadius: '10px',
                  boxShadow: isBtnDisabled ? 'none' : '0 4px 14px rgba(79, 70, 229, 0.4)',
                  cursor: isBtnDisabled ? 'not-allowed' : 'pointer',
                  opacity: isBtnDisabled ? 0.65 : 1,
                  transition: 'all 0.2s ease',
                }}
                title="Trigger Pass 2 LLM Cognitive Reflection on overrides and open full leaderboard"
              >
                {isReverifyingAll ? (
                  <>
                    <div
                      style={{
                        width: 16,
                        height: 16,
                        border: '2px solid rgba(255, 255, 255, 0.3)',
                        borderTopColor: '#fff',
                        borderRadius: '50%',
                        animation: 'spin 0.8s linear infinite',
                      }}
                    />
                    <span>Calculating AI Scores...</span>
                  </>
                ) : (
                  <>
                    <Sparkles size={16} style={{ color: '#fde047' }} />
                    <span>Get AI Re-Scores & View Leaderboard</span>
                    <Trophy size={16} style={{ color: '#fde047' }} />
                  </>
                )}
              </button>
            );
          })()}

          {allCurrentReviewed && (
            <Link
              to={`/tenders/${tenderId}/vendors/${vendorReview.vendorId}/review-summary`}
              className="btn btn-secondary"
              style={{
                padding: '0.5rem 0.85rem',
                fontSize: '0.82rem',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                borderColor: '#10b981',
                color: '#34d399',
              }}
            >
              <CheckCircle2 size={15} />
              <span>Vendor Summary</span>
            </Link>
          )}

          <button
            type="button"
            onClick={fetchReviewData}
            className="btn btn-secondary"
            style={{ padding: '0.5rem 0.75rem', fontSize: '0.85rem' }}
            title="Refresh review workspace"
          >
            <RefreshCw size={14} />
          </button>
        </div>
      </div>

      {/* Pipeline Running Banner */}
      {(tenderStatus === 'PROCESSING' || tenderStatus === 'MATCHING') && (
        <div
          className="card"
          style={{
            marginBottom: '1rem',
            padding: '0.85rem 1.25rem',
            backgroundColor: 'var(--status-pending-bg)',
            borderColor: 'var(--status-pending-border)',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            color: 'var(--text-primary)',
          }}
        >
          <RefreshCw size={18} className="animate-spin" style={{ color: 'var(--status-pending)' }} />
          <div style={{ fontSize: '0.88rem' }}>
            <strong>Pipeline in Progress:</strong> Specification extraction and AI matching are currently running.
          </div>
        </div>
      )}

      {/* VENDOR SWITCHER BAR */}
      <div
        style={{
          backgroundColor: 'var(--bg-card)',
          border: '1px solid var(--border-color)',
          borderRadius: '12px',
          padding: '0.75rem 1.25rem',
          marginBottom: '1rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '1rem',
          boxShadow: 'var(--shadow-sm)',
        }}
      >
        <button
          type="button"
          onClick={handlePrevVendor}
          disabled={currentVendorIndex === 0}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            padding: '0.45rem 0.85rem',
            fontSize: '0.84rem',
            fontWeight: 600,
            color: currentVendorIndex === 0 ? 'var(--text-muted)' : 'var(--text-primary)',
            backgroundColor: currentVendorIndex === 0 ? 'transparent' : 'var(--bg-surface-hover)',
            border: '1px solid var(--border-color)',
            borderRadius: '8px',
            cursor: currentVendorIndex === 0 ? 'not-allowed' : 'pointer',
            opacity: currentVendorIndex === 0 ? 0.45 : 1,
            transition: 'all 0.15s ease',
          }}
        >
          <ChevronLeft size={16} />
          <span>Previous Vendor</span>
        </button>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.65rem',
            overflowX: 'auto',
            padding: '0.2rem 0',
            scrollbarWidth: 'thin',
          }}
        >
          {allVendors.map((vendor, idx) => {
            const isCurrent = idx === currentVendorIndex;
            const vProgress = vendor.progress || { reviewedCount: 0, totalCount: 0 };
            const isDone = vProgress.totalCount > 0 && vProgress.reviewedCount === vProgress.totalCount;

            return (
              <button
                key={vendor.vendorId}
                type="button"
                onClick={() => handleSelectVendor(idx)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '0.5rem 0.95rem',
                  borderRadius: '9px',
                  border: isCurrent ? '2px solid var(--accent-primary)' : '1px solid var(--border-color)',
                  backgroundColor: isCurrent ? 'rgba(37, 99, 235, 0.08)' : '#ffffff',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                  whiteSpace: 'nowrap',
                  boxShadow: isCurrent ? '0 1px 3px rgba(37, 99, 235, 0.1)' : '0 1px 2px rgba(0, 0, 0, 0.02)',
                }}
              >
                <div
                  style={{
                    width: 22,
                    height: 22,
                    borderRadius: '50%',
                    backgroundColor: isCurrent ? 'var(--accent-primary)' : '#e2e8f0',
                    color: isCurrent ? '#fff' : 'var(--text-secondary)',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {idx + 1}
                </div>
                <span style={{ fontWeight: isCurrent ? 700 : 500, color: 'var(--text-primary)', fontSize: '0.88rem' }}>
                  {vendor.vendorName}
                </span>

                {isDone ? (
                  <span
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '3px',
                      padding: '2px 7px',
                      borderRadius: '12px',
                      backgroundColor: 'var(--status-match-bg)',
                      color: 'var(--status-match)',
                      border: '1px solid var(--status-match-border)',
                      fontSize: '0.72rem',
                      fontWeight: 700,
                    }}
                  >
                    <Check size={11} /> Done ({vProgress.reviewedCount}/{vProgress.totalCount})
                  </span>
                ) : (
                  <span
                    style={{
                      padding: '2px 7px',
                      borderRadius: '12px',
                      backgroundColor: 'var(--tag-bg)',
                      color: 'var(--text-muted)',
                      fontSize: '0.72rem',
                      fontWeight: 600,
                    }}
                  >
                    {vProgress.reviewedCount}/{vProgress.totalCount}
                  </span>
                )}

                {vProgress.overrideCount > 0 && (
                  <span
                    style={{
                      padding: '2px 6px',
                      borderRadius: '12px',
                      backgroundColor: 'var(--status-pending-bg)',
                      color: 'var(--status-pending)',
                      border: '1px solid var(--status-pending-border)',
                      fontSize: '0.7rem',
                      fontWeight: 700,
                    }}
                    title={`${vProgress.overrideCount} human overrides`}
                  >
                    ⚡ {vProgress.overrideCount}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        <button
          type="button"
          onClick={handleNextVendor}
          disabled={currentVendorIndex === allVendors.length - 1}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            padding: '0.45rem 0.85rem',
            fontSize: '0.84rem',
            fontWeight: 600,
            color: currentVendorIndex === allVendors.length - 1 ? 'var(--text-muted)' : 'var(--text-primary)',
            backgroundColor: currentVendorIndex === allVendors.length - 1 ? 'transparent' : 'var(--bg-surface-hover)',
            border: '1px solid var(--border-color)',
            borderRadius: '8px',
            cursor: currentVendorIndex === allVendors.length - 1 ? 'not-allowed' : 'pointer',
            opacity: currentVendorIndex === allVendors.length - 1 ? 0.45 : 1,
            transition: 'all 0.15s ease',
          }}
        >
          <span>Next Vendor</span>
          <ChevronRight size={16} />
        </button>
      </div>

      {/* PRODUCTS TABS & WORKSPACE HEADER */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '10px',
          marginBottom: '1.25rem',
          padding: '0.65rem 1rem',
          backgroundColor: 'var(--bg-card)',
          border: '1px solid var(--border-color)',
          borderRadius: '12px',
          boxShadow: 'var(--shadow-sm)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
          <span
            style={{
              fontSize: '0.75rem',
              fontWeight: 700,
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              color: 'var(--text-muted)',
              display: 'flex',
              alignItems: 'center',
              gap: '5px',
            }}
          >
            <Package size={14} /> Products:
          </span>

          {products.map((prod, idx) => {
            const isSelected = idx === selectedProductIndex;
            const prodSpecs = prod.specPairs || [];
            const matchedInProd = prodSpecs.filter(
              (sp) => (sp.humanVerdict || sp.systemVerdict) === 'Match'
            ).length;
            const totalInProd = prodSpecs.length;

            return (
              <button
                key={prod.tenderProductId || idx}
                type="button"
                onClick={() => handleSelectProduct(idx)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '0.35rem 0.75rem',
                  borderRadius: '8px',
                  border: isSelected ? '2px solid var(--accent-primary)' : '1px solid var(--border-color)',
                  backgroundColor: isSelected ? 'rgba(37, 99, 235, 0.08)' : '#ffffff',
                  color: isSelected ? 'var(--accent-primary)' : 'var(--text-secondary)',
                  fontWeight: isSelected ? 700 : 500,
                  fontSize: '0.84rem',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                }}
              >
                <span>{prod.tenderProductName}</span>
                <span
                  style={{
                    fontSize: '0.7rem',
                    fontWeight: 700,
                    padding: '1px 6px',
                    borderRadius: '8px',
                    backgroundColor: matchedInProd === totalInProd ? 'var(--status-match-bg)' : 'var(--tag-bg)',
                    color: matchedInProd === totalInProd ? 'var(--status-match)' : 'var(--text-muted)',
                    border: matchedInProd === totalInProd ? '1px solid var(--status-match-border)' : '1px solid var(--border-color)',
                  }}
                >
                  {matchedInProd}/{totalInProd} Matched
                </span>
              </button>
            );
          })}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
          <span>Showing <strong>{activeSpecs.length}</strong> specs</span>
          <span>•</span>
          <span style={{ color: 'var(--accent-primary)', fontWeight: 600 }}>Click row to inspect & evaluate</span>
        </div>
      </div>

      {/* MAIN WORKSPACE: COMPARISON TABLE + INSPECTOR */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(0, 1.85fr) minmax(380px, 1.15fr)',
          gap: '1.25rem',
          alignItems: 'start',
        }}
      >
        {/* LEFT CONTAINER: UNCLUTTERED SPECIFICATION COMPARISON MATRIX */}
        <div
          className="card"
          style={{
            padding: 0,
            borderRadius: '14px',
            border: '1px solid var(--border-color)',
            backgroundColor: 'var(--card-bg)',
            boxShadow: 'var(--shadow-sm)',
            overflow: 'hidden',
          }}
        >
          {/* Table Header */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1.1fr 1.2fr',
              backgroundColor: 'var(--table-header-bg)',
              borderBottom: '2px solid var(--border-color)',
            }}
          >
            <div
              style={{
                padding: '0.8rem 1.15rem',
                borderRight: '1px solid var(--border-color)',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <FileText size={15} style={{ color: 'var(--accent-primary)' }} />
              <div>
                <span style={{ fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-muted)' }}>
                  Section 1
                </span>
                <div style={{ fontSize: '0.92rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                  Tender Requirement
                </div>
              </div>
            </div>

            <div
              style={{
                padding: '0.8rem 1.15rem',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Building2 size={15} style={{ color: 'var(--accent-cyan)' }} />
                <div>
                  <span style={{ fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-muted)' }}>
                    Section 2
                  </span>
                  <div style={{ fontSize: '0.92rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                    Vendor Proposal & Status
                  </div>
                </div>
              </div>

              <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 600 }}>
                Select to Edit
              </span>
            </div>
          </div>

          {/* Clean Line-by-Line Specifications Comparison List */}
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            {activeSpecs.map((pair, idx) => {
              const aiVerdict = pair.systemVerdict || 'Match';
              const humanVerdict = pair.humanVerdict || null;
              const effectiveVerdict = humanVerdict || aiVerdict;
              const isOverridden = humanVerdict && humanVerdict.toLowerCase() !== aiVerdict.toLowerCase();

              const isMatched = effectiveVerdict === 'Match';
              const isAiErr = effectiveVerdict === 'AI Err' || effectiveVerdict === "AI Can't Predict";
              const isSelected = pair.resultId === currentSpecPair?.resultId;

              const statusColor = isMatched ? '#10b981' : isAiErr ? '#f59e0b' : '#ef4444';

              return (
                <div
                  key={pair.resultId}
                  onClick={(e) => handleSelectSpec(pair, e)}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: '1.1fr 1.2fr',
                    borderBottom: idx === activeSpecs.length - 1 ? 'none' : '1px solid var(--border-color)',
                    borderLeft: `4px solid ${statusColor}`,
                    backgroundColor: isSelected ? 'rgba(37, 99, 235, 0.08)' : idx % 2 === 0 ? '#ffffff' : 'var(--bg-surface)',
                    cursor: 'pointer',
                    transition: 'all 0.12s ease',
                    outline: isSelected ? '2px solid var(--accent-primary)' : 'none',
                    outlineOffset: '-2px',
                  }}
                  title="Click to inspect AI reasoning and provide human decision"
                >
                  {/* Left Column: Requirement */}
                  <div
                    style={{
                      padding: '0.75rem 1rem',
                      borderRight: '1px solid var(--border-color)',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '4px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-muted)' }}>
                        #{idx + 1}
                      </span>
                      <strong style={{ fontSize: '0.88rem', color: 'var(--text-primary)' }}>
                        {pair.requiredSpecName || 'Specification'}
                      </strong>
                    </div>

                    <div style={{ fontSize: '0.82rem', color: 'var(--accent-primary)', fontWeight: 600, lineHeight: 1.35 }}>
                      {formatRequirementCriteria(pair.comparisonOperator, pair.requiredValue, pair.requiredUnit)}
                    </div>
                  </div>

                  {/* Right Column: Vendor Offering + Dual Review Badges */}
                  <div
                    style={{
                      padding: '0.75rem 1rem',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      gap: '6px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '8px' }}>
                      <div style={{ fontSize: '0.84rem', color: pair.offeredValue ? 'var(--text-primary)' : 'var(--status-unmatch)', fontWeight: 600, lineHeight: 1.35 }}>
                        {pair.offeredValue ? `${pair.offeredValue} ${pair.offeredUnit || ''}` : pair.offeredSpecName || 'Not Offered'}
                      </div>

                      {/* Status Badges Group */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '5px', flexShrink: 0 }}>
                        {/* AI Initial Verdict Badge */}
                        <span
                          style={{
                            fontSize: '0.68rem',
                            fontWeight: 700,
                            padding: '2px 6px',
                            borderRadius: '6px',
                            backgroundColor: aiVerdict === 'Match' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                            color: aiVerdict === 'Match' ? '#10b981' : '#ef4444',
                            border: `1px solid ${aiVerdict === 'Match' ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
                          }}
                          title={`AI Initial Assessment: ${aiVerdict}`}
                        >
                          AI: {aiVerdict.toUpperCase()}
                        </span>

                        {/* Human Override Badge (if different from AI) */}
                        {isOverridden && (
                          <span
                            style={{
                              fontSize: '0.68rem',
                              fontWeight: 800,
                              padding: '2px 6px',
                              borderRadius: '6px',
                              backgroundColor: humanVerdict === 'Match' ? '#10b981' : '#ef4444',
                              color: '#ffffff',
                              boxShadow: '0 1px 3px rgba(0,0,0,0.15)',
                            }}
                            title={`Human Overridden: ${humanVerdict}`}
                          >
                            ⚡ {humanVerdict.toUpperCase()}
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Review Note preview if exists */}
                    {pair.userComment && (
                      <div style={{ fontSize: '0.72rem', color: 'var(--accent-primary)', fontStyle: 'italic', display: '-webkit-box', WebkitLineClamp: 1, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                        Note: "{pair.userComment}"
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* RIGHT CONTAINER: SECTION 3 (Dual Review Inspector) */}
        <div
          className="card"
          style={{
            position: 'sticky',
            top: '1rem',
            padding: '1.15rem',
            borderRadius: '14px',
            border: '1px solid var(--border-color)',
            backgroundColor: 'var(--card-bg)',
            boxShadow: 'var(--shadow-md)',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
          }}
        >
          {/* Section 3 Header */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              borderBottom: '1px solid var(--border-color)',
              paddingBottom: '0.65rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <div
                style={{
                  background: 'rgba(79, 70, 229, 0.15)',
                  color: 'var(--accent-indigo)',
                  padding: '5px',
                  borderRadius: '7px',
                  display: 'flex',
                }}
              >
                <Scale size={15} />
              </div>
              <div>
                <span style={{ fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-muted)' }}>
                  Section 3
                </span>
                <div style={{ fontSize: '0.98rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                  Dual Review & Evaluation
                </div>
              </div>
            </div>

            <span
              style={{
                fontSize: '0.75rem',
                fontWeight: 700,
                color: 'var(--text-muted)',
                backgroundColor: 'var(--tag-bg)',
                padding: '2px 7px',
                borderRadius: '6px',
                border: '1px solid var(--border-color)',
              }}
            >
              Spec {activeSpecIndexInProduct + 1} of {activeSpecs.length}
            </span>
          </div>

          {currentSpecPair ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              {/* Focused Spec Title */}
              <div>
                <div style={{ fontSize: '0.95rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                  {currentSpecPair.requiredSpecName}
                </div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: '2px' }}>
                  Target: <strong>{formatRequirementCriteria(currentSpecPair.comparisonOperator, currentSpecPair.requiredValue, currentSpecPair.requiredUnit)}</strong> vs Offered: <strong>{currentSpecPair.offeredValue ? `${currentSpecPair.offeredValue} ${currentSpecPair.offeredUnit || ''}` : 'Not Offered'}</strong>
                </div>
              </div>

              {/* CARD 1: AI REVIEW ASSESSMENT (Read-Only) */}
              <div
                style={{
                  padding: '0.75rem 0.85rem',
                  borderRadius: '9px',
                  backgroundColor: 'rgba(79, 70, 229, 0.04)',
                  border: '1px solid rgba(79, 70, 229, 0.2)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '5px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '5px', fontSize: '0.74rem', fontWeight: 700, color: 'var(--accent-indigo)' }}>
                    <Sparkles size={13} /> AI Review Assessment
                  </span>
                  <span
                    style={{
                      fontSize: '0.68rem',
                      fontWeight: 800,
                      padding: '2px 6px',
                      borderRadius: '4px',
                      backgroundColor: currentSpecPair.systemVerdict === 'Match' ? 'var(--status-match-bg)' : 'var(--status-unmatch-bg)',
                      color: currentSpecPair.systemVerdict === 'Match' ? '#10b981' : '#ef4444',
                      border: `1px solid ${currentSpecPair.systemVerdict === 'Match' ? 'var(--status-match-border)' : 'var(--status-unmatch-border)'}`,
                    }}
                  >
                    AI Verdict: {currentSpecPair.systemVerdict || 'Match'}
                  </span>
                </div>

                <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', lineHeight: 1.45 }}>
                  {formatAiReasoning(
                    currentSpecPair.comparisonDetails || currentSpecPair.llmReasoning,
                    {
                      verdict: currentSpecPair.systemVerdict,
                      operator: currentSpecPair.comparisonOperator,
                      requiredValue: currentSpecPair.requiredValue,
                      requiredUnit: currentSpecPair.requiredUnit,
                      offeredValue: currentSpecPair.offeredValue,
                      offeredUnit: currentSpecPair.offeredUnit,
                    }
                  )}
                </div>

                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.7rem', color: 'var(--text-muted)', paddingTop: '2px' }}>
                  <span>Confidence: <strong>{Math.round((currentSpecPair.confidenceScore || 0.9) * 100)}%</strong></span>
                  <span>Method: <code>{currentSpecPair.comparisonMethod || 'exact'}</code></span>
                </div>
              </div>

              {/* CARD 2: HUMAN REVIEW & OVERRIDE (Interactive) */}
              <div
                style={{
                  padding: '0.75rem 0.85rem',
                  borderRadius: '9px',
                  backgroundColor: isOverrideActive ? 'rgba(245, 158, 11, 0.04)' : 'var(--bg-surface)',
                  border: isOverrideActive ? '1px solid rgba(245, 158, 11, 0.3)' : '1px solid var(--border-color)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ fontSize: '0.74rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    Human Decision:
                  </span>
                  {isOverrideActive && (
                    <span
                      style={{
                        fontSize: '0.68rem',
                        fontWeight: 800,
                        color: 'var(--status-pending)',
                        backgroundColor: 'var(--status-pending-bg)',
                        border: '1px solid var(--status-pending-border)',
                        padding: '1px 6px',
                        borderRadius: '6px',
                      }}
                    >
                      ⚡ Override Active
                    </span>
                  )}
                </div>

                {/* 3 Interactive Decision Buttons */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '6px' }}>
                  <button
                    type="button"
                    disabled={vendorReview.isReviewLocked}
                    onClick={() => setSelectedVerdictInInspector('Match')}
                    style={{
                      padding: '7px 4px',
                      borderRadius: '7px',
                      border: selectedVerdictInInspector === 'Match' ? '2px solid #10b981' : '1px solid var(--border-color)',
                      backgroundColor: selectedVerdictInInspector === 'Match' ? 'var(--status-match-bg)' : '#ffffff',
                      color: selectedVerdictInInspector === 'Match' ? '#10b981' : 'var(--text-secondary)',
                      fontWeight: 700,
                      fontSize: '0.78rem',
                      cursor: vendorReview.isReviewLocked ? 'not-allowed' : 'pointer',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '2px',
                      transition: 'all 0.12s ease',
                    }}
                  >
                    <CheckCircle2 size={14} />
                    <span>Match</span>
                    <span style={{ fontSize: '0.6rem', opacity: 0.6 }}>[1]</span>
                  </button>

                  <button
                    type="button"
                    disabled={vendorReview.isReviewLocked}
                    onClick={() => setSelectedVerdictInInspector('Unmatch')}
                    style={{
                      padding: '7px 4px',
                      borderRadius: '7px',
                      border: selectedVerdictInInspector === 'Unmatch' ? '2px solid #ef4444' : '1px solid var(--border-color)',
                      backgroundColor: selectedVerdictInInspector === 'Unmatch' ? 'var(--status-unmatch-bg)' : '#ffffff',
                      color: selectedVerdictInInspector === 'Unmatch' ? '#ef4444' : 'var(--text-secondary)',
                      fontWeight: 700,
                      fontSize: '0.78rem',
                      cursor: vendorReview.isReviewLocked ? 'not-allowed' : 'pointer',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '2px',
                      transition: 'all 0.12s ease',
                    }}
                  >
                    <XCircle size={14} />
                    <span>Unmatch</span>
                    <span style={{ fontSize: '0.6rem', opacity: 0.6 }}>[2]</span>
                  </button>

                  <button
                    type="button"
                    disabled={vendorReview.isReviewLocked}
                    onClick={() => setSelectedVerdictInInspector("AI Can't Predict")}
                    style={{
                      padding: '7px 4px',
                      borderRadius: '7px',
                      border: selectedVerdictInInspector === "AI Can't Predict" ? '2px solid #f59e0b' : '1px solid var(--border-color)',
                      backgroundColor: selectedVerdictInInspector === "AI Can't Predict" ? 'var(--status-pending-bg)' : '#ffffff',
                      color: selectedVerdictInInspector === "AI Can't Predict" ? '#f59e0b' : 'var(--text-secondary)',
                      fontWeight: 700,
                      fontSize: '0.78rem',
                      cursor: vendorReview.isReviewLocked ? 'not-allowed' : 'pointer',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '2px',
                      transition: 'all 0.12s ease',
                    }}
                  >
                    <HelpCircle size={14} />
                    <span>AI Err</span>
                    <span style={{ fontSize: '0.6rem', opacity: 0.6 }}>[3]</span>
                  </button>
                </div>

                {/* Reviewer Justification Input */}
                <div>
                  <label
                    htmlFor="reviewer-comment"
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      fontSize: '0.74rem',
                      fontWeight: 700,
                      color: 'var(--text-secondary)',
                      marginBottom: '3px',
                    }}
                  >
                    <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <MessageSquare size={12} style={{ color: 'var(--accent-primary)' }} />
                      <span>Reviewer Justification / Override Note:</span>
                    </span>
                  </label>
                  <textarea
                    id="reviewer-comment"
                    rows={2}
                    disabled={vendorReview.isReviewLocked}
                    value={inspectorComment}
                    onChange={(e) => setInspectorComment(e.target.value)}
                    placeholder="Add explanation for human decision/override (stored alongside AI evaluation)..."
                    style={{
                      width: '100%',
                      padding: '0.45rem 0.65rem',
                      fontSize: '0.78rem',
                      borderRadius: '7px',
                      border: '1px solid var(--border-color)',
                      backgroundColor: '#ffffff',
                      color: 'var(--text-primary)',
                      fontFamily: 'inherit',
                      resize: 'vertical',
                      boxSizing: 'border-box',
                      lineHeight: 1.35,
                    }}
                  />
                </div>

                {/* Save Decision & Next Action */}
                <button
                  type="button"
                  onClick={handleSaveAndNext}
                  disabled={isSaving || vendorReview.isReviewLocked}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                    width: '100%',
                    padding: '0.6rem 1rem',
                    fontSize: '0.85rem',
                    fontWeight: 700,
                    color: '#ffffff',
                    background: isSaving
                      ? '#9ca3af'
                      : 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
                    border: 'none',
                    borderRadius: '8px',
                    boxShadow: '0 2px 8px rgba(37, 99, 235, 0.2)',
                    cursor: isSaving || vendorReview.isReviewLocked ? 'not-allowed' : 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <Save size={15} />
                  <span>{isSaving ? 'Saving...' : 'Save Decision & Next'}</span>
                </button>
              </div>

              {/* Bulk Confirm Remaining Specs */}
              <button
                type="button"
                onClick={() => setIsBulkConfirmOpen(true)}
                disabled={vendorReview.isReviewLocked}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '5px',
                  width: '100%',
                  padding: '0.45rem 0.85rem',
                  fontSize: '0.76rem',
                  fontWeight: 600,
                  color: 'var(--text-secondary)',
                  backgroundColor: 'var(--bg-surface)',
                  border: '1px solid var(--border-color)',
                  borderRadius: '7px',
                  cursor: vendorReview.isReviewLocked ? 'not-allowed' : 'pointer',
                  transition: 'all 0.12s ease',
                }}
              >
                <CheckCheck size={14} />
                <span>Bulk Confirm Remaining Specs</span>
              </button>
            </div>
          ) : (
            <div style={{ textAlign: 'center', padding: '1.5rem', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
              Select a specification from the left to review
            </div>
          )}
        </div>
      </div>

      {/* Bulk Confirm Modal */}
      <BulkConfirmModal
        isOpen={isBulkConfirmOpen}
        onClose={() => setIsBulkConfirmOpen(false)}
        onConfirm={handleConfirmBulk}
        unreviewedCount={Math.max(0, progress.totalCount - progress.reviewedCount)}
        isConfirming={isConfirmingBulk}
      />

      {/* Keyboard Shortcuts Overlay */}
      <KeyboardShortcutHelp
        isOpen={isHelpOpen}
        onClose={() => setIsHelpOpen(false)}
      />

      {/* Cognitive AI Reflection Overlay while re-scoring */}
      {isReverifyingAll && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.65)',
            backdropFilter: 'blur(8px)',
            WebkitBackdropFilter: 'blur(8px)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            padding: '2rem',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              border: '1px solid var(--border-color)',
              borderRadius: '16px',
              padding: '2.5rem',
              maxWidth: '540px',
              boxShadow: 'var(--shadow-lg)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '1.25rem',
            }}
          >
            <div
              style={{
                width: 68,
                height: 68,
                borderRadius: '50%',
                background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(79, 70, 229, 0.1))',
                border: '2px solid rgba(37, 99, 235, 0.3)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <BrainCircuit size={36} style={{ color: 'var(--accent-primary)', animation: 'pulse 1.5s infinite' }} />
            </div>

            <h3 style={{ fontSize: '1.35rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
              AI Cognitive Reflection in Progress
            </h3>

            <p style={{ fontSize: '0.92rem', color: 'var(--text-secondary)', lineHeight: 1.6, margin: 0 }}>
              {reflectionStatus}
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReviewPage;
