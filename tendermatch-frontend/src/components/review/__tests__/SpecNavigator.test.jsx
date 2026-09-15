import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import SpecNavigator from '../SpecNavigator';

describe('SpecNavigator', () => {
  it('renders position and hierarchical context', () => {
    render(
      <SpecNavigator
        currentIndex={2}
        totalSpecs={10}
        currentProduct="Business Laptop"
        vendorName="Lenovo"
        onPrev={vi.fn()}
        onNext={vi.fn()}
        canPrev={true}
        canNext={true}
      />
    );

    expect(screen.getByText('Spec 3 of 10')).toBeInTheDocument();
    expect(screen.getByText('Business Laptop')).toBeInTheDocument();
    expect(screen.getByText('Lenovo')).toBeInTheDocument();
  });

  it('disables Prev button on first spec and enables Next', () => {
    const onPrev = vi.fn();
    const onNext = vi.fn();

    render(
      <SpecNavigator
        currentIndex={0}
        totalSpecs={5}
        onPrev={onPrev}
        onNext={onNext}
        canPrev={false}
        canNext={true}
      />
    );

    const prevBtn = screen.getByRole('button', { name: /Prev/i });
    const nextBtn = screen.getByRole('button', { name: /Next/i });

    expect(prevBtn).toBeDisabled();
    expect(nextBtn).toBeEnabled();

    fireEvent.click(nextBtn);
    expect(onNext).toHaveBeenCalledTimes(1);
  });
});
