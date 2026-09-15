import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import TrainingDataStats from '../TrainingDataStats';

describe('TrainingDataStats', () => {
  const mockStats = {
    totalCorrectionLogs: 620,
    totalOverrides: 78,
    totalAutoConfirmed: 450,
    totalMatchResults: 1300,
    totalTenderOutcomes: 210,
    confidenceModelReady: true,
    operatorModelReady: true,
    rankingModelReady: true,
  };

  it('renders stats counters and readiness badges', () => {
    render(<TrainingDataStats stats={mockStats} />);

    expect(screen.getByText('Training Data Readiness & Feedback Loop')).toBeInTheDocument();
    expect(screen.getByText('620')).toBeInTheDocument();
    expect(screen.getByText('78')).toBeInTheDocument();
    expect(screen.getByText('210')).toBeInTheDocument();
    expect(screen.getAllByText('Ready for Retraining').length).toBeGreaterThan(0);
  });
});
