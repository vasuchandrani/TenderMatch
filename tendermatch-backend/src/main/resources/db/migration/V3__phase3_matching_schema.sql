-- ====================================================================
-- Phase 3: Matching Engine Schema Migration
-- ====================================================================

CREATE TABLE IF NOT EXISTS match_results (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tender_id               UUID NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    vendor_submission_id    UUID NOT NULL REFERENCES vendor_submissions(id) ON DELETE CASCADE,
    tender_spec_id          UUID NOT NULL REFERENCES tender_specs(id) ON DELETE CASCADE,
    vendor_spec_id          UUID REFERENCES vendor_specs(id) ON DELETE SET NULL,
    tender_product_id       UUID NOT NULL REFERENCES tender_products(id) ON DELETE CASCADE,
    vendor_product_id       UUID REFERENCES vendor_products(id) ON DELETE SET NULL,

    -- Comparison details
    comparison_operator     VARCHAR(20),
    comparison_method       VARCHAR(50),

    -- Scores and Signals
    cross_encoder_score     DOUBLE PRECISION,
    llm_verdict             VARCHAR(20),
    llm_reasoning           TEXT,
    numeric_comparison      TEXT,

    -- Final System Output
    system_verdict          VARCHAR(20) NOT NULL,
    confidence_score        DOUBLE PRECISION NOT NULL,
    confidence_features     TEXT,
    details                 TEXT,

    -- Human Review
    human_verdict           VARCHAR(20),
    reviewed_at             TIMESTAMP WITH TIME ZONE,
    reviewed_by             UUID REFERENCES users(id),

    created_at              TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_match_results_tender ON match_results(tender_id);
CREATE INDEX IF NOT EXISTS idx_match_results_vendor ON match_results(vendor_submission_id);
CREATE INDEX IF NOT EXISTS idx_match_results_tender_spec ON match_results(tender_spec_id);
CREATE INDEX IF NOT EXISTS idx_match_results_verdict ON match_results(system_verdict);
