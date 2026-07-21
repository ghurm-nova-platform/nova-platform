-- Knowledge & Memory Engine (Sprint 6 Phase 2)

ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_source;
ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_source CHECK (source IN (
    'PORTAL', 'REST_API', 'SYSTEM', 'SCHEDULER', 'MERGE_AGENT', 'RELEASE_MANAGER',
    'DEPLOYMENT_OBSERVATION', 'ROLLBACK_MANAGER', 'RELEASE_POLICIES', 'ENVIRONMENT_MANAGEMENT',
    'ORCHESTRATION', 'PLANNER', 'CODING', 'REVIEW', 'TESTING', 'PATCH', 'GIT_INTEGRATION',
    'PULL_REQUEST', 'CI_OBSERVATION', 'REPAIR', 'APPROVAL_GATE', 'DEPLOYMENT_EXECUTION',
    'COLLABORATION', 'KNOWLEDGE'
));

ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_entity_type;
ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_entity_type CHECK (entity_type IN (
    'RELEASE', 'DEPLOYMENT', 'ROLLBACK', 'ENVIRONMENT', 'POLICY', 'MERGE', 'APPROVAL',
    'TASK', 'REPOSITORY', 'USER', 'CONFIGURATION', 'KNOWLEDGE'
));

ALTER TABLE audit_entities DROP CONSTRAINT IF EXISTS chk_audit_entities_type;
ALTER TABLE audit_entities ADD CONSTRAINT chk_audit_entities_type CHECK (entity_type IN (
    'RELEASE', 'DEPLOYMENT', 'ROLLBACK', 'ENVIRONMENT', 'POLICY', 'MERGE', 'APPROVAL',
    'TASK', 'REPOSITORY', 'USER', 'CONFIGURATION', 'KNOWLEDGE'
));

CREATE TABLE knowledge_engine_documents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID,
    title VARCHAR(500) NOT NULL,
    summary VARCHAR(2000),
    content TEXT NOT NULL,
    content_format VARCHAR(30) NOT NULL,
    knowledge_type VARCHAR(40) NOT NULL,
    category VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    visibility VARCHAR(30) NOT NULL,
    author_id UUID NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ke_documents_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ke_documents_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_ke_documents_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT chk_ke_documents_content_format CHECK (content_format IN (
        'MARKDOWN', 'PLAIN_TEXT', 'HTML', 'CODE', 'JSON', 'YAML', 'SQL', 'XML'
    )),
    CONSTRAINT chk_ke_documents_knowledge_type CHECK (knowledge_type IN (
        'PROJECT', 'CODE', 'DOCUMENTATION', 'ADR', 'PULL_REQUEST', 'RELEASE', 'DEPLOYMENT',
        'PIPELINE', 'TEST', 'BUG', 'FIX', 'DECISION', 'BEST_PRACTICE', 'RUNBOOK', 'API'
    )),
    CONSTRAINT chk_ke_documents_category CHECK (category IN (
        'Architecture', 'Backend', 'Frontend', 'Database', 'Infrastructure', 'Security',
        'Testing', 'Deployment', 'Operations', 'AI', 'General'
    )),
    CONSTRAINT chk_ke_documents_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED', 'DELETED')),
    CONSTRAINT chk_ke_documents_visibility CHECK (visibility IN (
        'PRIVATE', 'PROJECT', 'ORGANIZATION', 'PUBLIC'
    ))
);

CREATE INDEX idx_ke_documents_org ON knowledge_engine_documents (organization_id, created_at DESC);
CREATE INDEX idx_ke_documents_project ON knowledge_engine_documents (organization_id, project_id);
CREATE INDEX idx_ke_documents_status ON knowledge_engine_documents (organization_id, status);
CREATE INDEX idx_ke_documents_type ON knowledge_engine_documents (organization_id, knowledge_type);
CREATE INDEX idx_ke_documents_category ON knowledge_engine_documents (organization_id, category);
CREATE INDEX idx_ke_documents_author ON knowledge_engine_documents (organization_id, author_id);

CREATE TABLE knowledge_engine_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    chunk_number INTEGER NOT NULL,
    start_offset INTEGER NOT NULL,
    end_offset INTEGER NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ke_chunks_document FOREIGN KEY (document_id) REFERENCES knowledge_engine_documents (id),
    CONSTRAINT uq_ke_chunks_document_number UNIQUE (document_id, chunk_number),
    CONSTRAINT chk_ke_chunks_offsets CHECK (start_offset >= 0 AND end_offset > start_offset)
);

CREATE INDEX idx_ke_chunks_document ON knowledge_engine_chunks (document_id, chunk_number);
CREATE INDEX idx_ke_chunks_org ON knowledge_engine_chunks (organization_id);

CREATE TABLE knowledge_engine_tags (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ke_tags_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_ke_tags_org_name UNIQUE (organization_id, name)
);

CREATE INDEX idx_ke_tags_org ON knowledge_engine_tags (organization_id, name);

CREATE TABLE knowledge_engine_document_tags (
    document_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (document_id, tag_id),
    CONSTRAINT fk_ke_document_tags_document FOREIGN KEY (document_id) REFERENCES knowledge_engine_documents (id),
    CONSTRAINT fk_ke_document_tags_tag FOREIGN KEY (tag_id) REFERENCES knowledge_engine_tags (id)
);

CREATE INDEX idx_ke_document_tags_tag ON knowledge_engine_document_tags (tag_id);

CREATE TABLE knowledge_engine_relations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    source_document_id UUID NOT NULL,
    target_document_id UUID,
    relation_type VARCHAR(40) NOT NULL,
    target_ref_id UUID,
    target_ref_type VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ke_relations_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ke_relations_source FOREIGN KEY (source_document_id) REFERENCES knowledge_engine_documents (id),
    CONSTRAINT fk_ke_relations_target FOREIGN KEY (target_document_id) REFERENCES knowledge_engine_documents (id),
    CONSTRAINT chk_ke_relations_type CHECK (relation_type IN (
        'REFERENCES', 'RELATED_ADR', 'RELATED_PR', 'RELATED_RELEASE', 'RELATED_DEPLOYMENT',
        'RELATED_PROJECT', 'RELATED_DECISION'
    ))
);

CREATE INDEX idx_ke_relations_source ON knowledge_engine_relations (source_document_id);
CREATE INDEX idx_ke_relations_target ON knowledge_engine_relations (target_document_id);
CREATE INDEX idx_ke_relations_org ON knowledge_engine_relations (organization_id);

CREATE TABLE knowledge_engine_attachments (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    storage_ref VARCHAR(1000) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ke_attachments_document FOREIGN KEY (document_id) REFERENCES knowledge_engine_documents (id)
);

CREATE INDEX idx_ke_attachments_document ON knowledge_engine_attachments (document_id);

CREATE TABLE knowledge_engine_access_logs (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    user_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ke_access_logs_document FOREIGN KEY (document_id) REFERENCES knowledge_engine_documents (id),
    CONSTRAINT fk_ke_access_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_ke_access_logs_document ON knowledge_engine_access_logs (document_id, created_at DESC);
CREATE INDEX idx_ke_access_logs_org ON knowledge_engine_access_logs (organization_id, created_at DESC);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333333913', 'KNOWLEDGE_WRITE', 'Write knowledge documents', 'Create, update, import, and relate knowledge engine documents', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333914', 'KNOWLEDGE_ADMIN', 'Administer knowledge documents', 'Archive, restore, and delete knowledge engine documents', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333333913'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333333914'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333333913'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333333914'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333333913'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333333913');
