import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { FileText, PlusCircle, LayoutDashboard, LogOut, Sparkles, Cpu } from 'lucide-react';

export const Navbar = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!isAuthenticated) return null;

  return (
    <header style={{
      background: 'var(--nav-bg)',
      backdropFilter: 'blur(12px)',
      WebkitBackdropFilter: 'blur(12px)',
      borderBottom: '1px solid var(--border-color)',
      boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.04)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
      padding: '0.65rem 1.25rem',
    }}>
      <div className="navbar-container">
        {/* Brand Logo */}
        <Link to="/dashboard" style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', textDecoration: 'none' }}>
          <div style={{
            background: 'linear-gradient(135deg, #2563eb 0%, #4f46e5 100%)',
            padding: '0.45rem',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            boxShadow: '0 2px 8px rgba(37, 99, 235, 0.3)',
          }}>
            <Sparkles size={20} />
          </div>
          <div>
            <span style={{ fontSize: '1.2rem', fontWeight: 800, letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
              Tender<span style={{ color: 'var(--accent-primary)' }}>Match</span>
            </span>
            <span style={{
              display: 'block',
              fontSize: '0.65rem',
              color: 'var(--text-muted)',
              textTransform: 'uppercase',
              letterSpacing: '0.08em',
              marginTop: -3,
              fontWeight: 600,
            }}>
              AI Matching Engine
            </span>
          </div>
        </Link>

        {/* Navigation Links */}
        <nav className="navbar-nav" style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <Link
            to="/dashboard"
            className={`btn ${location.pathname === '/dashboard' ? 'btn-primary' : 'btn-secondary'}`}
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.825rem', whiteSpace: 'nowrap' }}
          >
            <LayoutDashboard size={15} />
            Dashboard
          </Link>
          <Link
            to="/tenders/new"
            className={`btn ${location.pathname === '/tenders/new' ? 'btn-primary' : 'btn-secondary'}`}
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.825rem', whiteSpace: 'nowrap' }}
          >
            <PlusCircle size={15} />
            New Match
          </Link>
          <Link
            to="/admin/models"
            className={`btn ${location.pathname === '/admin/models' ? 'btn-primary' : 'btn-secondary'}`}
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.825rem', whiteSpace: 'nowrap' }}
          >
            <Cpu size={15} />
            AI Models
          </Link>
        </nav>

        {/* User Profile & Actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              {user?.name || 'User'}
            </div>
            <div className="navbar-user-email" style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
              {user?.email}
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="btn btn-secondary"
            title="Log Out"
            style={{ padding: '0.45rem', borderRadius: 'var(--radius-md)' }}
          >
            <LogOut size={16} />
          </button>
        </div>
      </div>
    </header>
  );
};
