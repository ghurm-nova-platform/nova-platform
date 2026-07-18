-- Model capabilities and organization-scoped aliases.

CREATE TABLE ai_model_capabilities (
    model_id UUID NOT NULL,
    capability VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_aimc PRIMARY KEY (model_id, capability),
    CONSTRAINT fk_aimc_model FOREIGN KEY (model_id) REFERENCES ai_models (id),
    CONSTRAINT chk_aimc_capability CHECK (capability IN (
        'CHAT',
        'EMBEDDINGS',
        'VISION',
        'IMAGE_GENERATION',
        'IMAGE_UNDERSTANDING',
        'AUDIO_INPUT',
        'AUDIO_OUTPUT',
        'TRANSCRIPTION',
        'TEXT_TO_SPEECH',
        'FUNCTION_CALLING',
        'TOOL_CALLING',
        'PARALLEL_TOOL_CALLING',
        'JSON_MODE',
        'STRUCTURED_OUTPUT',
        'REASONING',
        'STREAMING',
        'BATCH',
        'FINE_TUNING'
    ))
);

CREATE INDEX idx_aimc_capability ON ai_model_capabilities (capability);
CREATE INDEX idx_aimc_enabled ON ai_model_capabilities (enabled);

CREATE TABLE ai_model_aliases (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    model_id UUID NOT NULL,
    alias VARCHAR(150) NOT NULL,
    normalized_alias VARCHAR(150) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_aima_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_aima_model FOREIGN KEY (model_id) REFERENCES ai_models (id),
    CONSTRAINT fk_aima_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_aima_org_normalized UNIQUE (organization_id, normalized_alias)
);

CREATE INDEX idx_aima_organization_id ON ai_model_aliases (organization_id);
CREATE INDEX idx_aima_model_id ON ai_model_aliases (model_id);
CREATE INDEX idx_aima_normalized_alias ON ai_model_aliases (normalized_alias);

-- Backfill capabilities for existing seed / legacy boolean flags.
INSERT INTO ai_model_capabilities (model_id, capability, enabled, metadata_json, created_at)
SELECT id, 'CHAT', TRUE, NULL, CURRENT_TIMESTAMP
FROM ai_models
WHERE model_type IN ('CHAT', 'TEXT_GENERATION', 'REASONING', 'MULTIMODAL');

INSERT INTO ai_model_capabilities (model_id, capability, enabled, metadata_json, created_at)
SELECT id, 'EMBEDDINGS', TRUE, NULL, CURRENT_TIMESTAMP
FROM ai_models
WHERE model_type = 'EMBEDDING';

INSERT INTO ai_model_capabilities (model_id, capability, enabled, metadata_json, created_at)
SELECT id, 'TOOL_CALLING', TRUE, NULL, CURRENT_TIMESTAMP
FROM ai_models
WHERE supports_tools = TRUE;

INSERT INTO ai_model_capabilities (model_id, capability, enabled, metadata_json, created_at)
SELECT id, 'FUNCTION_CALLING', TRUE, NULL, CURRENT_TIMESTAMP
FROM ai_models
WHERE supports_tools = TRUE;

INSERT INTO ai_model_capabilities (model_id, capability, enabled, metadata_json, created_at)
SELECT id, 'JSON_MODE', TRUE, NULL, CURRENT_TIMESTAMP
FROM ai_models
WHERE supports_json_output = TRUE;

INSERT INTO ai_model_capabilities (model_id, capability, enabled, metadata_json, created_at)
SELECT id, 'STREAMING', TRUE, NULL, CURRENT_TIMESTAMP
FROM ai_models
WHERE supports_streaming = TRUE;
