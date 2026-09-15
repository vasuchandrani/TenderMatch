import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ComparisonMatrix from '../ComparisonMatrix';

describe('ComparisonMatrix', () => {
  const mockMatrix = {
    tenderId: 'tender-1',
    tenderTitle: 'Laptop Procurement',
    vendorNames: ['Dell Technologies', 'HP Inc.'],
    rows: [
      {
        tenderSpecId: 'spec-1',
        requiredSpecName: 'RAM Capacity',
        canonicalSpecName: 'ram_capacity',
        requiredValue: '16',
        requiredUnit: 'GB',
        comparisonOperator: '>=',
        weight: 2.0,
        weightCategory: 'high',
        vendorCells: [
          {
            vendorSubmissionId: 'v-1',
            vendorName: 'Dell Technologies',
            offeredValue: '32',
            offeredUnit: 'GB',
            systemVerdict: 'Match',
            humanVerdict: 'Match',
            confidenceScore: 0.95,
            cellScore: 1.0,
            weight: 2.0,
          },
          {
            vendorSubmissionId: 'v-2',
            vendorName: 'HP Inc.',
            offeredValue: '8',
            offeredUnit: 'GB',
            systemVerdict: 'Unmatch',
            humanVerdict: 'Unmatch',
            confidenceScore: 0.95,
            cellScore: 0.0,
            weight: 2.0,
          },
        ],
      },
    ],
  };

  it('renders vendors as columns and specs as rows', () => {
    render(<ComparisonMatrix matrix={mockMatrix} />);

    expect(screen.getByText('Dell Technologies')).toBeInTheDocument();
    expect(screen.getByText('HP Inc.')).toBeInTheDocument();
    expect(screen.getByText('RAM Capacity')).toBeInTheDocument();
    expect(screen.getByText(/Min 16 GB/i)).toBeInTheDocument();
  });

  it('renders color-coded cells with offered values and verdicts', () => {
    render(<ComparisonMatrix matrix={mockMatrix} />);

    expect(screen.getByText('32 GB')).toBeInTheDocument();
    expect(screen.getByText('8 GB')).toBeInTheDocument();
    expect(screen.getByText('Match')).toBeInTheDocument();
    expect(screen.getByText('Unmatch')).toBeInTheDocument();
  });
});
