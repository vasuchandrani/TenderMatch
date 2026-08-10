-- ====================================================================
-- Phase 5: Scoring and Ranking Engine Schema Migration
-- ====================================================================

-- Scoring weights (per-tender or reusable template)
CREATE TABLE IF NOT EXISTS scoring_weights (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tender_id           UUID REFERENCES tenders(id) ON DELETE CASCADE,
    template_name       VARCHAR(255),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    canonical_spec_name VARCHAR(500) NOT NULL,
    weight_category     VARCHAR(50) NOT NULL DEFAULT 'medium',
    weight_value        DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scoring_weights_tender ON scoring_weights(tender_id);
CREATE INDEX IF NOT EXISTS idx_scoring_weights_template ON scoring_weights(template_name);

-- Vendor scores (calculated results and leaderboard rankings)
CREATE TABLE IF NOT EXISTS vendor_scores (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tender_id               UUID NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    vendor_submission_id    UUID NOT NULL REFERENCES vendor_submissions(id) ON DELETE CASCADE,
    
    overall_score           DOUBLE PRECISION NOT NULL,
    rank                    INTEGER,
    
    total_specs             INTEGER NOT NULL,
    matched_count           INTEGER NOT NULL,
    unmatched_count         INTEGER NOT NULL,
    uncertain_count         INTEGER NOT NULL,
    
    product_scores          TEXT,
    score_breakdown         TEXT,
    
    scoring_method          VARCHAR(50) NOT NULL DEFAULT 'weighted_average',
    weights_snapshot        TEXT,
    
    scored_at               TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT uq_vendor_scores_tender_vendor UNIQUE(tender_id, vendor_submission_id)
);

CREATE INDEX IF NOT EXISTS idx_vendor_scores_tender ON vendor_scores(tender_id);
CREATE INDEX IF NOT EXISTS idx_vendor_scores_rank ON vendor_scores(tender_id, rank);

-- Tender award outcomes (for downstream learning-to-rank training)
CREATE TABLE IF NOT EXISTS tender_outcomes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tender_id           UUID NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    winning_vendor_id   UUID REFERENCES vendor_submissions(id) ON DELETE SET NULL,
    outcome_notes       TEXT,
    recorded_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    recorded_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT uq_tender_outcomes_tender UNIQUE(tender_id)
);

CREATE INDEX IF NOT EXISTS idx_tender_outcomes_tender ON tender_outcomes(tender_id);
