import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ModelVersionHistory from '../ModelVersionHistory';

describe('ModelVersionHistory', () => {
  const mockVersions = [
    {
      modelName: 'confidence_model',
      version: 2,
      status: 'shadow',
      primaryMetric: 'cv_auc_mean',
      primaryMetricValue: 0.85,
      trainingDataCount: 650,
      trainedAt: '2026-03-02T10:00:00Z',
    },
    {
      modelName: 'confidence_model',
      version: 1,
      status: 'promoted',
      primaryMetric: 'cv_auc_mean',
      primaryMetricValue: 0.82,
      trainingDataCount: 500,
      trainedAt: '2026-03-01T10:00:00Z',
    },
  ];

  it('renders version history rows and promotes version', () => {
    const handlePromote = vi.fn();
    render(
      <ModelVersionHistory
        modelName="confidence_model"
        versions={mockVersions}
        onPromote={handlePromote}
        onRollback={vi.fn()}
        onClose={vi.fn()}
      />
    );

    expect(screen.getByText(/Version History: confidence_model/i)).toBeInTheDocument();
    expect(screen.getByText('v2')).toBeInTheDocument();
    expect(screen.getByText('v1')).toBeInTheDocument();

    const promoteBtn = screen.getByRole('button', { name: /Promote to Production/i });
    fireEvent.click(promoteBtn);

    expect(handlePromote).toHaveBeenCalledWith('confidence_model', 2);
  });
});
