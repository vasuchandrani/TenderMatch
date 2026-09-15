import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import ProcessingStatus from '../ProcessingStatus';

describe('ProcessingStatus Component', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('1. should show spinner during processing', () => {
    const mockStatus = {
      tenderStatus: 'PROCESSING',
      totalDocuments: 3,
      completedDocuments: 1,
      requirementFileName: 'rfp.pdf',
      requirementStatus: 'PROCESSING',
      vendorDocuments: [
        { submissionId: '1', vendorName: 'Dell', fileName: 'dell.pdf', status: 'PROCESSING', productCount: 0 }
      ]
    };

    render(<ProcessingStatus statusData={mockStatus} onRefresh={vi.fn()} />);

    expect(screen.getByTestId('processing-spinner')).toBeInTheDocument();
    expect(screen.getByText(/processing documents/i)).toBeInTheDocument();
  });

  it('2. should display per-document status', () => {
    const mockStatus = {
      tenderStatus: 'EXTRACTED',
      totalDocuments: 2,
      completedDocuments: 2,
      requirementFileName: 'rfp.pdf',
      requirementStatus: 'DONE',
      vendorDocuments: [
        { submissionId: '1', vendorName: 'Lenovo', fileName: 'lenovo.pdf', status: 'EXTRACTED', productCount: 4 }
      ]
    };

    render(<ProcessingStatus statusData={mockStatus} onRefresh={vi.fn()} />);

    expect(screen.getByText('rfp.pdf')).toBeInTheDocument();
    expect(screen.getByText('Lenovo')).toBeInTheDocument();
    expect(screen.getByText(/4 specs/i)).toBeInTheDocument();
  });

  it('3. should poll for updates every 3 seconds when processing', async () => {
    const onRefresh = vi.fn();
    const mockStatus = {
      tenderStatus: 'PROCESSING',
      totalDocuments: 2,
      completedDocuments: 0,
      requirementFileName: 'rfp.pdf',
      requirementStatus: 'PROCESSING',
      vendorDocuments: []
    };

    render(<ProcessingStatus statusData={mockStatus} onRefresh={onRefresh} isPolling={true} />);

    // Fast-forward 3 seconds
    vi.advanceTimersByTime(3000);
    expect(onRefresh).toHaveBeenCalledTimes(1);

    // Fast-forward another 3 seconds
    vi.advanceTimersByTime(3000);
    expect(onRefresh).toHaveBeenCalledTimes(2);
  });
});
