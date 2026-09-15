import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import ShadowModeStats from '../ShadowModeStats';

describe('ShadowModeStats', () => {
  const mockStats = {
    modelName: 'confidence_model',
    totalPredictions: 120,
    agreementCount: 111,
    disagreementCount: 9,
    agreementRate: 0.925,
    disagreements: [
      {
        inputHash: 'hash-abc',
        ruleResult: 'Match',
        mlResult: 'Unmatch',
        inputSample: 'Spec: Storage 512GB vs 256GB',
      },
    ],
  };

  it('renders agreement rate and disagreement details', () => {
    render(<ShadowModeStats stats={mockStats} onClose={vi.fn()} />);

    expect(screen.getByText(/Shadow Mode Agreement Evaluation/i)).toBeInTheDocument();
    expect(screen.getByText('92.5%')).toBeInTheDocument();
    expect(screen.getByText('111')).toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
    expect(screen.getByText(/Storage 512GB vs 256GB/)).toBeInTheDocument();
  });
});
