-- Environment Management (Sprint 4 Phase 5). Org/project-scoped environment metadata.
-- Does not deploy, rollback, execute scripts, store secrets, or modify releases/deployments.
-- Extends existing global deployment_environments catalog (V46 seeds preserved).

ALTER TABLE deployment_environments DROP CONSTRAINT IF EXISTS uq_deployment_environments_code;
ALTER TABLE deployment_environments DROP CONSTRAINT IF EXISTS chk_deployment_env_type;

ALTER TABLE deployment_environments ADD COLUMN organization_id UUID;
ALTER TABLE deployment_environments ADD COLUMN project_id UUID;
ALTER TABLE deployment_environments ADD COLUMN description VARCHAR(2000);
ALTER TABLE deployment_environments ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE deployment_environments ADD COLUMN region VARCHAR(80);
ALTER TABLE deployment_environments ADD COLUMN provider VARCHAR(80);
ALTER TABLE deployment_environments ADD COLUMN cluster_name VARCHAR(120);
ALTER TABLE deployment_environments ADD COLUMN namespace_name VARCHAR(120);
ALTER TABLE deployment_environments ADD COLUMN cloud_provider VARCHAR(80);
ALTER TABLE deployment_environments ADD COLUMN platform VARCHAR(80);
ALTER TABLE deployment_environments ADD COLUMN owner_name VARCHAR(120);
ALTER TABLE deployment_environments ADD COLUMN business_unit VARCHAR(120);
ALTER TABLE deployment_environments ADD COLUMN cost_center VARCHAR(80);
ALTER TABLE deployment_environments ADD COLUMN tags_json TEXT;
ALTER TABLE deployment_environments ADD COLUMN created_by UUID;
ALTER TABLE deployment_environments ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

UPDATE deployment_environments
SET status = 'ACTIVE',
    updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE deployment_environments ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE deployment_environments
    ADD CONSTRAINT fk_deployment_env_organization FOREIGN KEY (organization_id) REFERENCES organizations (id);
ALTER TABLE deployment_environments
    ADD CONSTRAINT fk_deployment_env_project FOREIGN KEY (project_id) REFERENCES projects (id);
ALTER TABLE deployment_environments
    ADD CONSTRAINT fk_deployment_env_created_by FOREIGN KEY (created_by) REFERENCES users (id);
ALTER TABLE deployment_environments
    ADD CONSTRAINT chk_deployment_env_type CHECK (environment_type IN (
        'DEVELOPMENT', 'TESTING', 'QA', 'STAGING', 'PRODUCTION', 'CUSTOM', 'DR'
    ));
ALTER TABLE deployment_environments
    ADD CONSTRAINT chk_deployment_env_status CHECK (status IN (
        'ACTIVE', 'DISABLED', 'MAINTENANCE', 'ARCHIVED'
    ));

CREATE UNIQUE INDEX uq_deployment_env_code ON deployment_environments (code);

CREATE UNIQUE INDEX uq_deployment_env_project_name
    ON deployment_environments (organization_id, project_id, name);

CREATE INDEX idx_deployment_env_org_project ON deployment_environments (organization_id, project_id, status);

CREATE TABLE environment_labels (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    label_key VARCHAR(120) NOT NULL,
    label_value VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_environment_labels_environment FOREIGN KEY (environment_id)
        REFERENCES deployment_environments (id) ON DELETE CASCADE,
    CONSTRAINT uq_environment_labels_key UNIQUE (environment_id, label_key)
);

CREATE INDEX idx_environment_labels_environment ON environment_labels (environment_id);

CREATE TABLE environment_variables_metadata (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    variable_name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    required BOOLEAN NOT NULL,
    masked BOOLEAN NOT NULL,
    scope VARCHAR(60) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_environment_variables_environment FOREIGN KEY (environment_id)
        REFERENCES deployment_environments (id) ON DELETE CASCADE,
    CONSTRAINT uq_environment_variables_name UNIQUE (environment_id, variable_name)
);

CREATE INDEX idx_environment_variables_environment ON environment_variables_metadata (environment_id);

CREATE TABLE environment_events (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    detail VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_environment_events_environment FOREIGN KEY (environment_id)
        REFERENCES deployment_environments (id) ON DELETE CASCADE,
    CONSTRAINT chk_environment_events_type CHECK (event_type IN (
        'CREATED', 'UPDATED', 'ENABLED', 'DISABLED', 'ARCHIVED', 'VALIDATED', 'IDEMPOTENT_RETURN', 'FAILED'
    ))
);

CREATE INDEX idx_environment_events_environment ON environment_events (environment_id, created_at);

CREATE TABLE environment_history (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    change_type VARCHAR(60) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_environment_history_environment FOREIGN KEY (environment_id)
        REFERENCES deployment_environments (id) ON DELETE CASCADE,
    CONSTRAINT fk_environment_history_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_environment_history_change CHECK (change_type IN (
        'CREATED', 'UPDATED', 'STATUS_CHANGED'
    ))
);

CREATE INDEX idx_environment_history_environment ON environment_history (environment_id, created_at DESC);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331083', 'ENVIRONMENT_RUN', 'Run environment management', 'Create, update, enable, disable, and archive project environments', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331084', 'ENVIRONMENT_READ', 'Read environment management', 'View project environments and history', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331083'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331084'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331083'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331084'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331083'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331084'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331083'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331084');
