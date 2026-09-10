import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import AdminModelDashboard from '../AdminModelDashboard';
import { adminApi } from '../../services/api';

vi.mock('../../services/api', () => ({
  adminApi: {
    getModels: vi.fn(),
    getTrainingDataStats: vi.fn(),
    checkRetrain: vi.fn(),
    getModelVersions: vi.fn(),
    getShadowStats: vi.fn(),
    promoteModel: vi.fn(),
    rollbackModel: vi.fn(),
    triggerRetrain: vi.fn(),
  },
}));

describe('AdminModelDashboard', () => {
  const mockModels = [
    {
      modelName: 'confidence_model',
      version: 1,
      status: 'shadow',
      modelType: 'GradientBoostingClassifier',
      primaryMetric: 'cv_auc_mean',
      primaryMetricValue: 0.824,
      trainingDataCount: 600,
      trainedAt: '2026-03-01T10:00:00Z',
    },
    {
      modelName: 'operator_model',
      version: 1,
      status: 'promoted',
      modelType: 'DecisionTreeClassifier',
      primaryMetric: 'cv_accuracy_mean',
      primaryMetricValue: 0.912,
      trainingDataCount: 550,
      trainedAt: '2026-03-01T10:00:00Z',
    },
  ];

  const mockStats = {
    totalCorrectionLogs: 650,
    totalOverrides: 85,
    totalAutoConfirmed: 420,
    totalMatchResults: 1200,
    totalTenderOutcomes: 210,
    confidenceModelReady: true,
    operatorModelReady: true,
    rankingModelReady: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    adminApi.getModels.mockResolvedValue({ success: true, data: mockModels });
    adminApi.getTrainingDataStats.mockResolvedValue({ success: true, data: mockStats });
    adminApi.checkRetrain.mockResolvedValue({ success: true, data: [] });
  });

  const renderComponent = () =>
    render(
      <BrowserRouter>
        <AdminModelDashboard />
      </BrowserRouter>
    );

  it('renders dashboard with models and training data readiness stats', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('AI Active Learning & Model Management')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getAllByText('Confidence Meta-Model').length).toBeGreaterThan(0);
    });

    expect(screen.getByText('Operator Inference Model')).toBeInTheDocument();
    expect(screen.getByText('650')).toBeInTheDocument();

    expect(adminApi.getModels).toHaveBeenCalled();
    expect(adminApi.getTrainingDataStats).toHaveBeenCalled();
  });

  it('triggers retrain when retrain is clicked on a model card', async () => {
    adminApi.triggerRetrain.mockResolvedValue({
      success: true,
      data: {
        modelName: 'confidence_model',
        version: 2,
        status: 'trained',
        primaryMetric: 'cv_auc_mean',
        primaryMetricValue: 0.86,
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByRole('button', { name: /Retrain/i }).length).toBeGreaterThan(0);
    });

    const retrainBtns = screen.getAllByRole('button', { name: /Retrain/i });
    fireEvent.click(retrainBtns[0]);

    await waitFor(() => {
      expect(adminApi.triggerRetrain).toHaveBeenCalledWith('confidence_model');
    });
  });
});
