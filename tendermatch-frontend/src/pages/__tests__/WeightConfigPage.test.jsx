import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { WeightConfigPage } from '../WeightConfigPage';
import { tendersApi } from '../../services/api';

vi.mock('../../services/api', () => ({
  tendersApi: {
    getWeights: vi.fn(),
    getWeightTemplates: vi.fn(),
    updateWeights: vi.fn(),
    saveWeightTemplate: vi.fn(),
    applyWeightTemplate: vi.fn(),
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'tender-123' }),
  };
});

describe('WeightConfigPage', () => {
  const mockWeights = [
    {
      scoringWeightId: 'w-1',
      canonicalSpecName: 'ram_capacity',
      weightCategory: 'critical',
      weightValue: 3.0,
      isCustom: true,
    },
    {
      scoringWeightId: 'w-2',
      canonicalSpecName: 'processor_speed',
      weightCategory: 'medium',
      weightValue: 1.0,
      isCustom: false,
    },
  ];

  const mockTemplates = [
    {
      templateName: 'High Performance RFP',
      createdBy: 'admin@tendermatch.com',
      weights: [
        {
          canonicalSpecName: 'ram_capacity',
          weightCategory: 'critical',
          weightValue: 3.0,
        },
      ],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    tendersApi.getWeights.mockResolvedValue({ success: true, data: mockWeights });
    tendersApi.getWeightTemplates.mockResolvedValue({ success: true, data: mockTemplates });
  });

  const renderComponent = () =>
    render(
      <BrowserRouter>
        <WeightConfigPage />
      </BrowserRouter>
    );

  it('renders weights and template selector correctly', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Specification Importance Weights')).toBeInTheDocument();
      expect(screen.getByText('ram_capacity')).toBeInTheDocument();
      expect(screen.getByText('processor_speed')).toBeInTheDocument();
      expect(screen.getByText(/High Performance RFP/)).toBeInTheDocument();
    });

    expect(tendersApi.getWeights).toHaveBeenCalledWith('tender-123');
    expect(tendersApi.getWeightTemplates).toHaveBeenCalled();
  });

  it('updates weights when Save Weights is clicked', async () => {
    tendersApi.updateWeights.mockResolvedValue({
      success: true,
      data: mockWeights,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Save Weights/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Save Weights/i }));

    await waitFor(() => {
      expect(tendersApi.updateWeights).toHaveBeenCalledWith(
        'tender-123',
        expect.arrayContaining([
          expect.objectContaining({ canonicalSpecName: 'ram_capacity' }),
        ])
      );
    });

    expect(
      screen.getByText(/Specification weights updated successfully/i)
    ).toBeInTheDocument();
  });

  it('applies selected template when Apply Template is clicked', async () => {
    tendersApi.applyWeightTemplate.mockResolvedValue({
      success: true,
      data: mockWeights,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Apply Template/i })).toBeInTheDocument();
    });

    const select = screen.getByDisplayValue('Select a template...');
    fireEvent.change(select, { target: { value: 'High Performance RFP' } });

    const applyBtn = screen.getByRole('button', { name: /Apply Template/i });
    expect(applyBtn).not.toBeDisabled();

    fireEvent.click(applyBtn);

    await waitFor(() => {
      expect(tendersApi.applyWeightTemplate).toHaveBeenCalledWith('tender-123', 'High Performance RFP');
    });

    expect(
      screen.getByText(/Applied template "High Performance RFP"/i)
    ).toBeInTheDocument();
  });
});
