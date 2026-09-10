import React, { useState, useEffect, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import { tendersApi } from '../services/api';
import ComparisonMatrix from '../components/scoring/ComparisonMatrix';
import ExportButtons from '../components/scoring/ExportButtons';
import {
  ArrowLeft,
  Award,
  Sliders,
  Table,
  Columns,
  Search,
  Filter,
  RefreshCw,
  AlertCircle,
  LayoutGrid,
} from 'lucide-react';
import { formatRequirementCriteria } from '../utils/formatters';

export const ComparisonMatrixPage = () => {
  const { id: tenderId } = useParams();

  const [matrixData, setMatrixData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [verdictFilter, setVerdictFilter] = useState('ALL');
  const [viewMode, setViewMode] = useState('grid'); // 'grid' (All Vendors) or 'single' (1-on-1 Focus)
  const [selectedVendorIndex, setSelectedVendorIndex] = useState(0);
  const [exportingPdf, setExportingPdf] = useState(false);
  const [exportingCsv, setExportingCsv] = useState(false);

  const fetchMatrix = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await tendersApi.getComparisonMatrix(tenderId);
      if (res?.success && res?.data) {
        setMatrixData(res.data);
      } else {
        setError('Failed to load comparison matrix.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error fetching comparison matrix.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMatrix();
  }, [tenderId]);

  const handleExportPdf = async () => {
    try {
      setExportingPdf(true);
      const blob = await tendersApi.exportPdf(tenderId);
      const url = window.URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `tender-${tenderId}-matrix.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('Failed to export PDF: ' + (err.response?.data?.message || err.message));
    } finally {
      setExportingPdf(false);
    }
  };

  const handleExportCsv = async () => {
    try {
      setExportingCsv(true);
      if (matrixData && matrixData.rows && matrixData.rows.length > 0) {
        const vendorNames = matrixData.vendorNames || [];
        const headers = ['Specification', 'Canonical Key', 'Requirement', 'Weight', ...vendorNames];
        let csvContent = '\uFEFF' + headers.map((h) => `"${(h || '').replace(/"/g, '""')}"`).join(',') + '\n';

        matrixData.rows.forEach((row) => {
          const reqStr = formatRequirementCriteria(row.comparisonOperator, row.requiredValue, row.requiredUnit);
          const vendorVals = vendorNames.map((vName) => {
            const cell = row.vendorCells?.find((c) => c.vendorName === vName);
            if (!cell) return '-';
            const val = cell.offeredValue !== '-' ? `${cell.offeredValue} ${cell.offeredUnit || ''}`.trim() : '-';
            const verdict = cell.humanVerdict || cell.systemVerdict || 'Unmatch';
            return `${val} [${verdict}]`;
          });

          const rowData = [
            row.requiredSpecName,
            row.canonicalSpecName || '',
            reqStr,
            `${row.weight}x`,
            ...vendorVals,
          ];
          csvContent += rowData.map((d) => `"${(d || '').toString().replace(/"/g, '""')}"`).join(',') + '\n';
        });

        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `tender-${tenderId}-comparison-matrix.csv`);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
      } else {
        const blob = await tendersApi.exportCsv(tenderId);
        const url = window.URL.createObjectURL(new Blob([blob], { type: 'text/csv;charset=utf-8;' }));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `tender-${tenderId}-matrix.csv`);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
      }
    } catch (err) {
      alert('Failed to export CSV: ' + (err.response?.data?.message || err.message));
    } finally {
      setExportingCsv(false);
    }
  };

  const filteredMatrix = useMemo(() => {
    if (!matrixData || !matrixData.rows) return null;

    let rows = matrixData.rows;

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      rows = rows.filter(
        (r) =>
          r.requiredSpecName?.toLowerCase().includes(q) ||
          r.canonicalSpecName?.toLowerCase().includes(q) ||
          r.requiredValue?.toLowerCase().includes(q)
      );
    }

    if (verdictFilter !== 'ALL') {
      rows = rows.filter((r) =>
        r.vendorCells?.some((c) => {
          const effective = c.humanVerdict || c.systemVerdict || 'Unmatch';
          return effective.toUpperCase() === verdictFilter.toUpperCase();
        })
      );
    }

    return {
      ...matrixData,
      rows,
    };
  }, [matrixData, searchQuery, verdictFilter]);

  return (
    <div className="main-content" style={{ maxWidth: '1440px' }}>
      {/* Header Bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '1.5rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Link
            to={`/tenders/${tenderId}`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <ArrowLeft size={16} /> Tender Overview
          </Link>

          <Link
            to={`/tenders/${tenderId}/scores`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Award size={16} /> Leaderboard
          </Link>

          <Link
            to={`/tenders/${tenderId}/weights`}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Sliders size={16} /> Configure Weights
          </Link>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            type="button"
            onClick={fetchMatrix}
            className="btn btn-secondary"
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem' }}
          >
            <RefreshCw size={15} />
            <span>Refresh</span>
          </button>

          <ExportButtons
            onExportPdf={handleExportPdf}
            onExportCsv={handleExportCsv}
            isExportingPdf={exportingPdf}
            isExportingCsv={exportingCsv}
          />
        </div>
      </div>

      {error && (
        <div
          className="card"
          style={{
            marginBottom: '1.5rem',
            padding: '1rem 1.25rem',
            borderColor: 'var(--status-unmatch-border)',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            color: 'var(--status-unmatch)',
          }}
        >
          <AlertCircle size={18} />
          <span style={{ fontSize: '0.9rem' }}>{error}</span>
        </div>
      )}

      {/* Main Container */}
      <div className="card" style={{ padding: '1.5rem', borderRadius: '16px' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '1.25rem',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div>
            <h2 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Table size={20} style={{ color: 'var(--accent-primary)' }} />
              Side-by-Side Comparison Matrix
            </h2>
            <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Cross-vendor evaluation against tender requirements with specification weights.
            </p>
          </div>

          {/* View Mode Toggle & Search / Filter Controls */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
            {/* View Mode Switcher */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                backgroundColor: 'var(--bg-secondary)',
                padding: '3px',
                borderRadius: '8px',
                border: '1px solid var(--border-color)',
              }}
            >
              <button
                type="button"
                onClick={() => setViewMode('grid')}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '5px 12px',
                  borderRadius: '6px',
                  border: 'none',
                  fontSize: '0.82rem',
                  fontWeight: viewMode === 'grid' ? 700 : 500,
                  cursor: 'pointer',
                  backgroundColor: viewMode === 'grid' ? '#ffffff' : 'transparent',
                  color: viewMode === 'grid' ? 'var(--accent-primary)' : 'var(--text-secondary)',
                  boxShadow: viewMode === 'grid' ? 'var(--shadow-sm)' : 'none',
                  transition: 'all 0.15s ease',
                }}
              >
                <LayoutGrid size={14} />
                <span>All Vendors (Matrix)</span>
              </button>

              <button
                type="button"
                onClick={() => setViewMode('single')}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '5px 12px',
                  borderRadius: '6px',
                  border: 'none',
                  fontSize: '0.82rem',
                  fontWeight: viewMode === 'single' ? 700 : 500,
                  cursor: 'pointer',
                  backgroundColor: viewMode === 'single' ? '#ffffff' : 'transparent',
                  color: viewMode === 'single' ? 'var(--accent-primary)' : 'var(--text-secondary)',
                  boxShadow: viewMode === 'single' ? 'var(--shadow-sm)' : 'none',
                  transition: 'all 0.15s ease',
                }}
              >
                <Columns size={14} />
                <span>Single Vendor (1-on-1)</span>
              </button>
            </div>

            {/* Search */}
            <div style={{ position: 'relative', width: '220px' }}>
              <Search size={15} style={{ position: 'absolute', left: 10, top: 10, color: 'var(--text-muted)' }} />
              <input
                type="text"
                placeholder="Search specifications..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="input"
                style={{ paddingLeft: '32px', fontSize: '0.85rem', width: '100%' }}
              />
            </div>

            {/* Verdict Filter */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Filter size={15} style={{ color: 'var(--text-muted)' }} />
              <select
                value={verdictFilter}
                onChange={(e) => setVerdictFilter(e.target.value)}
                className="input"
                style={{ padding: '6px 12px', fontSize: '0.85rem' }}
              >
                <option value="ALL">All Verdicts</option>
                <option value="MATCH">Has Match</option>
                <option value="UNMATCH">Has Unmatch</option>
                <option value="UNCERTAIN">Has Uncertain</option>
              </select>
            </div>
          </div>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
            <RefreshCw size={24} className="animate-spin" style={{ margin: '0 auto 0.75rem' }} />
            <div>Generating comparison matrix...</div>
          </div>
        ) : (
          <ComparisonMatrix
            matrix={filteredMatrix}
            viewMode={viewMode}
            selectedVendorIndex={selectedVendorIndex}
            onSelectVendorIndex={setSelectedVendorIndex}
          />
        )}
      </div>
    </div>
  );
};

export default ComparisonMatrixPage;
