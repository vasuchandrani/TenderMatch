import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { DashboardPage } from '../DashboardPage';
import { tendersApi } from '../../services/api';

vi.mock('../../services/api', () => ({
  tendersApi: {
    list: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderComponent = () =>
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

  it('should render loading state initially', () => {
    tendersApi.list.mockImplementation(() => new Promise(() => {})); // Never resolves
    renderComponent();

    expect(screen.getByText(/fetching tender sessions\.\.\./i)).toBeInTheDocument();
  });

  it('should display tender list from API response', async () => {
    const mockTenders = [
      {
        id: 't-1',
        name: 'Ministry IT Hardware',
        description: 'Server and laptop procurement',
        status: 'CREATED',
        requirementFileName: 'specs.pdf',
        vendorSubmissions: [{}, {}],
        createdAt: '2026-03-01T10:00:00Z',
      },
      {
        id: 't-2',
        name: 'Smart City Cloud Infrastructure',
        description: 'Cloud hosting RFP',
        status: 'COMPLETED',
        requirementFileName: 'cloud_rfp.pdf',
        vendorSubmissions: [{}],
        createdAt: '2026-03-02T10:00:00Z',
      },
    ];

    tendersApi.list.mockResolvedValueOnce({
      success: true,
      data: {
        content: mockTenders,
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Ministry IT Hardware')).toBeInTheDocument();
      expect(screen.getByText('Smart City Cloud Infrastructure')).toBeInTheDocument();
    });

    expect(screen.getByText(/2 files/i)).toBeInTheDocument();
    expect(screen.getByText(/1 files/i)).toBeInTheDocument();
  });

  it('should show empty state when no tenders are returned', async () => {
    tendersApi.list.mockResolvedValueOnce({
      success: true,
      data: {
        content: [],
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/no tender sessions yet/i)).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /start matching now/i })).toBeInTheDocument();
    });
  });

  it('should filter tenders based on search input', async () => {
    const mockTenders = [
      {
        id: 't-1',
        name: 'Railway Signaling System',
        description: 'Track equipment',
        status: 'CREATED',
        vendorSubmissions: [],
        createdAt: '2026-03-01T10:00:00Z',
      },
      {
        id: 't-2',
        name: 'Highway Cameras',
        description: 'Surveillance hardware',
        status: 'SCORED',
        vendorSubmissions: [],
        createdAt: '2026-03-02T10:00:00Z',
      },
    ];

    tendersApi.list.mockResolvedValueOnce({
      success: true,
      data: {
        content: mockTenders,
      },
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Railway Signaling System')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/search tender sessions\.\.\./i);
    fireEvent.change(searchInput, { target: { value: 'Railway' } });

    expect(screen.getByText('Railway Signaling System')).toBeInTheDocument();
    expect(screen.queryByText('Highway Cameras')).not.toBeInTheDocument();
  });
});
