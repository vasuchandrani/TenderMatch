import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import SpecTable from '../SpecTable';

describe('SpecTable Component', () => {
  it('1. should render spec rows with name, value, unit', () => {
    const specs = [
      {
        id: 's1',
        raw_spec_name: 'RAM',
        canonical_spec_name: 'ram_capacity',
        spec_value: '16GB',
        unit: 'GB',
        extraction_confidence: 'high',
        normalization_confidence: 'high'
      },
      {
        id: 's2',
        raw_spec_name: 'SSD',
        canonical_spec_name: 'storage_capacity',
        spec_value: '512GB',
        unit: 'GB',
        extraction_confidence: 'medium',
        normalization_confidence: 'high'
      }
    ];

    render(<SpecTable specifications={specs} productName="HP EliteBook" />);

    expect(screen.getByText('RAM')).toBeInTheDocument();
    expect(screen.getByText('ram_capacity')).toBeInTheDocument();
    expect(screen.getByText('16GB')).toBeInTheDocument();
    expect(screen.getByText('SSD')).toBeInTheDocument();
    expect(screen.getByText('512GB')).toBeInTheDocument();
  });

  it('2. should show confidence badges', () => {
    const specs = [
      {
        id: 's1',
        raw_spec_name: 'Display',
        canonical_spec_name: 'display_size',
        spec_value: '14 inch',
        extraction_confidence: 'high',
        normalization_confidence: 'low'
      }
    ];

    render(<SpecTable specifications={specs} />);

    expect(screen.getByTestId('confidence-high')).toBeInTheDocument();
    expect(screen.getByTestId('confidence-low')).toBeInTheDocument();
  });

  it('3. should handle empty spec list', () => {
    render(<SpecTable specifications={[]} />);

    expect(screen.getByText(/no specifications extracted/i)).toBeInTheDocument();
  });
});
