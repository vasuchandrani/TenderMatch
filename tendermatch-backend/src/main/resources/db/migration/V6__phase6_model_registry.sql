-- Phase 6: Active Learning & Model Upgrades Schema Migration

-- 1. Model Registry: Tracks versioning, serialized model artifacts, training details, evaluation metrics, and deployment status
CREATE TABLE IF NOT EXISTS model_registry (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_name                  VARCHAR(100) NOT NULL,
    version                     INTEGER NOT NULL,
    model_path                  VARCHAR(1000) NOT NULL,
    training_data_count         INTEGER NOT NULL,
    trained_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    training_duration_seconds   INTEGER,
    eval_metrics                JSONB NOT NULL,
    status                      VARCHAR(50) NOT NULL DEFAULT 'trained',
    promoted_at                 TIMESTAMP WITH TIME ZONE,
    retired_at                  TIMESTAMP WITH TIME ZONE,
    created_at                  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_model_registry_name_version UNIQUE (model_name, version)
);

CREATE INDEX IF NOT EXISTS idx_model_registry_name ON model_registry(model_name);
CREATE INDEX IF NOT EXISTS idx_model_registry_status ON model_registry(model_name, status);

-- 2. Shadow Predictions: Captures dual predictions (rule-based vs ML) in shadow mode for agreement tracking & safety verification
CREATE TABLE IF NOT EXISTS shadow_predictions (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_name                  VARCHAR(100) NOT NULL,
    model_version               INTEGER NOT NULL,
    input_hash                  VARCHAR(64) NOT NULL,
    rule_result                 JSONB NOT NULL,
    ml_result                   JSONB NOT NULL,
    agreed                      BOOLEAN NOT NULL,
    human_verdict               VARCHAR(20),
    rule_correct                BOOLEAN,
    ml_correct                  BOOLEAN,
    created_at                  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shadow_predictions_model ON shadow_predictions(model_name, created_at);
CREATE INDEX IF NOT EXISTS idx_shadow_predictions_disagreement ON shadow_predictions(model_name, agreed) WHERE agreed = FALSE;
