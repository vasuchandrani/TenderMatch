import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ModelCard from '../ModelCard';

describe('ModelCard', () => {
  const mockModel = {
    modelName: 'confidence_model',
    version: 2,
    status: 'shadow',
    modelType: 'GradientBoostingClassifier',
    primaryMetric: 'cv_auc_mean',
    primaryMetricValue: 0.854,
    trainingDataCount: 650,
    trainedAt: '2026-03-01T10:00:00Z',
    metrics: { cv_auc_mean: 0.854, train_accuracy: 0.91 },
  };

  it('renders model name, version, and status badge', () => {
    render(<ModelCard model={mockModel} />);

    expect(screen.getByText('Confidence Meta-Model')).toBeInTheDocument();
    expect(screen.getByText('v2')).toBeInTheDocument();
    expect(screen.getByText('SHADOW')).toBeInTheDocument();
    expect(screen.getByText(/GradientBoostingClassifier/i)).toBeInTheDocument();
    expect(screen.getByText(/85\.4%/)).toBeInTheDocument();
  });

  it('triggers onSelectVersions when View History is clicked', () => {
    const handleSelect = vi.fn();
    render(<ModelCard model={mockModel} onSelectVersions={handleSelect} />);

    fireEvent.click(screen.getByRole('button', { name: /History/i }));
    expect(handleSelect).toHaveBeenCalledWith('confidence_model');
  });

  it('triggers onSelectShadow when Shadow Stats is clicked', () => {
    const handleShadow = vi.fn();
    render(<ModelCard model={mockModel} onSelectShadow={handleShadow} />);

    fireEvent.click(screen.getByRole('button', { name: /Shadow/i }));
    expect(handleShadow).toHaveBeenCalledWith('confidence_model');
  });
});
