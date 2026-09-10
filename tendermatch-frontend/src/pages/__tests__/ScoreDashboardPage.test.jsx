import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ScoreDashboardPage } from '../ScoreDashboardPage';
import { tendersApi } from '../../services/api';

vi.mock('../../services/api', () => ({
  tendersApi: {
    getScores: vi.fn(),
    calculateScores: vi.fn(),
    getOutcome: vi.fn(),
    recordOutcome: vi.fn(),
    getMatchResults: vi.fn(),
    exportPdf: vi.fn(),
    exportCsv: vi.fn(),
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'tender-123' }),
  };
});

describe('ScoreDashboardPage', () => {
  const mockScoresData = {
    tenderId: 'tender-123',
    scores: [
      {
        vendorScoreId: 'vs-1',
        vendorSubmissionId: 'sub-1',
        vendorName: 'Acme Systems',
        fileName: 'acme_bid.pdf',
        overallScore: 92.5,
        rank: 1,
        totalSpecs: 10,
        matched: 9,
        unmatched: 1,
        uncertain: 0,
        isReviewLocked: false,
        productScores: [],
      },
      {
        vendorScoreId: 'vs-2',
        vendorSubmissionId: 'sub-2',
        vendorName: 'Beta Tech',
        fileName: 'beta_bid.pdf',
        overallScore: 78.0,
        rank: 2,
        totalSpecs: 10,
        matched: 7,
        unmatched: 2,
        uncertain: 1,
        isReviewLocked: false,
        productScores: [],
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    tendersApi.getScores.mockResolvedValue({ success: true, data: mockScoresData });
    tendersApi.getOutcome.mockResolvedValue({ success: true, data: null });
    tendersApi.getMatchResults.mockResolvedValue({ success: true, data: [] });
  });

  const renderComponent = () =>
    render(
      <BrowserRouter>
        <ScoreDashboardPage />
      </BrowserRouter>
    );

  it('renders leaderboard and scores table successfully', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Vendor Leaderboard & Overall Rankings')).toBeInTheDocument();
      expect(screen.getByText('Leaderboard Leader')).toBeInTheDocument();
      expect(screen.getAllByText('Acme Systems').length).toBeGreaterThan(0);
      expect(screen.getByText('Beta Tech')).toBeInTheDocument();
    });

    expect(screen.getAllByText('92.5%').length).toBeGreaterThan(0);
    expect(screen.getByText('78%')).toBeInTheDocument();
  });

  it('calculates scores when Recalculate Scores button is clicked', async () => {
    tendersApi.calculateScores.mockResolvedValue({
      success: true,
      data: mockScoresData,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Recalculate Scores/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Recalculate Scores/i }));

    await waitFor(() => {
      expect(tendersApi.calculateScores).toHaveBeenCalledWith('tender-123');
    });
  });

  it('records award outcome when decision is submitted', async () => {
    tendersApi.recordOutcome.mockResolvedValue({
      success: true,
      data: {
        winningVendorId: 'sub-1',
        winningVendorName: 'Acme Systems',
        outcomeNotes: 'Best overall performance',
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Record Award Decision/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Record Award Decision/i }));

    expect(screen.getByText('Record Tender Award Decision')).toBeInTheDocument();

    const noteInput = screen.getByPlaceholderText(/e\.g\. Awarded based on highest compliance/i);
    fireEvent.change(noteInput, { target: { value: 'Best overall performance' } });

    const confirmBtn = screen.getByRole('button', { name: /Confirm Award/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(tendersApi.recordOutcome).toHaveBeenCalledWith('tender-123', 'sub-1', 'Best overall performance');
    });
  });
});
