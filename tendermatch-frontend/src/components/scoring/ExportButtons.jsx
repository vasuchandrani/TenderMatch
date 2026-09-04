import React from 'react';
import { FileDown, FileSpreadsheet, Loader2 } from 'lucide-react';

const ExportButtons = ({ onExportPdf, onExportCsv, isExportingPdf = false, isExportingCsv = false }) => {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
      <button
        type="button"
        disabled={isExportingPdf}
        onClick={onExportPdf}
        className="btn btn-secondary"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          fontSize: '0.85rem',
          padding: '0.45rem 0.95rem',
        }}
      >
        {isExportingPdf ? <Loader2 size={15} className="spin" /> : <FileDown size={15} style={{ color: '#ef4444' }} />}
        <span>{isExportingPdf ? 'Exporting PDF...' : 'Export as PDF'}</span>
      </button>

      <button
        type="button"
        disabled={isExportingCsv}
        onClick={onExportCsv}
        className="btn btn-secondary"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          fontSize: '0.85rem',
          padding: '0.45rem 0.95rem',
        }}
      >
        {isExportingCsv ? <Loader2 size={15} className="spin" /> : <FileSpreadsheet size={15} style={{ color: '#10b981' }} />}
        <span>{isExportingCsv ? 'Exporting CSV...' : 'Export as CSV'}</span>
      </button>
    </div>
  );
};

export default ExportButtons;
