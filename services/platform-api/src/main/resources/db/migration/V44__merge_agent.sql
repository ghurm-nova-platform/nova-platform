-- Merge Agent (Sprint 3 Phase 7). Sole component allowed to merge Pull Requests.
-- Requires VALID APPROVED ApprovalDecision. Never bypasses Approval Gate, deploys, or stores secrets.

CREATE TABLE merge_operations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    approval_decision_id UUID NOT NULL,
    pull_request_operation_id UUID NOT NULL,
    git_operation_id UUID NOT NULL,
    patch_result_id UUID NOT NULL,
    ci_observation_operation_id UUID,
    status VARCHAR(30) NOT NULL,
    merge_method VARCHAR(20) NOT NULL,
    evidence_fingerprint VARCHAR(64) NOT NULL,
    decision_fingerprint VARCHAR(64) NOT NULL,
    expected_patch_hash VARCHAR(64) NOT NULL,
    expected_commit_hash VARCHAR(64) NOT NULL,
    expected_pr_head_sha VARCHAR(64) NOT NULL,
    pull_request_number BIGINT NOT NULL,
    repository_owner VARCHAR(255) NOT NULL,
    repository_name VARCHAR(255) NOT NULL,
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_merge_ops_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_merge_ops_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_merge_ops_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT fk_merge_ops_approval FOREIGN KEY (approval_decision_id) REFERENCES approval_decisions (id),
    CONSTRAINT fk_merge_ops_pr FOREIGN KEY (pull_request_operation_id) REFERENCES pull_request_operations (id),
    CONSTRAINT fk_merge_ops_git FOREIGN KEY (git_operation_id) REFERENCES git_operations (id),
    CONSTRAINT fk_merge_ops_patch FOREIGN KEY (patch_result_id) REFERENCES patch_results (id),
    CONSTRAINT fk_merge_ops_ci FOREIGN KEY (ci_observation_operation_id) REFERENCES ci_observation_operations (id),
    CONSTRAINT chk_merge_ops_status CHECK (status IN (
        'PENDING', 'VALIDATING', 'MERGING', 'VERIFYING', 'SUCCEEDED', 'FAILED'
    )),
    CONSTRAINT chk_merge_ops_method CHECK (merge_method IN ('MERGE', 'SQUASH', 'REBASE')),
    CONSTRAINT uq_merge_ops_task_decision UNIQUE (organization_id, task_id, approval_decision_id)
);

CREATE INDEX idx_merge_ops_task ON merge_operations (task_id);
CREATE INDEX idx_merge_ops_org_task_created ON merge_operations (organization_id, task_id, created_at DESC);
CREATE INDEX idx_merge_ops_pr_number ON merge_operations (organization_id, repository_owner, repository_name, pull_request_number);

CREATE TABLE merge_validations (
    id UUID PRIMARY KEY,
    merge_operation_id UUID NOT NULL,
    check_code VARCHAR(80) NOT NULL,
    expected_value VARCHAR(2000),
    actual_value VARCHAR(2000),
    result VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(2000),
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_merge_validations_operation FOREIGN KEY (merge_operation_id)
        REFERENCES merge_operations (id) ON DELETE CASCADE,
    CONSTRAINT chk_merge_validations_result CHECK (result IN ('PASSED', 'FAILED', 'SKIPPED', 'ERROR'))
);

CREATE INDEX idx_merge_validations_operation ON merge_validations (merge_operation_id);

CREATE TABLE merge_results (
    id UUID PRIMARY KEY,
    merge_operation_id UUID NOT NULL,
    merge_method VARCHAR(20) NOT NULL,
    merged_commit VARCHAR(64),
    pull_request_number BIGINT NOT NULL,
    pull_request_url VARCHAR(2000),
    merged_at TIMESTAMP WITH TIME ZONE,
    merged_by_user_id UUID,
    provider VARCHAR(40) NOT NULL,
    provider_message VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_merge_results_operation FOREIGN KEY (merge_operation_id)
        REFERENCES merge_operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_merge_results_user FOREIGN KEY (merged_by_user_id) REFERENCES users (id),
    CONSTRAINT chk_merge_results_method CHECK (merge_method IN ('MERGE', 'SQUASH', 'REBASE')),
    CONSTRAINT uq_merge_results_operation UNIQUE (merge_operation_id)
);

CREATE TABLE merge_events (
    id UUID PRIMARY KEY,
    merge_operation_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    detail VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_merge_events_operation FOREIGN KEY (merge_operation_id)
        REFERENCES merge_operations (id) ON DELETE CASCADE,
    CONSTRAINT chk_merge_events_type CHECK (event_type IN (
        'OPERATION_CREATED', 'VALIDATION_STARTED', 'VALIDATION_PASSED', 'VALIDATION_FAILED',
        'MERGE_STARTED', 'MERGE_SUCCEEDED', 'MERGE_FAILED', 'VERIFY_STARTED', 'VERIFY_PASSED',
        'VERIFY_FAILED', 'ALREADY_MERGED', 'COMPLETED'
    ))
);

CREATE INDEX idx_merge_events_operation ON merge_events (merge_operation_id);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331073', 'MERGE_RUN', 'Run merge agent', 'Merge approved pull requests after Approval Gate validation', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331074', 'MERGE_READ', 'Read merge operations', 'View merge agent results for orchestration tasks', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331073'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331074'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331073'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331074'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331073'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331074'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331073'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331074');
