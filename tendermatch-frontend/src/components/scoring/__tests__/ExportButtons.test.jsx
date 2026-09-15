import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ExportButtons from '../ExportButtons';

describe('ExportButtons', () => {
  it('triggers onExportPdf when Export as PDF button is clicked', () => {
    const onPdf = vi.fn();
    render(<ExportButtons onExportPdf={onPdf} onExportCsv={vi.fn()} />);

    const pdfBtn = screen.getByRole('button', { name: /Export as PDF/i });
    fireEvent.click(pdfBtn);

    expect(onPdf).toHaveBeenCalledTimes(1);
  });

  it('triggers onExportCsv when Export as CSV button is clicked', () => {
    const onCsv = vi.fn();
    render(<ExportButtons onExportPdf={vi.fn()} onExportCsv={onCsv} />);

    const csvBtn = screen.getByRole('button', { name: /Export as CSV/i });
    fireEvent.click(csvBtn);

    expect(onCsv).toHaveBeenCalledTimes(1);
  });
});
