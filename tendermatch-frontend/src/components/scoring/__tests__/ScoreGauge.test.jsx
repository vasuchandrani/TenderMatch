import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ScoreGauge from '../ScoreGauge';

describe('ScoreGauge', () => {
  it('renders percentage score and label', () => {
    render(<ScoreGauge score={87.5} label="Overall Match" />);

    expect(screen.getByText('87.5%')).toBeInTheDocument();
    expect(screen.getByText('Overall Match')).toBeInTheDocument();
  });

  it('renders green color for score >= 80', () => {
    const { container } = render(<ScoreGauge score={90} />);
    const circle = container.querySelector('[data-testid="gauge-progress"]');
    expect(circle).toHaveAttribute('stroke', '#10b981');
  });

  it('renders yellow/amber color for score between 50 and 79', () => {
    const { container } = render(<ScoreGauge score={65} />);
    const circle = container.querySelector('[data-testid="gauge-progress"]');
    expect(circle).toHaveAttribute('stroke', '#f59e0b');
  });

  it('renders red color for score < 50', () => {
    const { container } = render(<ScoreGauge score={40} />);
    const circle = container.querySelector('[data-testid="gauge-progress"]');
    expect(circle).toHaveAttribute('stroke', '#ef4444');
  });
});
