import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import RankingTable from '../RankingTable';

describe('RankingTable', () => {
  const mockScores = [
    {
      vendorScoreId: 'vs-1',
      vendorSubmissionId: 'v-1',
      vendorName: 'Dell Technologies',
      fileName: 'dell.pdf',
      overallScore: 87.5,
      rank: 1,
      totalSpecs: 16,
      matched: 14,
      unmatched: 1,
      uncertain: 1,
      productScores: [
        {
          tenderProductId: 'p-1',
          productName: 'Workstation',
          score: 85.0,
          totalSpecs: 10,
          matched: 9,
          unmatched: 1,
          uncertain: 0,
        },
      ],
    },
    {
      vendorScoreId: 'vs-2',
      vendorSubmissionId: 'v-2',
      vendorName: 'HP Inc.',
      fileName: 'hp.pdf',
      overallScore: 81.3,
      rank: 2,
      totalSpecs: 16,
      matched: 13,
      unmatched: 2,
      uncertain: 1,
      productScores: [],
    },
    {
      vendorScoreId: 'vs-3',
      vendorSubmissionId: 'v-3',
      vendorName: 'Lenovo',
      fileName: 'lenovo.pdf',
      overallScore: 45.0,
      rank: 3,
      totalSpecs: 16,
      matched: 7,
      unmatched: 8,
      uncertain: 1,
      productScores: [],
    },
  ];

  it('renders vendors sorted by rank with medal icons for top 3', () => {
    render(<RankingTable scores={mockScores} onSelectVendor={vi.fn()} />);

    expect(screen.getByText('Dell Technologies')).toBeInTheDocument();
    expect(screen.getByText('HP Inc.')).toBeInTheDocument();
    expect(screen.getByText('Lenovo')).toBeInTheDocument();

    // Medals
    expect(screen.getByText('🥇')).toBeInTheDocument();
    expect(screen.getByText('🥈')).toBeInTheDocument();
    expect(screen.getByText('🥉')).toBeInTheDocument();
  });

  it('colors score values based on threshold', () => {
    render(<RankingTable scores={mockScores} onSelectVendor={vi.fn()} />);

    const dellScore = screen.getByText('87.5%');
    expect(dellScore).toHaveStyle({ color: 'rgb(16, 185, 129)' }); // green

    const lenovoScore = screen.getByText('45%');
    expect(lenovoScore).toHaveStyle({ color: 'rgb(239, 68, 68)' }); // red
  });

  it('expands row to show product breakdown when clicked', () => {
    render(<RankingTable scores={mockScores} onSelectVendor={vi.fn()} />);

    // Expand Dell row
    const expandBtn = screen.getByTestId('expand-vs-1');
    fireEvent.click(expandBtn);

    expect(screen.getByText('Workstation')).toBeInTheDocument();
    expect(screen.getByText('85%')).toBeInTheDocument();
  });
});
