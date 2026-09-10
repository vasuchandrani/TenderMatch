import React, { useState, useEffect } from 'react';
import { adminApi } from '../services/api';
import ModelCard from '../components/admin/ModelCard';
import TrainingDataStats from '../components/admin/TrainingDataStats';
import ShadowModeStats from '../components/admin/ShadowModeStats';
import ModelVersionHistory from '../components/admin/ModelVersionHistory';
import { Cpu, RefreshCw, AlertCircle, CheckCircle2 } from 'lucide-react';

export const AdminModelDashboard = () => {
  const [models, setModels] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Active overlays/subviews
  const [selectedShadowModel, setSelectedShadowModel] = useState(null);
  const [shadowStats, setShadowStats] = useState(null);
  const [selectedHistoryModel, setSelectedHistoryModel] = useState(null);
  const [modelVersions, setModelVersions] = useState([]);
  const [retrainingModel, setRetrainingModel] = useState(null);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      const [modelsRes, statsRes] = await Promise.allSettled([
        adminApi.getModels(),
        adminApi.getTrainingDataStats(),
      ]);

      if (modelsRes.status === 'fulfilled' && modelsRes.value) {
        // Support array directly or ApiResponse envelope
        const data = modelsRes.value.data || modelsRes.value;
        setModels(Array.isArray(data) ? data : []);
      }

      if (statsRes.status === 'fulfilled' && statsRes.value) {
        const sData = statsRes.value.data || statsRes.value;
        setStats(sData);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load model registry data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const handleSelectShadow = async (modelName) => {
    try {
      setSelectedShadowModel(modelName);
      setSelectedHistoryModel(null);
      const res = await adminApi.getShadowStats(modelName);
      const sData = res?.data || res;
      setShadowStats(sData);
    } catch (err) {
      alert('Failed to load shadow statistics: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleSelectVersions = async (modelName) => {
    try {
      setSelectedHistoryModel(modelName);
      setSelectedShadowModel(null);
      const res = await adminApi.getModelVersions(modelName);
      const vData = res?.data || res;
      setModelVersions(Array.isArray(vData) ? vData : []);
    } catch (err) {
      alert('Failed to load version history: ' + (err.response?.data?.message || err.message));
    }
  };

  const handlePromote = async (modelName, version) => {
    try {
      setError(null);
      const res = await adminApi.promoteModel(modelName, version);
      setSuccessMessage(`Model ${modelName} v${version} promoted to active serving.`);
      setTimeout(() => setSuccessMessage(null), 5000);
      await fetchDashboardData();
      if (selectedHistoryModel === modelName) {
        await handleSelectVersions(modelName);
      }
    } catch (err) {
      alert('Promotion failed: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleRollback = async (modelName, version) => {
    try {
      setError(null);
      const res = await adminApi.rollbackModel(modelName, version);
      setSuccessMessage(`Model ${modelName} rolled back to shadow mode.`);
      setTimeout(() => setSuccessMessage(null), 5000);
      await fetchDashboardData();
      if (selectedHistoryModel === modelName) {
        await handleSelectVersions(modelName);
      }
    } catch (err) {
      alert('Rollback failed: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleRetrain = async (modelName) => {
    try {
      setRetrainingModel(modelName);
      setError(null);
      const res = await adminApi.triggerRetrain(modelName);
      const trained = res?.data || res;
      setSuccessMessage(`Successfully retrained ${modelName} to v${trained.version || 'new'}!`);
      setTimeout(() => setSuccessMessage(null), 5000);
      await fetchDashboardData();
    } catch (err) {
      alert('Retraining failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setRetrainingModel(null);
    }
  };

  return (
    <div className="main-content" style={{ maxWidth: '1280px' }}>
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
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Cpu size={24} style={{ color: 'var(--accent-primary)' }} />
            <h1 style={{ margin: 0, fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              AI Active Learning & Model Management
            </h1>
          </div>
          <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Monitor on-premise ML pipelines, audit shadow mode agreement, and govern continuous learning checkpoints.
          </p>
        </div>

        <button
          type="button"
          disabled={loading}
          onClick={fetchDashboardData}
          className="btn btn-secondary"
          style={{ fontSize: '0.85rem', padding: '0.45rem 0.95rem' }}
        >
          <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Success Banner */}
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

      {/* Error Banner */}
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

      {/* Training Data Stats Component */}
      <TrainingDataStats stats={stats} />

      {/* Shadow Mode Stats Detail Overlay */}
      {selectedShadowModel && shadowStats && (
        <ShadowModeStats
          stats={shadowStats}
          onClose={() => {
            setSelectedShadowModel(null);
            setShadowStats(null);
          }}
        />
      )}

      {/* Version History Detail Overlay */}
      {selectedHistoryModel && (
        <ModelVersionHistory
          modelName={selectedHistoryModel}
          versions={modelVersions}
          onPromote={handlePromote}
          onRollback={handleRollback}
          onClose={() => {
            setSelectedHistoryModel(null);
            setModelVersions([]);
          }}
        />
      )}

      {/* Model Cards Grid */}
      <div style={{ marginBottom: '1rem' }}>
        <h2 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
          Registered AI/ML Models
        </h2>
        <p style={{ margin: '0 0 1.25rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          Each model is versioned independently and guarded by shadow mode evaluation prior to production promotion.
        </p>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: '1.25rem',
        }}
      >
        {models.map((model) => (
          <ModelCard
            key={model.modelName}
            model={model}
            onSelectVersions={handleSelectVersions}
            onSelectShadow={handleSelectShadow}
            onRetrain={handleRetrain}
            isRetraining={retrainingModel === model.modelName}
          />
        ))}
      </div>
    </div>
  );
};

export default AdminModelDashboard;
