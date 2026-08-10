-- ====================================================================
-- Phase 4: Review Workflow and Correction Logging Schema Migration
-- ====================================================================

-- Add review locking columns to vendor_submissions
ALTER TABLE vendor_submissions 
    ADD COLUMN IF NOT EXISTS is_review_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS review_finalized_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS review_finalized_by UUID REFERENCES users(id);

-- Correction logs: records every human confirmation and override for active learning
CREATE TABLE IF NOT EXISTS correction_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_result_id     UUID NOT NULL REFERENCES match_results(id) ON DELETE CASCADE,
    tender_id           UUID NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Verdict tracking
    system_verdict      VARCHAR(20) NOT NULL,
    human_verdict       VARCHAR(20) NOT NULL,
    is_override         BOOLEAN NOT NULL,
    is_auto_confirmed   BOOLEAN NOT NULL DEFAULT FALSE,

    -- Denormalized context snapshot at review time
    context             TEXT NOT NULL,
    user_comment        TEXT,

    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_correction_logs_tender ON correction_logs(tender_id);
CREATE INDEX IF NOT EXISTS idx_correction_logs_match_result ON correction_logs(match_result_id);
CREATE INDEX IF NOT EXISTS idx_correction_logs_is_override ON correction_logs(is_override);
CREATE INDEX IF NOT EXISTS idx_correction_logs_created ON correction_logs(created_at);
