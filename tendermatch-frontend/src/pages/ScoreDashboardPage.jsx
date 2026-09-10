import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { tendersApi } from '../services/api';
import RankingTable from '../components/scoring/RankingTable';
import ScoreGauge from '../components/scoring/ScoreGauge';
import ExportButtons from '../components/scoring/ExportButtons';
import {
  ArrowLeft,
  Award,
  Sliders,
  Table,
  Calculator,
  RefreshCw,
  AlertCircle,
  Trophy,
  CheckCircle2,
  FileCheck,
} from 'lucide-react';

export const ScoreDashboardPage = () => {
  const { id: tenderId } = useParams();
  const navigate = useNavigate();

  const [scoresData, setScoresData] = useState(null);
  const [outcomeData, setOutcomeData] = useState(null);
  const [matchResults, setMatchResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [calculating, setCalculating] = useState(false);
  const [exportingPdf, setExportingPdf] = useState(false);
  const [exportingCsv, setExportingCsv] = useState(false);
  const [error, setError] = useState(null);

  // Award Outcome Modal / Form State
  const [showOutcomeModal, setShowOutcomeModal] = useState(false);
  const [selectedWinnerId, setSelectedWinnerId] = useState('');
  const [outcomeNotes, setOutcomeNotes] = useState('');
  const [recordingOutcome, setRecordingOutcome] = useState(false);

  const fetchScoresAndOutcome = async () => {
    try {
      setLoading(true);
      setError(null);

      const [scoresRes, outcomeRes, resultsRes] = await Promise.allSettled([
        tendersApi.getScores(tenderId),
        tendersApi.getOutcome(tenderId),
        tendersApi.getMatchResults(tenderId),
      ]);

      if (scoresRes.status === 'fulfilled' && scoresRes.value?.success) {
        setScoresData(scoresRes.value.data);
      } else {
        setScoresData(null);
      }

      if (outcomeRes.status === 'fulfilled' && outcomeRes.value?.success) {
        setOutcomeData(outcomeRes.value.data);
      } else {
        setOutcomeData(null);
      }

      if (resultsRes.status === 'fulfilled' && resultsRes.value?.success) {
        setMatchResults(resultsRes.value.data || []);
      } else {
        setMatchResults([]);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch scores data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchScoresAndOutcome();
  }, [tenderId]);

  const handleCalculateScores = async () => {
    try {
      setCalculating(true);
      setError(null);
      const res = await tendersApi.calculateScores(tenderId);
      if (res?.success && res?.data) {
        setScoresData(res.data);
        try {
          const resultsRes = await tendersApi.getMatchResults(tenderId);
          if (resultsRes?.success && resultsRes?.data) {
            setMatchResults(resultsRes.data);
          }
        } catch (_) {}
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error calculating scores');
    } finally {
      setCalculating(false);
    }
  };

  const handleExportPdf = async () => {
    try {
      setExportingPdf(true);
      const blob = await tendersApi.exportPdf(tenderId);
      const url = window.URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `tender-${tenderId}-evaluation.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('Failed to export PDF: ' + (err.response?.data?.message || err.message));
    } finally {
      setExportingPdf(false);
    }
  };

  const handleExportCsv = async () => {
    try {
      setExportingCsv(true);
      const blob = await tendersApi.exportCsv(tenderId);
      const url = window.URL.createObjectURL(new Blob([blob], { type: 'text/csv;charset=utf-8;' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `tender-${tenderId}-evaluation.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('Failed to export CSV: ' + (err.response?.data?.message || err.message));
    } finally {
      setExportingCsv(false);
    }
  };

  const handleRecordOutcome = async () => {
    if (!selectedWinnerId) return;
    try {
      setRecordingOutcome(true);
      const res = await tendersApi.recordOutcome(tenderId, selectedWinnerId, outcomeNotes);
      if (res?.success && res?.data) {
        setOutcomeData(res.data);
        setShowOutcomeModal(false);
      }
    } catch (err) {
      alert('Failed to record award outcome: ' + (err.response?.data?.message || err.message));
    } finally {
      setRecordingOutcome(false);
    }
  };

  const scores = scoresData?.scores || [];
  const topVendor = scores.length > 0 ? scores[0] : null;

  return (
    <div className="main-content" style={{ maxWidth: '1280px' }}>
      {/* Navigation & Header */}
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
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Link
            to={`/tenders/${tenderId}`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <ArrowLeft size={16} /> Tender Overview
          </Link>

          <Link
            to={`/tenders/${tenderId}/matrix`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Table size={16} /> Comparison Matrix
          </Link>

          <Link
            to={`/tenders/${tenderId}/weights`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Sliders size={16} /> Configure Weights
          </Link>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            type="button"
            disabled={calculating}
            onClick={handleCalculateScores}
            className="btn btn-primary"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.85rem',
              padding: '0.45rem 1rem',
              backgroundColor: '#6366f1',
              borderColor: '#6366f1',
            }}
          >
            <Calculator size={16} />
            <span>{calculating ? 'Calculating...' : 'Recalculate Scores'}</span>
          </button>

          <ExportButtons
            onExportPdf={handleExportPdf}
            onExportCsv={handleExportCsv}
            isExportingPdf={exportingPdf}
            isExportingCsv={exportingCsv}
          />
        </div>
      </div>

      {error && (
        <div
          className="card"
          style={{
            marginBottom: '1.5rem',
            padding: '1rem 1.25rem',
            borderColor: 'var(--status-unmatch-border)',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            color: 'var(--status-unmatch)',
          }}
        >
          <AlertCircle size={18} />
          <span style={{ fontSize: '0.9rem' }}>{error}</span>
        </div>
      )}

      {/* Hero Banner: Award Winner or Top Ranker */}
      {topVendor && (
        <div
          className="card"
          style={{
            marginBottom: '1.5rem',
            background: 'linear-gradient(135deg, #fffbeb 0%, #ffffff 60%, #f8fafc 100%)',
            border: '1px solid rgba(245, 158, 11, 0.3)',
            borderRadius: '16px',
            padding: '1.75rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1.5rem',
            boxShadow: '0 4px 14px rgba(245, 158, 11, 0.08), 0 1px 3px rgba(0, 0, 0, 0.03)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
            <div
              style={{
                width: 64,
                height: 64,
                borderRadius: '16px',
                backgroundColor: '#fef3c7',
                border: '1px solid #fde68a',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#d97706',
              }}
            >
              <Trophy size={32} />
            </div>

            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                <span
                  style={{
                    textTransform: 'uppercase',
                    fontSize: '0.75rem',
                    fontWeight: 800,
                    letterSpacing: '0.05em',
                    color: 'var(--status-pending)',
                  }}
                >
                  Leaderboard Leader
                </span>
                {outcomeData && outcomeData.winningVendorId === topVendor.vendorSubmissionId && (
                  <span
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '4px',
                      backgroundColor: 'var(--status-match-bg)',
                      color: 'var(--status-match)',
                      border: '1px solid var(--status-match-border)',
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      padding: '2px 8px',
                      borderRadius: '9999px',
                    }}
                  >
                    <CheckCircle2 size={12} /> Award Winner
                  </span>
                )}
              </div>

              <h2 style={{ margin: 0, fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                {topVendor.vendorName}
              </h2>
              <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                Leading with {topVendor.matched} matches out of {topVendor.totalSpecs} specifications evaluated.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', flexWrap: 'wrap' }}>
            <ScoreGauge score={topVendor.overallScore} size={110} strokeWidth={9} label="Leader Score" />

            <div>
              {outcomeData ? (
                <div
                  style={{
                    padding: '10px 16px',
                    borderRadius: '10px',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    border: '1px solid rgba(16, 185, 129, 0.3)',
                  }}
                >
                  <div style={{ fontSize: '0.75rem', color: '#10b981', fontWeight: 700, textTransform: 'uppercase' }}>
                    Tender Awarded To
                  </div>
                  <div style={{ fontSize: '1rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                    {outcomeData.winningVendorName}
                  </div>
                  {outcomeData.outcomeNotes && (
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px', maxWidth: '240px' }}>
                      "{outcomeData.outcomeNotes}"
                    </div>
                  )}
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => {
                    setSelectedWinnerId(topVendor.vendorSubmissionId);
                    setShowOutcomeModal(true);
                  }}
                  className="btn btn-secondary"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    fontSize: '0.85rem',
                    padding: '0.5rem 1rem',
                  }}
                >
                  <Award size={16} style={{ color: '#f59e0b' }} />
                  <span>Record Award Decision</span>
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Main Leaderboard Table */}
      <div className="card" style={{ padding: '1.5rem', borderRadius: '16px' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '1.25rem',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div>
            <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Award size={20} style={{ color: 'var(--accent-primary)' }} />
              Vendor Leaderboard & Overall Rankings
            </h3>
            <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Weighted compliance scores calculated across all canonical tender specifications. Click any vendor to view exact met and unmet feature details.
            </p>
          </div>

          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Total Vendors: <strong style={{ color: 'var(--text-primary)' }}>{scores.length}</strong>
          </div>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
            <RefreshCw size={24} className="animate-spin" style={{ margin: '0 auto 0.75rem' }} />
            <div>Loading leaderboard rankings...</div>
          </div>
        ) : (
          <RankingTable
            scores={scores}
            matchResults={matchResults}
            onSelectVendor={(vendorId) => navigate(`/tenders/${tenderId}/scores/${vendorId}`)}
          />
        )}
      </div>

      {/* Record Award Outcome Modal */}
      {showOutcomeModal && (
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
          <div className="card" style={{ maxWidth: 460, width: '100%', padding: '1.75rem', borderRadius: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '0.5rem' }}>
              <FileCheck size={22} style={{ color: '#10b981' }} />
              <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                Record Tender Award Decision
              </h3>
            </div>
            <p style={{ margin: '0 0 1.25rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Record the winning vendor and award notes for procurement audit trail and analytics.
            </p>

            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '4px' }}>
                Winning Vendor
              </label>
              <select
                value={selectedWinnerId}
                onChange={(e) => setSelectedWinnerId(e.target.value)}
                className="input"
                style={{ width: '100%', padding: '8px 12px' }}
              >
                <option value="">Select winning vendor...</option>
                {scores.map((s) => (
                  <option key={s.vendorSubmissionId} value={s.vendorSubmissionId}>
                    #{s.rank} - {s.vendorName} ({s.overallScore}%)
                  </option>
                ))}
              </select>
            </div>

            <div style={{ marginBottom: '1.25rem' }}>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '4px' }}>
                Outcome & Justification Notes
              </label>
              <textarea
                rows={3}
                placeholder="e.g. Awarded based on highest compliance score and competitive lead time."
                value={outcomeNotes}
                onChange={(e) => setOutcomeNotes(e.target.value)}
                className="input"
                style={{ width: '100%', padding: '8px 12px', resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                onClick={() => setShowOutcomeModal(false)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={!selectedWinnerId || recordingOutcome}
                onClick={handleRecordOutcome}
                className="btn btn-primary"
                style={{ backgroundColor: '#10b981', borderColor: '#10b981' }}
              >
                {recordingOutcome ? 'Recording...' : 'Confirm Award'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ScoreDashboardPage;
