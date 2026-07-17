CREATE TABLE agents (
    id               UUID PRIMARY KEY,
    organization_id  UUID NOT NULL,
    project_id       UUID NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(2000),
    system_prompt    TEXT NOT NULL,
    model_provider   VARCHAR(64) NOT NULL,
    model_name       VARCHAR(128) NOT NULL,
    temperature      DECIMAL(4, 2) NOT NULL,
    max_tokens       INTEGER,
    status           VARCHAR(32) NOT NULL,
    visibility       VARCHAR(32) NOT NULL,
    version          INTEGER NOT NULL DEFAULT 0,
    created_by       UUID NOT NULL,
    updated_by       UUID NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agents_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_agents_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_agents_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_agents_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_agents_project_name UNIQUE (project_id, name),
    CONSTRAINT ck_agents_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED')),
    CONSTRAINT ck_agents_visibility CHECK (visibility IN ('PRIVATE', 'PROJECT', 'ORGANIZATION')),
    CONSTRAINT ck_agents_temperature CHECK (temperature >= 0 AND temperature <= 2),
    CONSTRAINT ck_agents_max_tokens CHECK (max_tokens IS NULL OR max_tokens > 0)
);

CREATE INDEX idx_agents_organization_id ON agents (organization_id);
CREATE INDEX idx_agents_project_id ON agents (project_id);
CREATE INDEX idx_agents_status ON agents (status);
CREATE INDEX idx_agents_created_at ON agents (created_at);

CREATE TABLE agent_audit_log (
    id               UUID PRIMARY KEY,
    agent_id         UUID NOT NULL,
    organization_id  UUID NOT NULL,
    project_id       UUID NOT NULL,
    action           VARCHAR(32) NOT NULL,
    old_value        TEXT,
    new_value        TEXT,
    performed_by     UUID NOT NULL,
    performed_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_audit_agent FOREIGN KEY (agent_id) REFERENCES agents (id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_audit_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_agent_audit_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_agent_audit_user FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT ck_agent_audit_action CHECK (action IN ('CREATED', 'UPDATED', 'ACTIVATED', 'PAUSED', 'ARCHIVED'))
);

CREATE INDEX idx_agent_audit_agent_id ON agent_audit_log (agent_id);
CREATE INDEX idx_agent_audit_org_id ON agent_audit_log (organization_id);
