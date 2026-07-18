-- Knowledge bases, documents, content, and chunks.

CREATE TABLE knowledge_bases (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    knowledge_key VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(30) NOT NULL,
    embedding_provider_key VARCHAR(100) NOT NULL,
    embedding_model VARCHAR(255) NOT NULL,
    embedding_dimensions INTEGER NOT NULL,
    chunk_size INTEGER NOT NULL,
    chunk_overlap INTEGER NOT NULL,
    default_top_k INTEGER NOT NULL,
    minimum_score DECIMAL(8, 6),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_kb_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_kb_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_kb_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_kb_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_kb_project_key UNIQUE (project_id, knowledge_key),
    CONSTRAINT chk_kb_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_kb_embedding_dimensions CHECK (embedding_dimensions BETWEEN 8 AND 4096),
    CONSTRAINT chk_kb_chunk_size CHECK (chunk_size BETWEEN 100 AND 5000),
    CONSTRAINT chk_kb_chunk_overlap CHECK (chunk_overlap >= 0 AND chunk_overlap < chunk_size),
    CONSTRAINT chk_kb_default_top_k CHECK (default_top_k BETWEEN 1 AND 20),
    CONSTRAINT chk_kb_minimum_score CHECK (minimum_score IS NULL OR (minimum_score >= -1 AND minimum_score <= 1))
);

CREATE INDEX idx_kb_organization_id ON knowledge_bases (organization_id);
CREATE INDEX idx_kb_project_id ON knowledge_bases (project_id);
CREATE INDEX idx_kb_status ON knowledge_bases (status);
CREATE INDEX idx_kb_embedding_provider_key ON knowledge_bases (embedding_provider_key);
CREATE INDEX idx_kb_project_status ON knowledge_bases (project_id, status);

CREATE TABLE knowledge_documents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    knowledge_base_id UUID NOT NULL,
    document_key VARCHAR(100) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    media_type VARCHAR(255) NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    extracted_character_count INTEGER,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    ingestion_error_code VARCHAR(100),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_kd_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_kd_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_kd_knowledge_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases (id),
    CONSTRAINT fk_kd_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_kd_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_kd_kb_document_key UNIQUE (knowledge_base_id, document_key),
    CONSTRAINT chk_kd_document_type CHECK (document_type IN ('TEXT', 'MARKDOWN', 'PDF')),
    CONSTRAINT chk_kd_status CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED', 'ARCHIVED')),
    CONSTRAINT chk_kd_file_size CHECK (file_size_bytes > 0),
    CONSTRAINT chk_kd_extracted_chars CHECK (extracted_character_count IS NULL OR extracted_character_count >= 0),
    CONSTRAINT chk_kd_chunk_count CHECK (chunk_count >= 0)
);

-- Duplicate content for non-archived documents is enforced in service (H2-safe).
CREATE INDEX idx_kd_knowledge_base_id ON knowledge_documents (knowledge_base_id);
CREATE INDEX idx_kd_project_id ON knowledge_documents (project_id);
CREATE INDEX idx_kd_status ON knowledge_documents (status);
CREATE INDEX idx_kd_content_hash ON knowledge_documents (content_hash);
CREATE INDEX idx_kd_created_at ON knowledge_documents (created_at);
CREATE INDEX idx_kd_kb_status ON knowledge_documents (knowledge_base_id, status);

CREATE TABLE knowledge_document_content (
    document_id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    knowledge_base_id UUID NOT NULL,
    extracted_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_kdc_document FOREIGN KEY (document_id) REFERENCES knowledge_documents (id),
    CONSTRAINT fk_kdc_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_kdc_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_kdc_knowledge_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases (id)
);

CREATE TABLE knowledge_chunks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    knowledge_base_id UUID NOT NULL,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    character_start INTEGER NOT NULL,
    character_end INTEGER NOT NULL,
    token_estimate INTEGER,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_kc_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_kc_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_kc_knowledge_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases (id),
    CONSTRAINT fk_kc_document FOREIGN KEY (document_id) REFERENCES knowledge_documents (id),
    CONSTRAINT uq_kc_document_index UNIQUE (document_id, chunk_index),
    CONSTRAINT chk_kc_chunk_index CHECK (chunk_index >= 0),
    CONSTRAINT chk_kc_character_start CHECK (character_start >= 0),
    CONSTRAINT chk_kc_character_end CHECK (character_end > character_start),
    CONSTRAINT chk_kc_token_estimate CHECK (token_estimate IS NULL OR token_estimate >= 0)
);

CREATE INDEX idx_kc_knowledge_base_id ON knowledge_chunks (knowledge_base_id);
CREATE INDEX idx_kc_document_id ON knowledge_chunks (document_id);
CREATE INDEX idx_kc_chunk_index ON knowledge_chunks (chunk_index);
CREATE INDEX idx_kc_content_hash ON knowledge_chunks (content_hash);

-- Demo knowledge base for local development (DRAFT initially; tests activate as needed).
INSERT INTO knowledge_bases (
    id, organization_id, project_id, knowledge_key, name, description, status,
    embedding_provider_key, embedding_model, embedding_dimensions,
    chunk_size, chunk_overlap, default_top_k, minimum_score,
    created_by, updated_by, created_at, updated_at, version
) VALUES (
    '88888888-8888-8888-8888-888888888801',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    'PRODUCT_DOCUMENTATION',
    'Product documentation',
    'Demo knowledge base for local development',
    'ACTIVE',
    'DETERMINISTIC_LOCAL',
    'deterministic-v1',
    64,
    1000,
    150,
    5,
    0.000000,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);
