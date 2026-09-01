import React, { useState, useRef } from 'react';
import { UploadCloud, File, X, AlertCircle } from 'lucide-react';

export const FileDropzone = ({
  label,
  description,
  multiple = false,
  files = [],
  onFilesChange,
  accept = '.pdf,application/pdf',
  maxSizeBytes = 50 * 1024 * 1024,
}) => {
  const [isDragOver, setIsDragOver] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  const handleFiles = (newFileList) => {
    setError(null);
    const validFiles = [];

    for (let i = 0; i < newFileList.length; i++) {
      const file = newFileList[i];
      if (!file.name.toLowerCase().endsWith('.pdf') && file.type !== 'application/pdf') {
        setError(`"${file.name}" is not a PDF file.`);
        return;
      }
      if (file.size > maxSizeBytes) {
        setError(`"${file.name}" exceeds maximum allowed size of 50MB.`);
        return;
      }
      validFiles.push(file);
    }

    if (multiple) {
      onFilesChange([...files, ...validFiles]);
    } else {
      onFilesChange(validFiles.slice(0, 1));
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFiles(e.dataTransfer.files);
    }
  };

  const removeFile = (indexToRemove) => {
    onFilesChange(files.filter((_, idx) => idx !== indexToRemove));
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {label && (
        <div style={{ marginBottom: '0.75rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-primary)' }}>{label}</h3>
          {description && (
            <p style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
              {description}
            </p>
          )}
        </div>
      )}

      {/* Drop Target Box */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
        onClick={() => fileInputRef.current?.click()}
        style={{
          border: `2px dashed ${
            isDragOver
              ? 'var(--accent-primary)'
              : isHovered
              ? 'var(--accent-primary)'
              : '#cbd5e1'
          }`,
          borderRadius: 'var(--radius-lg)',
          backgroundColor: isDragOver
            ? 'rgba(37, 99, 235, 0.08)'
            : isHovered
            ? '#f1f5f9'
            : '#f8fafc',
          padding: '2.25rem 1.5rem',
          textAlign: 'center',
          cursor: 'pointer',
          transition: 'all 0.2s ease',
          flex: files.length > 0 ? '0 0 auto' : '1',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '0.75rem',
          boxShadow: isHovered ? '0 4px 12px rgba(37, 99, 235, 0.06)' : 'none',
        }}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept={accept}
          multiple={multiple}
          onChange={(e) => {
            if (e.target.files) handleFiles(e.target.files);
            e.target.value = '';
          }}
          style={{ display: 'none' }}
        />

        <div style={{
          background: isDragOver
            ? 'rgba(37, 99, 235, 0.15)'
            : isHovered
            ? 'rgba(37, 99, 235, 0.12)'
            : '#ffffff',
          padding: '0.9rem',
          borderRadius: '50%',
          color: isDragOver || isHovered ? 'var(--accent-primary)' : 'var(--accent-primary)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 2px 6px rgba(0, 0, 0, 0.06)',
          border: '1px solid #e2e8f0',
          transition: 'all 0.2s ease',
        }}>
          <UploadCloud size={28} />
        </div>

        <div>
          <p style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
            Drag & drop PDF {multiple ? 'files' : 'file'} here, or <span style={{ color: 'var(--accent-primary)', textDecoration: 'underline' }}>browse</span>
          </p>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.35rem', marginBottom: 0 }}>
            PDF format only • Max 50MB per file
          </p>
        </div>
      </div>

      {error && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          backgroundColor: 'var(--status-unmatch-bg)',
          color: 'var(--status-unmatch)',
          padding: '0.6rem 0.8rem',
          borderRadius: 'var(--radius-md)',
          fontSize: '0.825rem',
          marginTop: '0.75rem',
          border: '1px solid var(--status-unmatch-border)',
        }}>
          <AlertCircle size={16} />
          <span>{error}</span>
        </div>
      )}

      {/* Selected Files List */}
      {files.length > 0 && (
        <div style={{
          marginTop: '1rem',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.5rem',
          maxHeight: 220,
          overflowY: 'auto',
        }}>
          {files.map((file, idx) => (
            <div
              key={idx}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0.65rem 0.85rem',
                background: 'var(--bg-surface)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--bg-card-border)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', overflow: 'hidden' }}>
                <div style={{ color: 'var(--accent-primary)', flexShrink: 0 }}>
                  <File size={18} />
                </div>
                <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  <div style={{ fontSize: '0.875rem', fontWeight: 500, color: 'var(--text-primary)', textOverflow: 'ellipsis', overflow: 'hidden' }}>
                    {file.name}
                  </div>
                  <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
                    {formatFileSize(file.size)}
                  </div>
                </div>
              </div>

              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  removeFile(idx);
                }}
                className="btn"
                style={{
                  padding: '0.25rem',
                  color: 'var(--text-muted)',
                  background: 'transparent',
                }}
                title="Remove file"
              >
                <X size={16} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
