-- AI provider registry, model catalog, project availability, and agent assignments.

CREATE TABLE ai_providers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    provider_key VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    provider_type VARCHAR(50) NOT NULL,
    adapter_key VARCHAR(100) NOT NULL,
    credential_reference VARCHAR(500),
    region VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    request_timeout_seconds INTEGER NOT NULL DEFAULT 60,
    max_concurrent_requests INTEGER NOT NULL DEFAULT 10,
    max_retries INTEGER NOT NULL DEFAULT 1,
    retry_backoff_ms INTEGER NOT NULL DEFAULT 250,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_aip_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_aip_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_aip_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_aip_org_key UNIQUE (organization_id, provider_key),
    CONSTRAINT chk_aip_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT chk_aip_provider_type CHECK (provider_type IN (
        'DETERMINISTIC_LOCAL', 'OPENAI', 'AZURE_OPENAI', 'ANTHROPIC',
        'GOOGLE_GEMINI', 'AWS_BEDROCK', 'CUSTOM_MANAGED'
    )),
    CONSTRAINT chk_aip_timeout CHECK (request_timeout_seconds BETWEEN 1 AND 300),
    CONSTRAINT chk_aip_concurrency CHECK (max_concurrent_requests BETWEEN 1 AND 1000),
    CONSTRAINT chk_aip_retries CHECK (max_retries BETWEEN 0 AND 5),
    CONSTRAINT chk_aip_backoff CHECK (retry_backoff_ms BETWEEN 0 AND 10000)
);

CREATE INDEX idx_aip_organization_id ON ai_providers (organization_id);
CREATE INDEX idx_aip_provider_type ON ai_providers (provider_type);
CREATE INDEX idx_aip_status ON ai_providers (status);
CREATE INDEX idx_aip_adapter_key ON ai_providers (adapter_key);
CREATE INDEX idx_aip_org_status ON ai_providers (organization_id, status);

CREATE TABLE ai_models (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    model_key VARCHAR(100) NOT NULL,
    provider_model_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    model_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    context_window_tokens INTEGER NOT NULL,
    max_output_tokens INTEGER NOT NULL,
    supports_tools BOOLEAN NOT NULL DEFAULT FALSE,
    supports_knowledge_context BOOLEAN NOT NULL DEFAULT TRUE,
    supports_json_output BOOLEAN NOT NULL DEFAULT FALSE,
    supports_streaming BOOLEAN NOT NULL DEFAULT FALSE,
    supports_system_messages BOOLEAN NOT NULL DEFAULT TRUE,
    input_cost_per_million DECIMAL(18, 8),
    output_cost_per_million DECIMAL(18, 8),
    currency_code VARCHAR(3),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_aim_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_aim_provider FOREIGN KEY (provider_id) REFERENCES ai_providers (id),
    CONSTRAINT fk_aim_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_aim_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_aim_provider_key UNIQUE (provider_id, model_key),
    CONSTRAINT chk_aim_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT chk_aim_model_type CHECK (model_type IN (
        'TEXT_GENERATION', 'CHAT', 'REASONING', 'EMBEDDING', 'MULTIMODAL'
    )),
    CONSTRAINT chk_aim_context_window CHECK (context_window_tokens BETWEEN 256 AND 2000000),
    CONSTRAINT chk_aim_max_output CHECK (
        max_output_tokens >= 1 AND max_output_tokens <= context_window_tokens
    ),
    CONSTRAINT chk_aim_costs CHECK (
        (input_cost_per_million IS NULL OR input_cost_per_million >= 0)
        AND (output_cost_per_million IS NULL OR output_cost_per_million >= 0)
    ),
    CONSTRAINT chk_aim_currency CHECK (
        (input_cost_per_million IS NULL AND output_cost_per_million IS NULL)
        OR currency_code IS NOT NULL
    )
);

CREATE INDEX idx_aim_organization_id ON ai_models (organization_id);
CREATE INDEX idx_aim_provider_id ON ai_models (provider_id);
CREATE INDEX idx_aim_model_type ON ai_models (model_type);
CREATE INDEX idx_aim_status ON ai_models (status);
CREATE INDEX idx_aim_provider_status ON ai_models (provider_id, status);

CREATE TABLE project_models (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    model_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    maximum_input_tokens_override INTEGER,
    maximum_output_tokens_override INTEGER,
    daily_request_limit INTEGER,
    monthly_request_limit INTEGER,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_pm_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_pm_model FOREIGN KEY (model_id) REFERENCES ai_models (id),
    CONSTRAINT fk_pm_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_pm_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_pm_project_model UNIQUE (project_id, model_id),
    CONSTRAINT chk_pm_input_override CHECK (
        maximum_input_tokens_override IS NULL OR maximum_input_tokens_override > 0
    ),
    CONSTRAINT chk_pm_output_override CHECK (
        maximum_output_tokens_override IS NULL OR maximum_output_tokens_override > 0
    ),
    CONSTRAINT chk_pm_daily_limit CHECK (daily_request_limit IS NULL OR daily_request_limit > 0),
    CONSTRAINT chk_pm_monthly_limit CHECK (monthly_request_limit IS NULL OR monthly_request_limit > 0)
);

-- Single enabled default per project is enforced in service (H2-safe).
CREATE INDEX idx_pm_organization_id ON project_models (organization_id);
CREATE INDEX idx_pm_project_id ON project_models (project_id);
CREATE INDEX idx_pm_model_id ON project_models (model_id);
CREATE INDEX idx_pm_enabled ON project_models (enabled);
CREATE INDEX idx_pm_is_default ON project_models (is_default);

CREATE TABLE agent_model_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    agent_id UUID NOT NULL,
    model_id UUID NOT NULL,
    priority INTEGER NOT NULL DEFAULT 1,
    assignment_role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    temperature_override DECIMAL(5, 4),
    maximum_output_tokens_override INTEGER,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_ama_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ama_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_ama_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_ama_model FOREIGN KEY (model_id) REFERENCES ai_models (id),
    CONSTRAINT fk_ama_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_ama_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_ama_agent_model UNIQUE (agent_id, model_id),
    CONSTRAINT uq_ama_agent_role_priority UNIQUE (agent_id, assignment_role, priority),
    CONSTRAINT chk_ama_role CHECK (assignment_role IN ('PRIMARY', 'FALLBACK')),
    CONSTRAINT chk_ama_priority CHECK (priority BETWEEN 1 AND 10),
    CONSTRAINT chk_ama_temperature CHECK (
        temperature_override IS NULL
        OR (temperature_override >= 0 AND temperature_override <= 2)
    ),
    CONSTRAINT chk_ama_output_override CHECK (
        maximum_output_tokens_override IS NULL OR maximum_output_tokens_override > 0
    )
);

CREATE INDEX idx_ama_agent_id ON agent_model_assignments (agent_id);
CREATE INDEX idx_ama_model_id ON agent_model_assignments (model_id);
CREATE INDEX idx_ama_project_id ON agent_model_assignments (project_id);
CREATE INDEX idx_ama_assignment_role ON agent_model_assignments (assignment_role);
CREATE INDEX idx_ama_priority ON agent_model_assignments (priority);
CREATE INDEX idx_ama_enabled ON agent_model_assignments (enabled);

-- Demo deterministic provider + chat model + project/agent wiring.
INSERT INTO ai_providers (
    id, organization_id, provider_key, name, description, provider_type, adapter_key,
    credential_reference, region, status,
    request_timeout_seconds, max_concurrent_requests, max_retries, retry_backoff_ms,
    created_by, updated_by, created_at, updated_at, version
) VALUES (
    '99999999-9999-9999-9999-999999999901',
    '11111111-1111-1111-1111-111111111111',
    'NOVA_LOCAL',
    'Nova deterministic provider',
    'Local deterministic provider for development and tests',
    'DETERMINISTIC_LOCAL',
    'DETERMINISTIC_LOCAL',
    NULL,
    NULL,
    'ACTIVE',
    30,
    10,
    1,
    250,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO ai_models (
    id, organization_id, provider_id, model_key, provider_model_id, display_name, description,
    model_type, status, context_window_tokens, max_output_tokens,
    supports_tools, supports_knowledge_context, supports_json_output, supports_streaming,
    supports_system_messages, input_cost_per_million, output_cost_per_million, currency_code,
    created_by, updated_by, created_at, updated_at, version
) VALUES (
    '99999999-9999-9999-9999-999999999911',
    '11111111-1111-1111-1111-111111111111',
    '99999999-9999-9999-9999-999999999901',
    'DETERMINISTIC_CHAT_V1',
    'deterministic-chat-v1',
    'Deterministic Chat v1',
    'Deterministic local chat model for tests and development',
    'CHAT',
    'ACTIVE',
    8192,
    2048,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    TRUE,
    NULL,
    NULL,
    NULL,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO project_models (
    id, organization_id, project_id, model_id, enabled, is_default,
    maximum_input_tokens_override, maximum_output_tokens_override,
    daily_request_limit, monthly_request_limit,
    created_by, updated_by, created_at, updated_at, version
) VALUES (
    '99999999-9999-9999-9999-999999999921',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    '99999999-9999-9999-9999-999999999911',
    TRUE,
    TRUE,
    NULL,
    NULL,
    NULL,
    NULL,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO agent_model_assignments (
    id, organization_id, project_id, agent_id, model_id, priority, assignment_role, enabled,
    temperature_override, maximum_output_tokens_override,
    created_by, updated_by, created_at, updated_at, version
) VALUES (
    '99999999-9999-9999-9999-999999999931',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    '66666666-6666-6666-6666-666666666601',
    '99999999-9999-9999-9999-999999999911',
    1,
    'PRIMARY',
    TRUE,
    NULL,
    NULL,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);
