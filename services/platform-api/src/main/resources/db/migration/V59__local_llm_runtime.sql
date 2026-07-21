-- Local LLM Runtime (Sprint 6 Phase 5)

ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_source;
ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_source CHECK (source IN (
    'PORTAL', 'REST_API', 'SYSTEM', 'SCHEDULER', 'MERGE_AGENT', 'RELEASE_MANAGER',
    'DEPLOYMENT_OBSERVATION', 'ROLLBACK_MANAGER', 'RELEASE_POLICIES', 'ENVIRONMENT_MANAGEMENT',
    'ORCHESTRATION', 'PLANNER', 'CODING', 'REVIEW', 'TESTING', 'PATCH', 'GIT_INTEGRATION',
    'PULL_REQUEST', 'CI_OBSERVATION', 'REPAIR', 'APPROVAL_GATE', 'DEPLOYMENT_EXECUTION',
    'COLLABORATION', 'KNOWLEDGE', 'PR_REVIEW', 'IDENTITY', 'LLM_RUNTIME'
));

ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_entity_type;
ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_entity_type CHECK (entity_type IN (
    'RELEASE', 'DEPLOYMENT', 'ROLLBACK', 'ENVIRONMENT', 'POLICY', 'MERGE', 'APPROVAL',
    'TASK', 'REPOSITORY', 'USER', 'CONFIGURATION', 'KNOWLEDGE', 'PR_REVIEW', 'IDENTITY', 'LLM_RUNTIME'
));

ALTER TABLE audit_entities DROP CONSTRAINT IF EXISTS chk_audit_entities_type;
ALTER TABLE audit_entities ADD CONSTRAINT chk_audit_entities_type CHECK (entity_type IN (
    'RELEASE', 'DEPLOYMENT', 'ROLLBACK', 'ENVIRONMENT', 'POLICY', 'MERGE', 'APPROVAL',
    'TASK', 'REPOSITORY', 'USER', 'CONFIGURATION', 'KNOWLEDGE', 'PR_REVIEW', 'IDENTITY', 'LLM_RUNTIME'
));

CREATE TABLE llm_models (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    code VARCHAR(120) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    family VARCHAR(80) NOT NULL,
    provider_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    capabilities_json TEXT NOT NULL DEFAULT '[]',
    tags_json TEXT NOT NULL DEFAULT '[]',
    owner VARCHAR(255),
    context_length INT NOT NULL DEFAULT 4096,
    tokenizer VARCHAR(120),
    memory_mb INT,
    gpu_required BOOLEAN NOT NULL DEFAULT FALSE,
    cpu_cores INT,
    endpoint_url VARCHAR(1000),
    metadata_json TEXT NOT NULL DEFAULT '{}',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_llm_models_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_llm_models_org_code UNIQUE (organization_id, code),
    CONSTRAINT chk_llm_models_provider CHECK (provider_type IN (
        'OLLAMA', 'LLAMA_CPP', 'VLLM', 'OPENAI', 'AZURE_OPENAI', 'ANTHROPIC', 'GEMINI', 'MISTRAL', 'DETERMINISTIC'
    )),
    CONSTRAINT chk_llm_models_status CHECK (status IN (
        'REGISTERED', 'DOWNLOADING', 'INSTALLED', 'LOADING', 'READY', 'UNLOADING', 'STOPPED', 'ERROR', 'DISABLED'
    )),
    CONSTRAINT chk_llm_models_family CHECK (family IN (
        'LLAMA_3', 'LLAMA_3_1', 'LLAMA_3_2', 'QWEN', 'DEEPSEEK', 'PHI', 'GEMMA', 'MISTRAL',
        'TINYLLAMA', 'CODELLAMA', 'CUSTOM'
    ))
);

CREATE INDEX idx_llm_models_org_status ON llm_models (organization_id, status);
CREATE INDEX idx_llm_models_org_provider ON llm_models (organization_id, provider_type);

CREATE TABLE llm_model_versions (
    id UUID PRIMARY KEY,
    model_id UUID NOT NULL,
    version_label VARCHAR(80) NOT NULL,
    artifact_uri VARCHAR(1000),
    checksum VARCHAR(128),
    size_bytes BIGINT,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_llm_model_versions_model FOREIGN KEY (model_id) REFERENCES llm_models (id) ON DELETE CASCADE,
    CONSTRAINT uq_llm_model_versions UNIQUE (model_id, version_label)
);

CREATE INDEX idx_llm_model_versions_model ON llm_model_versions (model_id);

CREATE TABLE llm_prompt_templates (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    code VARCHAR(120) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(60) NOT NULL,
    system_prompt TEXT,
    user_prompt_template TEXT NOT NULL,
    assistant_prompt_template TEXT,
    variables_json TEXT NOT NULL DEFAULT '[]',
    template_version INT NOT NULL DEFAULT 1,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_llm_prompt_templates_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_llm_prompt_templates_org_code UNIQUE (organization_id, code),
    CONSTRAINT chk_llm_prompt_category CHECK (category IN (
        'CHAT', 'RAG', 'SUMMARIZATION', 'CLASSIFICATION', 'TRANSLATION', 'SQL_GENERATION',
        'CODE_GENERATION', 'PR_REVIEW', 'KNOWLEDGE_SEARCH', 'WORKFLOW_AUTOMATION', 'CUSTOM'
    ))
);

CREATE INDEX idx_llm_prompt_templates_org_cat ON llm_prompt_templates (organization_id, category);

CREATE TABLE llm_conversations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID,
    user_id UUID NOT NULL,
    model_id UUID,
    title VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    summary TEXT,
    token_usage_input INT NOT NULL DEFAULT 0,
    token_usage_output INT NOT NULL DEFAULT 0,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_llm_conversations_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_llm_conversations_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_llm_conversations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_llm_conversations_model FOREIGN KEY (model_id) REFERENCES llm_models (id),
    CONSTRAINT chk_llm_conversations_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

CREATE INDEX idx_llm_conversations_org_user ON llm_conversations (organization_id, user_id, updated_at DESC);

CREATE TABLE llm_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    token_count INT,
    sequence_no INT NOT NULL,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_llm_messages_conversation FOREIGN KEY (conversation_id) REFERENCES llm_conversations (id) ON DELETE CASCADE,
    CONSTRAINT chk_llm_messages_role CHECK (role IN ('SYSTEM', 'USER', 'ASSISTANT', 'TOOL')),
    CONSTRAINT uq_llm_messages_seq UNIQUE (conversation_id, sequence_no)
);

CREATE INDEX idx_llm_messages_conversation ON llm_messages (conversation_id, sequence_no);

CREATE TABLE llm_runtime_config (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    config_key VARCHAR(120) NOT NULL,
    config_value TEXT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_llm_runtime_config_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_llm_runtime_config UNIQUE (organization_id, config_key)
);

CREATE TABLE llm_usage_metrics (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    model_id UUID,
    conversation_id UUID,
    provider_type VARCHAR(40) NOT NULL,
    request_type VARCHAR(40) NOT NULL,
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    context_tokens INT NOT NULL DEFAULT 0,
    queue_time_ms BIGINT NOT NULL DEFAULT 0,
    inference_time_ms BIGINT NOT NULL DEFAULT 0,
    streaming_duration_ms BIGINT,
    success BOOLEAN NOT NULL,
    error_code VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_llm_usage_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_llm_usage_model FOREIGN KEY (model_id) REFERENCES llm_models (id),
    CONSTRAINT fk_llm_usage_conversation FOREIGN KEY (conversation_id) REFERENCES llm_conversations (id)
);

CREATE INDEX idx_llm_usage_org_created ON llm_usage_metrics (organization_id, created_at DESC);

CREATE TABLE llm_provider_status (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    provider_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    endpoint_url VARCHAR(1000),
    last_health_check_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_llm_provider_status_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_llm_provider_status UNIQUE (organization_id, provider_type),
    CONSTRAINT chk_llm_provider_status_type CHECK (provider_type IN (
        'OLLAMA', 'LLAMA_CPP', 'VLLM', 'OPENAI', 'AZURE_OPENAI', 'ANTHROPIC', 'GEMINI', 'MISTRAL', 'DETERMINISTIC'
    )),
    CONSTRAINT chk_llm_provider_status_value CHECK (status IN ('UNKNOWN', 'HEALTHY', 'DEGRADED', 'UNAVAILABLE', 'DISABLED'))
);

INSERT INTO permissions (id, code, name, description, created_at) VALUES
    ('33333333-3333-3333-3333-333333331109', 'LLM_READ', 'Read local LLM', 'View local LLM models, conversations, and configuration', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331110', 'LLM_ADMIN', 'Administer local LLM', 'Full local LLM runtime administration', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331111', 'LLM_INFER', 'Run local LLM inference', 'Invoke chat, completion, and streaming through the LLM gateway', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331112', 'LLM_MODEL_ADMIN', 'Administer LLM models', 'Register and manage local LLM models and lifecycle', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331113', 'LLM_PROMPT_ADMIN', 'Administer LLM prompts', 'Manage reusable LLM prompt templates', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331109'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331110'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331111'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331112'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331113'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331109'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331111'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331109'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331111');

-- Seed default deterministic model for demo org
INSERT INTO llm_models (
    id, organization_id, code, display_name, family, provider_type, status,
    capabilities_json, tags_json, owner, context_length, tokenizer, memory_mb,
    gpu_required, cpu_cores, endpoint_url, metadata_json, enabled, version, created_at, updated_at
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
    '11111111-1111-1111-1111-111111111111',
    'deterministic-chat-v1',
    'Deterministic Local Chat',
    'CUSTOM',
    'DETERMINISTIC',
    'READY',
    '["CHAT","COMPLETION"]',
    '["default","local"]',
    'system',
    8192,
    'none',
    256,
    FALSE,
    1,
    NULL,
    '{}',
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO llm_model_versions (id, model_id, version_label, artifact_uri, checksum, size_bytes, is_current, created_at)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
    '1.0.0',
    'local://deterministic-chat-v1',
    NULL,
    0,
    TRUE,
    CURRENT_TIMESTAMP
);

INSERT INTO llm_prompt_templates (
    id, organization_id, code, name, category, system_prompt, user_prompt_template,
    assistant_prompt_template, variables_json, template_version, enabled, version, created_at, updated_at
) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', '11111111-1111-1111-1111-111111111111',
     'chat.default', 'Default Chat', 'CHAT',
     'You are Nova, a helpful enterprise assistant.', '{{userMessage}}', NULL, '["userMessage"]', 1, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', '11111111-1111-1111-1111-111111111111',
     'rag.default', 'RAG Answer', 'RAG',
     'Answer using only the provided context. Cite sources when possible.',
     'Context:\n{{context}}\n\nQuestion:\n{{question}}', NULL, '["context","question"]', 1, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03', '11111111-1111-1111-1111-111111111111',
     'pr-review.default', 'PR Review', 'PR_REVIEW',
     'Review the pull request diff and provide structured findings.',
     'Diff:\n{{diff}}', NULL, '["diff"]', 1, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04', '11111111-1111-1111-1111-111111111111',
     'knowledge.search', 'Knowledge Search', 'KNOWLEDGE_SEARCH',
     'Search and synthesize knowledge documents.',
     'Query:\n{{query}}', NULL, '["query"]', 1, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO llm_provider_status (id, organization_id, provider_type, status, endpoint_url, last_health_check_at, last_error, metadata_json, version, updated_at)
VALUES
    ('cccccccc-cccc-cccc-cccc-cccccccccc01', '11111111-1111-1111-1111-111111111111', 'DETERMINISTIC', 'HEALTHY', NULL, CURRENT_TIMESTAMP, NULL, '{}', 0, CURRENT_TIMESTAMP),
    ('cccccccc-cccc-cccc-cccc-cccccccccc02', '11111111-1111-1111-1111-111111111111', 'OLLAMA', 'DISABLED', 'http://127.0.0.1:11434', NULL, NULL, '{}', 0, CURRENT_TIMESTAMP),
    ('cccccccc-cccc-cccc-cccc-cccccccccc03', '11111111-1111-1111-1111-111111111111', 'LLAMA_CPP', 'DISABLED', 'http://127.0.0.1:8080', NULL, NULL, '{}', 0, CURRENT_TIMESTAMP),
    ('cccccccc-cccc-cccc-cccc-cccccccccc04', '11111111-1111-1111-1111-111111111111', 'VLLM', 'DISABLED', 'http://127.0.0.1:8000', NULL, NULL, '{}', 0, CURRENT_TIMESTAMP);
