import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { tendersApi } from '../services/api';
import {
  ArrowLeft,
  FileText,
  Users,
  Clock,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Sparkles,
  Cpu,
  Target,
  ChevronDown,
  ChevronUp,
  CheckSquare,
  Award,
  Sliders,
  Table,
  Package,
  Layers,
  Building2,
  ExternalLink,
  ShieldCheck,
} from 'lucide-react';
import ProcessingStatus from '../components/ProcessingStatus';
import SpecTable from '../components/SpecTable';
import MatchSummaryCard from '../components/matching/MatchSummaryCard';
import VerdictBadge from '../components/matching/VerdictBadge';
import SpecConfidenceBadge from '../components/SpecConfidenceBadge';
import { useNotifications } from '../context/NotificationContext';

export const TenderDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { trackTender } = useNotifications();
  const [tender, setTender] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [processingTriggering, setProcessingTriggering] = useState(false);
  const [matchingTriggering, setMatchingTriggering] = useState(false);
  const [statusData, setStatusData] = useState(null);
  
  // Requirement Extracted Products & Specs
  const [products, setProducts] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);

  // Vendor Extracted Products & Specs ("What Provided")
  const [vendorProducts, setVendorProducts] = useState([]);
  const [selectedVendorProduct, setSelectedVendorProduct] = useState(null);

  // Active Tab for Specifications Section: 'requirements' | 'vendor_offerings'
  const [specTab, setSpecTab] = useState('requirements');

  // Match evaluation results
  const [matchSummaries, setMatchSummaries] = useState([]);
  const [selectedVendorSummary, setSelectedVendorSummary] = useState(null);
  const [vendorResults, setVendorResults] = useState([]);
  const [loadingResults, setLoadingResults] = useState(false);

  const fetchTenderDetails = async (isManual = false) => {
    if (isManual) setRefreshing(true);
    try {
      const res = await tendersApi.getById(id);
      if (res?.success && res?.data) {
        setTender(res.data);
      }

      // 1. Fetch extraction processing status
      try {
        if (tendersApi.getProcessingStatus) {
          const statusRes = await tendersApi.getProcessingStatus(id);
          if (statusRes?.success && statusRes?.data) {
            setStatusData(statusRes.data);
          }
        }
      } catch (_) {}

      // 2. Fetch requirement extracted products
      try {
        if (tendersApi.getProducts) {
          const prodRes = await tendersApi.getProducts(id);
          if (prodRes?.success && Array.isArray(prodRes?.data)) {
            setProducts(prodRes.data);
            if (prodRes.data.length > 0) {
              setSelectedProduct((prev) => {
                if (!prev) return prodRes.data[0];
                const match = prodRes.data.find((p) => p.id === prev.id);
                return match || prodRes.data[0];
              });
            }
          }
        }
      } catch (_) {}

      // 3. Fetch vendor extracted products ("What Provided")
      try {
        if (tendersApi.getVendorProducts) {
          const vProdRes = await tendersApi.getVendorProducts(id);
          if (vProdRes?.success && Array.isArray(vProdRes?.data)) {
            setVendorProducts(vProdRes.data);
            if (vProdRes.data.length > 0) {
              setSelectedVendorProduct((prev) => {
                if (!prev) return vProdRes.data[0];
                const match = vProdRes.data.find((p) => p.id === prev.id);
                return match || vProdRes.data[0];
              });
            }
          }
        }
      } catch (_) {}

      // 4. Fetch match evaluation summaries
      try {
        if (tendersApi.getMatchSummary) {
          const sumRes = await tendersApi.getMatchSummary(id);
          if (sumRes?.success && Array.isArray(sumRes?.data)) {
            setMatchSummaries(sumRes.data);
          } else if (Array.isArray(sumRes)) {
            setMatchSummaries(sumRes);
          }
        }
      } catch (_) {}
    } catch (err) {
      setError('Failed to fetch tender session details.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchTenderDetails();
  }, [id]);

  // Live Auto-Sync Polling
  useEffect(() => {
    const isOngoing =
      tender?.status === 'PROCESSING' ||
      tender?.status === 'MATCHING' ||
      statusData?.tenderStatus === 'PROCESSING' ||
      statusData?.tenderStatus === 'MATCHING' ||
      (statusData && statusData.totalDocuments > 0 && statusData.completedDocuments < statusData.totalDocuments);

    if (!isOngoing) return;

    const interval = setInterval(() => {
      fetchTenderDetails(false);
    }, 2000);

    return () => clearInterval(interval);
  }, [id, tender?.status, statusData?.tenderStatus, statusData?.completedDocuments, statusData?.totalDocuments]);

  const handleTriggerProcess = async () => {
    setProcessingTriggering(true);
    trackTender(id, 'PROCESSING');
    try {
      const res = await tendersApi.triggerProcess(id);
      if (res?.success && res?.data) {
        setStatusData(res.data);
        if (tender) {
          setTender({ ...tender, status: 'PROCESSING' });
        }
      }
      setTimeout(() => fetchTenderDetails(false), 800);
    } catch (err) {
      alert('Failed to trigger processing: ' + (err.response?.data?.message || err.message));
    } finally {
      setProcessingTriggering(false);
    }
  };

  const handleTriggerMatch = async () => {
    setMatchingTriggering(true);
    trackTender(id, 'MATCHING');
    try {
      if (!statusData || statusData.completedDocuments < statusData.totalDocuments) {
        await tendersApi.triggerProcess(id);
      }
      const res = await tendersApi.triggerMatch(id);
      if (res?.success) {
        if (tender) {
          setTender({ ...tender, status: 'MATCHING' });
        }
        await fetchTenderDetails(false);
      }
    } catch (err) {
      alert('Failed to trigger matching: ' + (err.response?.data?.message || err.message));
    } finally {
      setMatchingTriggering(false);
    }
  };

  const handleSelectVendorSummary = async (summary) => {
    setSelectedVendorSummary(summary);
    setLoadingResults(true);
    try {
      const res = await tendersApi.getVendorResults(id, summary.submissionId);
      if (res?.success && res?.data) {
        setVendorResults(res.data);
      }
    } catch (err) {
      console.error('Failed to load vendor match results', err);
    } finally {
      setLoadingResults(false);
    }
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
      case 'SCORED':
        return (
          <span className="badge badge-match">
            <CheckCircle2 size={12} /> {status}
          </span>
        );
      case 'EXTRACTED':
        return (
          <span className="badge badge-match">
            <CheckCircle2 size={12} /> EXTRACTED
          </span>
        );
      case 'PROCESSING':
      case 'MATCHING':
        return (
          <span className="badge badge-pending">
            <Clock size={12} /> {status}
          </span>
        );
      case 'FAILED':
        return (
          <span className="badge badge-unmatch">
            <AlertCircle size={12} /> {status}
          </span>
        );
      default:
        return <span className="badge badge-info">{status}</span>;
    }
  };

  if (loading) {
    return (
      <div
        className="main-content"
        style={{ textAlign: 'center', padding: '6rem 2rem', color: 'var(--text-secondary)' }}
      >
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
        <p>Loading tender details...</p>
      </div>
    );
  }

  if (error || !tender) {
    return (
      <div className="main-content">
        <div
          className="card"
          style={{ textAlign: 'center', padding: '3rem', borderColor: 'var(--status-unmatch-border)' }}
        >
          <AlertCircle size={40} style={{ color: 'var(--status-unmatch)', margin: '0 auto 1rem' }} />
          <h2 style={{ fontSize: '1.25rem', color: 'var(--text-primary)' }}>
            {error || 'Tender session not found'}
          </h2>
          <Link to="/dashboard" className="btn btn-secondary" style={{ marginTop: '1.5rem' }}>
            <ArrowLeft size={16} /> Back to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  const vendorDocs = statusData?.vendorDocuments || [];
  const hasVendorDocs = vendorDocs.length > 0;
  const hasTenderSubmissions = tender?.vendorSubmissions && tender.vendorSubmissions.length > 0;

  // All vendors must be in 'MATCHED' status
  const allVendorsMatched = hasVendorDocs
    ? vendorDocs.every((v) => v.status === 'MATCHED')
    : hasTenderSubmissions
    ? tender.vendorSubmissions.every((v) => v.status === 'MATCHED')
    : false;

  const isExtracting = tender.status === 'PROCESSING' || statusData?.tenderStatus === 'PROCESSING';
  const isMatching = tender.status === 'MATCHING' || statusData?.tenderStatus === 'MATCHING';

  // Pipeline is finished ONLY when all vendors are Matched & Scored AND tender status is COMPLETED or SCORED
  const isFullyMatchedAndScored =
    (tender.status === 'COMPLETED' || tender.status === 'SCORED') &&
    allVendorsMatched &&
    !isExtracting &&
    !isMatching;

  const isPipelineRunning = Boolean(
    processingTriggering ||
    matchingTriggering ||
    isExtracting ||
    isMatching
  );

  const canExtract = !isPipelineRunning && (tender.status === 'REQUIREMENT_UPLOADED' || tender.status === 'CREATED' || products.length === 0);
  const canMatch = !isPipelineRunning && tender.status !== 'FAILED';

  const totalReqSpecs = products.reduce((acc, p) => acc + (p.specifications?.length || 0), 0);
  const totalVendorSpecs = vendorProducts.reduce((acc, p) => acc + (p.specifications?.length || 0), 0);

  return (
    <div className="main-content">
      {/* Top Breadcrumb & Actions */}
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
        <Link
          to="/dashboard"
          className="btn btn-secondary"
          style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem' }}
        >
          <ArrowLeft size={16} /> Back to Dashboard
        </Link>

        <div style={{ display: 'flex', gap: '0.65rem', alignItems: 'center', flexWrap: 'wrap' }}>
          {canExtract && (
            <button
              onClick={handleTriggerProcess}
              disabled={processingTriggering || isPipelineRunning}
              className="btn btn-primary"
              style={{ padding: '0.45rem 1rem', fontSize: '0.85rem' }}
            >
              <Sparkles size={15} />
              {processingTriggering ? 'Starting Pipeline...' : 'Run Extraction Pipeline'}
            </button>
          )}

          {/* Button 1: AI Score / Run Match Evaluation */}
          {isPipelineRunning ? (
            <button
              disabled
              className="btn btn-primary"
              style={{
                padding: '0.45rem 1rem',
                fontSize: '0.85rem',
                opacity: 0.6,
                cursor: 'not-allowed',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
              title="Matching and scoring pipeline is currently running..."
            >
              <Target size={15} className="animate-spin" />
              <span>
                {matchingTriggering || isMatching
                  ? 'Matching in Progress...'
                  : processingTriggering || isExtracting
                  ? 'Extraction in Progress...'
                  : 'Matching in Progress...'}
              </span>
            </button>
          ) : (
            <button
              onClick={handleTriggerMatch}
              className="btn btn-primary"
              style={{
                padding: '0.45rem 1rem',
                fontSize: '0.85rem',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
              title="Run or re-run AI Match Evaluation across all vendors"
            >
              <Target size={15} />
              <span>Run Match Evaluation</span>
            </button>
          )}

          {/* Button 2: Review & Validate */}
          {!isFullyMatchedAndScored ? (
            <button
              disabled
              className="btn btn-secondary"
              style={{
                padding: '0.45rem 0.9rem',
                fontSize: '0.85rem',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                opacity: 0.5,
                cursor: 'not-allowed',
              }}
              title="Locked until all vendors are matched & scored in the pipeline."
            >
              <CheckSquare size={15} />
              <span>Review & Validate</span>
            </button>
          ) : (
            <Link
              to={`/tenders/${tender.id}/review`}
              className="btn btn-secondary"
              style={{
                padding: '0.45rem 0.9rem',
                fontSize: '0.85rem',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <CheckSquare size={15} />
              <span>Review & Validate</span>
            </Link>
          )}

          {/* Button 3: Scores & Leaderboard */}
          {!isFullyMatchedAndScored ? (
            <button
              disabled
              className="btn btn-primary"
              style={{
                padding: '0.45rem 1rem',
                fontSize: '0.85rem',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                opacity: 0.5,
                cursor: 'not-allowed',
              }}
              title="Locked until all vendors are matched & scored in the pipeline."
            >
              <Award size={15} />
              <span>Scores & Leaderboard</span>
            </button>
          ) : (
            <Link
              to={`/tenders/${tender.id}/scores`}
              className="btn btn-primary"
              style={{
                padding: '0.45rem 1rem',
                fontSize: '0.85rem',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <Award size={15} />
              <span>Scores & Leaderboard</span>
            </Link>
          )}

          {/* Button 4: Refresh */}
          <button
            onClick={() => fetchTenderDetails(true)}
            disabled={refreshing}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem' }}
          >
            <RefreshCw size={15} className={refreshing ? 'animate-spin' : ''} />
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>


      {/* Main Overview Header Card */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                marginBottom: '0.5rem',
              }}
            >
              <h1
                style={{
                  fontSize: '1.6rem',
                  fontWeight: 800,
                  color: 'var(--text-primary)',
                  letterSpacing: '-0.02em',
                }}
              >
                {tender.name}
              </h1>
              {getStatusBadge(tender.status)}
            </div>
            {tender.description && (
              <p
                style={{
                  color: 'var(--text-secondary)',
                  fontSize: '0.925rem',
                  maxWidth: 800,
                }}
              >
                {tender.description}
              </p>
            )}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '1.5rem',
                marginTop: '1rem',
                fontSize: '0.85rem',
                color: 'var(--text-muted)',
                flexWrap: 'wrap',
              }}
            >
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                <Clock size={14} /> Created: {new Date(tender.createdAt).toLocaleDateString()}
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                <FileText size={14} /> RFP:{' '}
                <strong style={{ color: 'var(--text-primary)' }}>
                  {tender.requirementFileName ? 'Document Attached' : 'None'}
                </strong>
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                <Users size={14} /> Vendor Submissions:{' '}
                <strong style={{ color: 'var(--text-primary)' }}>
                  {tender.vendorSubmissions?.length || 0}
                </strong>
              </span>
              {products.length > 0 && (
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <Cpu size={14} style={{ color: 'var(--accent-primary)' }} /> Extracted Requirements:{' '}
                  <strong style={{ color: 'var(--accent-primary)' }}>
                    {totalReqSpecs} specs
                  </strong>
                </span>
              )}
              {vendorProducts.length > 0 && (
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <Package size={14} style={{ color: '#10b981' }} /> Vendor Offerings:{' '}
                  <strong style={{ color: '#10b981' }}>
                    {totalVendorSpecs} specs
                  </strong>
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Live Pipeline Processing Banner */}
      {(isExtracting || isMatching || statusData) && (
        <ProcessingStatus
          statusData={statusData}
          onRefresh={() => fetchTenderDetails(false)}
          isPolling={isPipelineRunning}
        />
      )}

      {/* Quick Access Hero for Completed / Scored Evaluation */}
      {isFullyMatchedAndScored && (tender.status === 'COMPLETED' || tender.status === 'SCORED' || tender.status === 'REVIEW' || matchSummaries.length > 0) && (
        <div
          className="card"
          style={{
            marginBottom: '1.5rem',
            background: 'linear-gradient(135deg, #f0fdf4 0%, #ffffff 60%, #f8fafc 100%)',
            border: '1px solid rgba(16, 185, 129, 0.25)',
            borderRadius: '16px',
            padding: '1.25rem 1.75rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
            boxShadow: '0 4px 14px rgba(16, 185, 129, 0.06), 0 1px 3px rgba(0, 0, 0, 0.03)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: '12px',
                backgroundColor: '#dcfce7',
                border: '1px solid #bbf7d0',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#15803d',
              }}
            >
              <Award size={26} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--text-primary)', margin: 0 }}>
                Latest AI Evaluation & Ranked Leaderboard Available
              </h3>
              <p style={{ margin: '3px 0 0', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                Review winning vendor rankings, comparison matrix, specification breakdowns, and export PDF/CSV reports.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
            <Link
              to={`/tenders/${tender.id}/scores`}
              className="btn btn-primary"
              style={{ padding: '0.5rem 1.1rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px', backgroundColor: '#10b981', borderColor: '#10b981' }}
            >
              <Award size={15} />
              <span>Open Ranked Leaderboard</span>
            </Link>
            <Link
              to={`/tenders/${tender.id}/matrix`}
              className="btn btn-secondary"
              style={{ padding: '0.5rem 0.9rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <Table size={15} />
              <span>Comparison Matrix</span>
            </Link>
            <Link
              to={`/tenders/${tender.id}/review`}
              className="btn btn-secondary"
              style={{ padding: '0.5rem 0.9rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <CheckSquare size={15} />
              <span>3-Panel Review</span>
            </Link>
          </div>
        </div>
      )}

      {/* Match Results Summary Section (Always visible when matchSummaries exist) */}
      {matchSummaries.length > 0 && (
        <div style={{ marginBottom: '2.5rem' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              marginBottom: '1rem',
            }}
          >
            <Target size={22} style={{ color: '#10b981' }} />
            <h2
              style={{
                fontSize: '1.25rem',
                fontWeight: 700,
                margin: 0,
                color: 'var(--text-primary)',
              }}
            >
              Vendor Matching Results & Compliance
            </h2>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
              gap: '1.25rem',
              marginBottom: '1.5rem',
            }}
          >
            {matchSummaries.map((summary) => (
              <MatchSummaryCard
                key={summary.submissionId}
                summary={summary}
                onClick={handleSelectVendorSummary}
                isMatching={tender.status === 'MATCHING'}
              />
            ))}
          </div>

          {/* Breakdown Table for Selected Vendor */}
          {selectedVendorSummary && (
            <div
              style={{
                background: 'var(--card-bg)',
                border: '1px solid var(--border-color)',
                borderRadius: '14px',
                padding: '1.5rem',
                boxShadow: '0 8px 30px rgba(0, 0, 0, 0.08)',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '1rem',
                  borderBottom: '1px solid var(--border-color)',
                  paddingBottom: '0.75rem',
                }}
              >
                <div>
                  <h3
                    style={{
                      margin: 0,
                      fontSize: '1.1rem',
                      fontWeight: 700,
                      color: 'var(--text-primary)',
                    }}
                  >
                    {selectedVendorSummary.vendorName} — Detailed Specification Verdicts
                  </h3>
                  <p
                    style={{
                      margin: '4px 0 0 0',
                      fontSize: '0.8rem',
                      color: 'var(--text-muted)',
                    }}
                  >
                    Line-by-line comparison between RFP requirement and what {selectedVendorSummary.vendorName} provided.
                  </p>
                </div>
                <button
                  onClick={() => setSelectedVendorSummary(null)}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: 'var(--text-muted)',
                    fontSize: '1.2rem',
                    cursor: 'pointer',
                  }}
                >
                  ✕
                </button>
              </div>

              {loadingResults ? (
                <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                  Loading comparison records...
                </div>
              ) : vendorResults.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                  No match records available for this vendor.
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table
                    style={{
                      width: '100%',
                      borderCollapse: 'collapse',
                      fontSize: '0.875rem',
                      textAlign: 'left',
                    }}
                  >
                    <thead>
                      <tr
                        style={{
                          borderBottom: '1px solid var(--border-color)',
                          backgroundColor: 'var(--table-header-bg)',
                          color: 'var(--text-secondary)',
                          fontWeight: 700,
                        }}
                      >
                        <th style={{ padding: '10px 14px' }}>Requirement Spec</th>
                        <th style={{ padding: '10px 14px' }}>What Provided</th>
                        <th style={{ padding: '10px 14px' }}>Operator</th>
                        <th style={{ padding: '10px 14px' }}>Verdict</th>
                        <th style={{ padding: '10px 14px' }}>Confidence</th>
                        <th style={{ padding: '10px 14px' }}>AI Reasoning & Details</th>
                      </tr>
                    </thead>
                    <tbody>
                      {vendorResults.map((r, idx) => (
                        <tr
                          key={r.id || idx}
                          style={{
                            borderBottom: '1px solid var(--border-color)',
                          }}
                        >
                          <td style={{ padding: '10px 14px' }}>
                            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                              {r.requirementSpec?.raw_spec_name}
                            </div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--accent-primary)', fontWeight: 600 }}>
                              {r.requirementSpec?.spec_value} {r.requirementSpec?.unit || ''}
                            </div>
                          </td>
                          <td style={{ padding: '10px 14px' }}>
                            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                              {r.vendorSpec?.raw_spec_name || (
                                <span style={{ color: 'var(--status-unmatch)' }}>
                                  Not Specified
                                </span>
                              )}
                            </div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                              {r.vendorSpec?.spec_value || '—'} {r.vendorSpec?.unit || ''}
                            </div>
                          </td>
                          <td
                            style={{
                              padding: '10px 14px',
                              fontFamily: 'monospace',
                              fontWeight: 700,
                            }}
                          >
                            {r.comparisonOperator || '—'}
                          </td>
                          <td style={{ padding: '10px 14px' }}>
                            <VerdictBadge verdict={r.systemVerdict} />
                          </td>
                          <td style={{ padding: '10px 14px' }}>
                            <span style={{ fontWeight: 600 }}>
                              {Math.round((r.confidenceScore || 0) * 100)}%
                            </span>
                          </td>
                          <td
                            style={{
                              padding: '10px 14px',
                              color: 'var(--text-secondary)',
                              fontSize: '0.8rem',
                            }}
                          >
                            {r.details || r.llmReasoning || '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* EXTRACTED SPECIFICATIONS & VENDOR OFFERINGS DASHBOARD */}
      <div className="card" style={{ marginBottom: '2rem', padding: '1.5rem' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '1.25rem',
            flexWrap: 'wrap',
            gap: '1rem',
            borderBottom: '1px solid var(--border-color)',
            paddingBottom: '1rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: '10px',
                backgroundColor: 'rgba(56, 189, 248, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--accent-primary)',
              }}
            >
              <Cpu size={20} />
            </div>
            <div>
              <h2
                style={{
                  fontSize: '1.25rem',
                  fontWeight: 800,
                  margin: 0,
                  color: 'var(--text-primary)',
                }}
              >
                Extracted Products & Specifications
              </h2>
              <p style={{ margin: '2px 0 0', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Inspect extracted RFP requirement parameters and exact vendor offered specifications.
              </p>
            </div>
          </div>

          {/* Sub-Tab Navigation Bar */}
          <div
            style={{
              display: 'flex',
              backgroundColor: 'var(--bg-surface)',
              padding: '4px',
              borderRadius: '10px',
              border: '1px solid var(--border-color)',
              gap: '4px',
            }}
          >
            <button
              onClick={() => setSpecTab('requirements')}
              style={{
                padding: '6px 14px',
                borderRadius: '7px',
                border: 'none',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer',
                backgroundColor: specTab === 'requirements' ? 'var(--accent-primary)' : 'transparent',
                color: specTab === 'requirements' ? '#ffffff' : 'var(--text-secondary)',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                transition: 'all 0.15s ease',
              }}
            >
              <FileText size={14} />
              <span>RFP Requirements ({totalReqSpecs})</span>
            </button>

            <button
              onClick={() => setSpecTab('vendor_offerings')}
              style={{
                padding: '6px 14px',
                borderRadius: '7px',
                border: 'none',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer',
                backgroundColor: specTab === 'vendor_offerings' ? '#10b981' : 'transparent',
                color: specTab === 'vendor_offerings' ? '#ffffff' : 'var(--text-secondary)',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                transition: 'all 0.15s ease',
              }}
            >
              <Package size={14} />
              <span>What Vendors Provided ({vendorProducts.length})</span>
            </button>
          </div>
        </div>

        {/* TAB 1: RFP Requirement Specifications */}
        {specTab === 'requirements' && (
          <div>
            {products.length === 0 ? (
              <div
                style={{
                  padding: '3rem 2rem',
                  textAlign: 'center',
                  background: 'var(--bg-surface)',
                  borderRadius: '12px',
                  border: '1px dashed var(--border-color)',
                }}
              >
                <FileText size={40} style={{ color: 'var(--accent-primary)', margin: '0 auto 1rem', opacity: 0.8 }} />
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)', margin: '0 0 0.5rem' }}>
                  Requirements Ready for AI Extraction
                </h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: 500, margin: '0 auto 1.5rem' }}>
                  The RFP requirement file is uploaded. Trigger the extraction pipeline to extract technical specifications (Processor, RAM, Storage, OS, Warranty, etc.).
                </p>
                <button
                  onClick={handleTriggerProcess}
                  disabled={processingTriggering || isPipelineRunning}
                  className="btn btn-primary"
                  style={{ padding: '0.6rem 1.5rem', fontWeight: 600 }}
                >
                  <Sparkles size={16} />
                  {processingTriggering ? 'Extracting Specifications...' : 'Extract RFP Requirements Now'}
                </button>
              </div>
            ) : (
              <div>
                {/* Product selector tabs if multiple products/lots */}
                {products.length > 1 && (
                  <div style={{ display: 'flex', gap: '8px', marginBottom: '1rem', flexWrap: 'wrap' }}>
                    {products.map((prod) => (
                      <button
                        key={prod.id}
                        onClick={() => setSelectedProduct(prod)}
                        style={{
                          padding: '8px 16px',
                          borderRadius: '8px',
                          border:
                            selectedProduct?.id === prod.id
                              ? '1px solid #38bdf8'
                              : '1px solid var(--border-color)',
                          backgroundColor:
                            selectedProduct?.id === prod.id
                              ? 'rgba(56, 189, 248, 0.15)'
                              : 'var(--card-bg)',
                          color: selectedProduct?.id === prod.id ? '#38bdf8' : 'var(--text-secondary)',
                          fontWeight: 600,
                          fontSize: '0.875rem',
                          cursor: 'pointer',
                        }}
                      >
                        {prod.product_name} ({prod.specifications?.length || 0} specs)
                      </button>
                    ))}
                  </div>
                )}

                {selectedProduct && (
                  <SpecTable
                    specifications={selectedProduct.specifications || []}
                    productName={selectedProduct.product_name}
                  />
                )}
              </div>
            )}
          </div>
        )}

        {/* TAB 2: What Vendors Provided (Vendor Proposal Specs) */}
        {specTab === 'vendor_offerings' && (
          <div>
            {vendorProducts.length === 0 ? (
              <div
                style={{
                  padding: '3rem 2rem',
                  textAlign: 'center',
                  background: 'var(--bg-surface)',
                  borderRadius: '12px',
                  border: '1px dashed var(--border-color)',
                }}
              >
                <Package size={40} style={{ color: '#10b981', margin: '0 auto 1rem', opacity: 0.8 }} />
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)', margin: '0 0 0.5rem' }}>
                  Vendor Bid Offerings Ready for AI Extraction
                </h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: 500, margin: '0 auto 1.5rem' }}>
                  {tender.vendorSubmissions?.length || 0} vendor proposals are uploaded. Run document extraction to see what hardware and features each vendor provided.
                </p>
                <button
                  onClick={handleTriggerProcess}
                  disabled={processingTriggering || isPipelineRunning}
                  className="btn btn-primary"
                  style={{ padding: '0.6rem 1.5rem', fontWeight: 600, backgroundColor: '#10b981', borderColor: '#10b981' }}
                >
                  <Sparkles size={16} />
                  {processingTriggering ? 'Extracting Vendor Offerings...' : 'Extract Vendor Offerings Now'}
                </button>
              </div>
            ) : (
              <div>
                {/* Vendor Selector Pill Bar */}
                <div style={{ display: 'flex', gap: '8px', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
                  {vendorProducts.map((vp, idx) => {
                    const isSelected = selectedVendorProduct?.id === vp.id;
                    const vName = vp.vendor_name || vp.raw_text || vp.product_name;
                    return (
                      <button
                        key={vp.id || idx}
                        onClick={() => setSelectedVendorProduct(vp)}
                        style={{
                          padding: '8px 16px',
                          borderRadius: '8px',
                          border: isSelected ? '1px solid #10b981' : '1px solid var(--border-color)',
                          backgroundColor: isSelected ? 'rgba(16, 185, 129, 0.15)' : 'var(--bg-surface)',
                          color: isSelected ? '#10b981' : 'var(--text-secondary)',
                          fontWeight: 600,
                          fontSize: '0.875rem',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '6px',
                        }}
                      >
                        <Building2 size={14} />
                        <span>{vName}</span>
                        <span
                          style={{
                            fontSize: '0.75rem',
                            padding: '2px 6px',
                            borderRadius: '10px',
                            backgroundColor: isSelected ? '#10b981' : 'var(--bg-surface-hover)',
                            color: isSelected ? '#ffffff' : 'var(--text-muted)',
                          }}
                        >
                          {vp.specifications?.length || 0}
                        </span>
                      </button>
                    );
                  })}
                </div>

                {selectedVendorProduct && (
                  <div
                    style={{
                      background: '#ffffff',
                      borderRadius: '12px',
                      border: '1px solid var(--border-color)',
                      overflow: 'hidden',
                      boxShadow: 'var(--shadow-sm)',
                    }}
                  >
                    <div
                      style={{
                        padding: '1rem 1.25rem',
                        borderBottom: '1px solid var(--border-color)',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        backgroundColor: 'var(--table-header-bg)',
                        flexWrap: 'wrap',
                        gap: '0.5rem',
                      }}
                    >
                      <div>
                        <h4 style={{ margin: 0, fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                          {selectedVendorProduct.vendor_name ? `${selectedVendorProduct.vendor_name}: ` : ''}
                          {selectedVendorProduct.product_name}
                        </h4>
                        <p style={{ margin: '2px 0 0', fontSize: '0.775rem', color: 'var(--text-muted)' }}>
                          Source: {selectedVendorProduct.vendor_file_name || 'Vendor Proposal PDF'}
                        </p>
                      </div>
                      <span className="badge badge-match" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <ShieldCheck size={12} /> {selectedVendorProduct.specifications?.length || 0} Attributes Extracted
                      </span>
                    </div>

                    <div style={{ overflowX: 'auto' }}>
                      <table
                        style={{
                          width: '100%',
                          borderCollapse: 'collapse',
                          fontSize: '0.875rem',
                          textAlign: 'left',
                        }}
                      >
                        <thead>
                          <tr
                            style={{
                              borderBottom: '1px solid var(--border-color)',
                              backgroundColor: 'var(--table-header-bg)',
                              color: 'var(--text-secondary)',
                            }}
                          >
                            <th style={{ padding: '12px 16px', fontWeight: 600 }}>Raw Spec Name</th>
                            <th style={{ padding: '12px 16px', fontWeight: 600 }}>Canonical Key</th>
                            <th style={{ padding: '12px 16px', fontWeight: 600 }}>Provided Value</th>
                            <th style={{ padding: '12px 16px', fontWeight: 600 }}>Unit</th>
                            <th style={{ padding: '12px 16px', fontWeight: 600 }}>Extraction Confidence</th>
                          </tr>
                        </thead>
                        <tbody>
                          {(selectedVendorProduct.specifications || []).map((spec, index) => (
                            <tr
                              key={spec.id || index}
                              style={{
                                borderBottom:
                                  index < selectedVendorProduct.specifications.length - 1
                                    ? '1px solid var(--border-color)'
                                    : 'none',
                              }}
                            >
                              <td style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                                {spec.raw_spec_name}
                              </td>
                              <td style={{ padding: '12px 16px', fontFamily: 'monospace', color: 'var(--accent-primary)', fontWeight: 600 }}>
                                {spec.canonical_spec_name || '—'}
                              </td>
                              <td style={{ padding: '12px 16px', fontWeight: 600, color: '#0f172a' }}>
                                {spec.spec_value}
                              </td>
                              <td style={{ padding: '12px 16px', color: 'var(--text-muted)' }}>
                                {spec.unit || '—'}
                              </td>
                              <td style={{ padding: '12px 16px' }}>
                                <SpecConfidenceBadge level={spec.extraction_confidence || 'high'} />
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Grid: Requirement vs Vendor Submissions Document Overview */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
          gap: '1.5rem',
          marginBottom: '2rem',
        }}
      >
        {/* Requirement Document Card */}
        <div className="card">
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.65rem',
              marginBottom: '1rem',
              paddingBottom: '0.75rem',
              borderBottom: '1px solid var(--bg-card-border)',
            }}
          >
            <div
              style={{
                background: 'rgba(59, 130, 246, 0.15)',
                color: 'var(--accent-primary)',
                padding: '0.5rem',
                borderRadius: 'var(--radius-md)',
              }}
            >
              <FileText size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                Requirement Document
              </h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Master tender specification PDF
              </p>
            </div>
          </div>

          {tender.requirementFileName ? (
            <div
              style={{
                padding: '1rem',
                background: 'var(--bg-surface)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--bg-card-border)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                  overflow: 'hidden',
                }}
              >
                <FileText size={24} style={{ color: 'var(--accent-primary)', flexShrink: 0 }} />
                <div style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  <div
                    style={{
                      fontWeight: 600,
                      color: 'var(--text-primary)',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                    }}
                  >
                    {tender.requirementFileName}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    {formatFileSize(tender.requirementFileSize)} • Stored in MinIO
                  </div>
                </div>
              </div>
              <span className="badge badge-match">Uploaded</span>
            </div>
          ) : (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              No requirement file uploaded.
            </p>
          )}
        </div>

        {/* Vendor Bids Card */}
        <div className="card">
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.65rem',
              marginBottom: '1rem',
              paddingBottom: '0.75rem',
              borderBottom: '1px solid var(--bg-card-border)',
            }}
          >
            <div
              style={{
                background: 'rgba(6, 182, 212, 0.15)',
                color: 'var(--accent-cyan)',
                padding: '0.5rem',
                borderRadius: 'var(--radius-md)',
              }}
            >
              <Users size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                Vendor Bid Documents ({tender.vendorSubmissions?.length || 0})
              </h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Responses evaluated against RFP requirements
              </p>
            </div>
          </div>

          {tender.vendorSubmissions && tender.vendorSubmissions.length > 0 ? (
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                gap: '0.65rem',
                maxHeight: 300,
                overflowY: 'auto',
              }}
            >
              {tender.vendorSubmissions.map((sub, idx) => (
                <div
                  key={sub.id || idx}
                  style={{
                    padding: '0.85rem 1rem',
                    background: 'var(--bg-surface)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--bg-card-border)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.65rem',
                      overflow: 'hidden',
                    }}
                  >
                    <div
                      style={{
                        width: 28,
                        height: 28,
                        borderRadius: '50%',
                        background: 'rgba(6, 182, 212, 0.15)',
                        color: 'var(--accent-cyan)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '0.8rem',
                        fontWeight: 700,
                        flexShrink: 0,
                      }}
                    >
                      {idx + 1}
                    </div>
                    <div style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      <div
                        style={{
                          fontWeight: 600,
                          color: 'var(--text-primary)',
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          fontSize: '0.9rem',
                        }}
                      >
                        {sub.vendorName || sub.fileName}
                      </div>
                      <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
                        {sub.fileName} • {formatFileSize(sub.fileSize)}
                      </div>
                    </div>
                  </div>

                  {getStatusBadge(sub.status)}
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              No vendor bid files uploaded.
            </p>
          )}
        </div>
      </div>
    </div>
  );
};
