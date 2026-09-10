import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ComparisonMatrixPage } from '../ComparisonMatrixPage';
import { tendersApi } from '../../services/api';

vi.mock('../../services/api', () => ({
  tendersApi: {
    getComparisonMatrix: vi.fn(),
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

describe('ComparisonMatrixPage', () => {
  const mockMatrix = {
    tenderId: 'tender-123',
    vendorNames: ['Vendor Alpha', 'Vendor Beta'],
    rows: [
      {
        tenderSpecId: 'spec-1',
        requiredSpecName: 'RAM Memory',
        canonicalSpecName: 'ram_capacity',
        requiredValue: '32 GB',
        requiredUnit: '',
        comparisonOperator: '>=',
        weight: 2.0,
        vendorCells: [
          {
            vendorSubmissionId: 'sub-1',
            vendorName: 'Vendor Alpha',
            offeredValue: '64 GB',
            systemVerdict: 'Match',
            humanVerdict: null,
          },
          {
            vendorSubmissionId: 'sub-2',
            vendorName: 'Vendor Beta',
            offeredValue: '16 GB',
            systemVerdict: 'Unmatch',
            humanVerdict: null,
          },
        ],
      },
      {
        tenderSpecId: 'spec-2',
        requiredSpecName: 'Processor Cores',
        canonicalSpecName: 'cpu_cores',
        requiredValue: '16',
        requiredUnit: 'cores',
        comparisonOperator: '>=',
        weight: 1.0,
        vendorCells: [
          {
            vendorSubmissionId: 'sub-1',
            vendorName: 'Vendor Alpha',
            offeredValue: '32',
            systemVerdict: 'Match',
            humanVerdict: null,
          },
          {
            vendorSubmissionId: 'sub-2',
            vendorName: 'Vendor Beta',
            offeredValue: '16',
            systemVerdict: 'Match',
            humanVerdict: null,
          },
        ],
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    tendersApi.getComparisonMatrix.mockResolvedValue({ success: true, data: mockMatrix });
  });

  const renderComponent = () =>
    render(
      <BrowserRouter>
        <ComparisonMatrixPage />
      </BrowserRouter>
    );

  it('renders comparison matrix page and specs', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Side-by-Side Comparison Matrix')).toBeInTheDocument();
      expect(screen.getByText('Vendor Alpha')).toBeInTheDocument();
      expect(screen.getByText('Vendor Beta')).toBeInTheDocument();
      expect(screen.getByText('RAM Memory')).toBeInTheDocument();
      expect(screen.getByText('Processor Cores')).toBeInTheDocument();
    });

    expect(tendersApi.getComparisonMatrix).toHaveBeenCalledWith('tender-123');
  });

  it('filters rows when searching by spec name', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('RAM Memory')).toBeInTheDocument();
      expect(screen.getByText('Processor Cores')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/search specifications\.\.\./i);
    fireEvent.change(searchInput, { target: { value: 'RAM' } });

    expect(screen.getByText('RAM Memory')).toBeInTheDocument();
    expect(screen.queryByText('Processor Cores')).not.toBeInTheDocument();
  });

  it('switches between All Vendors and Single Vendor views', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('All Vendors (Matrix)')).toBeInTheDocument();
      expect(screen.getByText('Single Vendor (1-on-1)')).toBeInTheDocument();
    });

    const singleModeBtn = screen.getByText('Single Vendor (1-on-1)');
    fireEvent.click(singleModeBtn);

    expect(screen.getByText('Focused 1-on-1 Vendor Evaluation')).toBeInTheDocument();
    expect(screen.getByText('Tender Requirement & Criteria')).toBeInTheDocument();
  });
});
