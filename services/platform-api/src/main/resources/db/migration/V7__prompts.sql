-- Prompt management: prompts, versions, variables, tags, audit.

CREATE TABLE prompts (
    id                        UUID PRIMARY KEY,
    organization_id           UUID NOT NULL,
    project_id                UUID NOT NULL,
    name                      VARCHAR(255) NOT NULL,
    description               VARCHAR(2000),
    prompt_type               VARCHAR(50) NOT NULL,
    status                    VARCHAR(30) NOT NULL,
    current_draft_version_id  UUID,
    published_version_id      UUID,
    created_by                UUID NOT NULL,
    updated_by                UUID NOT NULL,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                   INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_prompts_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_prompts_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_prompts_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_prompts_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_prompts_project_name UNIQUE (project_id, name),
    CONSTRAINT ck_prompts_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_prompts_type CHECK (prompt_type IN (
        'CHAT', 'SYSTEM', 'CODING', 'TRANSLATION', 'SQL', 'REPORT',
        'SUMMARIZATION', 'CLASSIFICATION', 'EXTRACTION', 'RAG', 'CUSTOM'
    ))
);

CREATE INDEX idx_prompts_organization_id ON prompts (organization_id);
CREATE INDEX idx_prompts_project_id ON prompts (project_id);
CREATE INDEX idx_prompts_status ON prompts (status);
CREATE INDEX idx_prompts_prompt_type ON prompts (prompt_type);
CREATE INDEX idx_prompts_created_at ON prompts (created_at);
CREATE INDEX idx_prompts_updated_at ON prompts (updated_at);

CREATE TABLE prompt_versions (
    id               UUID PRIMARY KEY,
    prompt_id        UUID NOT NULL,
    organization_id  UUID NOT NULL,
    project_id       UUID NOT NULL,
    version_number   INTEGER NOT NULL,
    content          TEXT NOT NULL,
    change_summary   VARCHAR(1000),
    status           VARCHAR(30) NOT NULL,
    created_by       UUID NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_by     UUID,
    published_at     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_prompt_versions_prompt
        FOREIGN KEY (prompt_id) REFERENCES prompts (id) ON DELETE CASCADE,
    CONSTRAINT fk_prompt_versions_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_prompt_versions_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_prompt_versions_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_prompt_versions_published_by
        FOREIGN KEY (published_by) REFERENCES users (id),
    CONSTRAINT uq_prompt_versions_number UNIQUE (prompt_id, version_number),
    CONSTRAINT ck_prompt_versions_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'SUPERSEDED', 'ARCHIVED')),
    CONSTRAINT ck_prompt_versions_content CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT ck_prompt_versions_number CHECK (version_number > 0)
);

CREATE INDEX idx_prompt_versions_prompt_id ON prompt_versions (prompt_id);
CREATE INDEX idx_prompt_versions_organization_id ON prompt_versions (organization_id);
CREATE INDEX idx_prompt_versions_project_id ON prompt_versions (project_id);
CREATE INDEX idx_prompt_versions_status ON prompt_versions (status);
CREATE INDEX idx_prompt_versions_created_at ON prompt_versions (created_at);

ALTER TABLE prompts
    ADD CONSTRAINT fk_prompts_current_draft
        FOREIGN KEY (current_draft_version_id) REFERENCES prompt_versions (id);
ALTER TABLE prompts
    ADD CONSTRAINT fk_prompts_published_version
        FOREIGN KEY (published_version_id) REFERENCES prompt_versions (id);

CREATE TABLE prompt_variables (
    id                 UUID PRIMARY KEY,
    prompt_version_id  UUID NOT NULL,
    name               VARCHAR(100) NOT NULL,
    description        VARCHAR(500),
    data_type          VARCHAR(30) NOT NULL,
    required_flag      BOOLEAN NOT NULL DEFAULT FALSE,
    default_value      TEXT,
    sample_value       TEXT,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prompt_variables_version
        FOREIGN KEY (prompt_version_id) REFERENCES prompt_versions (id) ON DELETE CASCADE,
    CONSTRAINT uq_prompt_variables_name UNIQUE (prompt_version_id, name),
    CONSTRAINT ck_prompt_variables_data_type CHECK (data_type IN (
        'STRING', 'NUMBER', 'BOOLEAN', 'DATE', 'DATETIME', 'JSON', 'ARRAY', 'OBJECT'
    ))
);

CREATE INDEX idx_prompt_variables_version_id ON prompt_variables (prompt_version_id);

CREATE TABLE prompt_tags (
    id          UUID PRIMARY KEY,
    prompt_id   UUID NOT NULL,
    tag_name    VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prompt_tags_prompt
        FOREIGN KEY (prompt_id) REFERENCES prompts (id) ON DELETE CASCADE,
    CONSTRAINT uq_prompt_tags_name UNIQUE (prompt_id, tag_name)
);

CREATE INDEX idx_prompt_tags_prompt_id ON prompt_tags (prompt_id);

CREATE TABLE prompt_audit_log (
    id                  UUID PRIMARY KEY,
    prompt_id           UUID NOT NULL,
    prompt_version_id   UUID,
    organization_id     UUID NOT NULL,
    project_id          UUID NOT NULL,
    action              VARCHAR(50) NOT NULL,
    old_value           TEXT,
    new_value           TEXT,
    performed_by        UUID NOT NULL,
    performed_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id      VARCHAR(100),
    CONSTRAINT fk_prompt_audit_prompt
        FOREIGN KEY (prompt_id) REFERENCES prompts (id) ON DELETE CASCADE,
    CONSTRAINT fk_prompt_audit_version
        FOREIGN KEY (prompt_version_id) REFERENCES prompt_versions (id),
    CONSTRAINT fk_prompt_audit_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_prompt_audit_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_prompt_audit_user
        FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT ck_prompt_audit_action CHECK (action IN (
        'CREATED', 'UPDATED', 'DRAFT_CREATED', 'VERSION_UPDATED', 'PUBLISHED',
        'ROLLED_BACK', 'ARCHIVED', 'RESTORED', 'TAG_ADDED', 'TAG_REMOVED'
    ))
);

CREATE INDEX idx_prompt_audit_prompt_id ON prompt_audit_log (prompt_id);
CREATE INDEX idx_prompt_audit_organization_id ON prompt_audit_log (organization_id);
CREATE INDEX idx_prompt_audit_project_id ON prompt_audit_log (project_id);
