import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import VerdictBadge from '../VerdictBadge';

describe('VerdictBadge Component', () => {
  it('1. should render green badge for Match', () => {
    render(<VerdictBadge verdict="Match" />);
    const badge = screen.getByTestId('verdict-match');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent('Match');
  });

  it('2. should render red badge for Unmatch', () => {
    render(<VerdictBadge verdict="Unmatch" />);
    const badge = screen.getByTestId('verdict-unmatch');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent('Unmatch');
  });

  it('3. should render yellow badge for AI Err', () => {
    render(<VerdictBadge verdict="AI Err" />);
    const badge = screen.getByTestId('verdict-ai-err');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent('AI Err');
  });
});
