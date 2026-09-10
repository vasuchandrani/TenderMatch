import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { tendersApi } from '../services/api';
import { PlusCircle, Search, FileText, Users, ArrowRight, Trash2, Clock, CheckCircle2, AlertCircle } from 'lucide-react';

export const DashboardPage = () => {
  const [tenders, setTenders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [error, setError] = useState(null);

  const fetchTenders = async () => {
    try {
      setLoading(true);
      const res = await tendersApi.list();
      if (res?.success && res?.data?.content) {
        setTenders(res.data.content);
      }
    } catch (err) {
      setError('Failed to load tenders. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTenders();
  }, []);

  const handleDelete = async (id, e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!window.confirm('Are you sure you want to delete this tender session?')) return;

    try {
      await tendersApi.delete(id);
      setTenders(tenders.filter((t) => t.id !== id));
    } catch {
      alert('Failed to delete tender');
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
      case 'SCORED':
        return <span className="badge badge-match"><CheckCircle2 size={12} /> {status}</span>;
      case 'PROCESSING':
      case 'MATCHING':
        return <span className="badge badge-pending"><Clock size={12} /> {status}</span>;
      case 'FAILED':
        return <span className="badge badge-unmatch"><AlertCircle size={12} /> {status}</span>;
      default:
        return <span className="badge badge-info">{status}</span>;
    }
  };

  const filteredTenders = tenders.filter((t) =>
    t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    t.description?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="main-content">
      {/* Header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: '2rem',
        flexWrap: 'wrap',
        gap: '1rem',
      }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Tender Matching Sessions
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.25rem' }}>
            Manage, upload, and review AI requirement comparisons across multiple vendor bids.
          </p>
        </div>

        <Link to="/tenders/new" className="btn btn-primary btn-lg">
          <PlusCircle size={18} />
          Create New Match
        </Link>
      </div>

      {/* Search & Filter Bar */}
      <div style={{ marginBottom: '1.5rem', display: 'flex', gap: '1rem' }}>
        <div style={{ position: 'relative', flex: 1, maxWidth: 400 }}>
          <input
            type="text"
            className="form-input"
            style={{ paddingLeft: '2.5rem' }}
            placeholder="Search tender sessions..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          <Search size={18} style={{ position: 'absolute', left: '0.85rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
        </div>
      </div>

      {/* Loading State */}
      {loading && (
        <div style={{ textAlign: 'center', padding: '4rem 2rem', color: 'var(--text-secondary)' }}>
          <div style={{
            width: 36,
            height: 36,
            border: '3px solid rgba(59, 130, 246, 0.2)',
            borderTopColor: 'var(--accent-primary)',
            borderRadius: '50%',
            animation: 'spin 0.8s linear infinite',
            margin: '0 auto 1rem',
          }} />
          <p>Fetching tender sessions...</p>
        </div>
      )}

      {/* Error State */}
      {!loading && error && (
        <div className="card" style={{ textAlign: 'center', padding: '3rem', borderColor: 'var(--status-unmatch-border)' }}>
          <AlertCircle size={40} style={{ color: 'var(--status-unmatch)', margin: '0 auto 1rem' }} />
          <p style={{ color: 'var(--text-primary)', fontSize: '1.1rem', fontWeight: 600 }}>{error}</p>
          <button onClick={fetchTenders} className="btn btn-secondary" style={{ marginTop: '1rem' }}>
            Retry
          </button>
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && filteredTenders.length === 0 && (
        <div className="card" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
          <div style={{
            background: 'var(--bg-surface)',
            width: 64,
            height: 64,
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 1.25rem',
            color: 'var(--accent-primary)',
          }}>
            <FileText size={32} />
          </div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            {searchQuery ? 'No matching tenders found' : 'No tender sessions yet'}
          </h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', maxWidth: 460, margin: '0.5rem auto 1.5rem' }}>
            {searchQuery
              ? 'Try changing your search keywords or create a new session.'
              : 'Upload one requirement tender PDF and one or more vendor tender PDFs to get automated spec matching.'}
          </p>
          <Link to="/tenders/new" className="btn btn-primary">
            <PlusCircle size={18} />
            Start Matching Now
          </Link>
        </div>
      )}

      {/* Tenders Grid */}
      {!loading && !error && filteredTenders.length > 0 && (
        <div className="responsive-grid">
          {filteredTenders.map((tender) => {
            const isCompleted = tender.status === 'COMPLETED' || tender.status === 'SCORED';
            const primaryLink = isCompleted ? `/tenders/${tender.id}/scores` : `/tenders/${tender.id}`;

            return (
              <div
                key={tender.id}
                className="card"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  transition: 'all 0.2s ease',
                  border: isCompleted ? '1px solid rgba(16, 185, 129, 0.3)' : '1px solid var(--border-color)',
                  boxShadow: isCompleted ? '0 4px 12px rgba(16, 185, 129, 0.05)' : 'var(--shadow-sm)',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.85rem' }}>
                    {getStatusBadge(tender.status)}
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {new Date(tender.createdAt).toLocaleDateString()}
                    </span>
                  </div>

                  <Link
                    to={primaryLink}
                    style={{ textDecoration: 'none', color: 'inherit' }}
                  >
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
                      {tender.name}
                    </h3>
                  </Link>

                  {tender.description && (
                    <p style={{
                      fontSize: '0.85rem',
                      color: 'var(--text-secondary)',
                      marginBottom: '1rem',
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                      overflow: 'hidden',
                    }}>
                      {tender.description}
                    </p>
                  )}

                  <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.4rem',
                    fontSize: '0.825rem',
                    color: 'var(--text-secondary)',
                    padding: '0.75rem 0',
                    borderTop: '1px solid var(--bg-card-border)',
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <FileText size={15} style={{ color: 'var(--accent-primary)' }} />
                      <span>Req: <strong>{tender.requirementFileName || 'Pending upload'}</strong></span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <Users size={15} style={{ color: 'var(--accent-cyan)' }} />
                      <span>Vendors: <strong>{tender.vendorSubmissions?.length || 0} files</strong></span>
                    </div>
                  </div>
                </div>

                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  marginTop: '1rem',
                  paddingTop: '0.75rem',
                  borderTop: '1px solid var(--bg-card-border)',
                  gap: '8px',
                  flexWrap: 'wrap',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    {isCompleted ? (
                      <>
                        <Link
                          to={`/tenders/${tender.id}/scores`}
                          className="btn btn-primary"
                          style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                        >
                          <span>🏆 Leaderboard</span>
                          <ArrowRight size={13} />
                        </Link>
                        <Link
                          to={`/tenders/${tender.id}`}
                          className="btn btn-secondary"
                          style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                          title="Tender session setup and files overview"
                        >
                          Overview
                        </Link>
                      </>
                    ) : (
                      <Link
                        to={`/tenders/${tender.id}`}
                        style={{ color: 'var(--accent-primary)', fontSize: '0.875rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.35rem', textDecoration: 'none' }}
                      >
                        View Session <ArrowRight size={14} />
                      </Link>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={(e) => handleDelete(tender.id, e)}
                    style={{
                      background: 'transparent',
                      border: 'none',
                      color: 'var(--text-muted)',
                      cursor: 'pointer',
                      padding: '0.25rem',
                    }}
                    title="Delete Session"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
