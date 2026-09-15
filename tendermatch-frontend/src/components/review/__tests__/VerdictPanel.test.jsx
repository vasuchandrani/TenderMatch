import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import VerdictPanel from '../VerdictPanel';

describe('VerdictPanel', () => {
  const mockSpecPair = {
    resultId: 'res-1',
    systemVerdict: 'Match',
    confidenceScore: 0.95,
    comparisonMethod: 'numeric_exact',
    comparisonDetails: '32 >= 16 GB',
    humanVerdict: null,
    userComment: '',
  };

  it('renders system verdict badge and confidence bar', () => {
    render(<VerdictPanel specPair={mockSpecPair} onSaveVerdict={vi.fn()} onSkip={vi.fn()} />);

    expect(screen.getByText('System:')).toBeInTheDocument();
    expect(screen.getByText('32 >= 16 GB')).toBeInTheDocument();
    expect(screen.getByText('95%')).toBeInTheDocument();
  });

  it('pre-selects Match when system verdict is Match', () => {
    render(<VerdictPanel specPair={mockSpecPair} onSaveVerdict={vi.fn()} onSkip={vi.fn()} />);

    const matchBtn = screen.getByRole('button', { name: /^Match/i });
    expect(matchBtn).toHaveStyle({ color: '#10b981' });
  });

  it('allows user to switch selection to Unmatch and adds comment', () => {
    const onSave = vi.fn();
    render(<VerdictPanel specPair={mockSpecPair} onSaveVerdict={onSave} onSkip={vi.fn()} />);

    const unmatchBtn = screen.getByRole('button', { name: /^Unmatch/i });
    fireEvent.click(unmatchBtn);

    expect(screen.getByText(/Override Applied/i)).toBeInTheDocument();

    const textarea = screen.getByPlaceholderText(/Add explanation/i);
    fireEvent.change(textarea, { target: { value: 'Frequency too low' } });

    const saveBtn = screen.getByRole('button', { name: /Save & Next/i });
    fireEvent.click(saveBtn);

    expect(onSave).toHaveBeenCalledWith('Unmatch', 'Frequency too low');
  });

  it('disables saving and verdict buttons when review is locked', () => {
    render(<VerdictPanel specPair={mockSpecPair} onSaveVerdict={vi.fn()} onSkip={vi.fn()} isLocked={true} />);

    expect(screen.getByText(/Review is finalized and locked/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Save & Next/i })).not.toBeInTheDocument();
  });
});
