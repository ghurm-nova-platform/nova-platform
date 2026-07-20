-- Release Policies (Sprint 4 Phase 4). Evaluate whether a Release may advance.
-- Does not modify releases, deployments, rollbacks, merges, approvals, or store secrets.

CREATE TABLE release_policies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    policy_name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    policy_type VARCHAR(60) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority INT NOT NULL,
    evaluation_mode VARCHAR(30) NOT NULL,
    config_json TEXT NOT NULL,
    policy_fingerprint VARCHAR(64) NOT NULL,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_release_policies_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_release_policies_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_release_policies_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_release_policies_status CHECK (status IN ('ACTIVE', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT chk_release_policies_type CHECK (policy_type IN (
        'MINIMUM_APPROVALS', 'CI_REQUIRED', 'NO_FAILED_CHECKS', 'SIGNED_COMMITS_REQUIRED',
        'SEMANTIC_VERSION_REQUIRED', 'MANIFEST_INTEGRITY', 'RELEASE_NOTES_REQUIRED',
        'DEPLOYMENT_OBSERVATION_EXISTS', 'ROLLBACK_PLAN_EXISTS', 'CUSTOM_EXPRESSION'
    )),
    CONSTRAINT chk_release_policies_mode CHECK (evaluation_mode IN (
        'ALL_REQUIRED', 'FIRST_FAILURE', 'BEST_EFFORT'
    )),
    CONSTRAINT uq_release_policies_fingerprint UNIQUE (organization_id, policy_fingerprint),
    CONSTRAINT uq_release_policies_name UNIQUE (organization_id, project_id, policy_name)
);

CREATE INDEX idx_release_policies_org_project ON release_policies (organization_id, project_id, status, priority);

CREATE TABLE policy_versions (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL,
    version_number INT NOT NULL,
    policy_type VARCHAR(60) NOT NULL,
    evaluation_mode VARCHAR(30) NOT NULL,
    priority INT NOT NULL,
    config_json TEXT NOT NULL,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_policy_versions_policy FOREIGN KEY (policy_id) REFERENCES release_policies (id) ON DELETE CASCADE,
    CONSTRAINT fk_policy_versions_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_policy_versions_number UNIQUE (policy_id, version_number)
);

CREATE INDEX idx_policy_versions_policy ON policy_versions (policy_id, version_number DESC);

CREATE TABLE policy_evaluations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    policy_id UUID NOT NULL,
    policy_version_id UUID NOT NULL,
    release_operation_id UUID NOT NULL,
    decision VARCHAR(30) NOT NULL,
    evaluation_hash VARCHAR(64) NOT NULL,
    summary VARCHAR(2000),
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    completed BOOLEAN NOT NULL,
    evaluated_by UUID,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_policy_eval_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_policy_eval_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_policy_eval_policy FOREIGN KEY (policy_id) REFERENCES release_policies (id),
    CONSTRAINT fk_policy_eval_version FOREIGN KEY (policy_version_id) REFERENCES policy_versions (id),
    CONSTRAINT fk_policy_eval_release FOREIGN KEY (release_operation_id) REFERENCES release_operations (id),
    CONSTRAINT fk_policy_eval_evaluated_by FOREIGN KEY (evaluated_by) REFERENCES users (id),
    CONSTRAINT chk_policy_eval_decision CHECK (decision IN (
        'PASSED', 'FAILED', 'WARNING', 'SKIPPED', 'ERROR'
    )),
    CONSTRAINT uq_policy_eval_hash UNIQUE (organization_id, evaluation_hash)
);

CREATE INDEX idx_policy_eval_policy_created ON policy_evaluations (policy_id, created_at DESC);
CREATE INDEX idx_policy_eval_release ON policy_evaluations (release_operation_id);

CREATE TABLE policy_evidence (
    id UUID PRIMARY KEY,
    policy_evaluation_id UUID NOT NULL,
    evidence_key VARCHAR(120) NOT NULL,
    evidence_type VARCHAR(60) NOT NULL,
    reference_id UUID,
    passed BOOLEAN NOT NULL,
    detail VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_policy_evidence_evaluation FOREIGN KEY (policy_evaluation_id)
        REFERENCES policy_evaluations (id) ON DELETE CASCADE,
    CONSTRAINT uq_policy_evidence_key UNIQUE (policy_evaluation_id, evidence_key)
);

CREATE INDEX idx_policy_evidence_evaluation ON policy_evidence (policy_evaluation_id, created_at);

CREATE TABLE policy_events (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL,
    policy_evaluation_id UUID,
    event_type VARCHAR(60) NOT NULL,
    detail VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_policy_events_policy FOREIGN KEY (policy_id) REFERENCES release_policies (id) ON DELETE CASCADE,
    CONSTRAINT fk_policy_events_evaluation FOREIGN KEY (policy_evaluation_id) REFERENCES policy_evaluations (id),
    CONSTRAINT chk_policy_events_type CHECK (event_type IN (
        'CREATED', 'ENABLED', 'DISABLED', 'ARCHIVED', 'EVALUATION_STARTED',
        'EVALUATION_COMPLETED', 'IDEMPOTENT_RETURN', 'FAILED'
    ))
);

CREATE INDEX idx_policy_events_policy ON policy_events (policy_id, created_at);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331081', 'POLICY_RUN', 'Run release policies', 'Create, enable, disable, and evaluate release policies', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331082', 'POLICY_READ', 'Read release policies', 'View release policies, evaluations, and history', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331081'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331082'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331081'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331082'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331081'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331082'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331081'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331082');
