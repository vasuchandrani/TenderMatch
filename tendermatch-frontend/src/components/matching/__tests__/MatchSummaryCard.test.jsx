import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import MatchSummaryCard from '../MatchSummaryCard';

describe('MatchSummaryCard Component', () => {
  const mockSummary = {
    submissionId: 'sub-123',
    vendorName: 'Lenovo Systems',
    fileName: 'lenovo_proposal.pdf',
    matchCount: 15,
    unmatchCount: 3,
    aiErrCount: 2,
    totalSpecs: 20,
    matchPercentage: 75.0,
  };

  it('1. should display vendor name and counts', () => {
    render(<MatchSummaryCard summary={mockSummary} />);

    expect(screen.getByText('Lenovo Systems')).toBeInTheDocument();
    expect(screen.getByText('15')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText(/75%/i)).toBeInTheDocument();
  });

  it('2. should show progress bar during matching', () => {
    render(<MatchSummaryCard summary={mockSummary} isMatching={true} />);

    expect(screen.getByTestId('match-progress-bar')).toBeInTheDocument();
  });

  it('3. should trigger onClick when card or inspect button is clicked', () => {
    const handleClick = vi.fn();
    render(<MatchSummaryCard summary={mockSummary} onClick={handleClick} />);

    const button = screen.getByRole('button', { name: /view match breakdown/i });
    fireEvent.click(button);

    expect(handleClick).toHaveBeenCalledWith(mockSummary);
  });
});
