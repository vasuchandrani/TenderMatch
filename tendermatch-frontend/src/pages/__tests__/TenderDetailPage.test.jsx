import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { TenderDetailPage } from '../TenderDetailPage';
import { tendersApi } from '../../services/api';

vi.mock('../../services/api', () => ({
  tendersApi: {
    getById: vi.fn(),
    getProcessingStatus: vi.fn(),
    getProducts: vi.fn(),
    getMatchSummary: vi.fn(),
    getVendorResults: vi.fn(),
    triggerMatch: vi.fn(),
    triggerProcess: vi.fn(),
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'tender-123' }),
  };
});

describe('TenderDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    tendersApi.getProcessingStatus.mockResolvedValue({ success: true, data: null });
    tendersApi.getProducts.mockResolvedValue({ success: true, data: [] });
    tendersApi.getMatchSummary.mockResolvedValue({ success: true, data: [] });
    tendersApi.getVendorResults.mockResolvedValue({ success: true, data: [] });
  });

  const renderComponent = () =>
    render(
      <BrowserRouter>
        <TenderDetailPage />
      </BrowserRouter>
    );

  it('renders loading state initially', () => {
    tendersApi.getById.mockImplementation(() => new Promise(() => {}));
    renderComponent();

    expect(screen.getByText(/loading tender details\.\.\./i)).toBeInTheDocument();
  });

  it('renders tender details and documents table when loaded', async () => {
    const mockTender = {
      id: 'tender-123',
      name: 'Server Procurement 2026',
      description: 'Rackmount servers RFP',
      status: 'EXTRACTED',
      createdAt: '2026-03-01T10:00:00Z',
      requirementFileName: 'req_specs.pdf',
      requirementFileSize: 204800,
      vendorSubmissions: [
        {
          id: 'doc-v1',
          fileName: 'vendor_bid.pdf',
          fileSize: 102400,
          status: 'EXTRACTED',
          vendorName: 'Acme Corp',
        },
      ],
    };

    tendersApi.getById.mockResolvedValue({ success: true, data: mockTender });
    tendersApi.getProcessingStatus.mockResolvedValue({
      success: true,
      data: {
        tenderId: 'tender-123',
        status: 'EXTRACTED',
        documents: [],
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Server Procurement 2026')).toBeInTheDocument();
      expect(screen.getByText('Rackmount servers RFP')).toBeInTheDocument();
    });

    expect(screen.getByText('req_specs.pdf')).toBeInTheDocument();
    expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Run Match Evaluation/i })).toBeInTheDocument();
  });

  it('renders match summaries when tender is COMPLETED', async () => {
    const mockTender = {
      id: 'tender-123',
      name: 'Server Procurement 2026',
      status: 'COMPLETED',
      createdAt: '2026-03-01T10:00:00Z',
      requirementFileName: 'req.pdf',
      requirementFileSize: 1024,
      vendorSubmissions: [{ id: 'doc-v1', fileName: 'vendor1.pdf', fileSize: 1024, status: 'COMPLETED' }],
    };

    const mockSummaries = [
      {
        submissionId: 'doc-v1',
        vendorDocId: 'doc-v1',
        vendorName: 'Dell Enterprise',
        fileName: 'vendor1.pdf',
        matchPercentage: 92.5,
        matchCount: 9,
        unmatchCount: 1,
        aiErrCount: 0,
        totalSpecs: 10,
      },
    ];

    tendersApi.getById.mockResolvedValue({ success: true, data: mockTender });
    tendersApi.getMatchSummary.mockResolvedValue({ success: true, data: mockSummaries });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Dell Enterprise')).toBeInTheDocument();
      expect(screen.getByText(/92\.5%/)).toBeInTheDocument();
      expect(screen.getByText(/Vendor Matching Results & Compliance/i)).toBeInTheDocument();
    });
  });

  it('triggers matching engine when Run Match Evaluation button is clicked', async () => {
    const mockTender = {
      id: 'tender-123',
      name: 'Server Procurement 2026',
      status: 'EXTRACTED',
      createdAt: '2026-03-01T10:00:00Z',
      requirementFileName: 'req.pdf',
      requirementFileSize: 1024,
      vendorSubmissions: [{ id: 'doc-v1', fileName: 'vendor1.pdf', fileSize: 1024, status: 'EXTRACTED' }],
    };

    tendersApi.getById.mockResolvedValue({ success: true, data: mockTender });
    tendersApi.triggerMatch.mockResolvedValue({ success: true, data: { status: 'MATCHING' } });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Run Match Evaluation/i })).toBeInTheDocument();
    });

    const matchBtn = screen.getByRole('button', { name: /Run Match Evaluation/i });
    fireEvent.click(matchBtn);

    await waitFor(() => {
      expect(tendersApi.triggerMatch).toHaveBeenCalledWith('tender-123');
    });
  });
});
