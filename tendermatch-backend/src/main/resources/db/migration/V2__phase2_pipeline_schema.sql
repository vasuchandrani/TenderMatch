-- Phase 2: PDF Pipeline Schema

-- Products extracted from requirement tender
CREATE TABLE IF NOT EXISTS tender_products (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tender_id           UUID NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    product_name        VARCHAR(500) NOT NULL,
    product_category    VARCHAR(100) DEFAULT 'electronics',
    display_order       INTEGER NOT NULL DEFAULT 0,
    raw_text            TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tender_products_tender ON tender_products(tender_id);

-- Specifications extracted from requirement tender
CREATE TABLE IF NOT EXISTS tender_specs (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tender_product_id           UUID NOT NULL REFERENCES tender_products(id) ON DELETE CASCADE,
    raw_spec_name               VARCHAR(500) NOT NULL,
    canonical_spec_name         VARCHAR(500),
    spec_value                  TEXT NOT NULL,
    numeric_value               DOUBLE PRECISION,
    min_value                   DOUBLE PRECISION,
    max_value                   DOUBLE PRECISION,
    unit                        VARCHAR(50),
    is_numeric                  BOOLEAN NOT NULL DEFAULT FALSE,
    raw_text                    TEXT,
    extraction_confidence       VARCHAR(20) DEFAULT 'high',
    extraction_source           VARCHAR(20) DEFAULT 'llm',
    normalization_confidence    VARCHAR(20) DEFAULT 'high',
    display_order               INTEGER NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tender_specs_product ON tender_specs(tender_product_id);

-- Products extracted from vendor submissions
CREATE TABLE IF NOT EXISTS vendor_products (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_submission_id        UUID NOT NULL REFERENCES vendor_submissions(id) ON DELETE CASCADE,
    product_name                VARCHAR(500) NOT NULL,
    product_category            VARCHAR(100) DEFAULT 'electronics',
    matched_tender_product_id   UUID REFERENCES tender_products(id) ON DELETE SET NULL,
    match_confidence            DOUBLE PRECISION,
    display_order               INTEGER NOT NULL DEFAULT 0,
    raw_text                    TEXT,
    created_at                  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vendor_products_submission ON vendor_products(vendor_submission_id);
CREATE INDEX IF NOT EXISTS idx_vendor_products_tender_product ON vendor_products(matched_tender_product_id);

-- Specifications extracted from vendor submissions
CREATE TABLE IF NOT EXISTS vendor_specs (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_product_id           UUID NOT NULL REFERENCES vendor_products(id) ON DELETE CASCADE,
    raw_spec_name               VARCHAR(500) NOT NULL,
    canonical_spec_name         VARCHAR(500),
    spec_value                  TEXT NOT NULL,
    numeric_value               DOUBLE PRECISION,
    min_value                   DOUBLE PRECISION,
    max_value                   DOUBLE PRECISION,
    unit                        VARCHAR(50),
    is_numeric                  BOOLEAN NOT NULL DEFAULT FALSE,
    raw_text                    TEXT,
    extraction_confidence       VARCHAR(20) DEFAULT 'high',
    extraction_source           VARCHAR(20) DEFAULT 'llm',
    normalization_confidence    VARCHAR(20) DEFAULT 'high',
    matched_tender_spec_id      UUID REFERENCES tender_specs(id) ON DELETE SET NULL,
    display_order               INTEGER NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vendor_specs_product ON vendor_specs(vendor_product_id);
CREATE INDEX IF NOT EXISTS idx_vendor_specs_tender_spec ON vendor_specs(matched_tender_spec_id);

-- Spec ontology table (canonical dictionary)
CREATE TABLE IF NOT EXISTS spec_ontology (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_name      VARCHAR(500) NOT NULL UNIQUE,
    display_name        VARCHAR(500) NOT NULL,
    category            VARCHAR(100),
    default_unit        VARCHAR(50),
    default_operator    VARCHAR(10) DEFAULT '>=',
    synonyms            TEXT[],
    usage_count         INTEGER DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_spec_ontology_canonical ON spec_ontology(canonical_name);
