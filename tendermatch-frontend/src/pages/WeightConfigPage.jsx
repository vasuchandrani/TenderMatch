import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { tendersApi } from '../services/api';
import WeightEditor from '../components/weights/WeightEditor';
import {
  ArrowLeft,
  Award,
  Table,
  Sliders,
  Bookmark,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
} from 'lucide-react';

export const WeightConfigPage = () => {
  const { id: tenderId } = useParams();

  const [weights, setWeights] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [selectedTemplate, setSelectedTemplate] = useState('');
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isApplyingTemplate, setIsApplyingTemplate] = useState(false);
  const [successMessage, setSuccessMessage] = useState(null);
  const [error, setError] = useState(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);

      const [weightsRes, templatesRes] = await Promise.allSettled([
        tendersApi.getWeights(tenderId),
        tendersApi.getWeightTemplates(),
      ]);

      if (weightsRes.status === 'fulfilled' && weightsRes.value?.success) {
        setWeights(weightsRes.value.data || []);
      }

      if (templatesRes.status === 'fulfilled' && templatesRes.value?.success) {
        setTemplates(templatesRes.value.data || []);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load weight settings.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [tenderId]);

  const handleSaveWeights = async (updatedWeights) => {
    try {
      setIsSaving(true);
      setError(null);
      setSuccessMessage(null);
      const res = await tendersApi.updateWeights(tenderId, updatedWeights);
      if (res?.success && res?.data) {
        setWeights(res.data);
        setSuccessMessage('Specification weights updated successfully. Remember to recalculate scores!');
        setTimeout(() => setSuccessMessage(null), 5000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update weights.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleSaveTemplate = async (templateName, templateWeights) => {
    try {
      setError(null);
      const res = await tendersApi.saveWeightTemplate(tenderId, templateName, templateWeights);
      if (res?.success && res?.data) {
        setSuccessMessage(`Weight template "${templateName}" saved successfully.`);
        // Refresh templates
        const tRes = await tendersApi.getWeightTemplates();
        if (tRes?.success) setTemplates(tRes.data || []);
        setTimeout(() => setSuccessMessage(null), 5000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save template.');
    }
  };

  const handleApplyTemplate = async () => {
    if (!selectedTemplate) return;
    try {
      setIsApplyingTemplate(true);
      setError(null);
      setSuccessMessage(null);
      const res = await tendersApi.applyWeightTemplate(tenderId, selectedTemplate);
      if (res?.success && res?.data) {
        setWeights(res.data);
        setSuccessMessage(`Applied template "${selectedTemplate}". Remember to recalculate scores!`);
        setTimeout(() => setSuccessMessage(null), 5000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to apply template.');
    } finally {
      setIsApplyingTemplate(false);
    }
  };

  return (
    <div className="main-content" style={{ maxWidth: '1100px' }}>
      {/* Header Navigation */}
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
            to={`/tenders/${tenderId}/scores`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Award size={16} /> Leaderboard
          </Link>

          <Link
            to={`/tenders/${tenderId}/matrix`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Table size={16} /> Comparison Matrix
          </Link>
        </div>

        <button
          type="button"
          onClick={fetchData}
          className="btn btn-secondary"
          style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem' }}
        >
          <RefreshCw size={15} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Success Notification */}
      {successMessage && (
        <div
          className="card"
          style={{
            marginBottom: '1.5rem',
            padding: '1rem 1.25rem',
            borderColor: 'var(--status-match-border)',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            color: 'var(--status-match)',
            backgroundColor: 'rgba(16, 185, 129, 0.1)',
          }}
        >
          <CheckCircle2 size={18} />
          <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>{successMessage}</span>
        </div>
      )}

      {/* Error Notification */}
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

      {/* Template Quick Loader Card */}
      {templates && templates.length > 0 && (
        <div
          className="card"
          style={{
            marginBottom: '1.5rem',
            padding: '1.25rem 1.5rem',
            borderRadius: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Bookmark size={20} style={{ color: 'var(--accent-primary)' }} />
            <div>
              <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-primary)' }}>
                Apply Preset Weight Template
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Apply standardized specification weighting categories configured across your organization.
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <select
              value={selectedTemplate}
              onChange={(e) => setSelectedTemplate(e.target.value)}
              className="input"
              style={{ fontSize: '0.85rem', padding: '6px 12px', minWidth: '220px' }}
            >
              <option value="">Select a template...</option>
              {templates.map((t) => (
                <option key={t.templateName} value={t.templateName}>
                  {t.templateName} ({t.weights?.length || 0} specs)
                </option>
              ))}
            </select>

            <button
              type="button"
              disabled={!selectedTemplate || isApplyingTemplate}
              onClick={handleApplyTemplate}
              className="btn btn-primary"
              style={{ fontSize: '0.85rem', padding: '0.45rem 0.95rem' }}
            >
              {isApplyingTemplate ? 'Applying...' : 'Apply Template'}
            </button>
          </div>
        </div>
      )}

      {/* Weight Editor Component */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '4rem', color: 'var(--text-muted)' }}>
          <RefreshCw size={28} className="animate-spin" style={{ margin: '0 auto 1rem' }} />
          <div>Loading specification weights...</div>
        </div>
      ) : (
        <WeightEditor
          weights={weights}
          onSave={handleSaveWeights}
          onReset={fetchData}
          onSaveTemplate={handleSaveTemplate}
          isSaving={isSaving}
        />
      )}
    </div>
  );
};

export default WeightConfigPage;
