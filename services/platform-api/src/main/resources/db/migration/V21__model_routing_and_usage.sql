-- Model routing policies, invocation records, and daily usage aggregation.

CREATE TABLE model_routing_policies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    agent_id UUID,
    policy_key VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(30) NOT NULL,
    strategy VARCHAR(50) NOT NULL,
    fallback_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    retry_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    maximum_provider_attempts INTEGER NOT NULL DEFAULT 2,
    maximum_total_duration_ms BIGINT NOT NULL DEFAULT 120000,
    require_tool_support BOOLEAN NOT NULL DEFAULT FALSE,
    require_knowledge_support BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_mrp_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_mrp_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_mrp_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_mrp_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_mrp_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_mrp_project_agent_key UNIQUE (project_id, agent_id, policy_key),
    CONSTRAINT chk_mrp_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_mrp_strategy CHECK (strategy IN (
        'FIXED_PRIMARY', 'PRIORITY_FALLBACK', 'CAPABILITY_BASED'
    )),
    CONSTRAINT chk_mrp_attempts CHECK (maximum_provider_attempts BETWEEN 1 AND 10),
    CONSTRAINT chk_mrp_duration CHECK (maximum_total_duration_ms BETWEEN 1000 AND 600000)
);

CREATE INDEX idx_mrp_organization_id ON model_routing_policies (organization_id);
CREATE INDEX idx_mrp_project_id ON model_routing_policies (project_id);
CREATE INDEX idx_mrp_agent_id ON model_routing_policies (agent_id);
CREATE INDEX idx_mrp_status ON model_routing_policies (status);
CREATE INDEX idx_mrp_strategy ON model_routing_policies (strategy);

CREATE TABLE model_invocations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    agent_id UUID NOT NULL,
    execution_id UUID NOT NULL,
    conversation_id UUID,
    provider_id UUID NOT NULL,
    model_id UUID NOT NULL,
    routing_policy_id UUID,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_request_id VARCHAR(255),
    input_character_count INTEGER NOT NULL,
    output_character_count INTEGER,
    estimated_input_tokens INTEGER,
    estimated_output_tokens INTEGER,
    provider_input_tokens INTEGER,
    provider_output_tokens INTEGER,
    duration_ms BIGINT,
    finish_reason VARCHAR(100),
    error_code VARCHAR(100),
    fallback_from_invocation_id UUID,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_by UUID NOT NULL,
    CONSTRAINT fk_mi_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_mi_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_mi_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_mi_execution FOREIGN KEY (execution_id) REFERENCES agent_executions (id),
    CONSTRAINT fk_mi_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_mi_provider FOREIGN KEY (provider_id) REFERENCES ai_providers (id),
    CONSTRAINT fk_mi_model FOREIGN KEY (model_id) REFERENCES ai_models (id),
    CONSTRAINT fk_mi_routing_policy FOREIGN KEY (routing_policy_id) REFERENCES model_routing_policies (id),
    CONSTRAINT fk_mi_fallback FOREIGN KEY (fallback_from_invocation_id) REFERENCES model_invocations (id),
    CONSTRAINT fk_mi_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_mi_execution_attempt UNIQUE (execution_id, attempt_number),
    CONSTRAINT chk_mi_status CHECK (status IN (
        'REQUESTED', 'RUNNING', 'COMPLETED', 'FAILED', 'TIMED_OUT', 'CANCELLED', 'RATE_LIMITED'
    )),
    CONSTRAINT chk_mi_attempt CHECK (attempt_number > 0),
    CONSTRAINT chk_mi_input_chars CHECK (input_character_count >= 0),
    CONSTRAINT chk_mi_output_chars CHECK (output_character_count IS NULL OR output_character_count >= 0),
    CONSTRAINT chk_mi_est_in CHECK (estimated_input_tokens IS NULL OR estimated_input_tokens >= 0),
    CONSTRAINT chk_mi_est_out CHECK (estimated_output_tokens IS NULL OR estimated_output_tokens >= 0),
    CONSTRAINT chk_mi_prov_in CHECK (provider_input_tokens IS NULL OR provider_input_tokens >= 0),
    CONSTRAINT chk_mi_prov_out CHECK (provider_output_tokens IS NULL OR provider_output_tokens >= 0),
    CONSTRAINT chk_mi_duration CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

CREATE INDEX idx_mi_execution_id ON model_invocations (execution_id);
CREATE INDEX idx_mi_conversation_id ON model_invocations (conversation_id);
CREATE INDEX idx_mi_provider_id ON model_invocations (provider_id);
CREATE INDEX idx_mi_model_id ON model_invocations (model_id);
CREATE INDEX idx_mi_status ON model_invocations (status);
CREATE INDEX idx_mi_started_at ON model_invocations (started_at);
CREATE INDEX idx_mi_project_id ON model_invocations (project_id);
CREATE INDEX idx_mi_organization_id ON model_invocations (organization_id);

CREATE TABLE model_usage_daily (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    model_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    request_count BIGINT NOT NULL DEFAULT 0,
    successful_request_count BIGINT NOT NULL DEFAULT 0,
    failed_request_count BIGINT NOT NULL DEFAULT 0,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(20, 8),
    currency_code VARCHAR(3),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_mud_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_mud_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_mud_provider FOREIGN KEY (provider_id) REFERENCES ai_providers (id),
    CONSTRAINT fk_mud_model FOREIGN KEY (model_id) REFERENCES ai_models (id),
    CONSTRAINT uq_mud_project_model_date UNIQUE (project_id, model_id, usage_date),
    CONSTRAINT chk_mud_request_count CHECK (request_count >= 0),
    CONSTRAINT chk_mud_success_count CHECK (successful_request_count >= 0),
    CONSTRAINT chk_mud_failed_count CHECK (failed_request_count >= 0),
    CONSTRAINT chk_mud_input_tokens CHECK (input_tokens >= 0),
    CONSTRAINT chk_mud_output_tokens CHECK (output_tokens >= 0),
    CONSTRAINT chk_mud_cost CHECK (estimated_cost IS NULL OR estimated_cost >= 0)
);

CREATE INDEX idx_mud_organization_id ON model_usage_daily (organization_id);
CREATE INDEX idx_mud_project_id ON model_usage_daily (project_id);
CREATE INDEX idx_mud_provider_id ON model_usage_daily (provider_id);
CREATE INDEX idx_mud_model_id ON model_usage_daily (model_id);
CREATE INDEX idx_mud_usage_date ON model_usage_daily (usage_date);

-- Demo project routing policy (PRIORITY_FALLBACK) for the demo agent.
INSERT INTO model_routing_policies (
    id, organization_id, project_id, agent_id, policy_key, name, description, status, strategy,
    fallback_enabled, retry_enabled, maximum_provider_attempts, maximum_total_duration_ms,
    require_tool_support, require_knowledge_support,
    created_by, updated_by, created_at, updated_at, version
) VALUES (
    '99999999-9999-9999-9999-999999999941',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    '66666666-6666-6666-6666-666666666601',
    'DEMO_AGENT_PRIORITY',
    'Demo agent priority fallback',
    'Default routing for demo agent',
    'ACTIVE',
    'PRIORITY_FALLBACK',
    TRUE,
    TRUE,
    2,
    120000,
    FALSE,
    FALSE,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);
