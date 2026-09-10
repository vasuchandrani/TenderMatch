import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tendersApi } from '../services/api';
import { FileDropzone } from '../components/FileDropzone';
import { Sparkles, FileText, Users, ArrowRight, CheckCircle2, AlertCircle } from 'lucide-react';

export const NewTenderPage = () => {
  const [tenderName, setTenderName] = useState('');
  const [description, setDescription] = useState('');
  const [requirementFiles, setRequirementFiles] = useState([]);
  const [vendorFiles, setVendorFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadStep, setUploadStep] = useState('');
  const [error, setError] = useState(null);

  const navigate = useNavigate();

  const isFormValid =
    tenderName.trim().length > 0 &&
    requirementFiles.length === 1 &&
    vendorFiles.length >= 1;

  const handleStartMatch = async (e) => {
    e.preventDefault();
    if (!isFormValid || uploading) return;

    setError(null);
    setUploading(true);

    try {
      // Step 1: Create Tender Session
      setUploadStep('Creating tender session...');
      const tenderRes = await tendersApi.create(tenderName.trim(), description.trim());
      if (!tenderRes?.success || !tenderRes?.data?.id) {
        throw new Error('Failed to create tender session');
      }
      const tenderId = tenderRes.data.id;

      // Step 2: Upload Requirement Tender PDF (Left Box)
      setUploadStep('Uploading requirement tender document...');
      await tendersApi.uploadRequirement(tenderId, requirementFiles[0]);

      // Step 3: Upload Vendor Tender PDFs (Right Box)
      setUploadStep(`Uploading ${vendorFiles.length} vendor bid documents...`);
      await tendersApi.uploadVendors(tenderId, vendorFiles);

      // Step 4: Trigger Matching Process
      setUploadStep('Initiating AI matching engine...');
      await tendersApi.triggerMatch(tenderId);

      // Navigate to detail page
      navigate(`/tenders/${tenderId}`);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'An error occurred during upload. Please try again.');
    } finally {
      setUploading(false);
      setUploadStep('');
    }
  };

  return (
    <div className="main-content">
      {/* Header */}
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
          New Tender Match
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.25rem' }}>
          Upload the master requirement tender document on the left, and all competing vendor response tenders on the right.
        </p>
      </div>

      {error && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.6rem',
          backgroundColor: 'var(--status-unmatch-bg)',
          color: 'var(--status-unmatch)',
          border: '1px solid var(--status-unmatch-border)',
          padding: '0.85rem 1.25rem',
          borderRadius: 'var(--radius-md)',
          fontSize: '0.9rem',
          marginBottom: '1.5rem',
        }}>
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleStartMatch}>
        {/* Session Metadata Card */}
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.25rem' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" htmlFor="tenderName">
                Tender Session Name <span style={{ color: 'var(--status-unmatch)' }}>*</span>
              </label>
              <input
                id="tenderName"
                type="text"
                required
                className="form-input"
                placeholder="e.g. IT Equipment Procurement 2026 - Laptops & Monitors"
                value={tenderName}
                onChange={(e) => setTenderName(e.target.value)}
                disabled={uploading}
              />
            </div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" htmlFor="description">
                Project Notes / Description (Optional)
              </label>
              <input
                id="description"
                type="text"
                className="form-input"
                placeholder="e.g. RFP #9821 - Evaluating Dell, HP, and Lenovo offers"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                disabled={uploading}
              />
            </div>
          </div>
        </div>

        {/* The Two-Box Layout (Requirement on Left, Vendors on Right) */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))',
          gap: '1.5rem',
          marginBottom: '2rem',
        }}>
          {/* Left Box: Requirement Tender PDF */}
          <div className="card" style={{ display: 'flex', flexDirection: 'column' }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.6rem',
              marginBottom: '1rem',
              paddingBottom: '0.75rem',
              borderBottom: '1px solid var(--bg-card-border)',
            }}>
              <div style={{
                background: 'rgba(59, 130, 246, 0.15)',
                color: 'var(--accent-primary)',
                padding: '0.5rem',
                borderRadius: 'var(--radius-md)',
              }}>
                <FileText size={20} />
              </div>
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  1. Requirement Tender PDF
                </h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  The single master RFP or technical requirement specification
                </p>
              </div>
            </div>

            <FileDropzone
              multiple={false}
              files={requirementFiles}
              onFilesChange={setRequirementFiles}
            />
          </div>

          {/* Right Box: Vendor Tender PDFs */}
          <div className="card" style={{ display: 'flex', flexDirection: 'column' }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.6rem',
              marginBottom: '1rem',
              paddingBottom: '0.75rem',
              borderBottom: '1px solid var(--bg-card-border)',
            }}>
              <div style={{
                background: 'rgba(6, 182, 212, 0.15)',
                color: 'var(--accent-cyan)',
                padding: '0.5rem',
                borderRadius: 'var(--radius-md)',
              }}>
                <Users size={20} />
              </div>
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  2. Vendor / Filled Tender PDFs
                </h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Upload one or multiple vendor bids / compliance sheets
                </p>
              </div>
            </div>

            <FileDropzone
              multiple={true}
              files={vendorFiles}
              onFilesChange={setVendorFiles}
            />
          </div>
        </div>

        {/* Sticky Action Footer */}
        <div className="card" style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
          padding: '1.25rem 1.75rem',
        }}>
          <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            {!isFormValid ? (
              <span>
                Please provide a session name, 1 requirement PDF, and at least 1 vendor PDF to proceed.
              </span>
            ) : (
              <span style={{ color: 'var(--status-match)', display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 500 }}>
                <CheckCircle2 size={16} /> Ready: 1 requirement document + {vendorFiles.length} vendor {vendorFiles.length === 1 ? 'file' : 'files'}
              </span>
            )}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            {uploading && (
              <span style={{ fontSize: '0.875rem', color: 'var(--accent-primary)', fontWeight: 500 }}>
                {uploadStep}
              </span>
            )}

            <button
              type="submit"
              disabled={!isFormValid || uploading}
              className="btn btn-primary btn-lg"
              style={{ minWidth: 180 }}
            >
              {uploading ? (
                <>
                  <div style={{
                    width: 18,
                    height: 18,
                    border: '2px solid rgba(255, 255, 255, 0.3)',
                    borderTopColor: '#fff',
                    borderRadius: '50%',
                    animation: 'spin 0.8s linear infinite',
                  }} />
                  Processing...
                </>
              ) : (
                <>
                  <Sparkles size={18} />
                  Match Specifications
                </>
              )}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};
