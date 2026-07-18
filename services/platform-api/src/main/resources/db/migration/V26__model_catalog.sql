-- Evolve ai_models into a richer catalog; add provider model-sync metadata.

-- Widen model_key and migrate seed to lowercase catalog format.
ALTER TABLE ai_models ALTER COLUMN model_key VARCHAR(150) NOT NULL;

UPDATE ai_models
SET model_key = 'deterministic-chat-v1'
WHERE id = '99999999-9999-9999-9999-999999999911'
  AND model_key = 'DETERMINISTIC_CHAT_V1';

ALTER TABLE ai_models DROP CONSTRAINT uq_aim_provider_key;

ALTER TABLE ai_models ADD CONSTRAINT uq_aim_org_model_key UNIQUE (organization_id, model_key);
ALTER TABLE ai_models ADD CONSTRAINT uq_aim_provider_provider_model UNIQUE (provider_id, provider_model_id);

ALTER TABLE ai_models DROP CONSTRAINT chk_aim_status;
ALTER TABLE ai_models ADD CONSTRAINT chk_aim_status CHECK (
    status IN ('DRAFT', 'ACTIVE', 'DISABLED', 'DEPRECATED', 'ARCHIVED')
);

ALTER TABLE ai_models ADD COLUMN model_family VARCHAR(150);
ALTER TABLE ai_models ADD COLUMN model_version VARCHAR(100);
ALTER TABLE ai_models ADD COLUMN source VARCHAR(30);
ALTER TABLE ai_models ADD COLUMN context_window INTEGER;
ALTER TABLE ai_models ADD COLUMN max_input_tokens INTEGER;
ALTER TABLE ai_models ADD COLUMN default_temperature DECIMAL(5, 4);
ALTER TABLE ai_models ADD COLUMN default_top_p DECIMAL(5, 4);
ALTER TABLE ai_models ADD COLUMN default_max_output_tokens INTEGER;
ALTER TABLE ai_models ADD COLUMN currency VARCHAR(10);
ALTER TABLE ai_models ADD COLUMN discovered_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ai_models ADD COLUMN last_synced_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ai_models ADD COLUMN last_seen_at TIMESTAMP WITH TIME ZONE;

UPDATE ai_models
SET source = 'MANUAL'
WHERE source IS NULL;

UPDATE ai_models
SET context_window = context_window_tokens
WHERE context_window IS NULL;

UPDATE ai_models
SET currency = currency_code
WHERE currency IS NULL AND currency_code IS NOT NULL;

ALTER TABLE ai_models ALTER COLUMN source SET NOT NULL;

ALTER TABLE ai_models ADD CONSTRAINT chk_aim_source CHECK (source IN ('MANUAL', 'PROVIDER_SYNC'));
ALTER TABLE ai_models ADD CONSTRAINT chk_aim_model_key_format CHECK (
    model_key ~ '^[a-z0-9][a-z0-9._:-]{1,149}$'
);
ALTER TABLE ai_models ADD CONSTRAINT chk_aim_default_temperature CHECK (
    default_temperature IS NULL
    OR (default_temperature >= 0 AND default_temperature <= 2)
);
ALTER TABLE ai_models ADD CONSTRAINT chk_aim_default_top_p CHECK (
    default_top_p IS NULL
    OR (default_top_p > 0 AND default_top_p <= 1)
);
ALTER TABLE ai_models ADD CONSTRAINT chk_aim_max_input_tokens CHECK (
    max_input_tokens IS NULL OR max_input_tokens > 0
);
ALTER TABLE ai_models ADD CONSTRAINT chk_aim_default_max_output CHECK (
    default_max_output_tokens IS NULL
    OR (
        default_max_output_tokens > 0
        AND (max_output_tokens IS NULL OR default_max_output_tokens <= max_output_tokens)
    )
);

CREATE INDEX idx_aim_source ON ai_models (source);
CREATE INDEX idx_aim_org_status ON ai_models (organization_id, status);
CREATE INDEX idx_aim_last_synced_at ON ai_models (last_synced_at);

-- Provider model-sync summary for portal UX.
ALTER TABLE ai_providers ADD COLUMN last_model_sync_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ai_providers ADD COLUMN last_model_sync_status VARCHAR(30);
ALTER TABLE ai_providers ADD COLUMN last_model_sync_error_code VARCHAR(100);
ALTER TABLE ai_providers ADD COLUMN last_model_sync_discovered_count INTEGER;
ALTER TABLE ai_providers ADD COLUMN last_model_sync_created_count INTEGER;
ALTER TABLE ai_providers ADD COLUMN last_model_sync_updated_count INTEGER;
ALTER TABLE ai_providers ADD COLUMN last_model_sync_unchanged_count INTEGER;

ALTER TABLE ai_providers ADD CONSTRAINT chk_aip_model_sync_status CHECK (
    last_model_sync_status IS NULL
    OR last_model_sync_status IN ('SUCCESS', 'STALE', 'FAILED', 'UNSUPPORTED')
);
