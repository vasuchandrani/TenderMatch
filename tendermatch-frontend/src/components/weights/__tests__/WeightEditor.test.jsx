import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import WeightEditor from '../WeightEditor';

describe('WeightEditor', () => {
  const mockWeights = [
    { canonicalSpecName: 'ram_capacity', weightCategory: 'high', weightValue: 2.0 },
    { canonicalSpecName: 'display_size', weightCategory: 'medium', weightValue: 1.0 },
  ];

  it('renders specification weights in editable table', () => {
    render(<WeightEditor weights={mockWeights} onSave={vi.fn()} onReset={vi.fn()} />);

    expect(screen.getByText('ram_capacity')).toBeInTheDocument();
    expect(screen.getByText('display_size')).toBeInTheDocument();
  });

  it('allows user to change weight category via select', () => {
    render(<WeightEditor weights={mockWeights} onSave={vi.fn()} onReset={vi.fn()} />);

    const selects = screen.getAllByRole('combobox');
    expect(selects.length).toBeGreaterThanOrEqual(2);

    fireEvent.change(selects[0], { target: { value: 'critical' } });
    expect(selects[0].value).toBe('critical');
  });

  it('calls onSave with updated weights when Save is clicked', () => {
    const onSave = vi.fn();
    render(<WeightEditor weights={mockWeights} onSave={onSave} onReset={vi.fn()} />);

    const saveBtn = screen.getByRole('button', { name: /Save Weights/i });
    fireEvent.click(saveBtn);

    expect(onSave).toHaveBeenCalledWith(mockWeights);
  });
});
