import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ReviewPage } from '../ReviewPage';
import { tendersApi } from '../../services/api';

vi.mock('../../services/api', () => ({
  tendersApi: {
    getVendorReview: vi.fn(),
    getTenderReview: vi.fn(),
    submitVerdict: vi.fn(),
    bulkConfirmReview: vi.fn(),
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'tender-123', vendorId: 'vendor-456' }),
  };
});

describe('ReviewPage', () => {
  const mockReviewData = {
    vendorId: 'vendor-456',
    vendorName: 'Dell Technologies',
    fileName: 'dell_bid.pdf',
    isReviewLocked: false,
    products: [
      {
        tenderProductId: 'prod-1',
        tenderProductName: 'Precision Workstation',
        vendorProductId: 'vprod-1',
        vendorProductName: 'Dell Precision 3660',
        specPairs: [
          {
            resultId: 'res-101',
            tenderSpecId: 'spec-1',
            requiredSpecName: 'RAM Capacity',
            canonicalName: 'ram_capacity',
            requiredValue: '32',
            requiredUnit: 'GB',
            requiredRawText: 'Minimum 32GB DDR5 RAM',
            vendorSpecId: 'vspec-1',
            offeredSpecName: 'System Memory',
            offeredValue: '64',
            offeredUnit: 'GB',
            offeredRawText: '64GB (2x32GB) DDR5 4800MHz',
            systemVerdict: 'Match',
            confidenceScore: 0.95,
            comparisonMethod: 'numeric_exact',
            comparisonOperator: '>=',
            comparisonDetails: '64 >= 32 GB',
            humanVerdict: null,
            isReviewed: false,
            isOverride: false,
            userComment: '',
          },
        ],
      },
    ],
    progress: {
      tenderId: 'tender-123',
      vendorId: 'vendor-456',
      reviewedCount: 0,
      totalCount: 1,
      percentage: 0.0,
      overrideCount: 0,
      isLocked: false,
    },
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderComponent = () =>
    render(
      <BrowserRouter>
        <ReviewPage />
      </BrowserRouter>
    );

  it('renders loading state initially', () => {
    tendersApi.getVendorReview.mockImplementation(() => new Promise(() => {}));
    renderComponent();

    expect(screen.getByText(/loading review workspace.../i)).toBeInTheDocument();
  });

  it('renders dual review workspace when data is loaded', async () => {
    tendersApi.getVendorReview.mockResolvedValueOnce({
      success: true,
      data: mockReviewData,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('RAM Capacity').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText(/Min 32 GB/i).length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText(/64 GB/i).length).toBeGreaterThanOrEqual(1);
    });

    expect(screen.getAllByText('Precision Workstation').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Dell Technologies').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Spec 1 of 1')).toBeInTheDocument();
    expect(screen.getByText('Tender Requirement')).toBeInTheDocument();
    expect(screen.getByText('Vendor Proposal & Status')).toBeInTheDocument();
    expect(screen.getByText('Dual Review & Evaluation')).toBeInTheDocument();
  });

  it('submits human verdict when Save Decision & Next is clicked', async () => {
    tendersApi.getVendorReview.mockResolvedValueOnce({
      success: true,
      data: mockReviewData,
    });

    tendersApi.submitVerdict.mockResolvedValueOnce({
      success: true,
      data: {
        resultId: 'res-101',
        systemVerdict: 'Match',
        humanVerdict: 'Match',
        isOverride: false,
        userComment: '',
        nextResultId: null,
        reviewProgress: {
          tenderId: 'tender-123',
          vendorId: 'vendor-456',
          reviewedCount: 1,
          totalCount: 1,
          percentage: 100.0,
          overrideCount: 0,
          isLocked: false,
        },
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Save Decision & Next/i })).toBeInTheDocument();
    });

    const saveBtn = screen.getByRole('button', { name: /Save Decision & Next/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(tendersApi.submitVerdict).toHaveBeenCalledWith(
        'tender-123',
        'res-101',
        'Match',
        ''
      );
    });
  });

  it('allows selecting Unmatch in inspector and saving with a comment', async () => {
    tendersApi.getVendorReview.mockResolvedValueOnce({
      success: true,
      data: mockReviewData,
    });

    tendersApi.submitVerdict.mockResolvedValueOnce({
      success: true,
      data: {
        resultId: 'res-101',
        systemVerdict: 'Match',
        humanVerdict: 'Unmatch',
        isOverride: true,
        userComment: 'Incorrect speed',
        nextResultId: null,
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Save Decision & Next/i })).toBeInTheDocument();
    });

    // Select Unmatch in inspector
    const unmatchBtn = screen.getByRole('button', { name: /Unmatch/i });
    fireEvent.click(unmatchBtn);

    const commentInput = screen.getByPlaceholderText(/Add explanation for human decision/i);
    fireEvent.change(commentInput, { target: { value: 'Incorrect speed' } });

    const saveBtn = screen.getByRole('button', { name: /Save Decision & Next/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(tendersApi.submitVerdict).toHaveBeenCalledWith(
        'tender-123',
        'res-101',
        'Unmatch',
        'Incorrect speed'
      );
    });
  });
});
