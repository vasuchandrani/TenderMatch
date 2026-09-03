import React from 'react';
import { Keyboard, X } from 'lucide-react';

const KeyboardShortcutHelp = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  const shortcuts = [
    { key: '1', desc: 'Select "Match"' },
    { key: '2', desc: 'Select "Unmatch"' },
    { key: '3', desc: 'Select "AI Can\'t Predict"' },
    { key: 'Enter', desc: 'Save Verdict & Advance to Next Spec' },
    { key: 'S', desc: 'Skip to Next Spec without Saving' },
    { key: 'N  or  →', desc: 'Next Specification' },
    { key: 'P  or  ←', desc: 'Previous Specification' },
    { key: '?', desc: 'Toggle This Help Dialog' },
  ];

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '1rem',
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '460px',
          padding: '1.75rem',
          borderRadius: '16px',
          position: 'relative',
        }}
      >
        <button
          type="button"
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '1rem',
            right: '1rem',
            background: 'transparent',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer',
          }}
        >
          <X size={20} />
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1.25rem' }}>
          <Keyboard size={24} style={{ color: 'var(--accent-primary)' }} />
          <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            Keyboard Shortcuts
          </h3>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '1.5rem' }}>
          {shortcuts.map(({ key, desc }) => (
            <div
              key={key}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '8px 12px',
                borderRadius: '8px',
                backgroundColor: 'var(--bg-surface)',
                border: '1px solid var(--border-color)',
              }}
            >
              <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>{desc}</span>
              <kbd
                style={{
                  padding: '3px 8px',
                  borderRadius: '6px',
                  backgroundColor: 'var(--tag-bg)',
                  border: '1px solid #cbd5e1',
                  boxShadow: '0 1px 2px rgba(0, 0, 0, 0.06)',
                  fontFamily: 'monospace',
                  fontSize: '0.8rem',
                  fontWeight: 700,
                  color: 'var(--accent-primary)',
                }}
              >
                {key}
              </kbd>
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={onClose}
          className="btn btn-primary"
          style={{ width: '100%', padding: '0.65rem' }}
        >
          Got it
        </button>
      </div>
    </div>
  );
};

export default KeyboardShortcutHelp;
