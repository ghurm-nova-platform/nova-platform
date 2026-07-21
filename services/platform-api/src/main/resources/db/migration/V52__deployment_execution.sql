-- Deployment Execution Engine (Sprint 5 Phase 1). Controlled deployments via pluggable providers.
-- Does not mutate releases, auto-rollback, blue/green, or canary.

ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_action;
ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_source;

ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_action CHECK (action IN (
    'CREATE', 'UPDATE', 'DELETE', 'ENABLE', 'DISABLE', 'ARCHIVE', 'APPROVE', 'REJECT',
    'MERGE', 'VALIDATE', 'OBSERVE', 'START', 'COMPLETE', 'FAIL', 'PREPARE', 'READY', 'PUBLISH',
    'QUEUE', 'VERIFY', 'CANCEL',
    'LOGIN', 'LOGOUT', 'ACCESS'
));

ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_source CHECK (source IN (
    'PORTAL', 'REST_API', 'SYSTEM', 'SCHEDULER', 'MERGE_AGENT', 'RELEASE_MANAGER',
    'DEPLOYMENT_OBSERVATION', 'ROLLBACK_MANAGER', 'RELEASE_POLICIES', 'ENVIRONMENT_MANAGEMENT',
    'ORCHESTRATION', 'PLANNER', 'CODING', 'REVIEW', 'TESTING', 'PATCH', 'GIT_INTEGRATION',
    'PULL_REQUEST', 'CI_OBSERVATION', 'REPAIR', 'APPROVAL_GATE', 'DEPLOYMENT_EXECUTION'
));

CREATE TABLE deployment_executions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    release_operation_id UUID NOT NULL,
    environment_id UUID NOT NULL,
    deployment_observation_id UUID,
    provider VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_step VARCHAR(80),
    current_stage VARCHAR(80),
    release_manifest_hash VARCHAR(64),
    release_content_fingerprint VARCHAR(64),
    execution_fingerprint VARCHAR(64) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    triggered_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    CONSTRAINT fk_deploy_exec_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_deploy_exec_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_deploy_exec_release FOREIGN KEY (release_operation_id) REFERENCES release_operations (id),
    CONSTRAINT fk_deploy_exec_environment FOREIGN KEY (environment_id) REFERENCES deployment_environments (id),
    CONSTRAINT fk_deploy_exec_observation FOREIGN KEY (deployment_observation_id) REFERENCES deployment_operations (id),
    CONSTRAINT fk_deploy_exec_triggered_by FOREIGN KEY (triggered_by) REFERENCES users (id),
    CONSTRAINT chk_deploy_exec_provider CHECK (provider IN (
        'LOCAL', 'REST', 'KUBERNETES', 'ARGOCD', 'HELM'
    )),
    CONSTRAINT chk_deploy_exec_status CHECK (status IN (
        'READY', 'QUEUED', 'STARTING', 'DEPLOYING', 'VERIFYING', 'COMPLETED', 'FAILED', 'CANCELLED'
    )),
    CONSTRAINT uq_deploy_exec_fingerprint UNIQUE (organization_id, execution_fingerprint)
);

CREATE INDEX idx_deploy_exec_org_project_created ON deployment_executions (organization_id, project_id, created_at DESC);
CREATE INDEX idx_deploy_exec_release ON deployment_executions (release_operation_id);
CREATE INDEX idx_deploy_exec_active ON deployment_executions (organization_id, environment_id, status);

CREATE TABLE deployment_execution_steps (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    step_key VARCHAR(80) NOT NULL,
    stage VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    sort_order INT NOT NULL,
    detail VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_deploy_exec_steps_execution FOREIGN KEY (execution_id)
        REFERENCES deployment_executions (id) ON DELETE CASCADE,
    CONSTRAINT chk_deploy_exec_steps_status CHECK (status IN (
        'PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED'
    ))
);

CREATE INDEX idx_deploy_exec_steps_execution ON deployment_execution_steps (execution_id, sort_order);

CREATE TABLE deployment_execution_logs (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    level VARCHAR(20) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deploy_exec_logs_execution FOREIGN KEY (execution_id)
        REFERENCES deployment_executions (id) ON DELETE CASCADE,
    CONSTRAINT chk_deploy_exec_logs_level CHECK (level IN ('DEBUG', 'INFO', 'WARN', 'ERROR'))
);

CREATE INDEX idx_deploy_exec_logs_execution ON deployment_execution_logs (execution_id, created_at);

CREATE TABLE deployment_execution_results (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    success BOOLEAN NOT NULL,
    summary VARCHAR(2000),
    provider_response_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deploy_exec_results_execution FOREIGN KEY (execution_id)
        REFERENCES deployment_executions (id) ON DELETE CASCADE,
    CONSTRAINT uq_deploy_exec_results_execution UNIQUE (execution_id)
);

CREATE TABLE deployment_execution_artifacts (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    artifact_type VARCHAR(60) NOT NULL,
    name VARCHAR(200) NOT NULL,
    content_ref VARCHAR(500),
    checksum VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deploy_exec_artifacts_execution FOREIGN KEY (execution_id)
        REFERENCES deployment_executions (id) ON DELETE CASCADE
);

CREATE INDEX idx_deploy_exec_artifacts_execution ON deployment_execution_artifacts (execution_id, created_at);

CREATE TABLE deployment_execution_validations (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    check_code VARCHAR(80) NOT NULL,
    passed BOOLEAN NOT NULL,
    message VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deploy_exec_validations_execution FOREIGN KEY (execution_id)
        REFERENCES deployment_executions (id) ON DELETE CASCADE
);

CREATE INDEX idx_deploy_exec_validations_execution ON deployment_execution_validations (execution_id, created_at);

CREATE TABLE deployment_execution_events (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    detail VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deploy_exec_events_execution FOREIGN KEY (execution_id)
        REFERENCES deployment_executions (id) ON DELETE CASCADE,
    CONSTRAINT chk_deploy_exec_events_type CHECK (event_type IN (
        'CREATED', 'QUEUED', 'STARTING', 'DEPLOYING', 'VERIFYING', 'COMPLETED', 'FAILED', 'CANCELLED',
        'IDEMPOTENT_RETURN', 'VALIDATION_FAILED'
    ))
);

CREATE INDEX idx_deploy_exec_events_execution ON deployment_execution_events (execution_id, created_at);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331087', 'EXECUTION_RUN', 'Run deployment executions', 'Create, queue, start, and cancel controlled deployment executions', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331087'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331087'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331087'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331087');
