import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { FileDropzone } from '../FileDropzone';

describe('FileDropzone', () => {
  it('should render drop area with instructions', () => {
    render(
      <FileDropzone
        label="Requirement Tender PDF"
        description="Upload official requirement document"
      />
    );

    expect(screen.getByText(/requirement tender pdf/i)).toBeInTheDocument();
    expect(screen.getByText(/upload official requirement document/i)).toBeInTheDocument();
    expect(screen.getByText(/drag & drop pdf/i)).toBeInTheDocument();
  });

  it('should accept PDF files and call onFilesChange', () => {
    const handleFilesChange = vi.fn();
    render(
      <FileDropzone
        label="Requirement PDF"
        onFilesChange={handleFilesChange}
      />
    );

    const file = new File(['pdf dummy content'], 'tender_requirement.pdf', {
      type: 'application/pdf',
    });

    const dropzone = screen.getByText(/drag & drop pdf/i).closest('div');
    fireEvent.drop(dropzone, {
      dataTransfer: {
        files: [file],
      },
    });

    expect(handleFilesChange).toHaveBeenCalledTimes(1);
    expect(handleFilesChange).toHaveBeenCalledWith([file]);
  });

  it('should reject non-PDF files with an error message', () => {
    const handleFilesChange = vi.fn();
    render(
      <FileDropzone
        label="Requirement PDF"
        onFilesChange={handleFilesChange}
      />
    );

    const docxFile = new File(['dummy docx'], 'specs.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    });

    const dropzone = screen.getByText(/drag & drop pdf/i).closest('div');
    fireEvent.drop(dropzone, {
      dataTransfer: {
        files: [docxFile],
      },
    });

    expect(screen.getByText(/"specs\.docx" is not a PDF file\./i)).toBeInTheDocument();
    expect(handleFilesChange).not.toHaveBeenCalled();
  });

  it('should display file name and formatted size after selection', () => {
    const mockFile = new File(['1234567890'], 'sample_bid.pdf', { type: 'application/pdf' });
    Object.defineProperty(mockFile, 'size', { value: 1024 * 50 }); // 50 KB

    render(
      <FileDropzone
        label="Vendor Bid"
        files={[mockFile]}
        onFilesChange={vi.fn()}
      />
    );

    expect(screen.getByText('sample_bid.pdf')).toBeInTheDocument();
    expect(screen.getByText('50 KB')).toBeInTheDocument();
  });

  it('should allow removing a dropped file', () => {
    const handleFilesChange = vi.fn();
    const file1 = new File(['1'], 'bid1.pdf', { type: 'application/pdf' });
    const file2 = new File(['2'], 'bid2.pdf', { type: 'application/pdf' });

    render(
      <FileDropzone
        label="Vendor Bids"
        multiple={true}
        files={[file1, file2]}
        onFilesChange={handleFilesChange}
      />
    );

    const removeButtons = screen.getAllByTitle(/remove file/i);
    expect(removeButtons.length).toBe(2);

    fireEvent.click(removeButtons[0]);

    expect(handleFilesChange).toHaveBeenCalledWith([file2]);
  });
});
