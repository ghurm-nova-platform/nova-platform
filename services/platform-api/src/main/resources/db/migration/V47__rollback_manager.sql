-- Rollback Manager (Sprint 4 Phase 3). Planning and validation only.
-- Does not execute rollback, deploy, modify releases/deployments/manifests, delete history, or store secrets.

CREATE TABLE rollback_operations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    release_operation_id UUID NOT NULL,
    deployment_operation_id UUID NOT NULL,
    target_release_operation_id UUID NOT NULL,
    current_version VARCHAR(64) NOT NULL,
    target_version VARCHAR(64) NOT NULL,
    environment_id UUID NOT NULL,
    environment_code VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    strategy VARCHAR(40) NOT NULL,
    rollback_plan_hash VARCHAR(64) NOT NULL,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    validated_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    CONSTRAINT fk_rollback_ops_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_rollback_ops_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_rollback_ops_release FOREIGN KEY (release_operation_id) REFERENCES release_operations (id),
    CONSTRAINT fk_rollback_ops_deployment FOREIGN KEY (deployment_operation_id) REFERENCES deployment_operations (id),
    CONSTRAINT fk_rollback_ops_target_release FOREIGN KEY (target_release_operation_id) REFERENCES release_operations (id),
    CONSTRAINT fk_rollback_ops_environment FOREIGN KEY (environment_id) REFERENCES deployment_environments (id),
    CONSTRAINT fk_rollback_ops_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_rollback_ops_status CHECK (status IN (
        'DRAFT', 'VALIDATING', 'READY', 'EXECUTING', 'SUCCEEDED', 'FAILED', 'CANCELLED'
    )),
    CONSTRAINT chk_rollback_ops_strategy CHECK (strategy IN (
        'PREVIOUS_RELEASE', 'PREVIOUS_STABLE', 'SPECIFIC_RELEASE', 'HOTFIX_ONLY', 'CUSTOM'
    )),
    CONSTRAINT uq_rollback_ops_plan_hash UNIQUE (organization_id, rollback_plan_hash)
);

CREATE INDEX idx_rollback_ops_org_project_created ON rollback_operations (organization_id, project_id, created_at DESC);
CREATE INDEX idx_rollback_ops_deployment ON rollback_operations (deployment_operation_id);
CREATE INDEX idx_rollback_ops_release ON rollback_operations (release_operation_id);
CREATE INDEX idx_rollback_ops_status ON rollback_operations (organization_id, status);

CREATE TABLE rollback_plans (
    id UUID PRIMARY KEY,
    rollback_operation_id UUID NOT NULL,
    current_release_operation_id UUID NOT NULL,
    target_release_operation_id UUID NOT NULL,
    deployment_operation_id UUID NOT NULL,
    environment_code VARCHAR(40) NOT NULL,
    strategy VARCHAR(40) NOT NULL,
    reason VARCHAR(2000),
    risk_level VARCHAR(30) NOT NULL,
    validation_result VARCHAR(30) NOT NULL,
    validation_message VARCHAR(2000),
    plan_json TEXT NOT NULL,
    immutable BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_rollback_plans_operation FOREIGN KEY (rollback_operation_id)
        REFERENCES rollback_operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_rollback_plans_current_release FOREIGN KEY (current_release_operation_id)
        REFERENCES release_operations (id),
    CONSTRAINT fk_rollback_plans_target_release FOREIGN KEY (target_release_operation_id)
        REFERENCES release_operations (id),
    CONSTRAINT fk_rollback_plans_deployment FOREIGN KEY (deployment_operation_id)
        REFERENCES deployment_operations (id),
    CONSTRAINT uq_rollback_plans_operation UNIQUE (rollback_operation_id),
    CONSTRAINT chk_rollback_plans_strategy CHECK (strategy IN (
        'PREVIOUS_RELEASE', 'PREVIOUS_STABLE', 'SPECIFIC_RELEASE', 'HOTFIX_ONLY', 'CUSTOM'
    )),
    CONSTRAINT chk_rollback_plans_risk CHECK (risk_level IN (
        'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    )),
    CONSTRAINT chk_rollback_plans_validation CHECK (validation_result IN (
        'PENDING', 'PASSED', 'FAILED'
    ))
);

CREATE TABLE rollback_targets (
    id UUID PRIMARY KEY,
    rollback_operation_id UUID NOT NULL,
    target_release_operation_id UUID NOT NULL,
    target_version VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_rollback_targets_operation FOREIGN KEY (rollback_operation_id)
        REFERENCES rollback_operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_rollback_targets_release FOREIGN KEY (target_release_operation_id)
        REFERENCES release_operations (id)
);

CREATE INDEX idx_rollback_targets_operation ON rollback_targets (rollback_operation_id, sort_order);

CREATE TABLE rollback_events (
    id UUID PRIMARY KEY,
    rollback_operation_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    detail VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_rollback_events_operation FOREIGN KEY (rollback_operation_id)
        REFERENCES rollback_operations (id) ON DELETE CASCADE,
    CONSTRAINT chk_rollback_events_type CHECK (event_type IN (
        'CREATED', 'VALIDATING', 'VALIDATION_PASSED', 'VALIDATION_FAILED',
        'READY', 'CANCELLED', 'FAILED', 'IDEMPOTENT_RETURN'
    ))
);

CREATE INDEX idx_rollback_events_operation ON rollback_events (rollback_operation_id, created_at);

CREATE TABLE rollback_validations (
    id UUID PRIMARY KEY,
    rollback_operation_id UUID NOT NULL,
    check_code VARCHAR(80) NOT NULL,
    passed BOOLEAN NOT NULL,
    message VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_rollback_validations_operation FOREIGN KEY (rollback_operation_id)
        REFERENCES rollback_operations (id) ON DELETE CASCADE
);

CREATE INDEX idx_rollback_validations_operation ON rollback_validations (rollback_operation_id, created_at);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331079', 'ROLLBACK_RUN', 'Run rollback planning', 'Create and validate rollback plans without executing rollback', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331080', 'ROLLBACK_READ', 'Read rollback plans', 'View rollback plans, validations, and history', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331079'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331080'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331079'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331080'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331079'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331080'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331079'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331080');
