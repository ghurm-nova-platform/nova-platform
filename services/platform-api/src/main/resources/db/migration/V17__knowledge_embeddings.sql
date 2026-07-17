-- Knowledge embeddings, agent assignments, and retrieval audit.

CREATE TABLE knowledge_embeddings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    knowledge_base_id UUID NOT NULL,
    document_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    provider_key VARCHAR(100) NOT NULL,
    model VARCHAR(255) NOT NULL,
    dimensions INTEGER NOT NULL,
    embedding TEXT NOT NULL,
    embedding_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ke_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ke_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_ke_knowledge_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases (id),
    CONSTRAINT fk_ke_document FOREIGN KEY (document_id) REFERENCES knowledge_documents (id),
    CONSTRAINT fk_ke_chunk FOREIGN KEY (chunk_id) REFERENCES knowledge_chunks (id),
    CONSTRAINT uq_ke_chunk_provider_model UNIQUE (chunk_id, provider_key, model),
    CONSTRAINT chk_ke_dimensions CHECK (dimensions BETWEEN 8 AND 4096)
);

CREATE INDEX idx_ke_knowledge_base_id ON knowledge_embeddings (knowledge_base_id);
CREATE INDEX idx_ke_document_id ON knowledge_embeddings (document_id);
CREATE INDEX idx_ke_chunk_id ON knowledge_embeddings (chunk_id);
CREATE INDEX idx_ke_provider_key ON knowledge_embeddings (provider_key);

CREATE TABLE agent_knowledge_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    agent_id UUID NOT NULL,
    knowledge_base_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    top_k_override INTEGER,
    minimum_score_override DECIMAL(8, 6),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_aka_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_aka_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_aka_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_aka_knowledge_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases (id),
    CONSTRAINT fk_aka_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_aka_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_aka_agent_kb UNIQUE (agent_id, knowledge_base_id),
    CONSTRAINT chk_aka_top_k CHECK (top_k_override IS NULL OR top_k_override BETWEEN 1 AND 20),
    CONSTRAINT chk_aka_min_score CHECK (
        minimum_score_override IS NULL
        OR (minimum_score_override >= -1 AND minimum_score_override <= 1)
    )
);

CREATE INDEX idx_aka_agent_id ON agent_knowledge_assignments (agent_id);
CREATE INDEX idx_aka_knowledge_base_id ON agent_knowledge_assignments (knowledge_base_id);
CREATE INDEX idx_aka_project_id ON agent_knowledge_assignments (project_id);
CREATE INDEX idx_aka_enabled ON agent_knowledge_assignments (enabled);

CREATE TABLE knowledge_retrieval_audit (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    knowledge_base_id UUID NOT NULL,
    agent_id UUID,
    execution_id UUID,
    conversation_id UUID,
    query_hash VARCHAR(64) NOT NULL,
    query_character_count INTEGER NOT NULL,
    requested_top_k INTEGER NOT NULL,
    candidate_count INTEGER NOT NULL,
    returned_count INTEGER NOT NULL,
    minimum_score DECIMAL(8, 6),
    duration_ms BIGINT NOT NULL,
    performed_by UUID NOT NULL,
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    correlation_id VARCHAR(100),
    CONSTRAINT fk_kra_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_kra_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_kra_knowledge_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases (id),
    CONSTRAINT fk_kra_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_kra_execution FOREIGN KEY (execution_id) REFERENCES agent_executions (id),
    CONSTRAINT fk_kra_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_kra_performed_by FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT chk_kra_query_chars CHECK (query_character_count >= 0),
    CONSTRAINT chk_kra_top_k CHECK (requested_top_k >= 1),
    CONSTRAINT chk_kra_candidate_count CHECK (candidate_count >= 0),
    CONSTRAINT chk_kra_returned_count CHECK (returned_count >= 0),
    CONSTRAINT chk_kra_duration CHECK (duration_ms >= 0)
);

CREATE INDEX idx_kra_organization_id ON knowledge_retrieval_audit (organization_id);
CREATE INDEX idx_kra_project_id ON knowledge_retrieval_audit (project_id);
CREATE INDEX idx_kra_knowledge_base_id ON knowledge_retrieval_audit (knowledge_base_id);
CREATE INDEX idx_kra_execution_id ON knowledge_retrieval_audit (execution_id);
CREATE INDEX idx_kra_performed_at ON knowledge_retrieval_audit (performed_at);

-- Assign demo knowledge base to demo agent.
INSERT INTO agent_knowledge_assignments (
    id, organization_id, project_id, agent_id, knowledge_base_id, enabled,
    top_k_override, minimum_score_override,
    created_by, updated_by, created_at, updated_at, version
) VALUES (
    '88888888-8888-8888-8888-888888888811',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    '66666666-6666-6666-6666-666666666601',
    '88888888-8888-8888-8888-888888888801',
    TRUE,
    NULL,
    NULL,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);
