-- Deployment Observation (Sprint 4 Phase 2). Observe-only deployment lifecycle tracking.
-- Does not deploy, restart, rollback, modify releases/manifests/environments, or store secrets.

CREATE TABLE deployment_environments (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    environment_type VARCHAR(30) NOT NULL,
    sort_order INT NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_deployment_environments_code UNIQUE (code),
    CONSTRAINT chk_deployment_env_type CHECK (environment_type IN (
        'DEVELOPMENT', 'TESTING', 'QA', 'STAGING', 'PRODUCTION', 'CUSTOM'
    ))
);

INSERT INTO deployment_environments (id, code, name, environment_type, sort_order, active, created_at)
VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001', 'DEVELOPMENT', 'Development', 'DEVELOPMENT', 10, TRUE, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002', 'TESTING', 'Testing', 'TESTING', 20, TRUE, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003', 'QA', 'QA', 'QA', 30, TRUE, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004', 'STAGING', 'Staging', 'STAGING', 40, TRUE, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb005', 'PRODUCTION', 'Production', 'PRODUCTION', 50, TRUE, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb006', 'CUSTOM', 'Custom', 'CUSTOM', 90, TRUE, CURRENT_TIMESTAMP);

CREATE TABLE deployment_operations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    release_operation_id UUID NOT NULL,
    environment_id UUID NOT NULL,
    custom_environment_name VARCHAR(100),
    semantic_version VARCHAR(64) NOT NULL,
    release_manifest_hash VARCHAR(64),
    status VARCHAR(30) NOT NULL,
    health VARCHAR(30) NOT NULL,
    health_message VARCHAR(2000),
    deployment_provider VARCHAR(80) NOT NULL,
    external_deployment_key VARCHAR(255),
    deployment_hash VARCHAR(64) NOT NULL,
    triggered_by UUID,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    log_metadata VARCHAR(2000),
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deploy_ops_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_deploy_ops_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_deploy_ops_release FOREIGN KEY (release_operation_id) REFERENCES release_operations (id),
    CONSTRAINT fk_deploy_ops_environment FOREIGN KEY (environment_id) REFERENCES deployment_environments (id),
    CONSTRAINT fk_deploy_ops_triggered_by FOREIGN KEY (triggered_by) REFERENCES users (id),
    CONSTRAINT chk_deploy_ops_status CHECK (status IN (
        'PENDING', 'STARTING', 'RUNNING', 'VERIFYING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'UNKNOWN'
    )),
    CONSTRAINT chk_deploy_ops_health CHECK (health IN (
        'HEALTHY', 'WARNING', 'DEGRADED', 'FAILED', 'UNKNOWN'
    )),
    CONSTRAINT uq_deploy_ops_hash UNIQUE (organization_id, deployment_hash)
);

CREATE INDEX idx_deploy_ops_org_project_created ON deployment_operations (organization_id, project_id, created_at DESC);
CREATE INDEX idx_deploy_ops_release ON deployment_operations (release_operation_id);
CREATE INDEX idx_deploy_ops_env_status ON deployment_operations (organization_id, environment_id, status);

CREATE TABLE deployment_events (
    id UUID PRIMARY KEY,
    deployment_operation_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    detail VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deploy_events_operation FOREIGN KEY (deployment_operation_id)
        REFERENCES deployment_operations (id) ON DELETE CASCADE,
    CONSTRAINT chk_deploy_events_type CHECK (event_type IN (
        'OBSERVED', 'STATUS_CHANGED', 'HEALTH_CHANGED', 'VERIFY_STARTED', 'VERIFY_PASSED',
        'VERIFY_FAILED', 'COMPLETED', 'CANCELLED', 'IDEMPOTENT_RETURN'
    ))
);

CREATE INDEX idx_deploy_events_operation ON deployment_events (deployment_operation_id, created_at);

CREATE TABLE deployment_health (
    id UUID PRIMARY KEY,
    deployment_operation_id UUID NOT NULL,
    health VARCHAR(30) NOT NULL,
    message VARCHAR(2000),
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deploy_health_operation FOREIGN KEY (deployment_operation_id)
        REFERENCES deployment_operations (id) ON DELETE CASCADE,
    CONSTRAINT chk_deploy_health_level CHECK (health IN (
        'HEALTHY', 'WARNING', 'DEGRADED', 'FAILED', 'UNKNOWN'
    ))
);

CREATE INDEX idx_deploy_health_operation ON deployment_health (deployment_operation_id, observed_at DESC);

CREATE TABLE deployment_artifacts (
    id UUID PRIMARY KEY,
    deployment_operation_id UUID NOT NULL,
    artifact_type VARCHAR(80) NOT NULL,
    artifact_uri VARCHAR(2000) NOT NULL,
    artifact_hash VARCHAR(64),
    label VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deploy_artifacts_operation FOREIGN KEY (deployment_operation_id)
        REFERENCES deployment_operations (id) ON DELETE CASCADE
);

CREATE INDEX idx_deploy_artifacts_operation ON deployment_artifacts (deployment_operation_id);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331077', 'DEPLOYMENT_RUN', 'Run deployment observation', 'Observe and verify deployment state across environments', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331078', 'DEPLOYMENT_READ', 'Read deployment observations', 'View deployment observation records and history', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331077'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331078'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331077'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331078'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331077'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331078'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331077'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331078');
