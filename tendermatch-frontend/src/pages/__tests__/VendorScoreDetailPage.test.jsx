import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { VendorScoreDetailPage } from '../VendorScoreDetailPage';
import { tendersApi } from '../../services/api';

vi.mock('../../services/api', () => ({
  tendersApi: {
    getVendorScore: vi.fn(),
    getVendorResults: vi.fn(),
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'tender-123', vendorId: 'vendor-456' }),
  };
});

describe('VendorScoreDetailPage', () => {
  const mockScoreData = {
    vendorScoreId: 'vs-1',
    vendorSubmissionId: 'vendor-456',
    vendorName: 'SuperServer Ltd',
    fileName: 'superserver_proposal.pdf',
    overallScore: 88.0,
    rank: 1,
    totalSpecs: 12,
    matched: 10,
    unmatched: 2,
    uncertain: 0,
    isReviewLocked: true,
    scoringMethod: 'WEIGHTED_AVERAGE',
    scoredAt: '2026-03-01T10:00:00Z',
    productScores: [
      {
        tenderProductId: 'p-1',
        productName: 'Storage Node',
        score: 85.0,
        totalSpecs: 6,
        matched: 5,
        unmatched: 1,
        uncertain: 0,
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    tendersApi.getVendorScore.mockResolvedValue({
      success: true,
      data: mockScoreData,
    });
    tendersApi.getVendorResults.mockResolvedValue({
      success: true,
      data: [],
    });
  });

  const renderComponent = () =>
    render(
      <BrowserRouter>
        <VendorScoreDetailPage />
      </BrowserRouter>
    );

  it('renders vendor score details and product breakdown', async () => {
    tendersApi.getVendorScore.mockResolvedValue({
      success: true,
      data: mockScoreData,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('SuperServer Ltd')).toBeInTheDocument();
      expect(screen.getByText(/Rank #1/)).toBeInTheDocument();
      expect(screen.getByText('superserver_proposal.pdf', { exact: false })).toBeInTheDocument();
      expect(screen.getByText('Finalized')).toBeInTheDocument();
      expect(screen.getByText('Storage Node')).toBeInTheDocument();
    });

    expect(screen.getByText('88%')).toBeInTheDocument();
    expect(screen.getByText('85%')).toBeInTheDocument();
    expect(tendersApi.getVendorScore).toHaveBeenCalledWith('tender-123', 'vendor-456');
  });

  it('renders error state when score fetching fails', async () => {
    tendersApi.getVendorScore.mockRejectedValue(new Error('Vendor score not found'));

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Error fetching vendor score\./i)).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Back to Leaderboard/i })).toBeInTheDocument();
    });
  });
});
