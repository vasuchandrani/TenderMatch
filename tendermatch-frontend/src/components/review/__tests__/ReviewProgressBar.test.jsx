import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ReviewProgressBar from '../ReviewProgressBar';

describe('ReviewProgressBar', () => {
  it('renders progress counts, percentage and override badge', () => {
    render(
      <ReviewProgressBar
        reviewedCount={12}
        totalCount={75}
        percentage={16.0}
        overrideCount={3}
        onBulkConfirm={vi.fn()}
      />
    );

    expect(screen.getByText('12 of 75 specs reviewed')).toBeInTheDocument();
    expect(screen.getByText('16%')).toBeInTheDocument();
    expect(screen.getByText('3 Overrides')).toBeInTheDocument();
  });

  it('triggers onBulkConfirm when bulk confirm button is clicked', () => {
    const onBulk = vi.fn();
    render(
      <ReviewProgressBar
        reviewedCount={5}
        totalCount={10}
        percentage={50.0}
        overrideCount={0}
        onBulkConfirm={onBulk}
      />
    );

    const bulkBtn = screen.getByRole('button', { name: /Confirm Remaining \(5\)/i });
    fireEvent.click(bulkBtn);

    expect(onBulk).toHaveBeenCalledTimes(1);
  });
});
