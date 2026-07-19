-- Approval Gate (Sprint 3 Phase 6). Evaluates governance eligibility for merge.
-- Never merges, never GitHub-approves, never modifies patches/commits/CI/deployments.

CREATE TABLE approval_policies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    version INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    is_default BOOLEAN NOT NULL,
    required_human_approvals INT NOT NULL,
    require_distinct_approvers BOOLEAN NOT NULL,
    prohibit_author_approval BOOLEAN NOT NULL,
    require_ci_success BOOLEAN NOT NULL,
    require_review_approved BOOLEAN NOT NULL,
    minimum_review_score INT,
    require_testing_success BOOLEAN NOT NULL,
    minimum_estimated_coverage INT,
    require_no_critical_findings BOOLEAN NOT NULL,
    require_no_high_findings BOOLEAN NOT NULL,
    require_repair_success_when_failed BOOLEAN NOT NULL,
    require_pull_request_open BOOLEAN NOT NULL,
    require_exact_commit_match BOOLEAN NOT NULL,
    decision_validity_minutes INT,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_approval_policies_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_approval_policies_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT chk_approval_policies_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'SUPERSEDED')),
    CONSTRAINT chk_approval_policies_version CHECK (version >= 1),
    CONSTRAINT chk_approval_policies_human CHECK (required_human_approvals >= 0),
    CONSTRAINT chk_approval_policies_score CHECK (minimum_review_score IS NULL OR (minimum_review_score >= 0 AND minimum_review_score <= 100)),
    CONSTRAINT chk_approval_policies_coverage CHECK (minimum_estimated_coverage IS NULL OR (minimum_estimated_coverage >= 0 AND minimum_estimated_coverage <= 100)),
    CONSTRAINT chk_approval_policies_validity CHECK (decision_validity_minutes IS NULL OR decision_validity_minutes > 0),
    CONSTRAINT uq_approval_policies_org_project_name_version UNIQUE (organization_id, project_id, name, version)
);

-- Default uniqueness for ACTIVE+is_default is enforced in ApprovalPolicyService (H2-safe).
CREATE INDEX idx_approval_policies_organization ON approval_policies (organization_id);
CREATE INDEX idx_approval_policies_project ON approval_policies (project_id);
CREATE INDEX idx_approval_policies_default ON approval_policies (organization_id, project_id, is_default, status);

CREATE TABLE approval_policy_rules (
    id UUID PRIMARY KEY,
    approval_policy_id UUID NOT NULL,
    rule_code VARCHAR(80) NOT NULL,
    rule_type VARCHAR(40) NOT NULL,
    operator VARCHAR(40) NOT NULL,
    expected_value VARCHAR(500),
    severity VARCHAR(20) NOT NULL,
    blocking BOOLEAN NOT NULL,
    display_order INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_approval_policy_rules_policy FOREIGN KEY (approval_policy_id)
        REFERENCES approval_policies (id) ON DELETE CASCADE,
    CONSTRAINT chk_approval_policy_rules_severity CHECK (severity IN ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT uq_approval_policy_rules_code UNIQUE (approval_policy_id, rule_code)
);

CREATE INDEX idx_approval_policy_rules_policy ON approval_policy_rules (approval_policy_id);

CREATE TABLE approval_gate_operations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    policy_id UUID NOT NULL,
    policy_version INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    decision VARCHAR(40),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_approval_ops_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_approval_ops_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_approval_ops_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT fk_approval_ops_policy FOREIGN KEY (policy_id) REFERENCES approval_policies (id),
    CONSTRAINT chk_approval_ops_status CHECK (status IN (
        'PENDING', 'COLLECTING', 'EVALUATING', 'WAITING_FOR_HUMAN', 'SUCCEEDED', 'FAILED'
    )),
    CONSTRAINT chk_approval_ops_decision CHECK (decision IS NULL OR decision IN (
        'PENDING', 'ELIGIBLE', 'BLOCKED', 'REQUIRES_HUMAN_APPROVAL', 'APPROVED', 'REJECTED',
        'EXPIRED', 'SUPERSEDED', 'INVALIDATED', 'ERROR'
    ))
);

CREATE INDEX idx_approval_ops_task ON approval_gate_operations (task_id);
CREATE INDEX idx_approval_ops_org_task_created ON approval_gate_operations (organization_id, task_id, created_at DESC);

CREATE TABLE approval_decisions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    approval_gate_operation_id UUID NOT NULL,
    policy_id UUID NOT NULL,
    policy_version INT NOT NULL,
    decision VARCHAR(40) NOT NULL,
    eligible_for_merge BOOLEAN NOT NULL,
    required_human_approvals INT NOT NULL,
    received_human_approvals INT NOT NULL,
    rejection_count INT NOT NULL,
    evidence_fingerprint VARCHAR(64) NOT NULL,
    decision_fingerprint VARCHAR(64) NOT NULL,
    patch_result_id UUID NOT NULL,
    patch_hash VARCHAR(64) NOT NULL,
    git_operation_id UUID NOT NULL,
    commit_hash VARCHAR(64) NOT NULL,
    pull_request_operation_id UUID NOT NULL,
    pull_request_number BIGINT NOT NULL,
    pull_request_url VARCHAR(2000),
    ci_observation_operation_id UUID,
    ci_overall_status VARCHAR(40),
    repair_operation_id UUID,
    reason_summary VARCHAR(4000),
    valid_until TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    superseded_at TIMESTAMP WITH TIME ZONE,
    invalidated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_approval_decisions_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_approval_decisions_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_approval_decisions_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT fk_approval_decisions_operation FOREIGN KEY (approval_gate_operation_id)
        REFERENCES approval_gate_operations (id),
    CONSTRAINT fk_approval_decisions_policy FOREIGN KEY (policy_id) REFERENCES approval_policies (id),
    CONSTRAINT fk_approval_decisions_patch FOREIGN KEY (patch_result_id) REFERENCES patch_results (id),
    CONSTRAINT fk_approval_decisions_git FOREIGN KEY (git_operation_id) REFERENCES git_operations (id),
    CONSTRAINT fk_approval_decisions_pr FOREIGN KEY (pull_request_operation_id) REFERENCES pull_request_operations (id),
    CONSTRAINT fk_approval_decisions_ci FOREIGN KEY (ci_observation_operation_id) REFERENCES ci_observation_operations (id),
    CONSTRAINT fk_approval_decisions_repair FOREIGN KEY (repair_operation_id) REFERENCES repair_operations (id),
    CONSTRAINT chk_approval_decisions_decision CHECK (decision IN (
        'PENDING', 'ELIGIBLE', 'BLOCKED', 'REQUIRES_HUMAN_APPROVAL', 'APPROVED', 'REJECTED',
        'EXPIRED', 'SUPERSEDED', 'INVALIDATED', 'ERROR'
    )),
    CONSTRAINT chk_approval_decisions_human CHECK (
        required_human_approvals >= 0 AND received_human_approvals >= 0 AND rejection_count >= 0
    ),
    CONSTRAINT uq_approval_decisions_fingerprint UNIQUE (organization_id, task_id, evidence_fingerprint, decision_fingerprint)
);

CREATE INDEX idx_approval_decisions_task ON approval_decisions (task_id);
CREATE INDEX idx_approval_decisions_org_task_created ON approval_decisions (organization_id, task_id, created_at DESC);
CREATE INDEX idx_approval_decisions_evidence_fp ON approval_decisions (organization_id, task_id, evidence_fingerprint);

CREATE TABLE approval_evidence (
    id UUID PRIMARY KEY,
    approval_decision_id UUID NOT NULL,
    evidence_type VARCHAR(40) NOT NULL,
    source_operation_id UUID NOT NULL,
    source_result_id UUID,
    source_version VARCHAR(80),
    source_hash VARCHAR(128),
    observed_status VARCHAR(80),
    observed_value VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_approval_evidence_decision FOREIGN KEY (approval_decision_id)
        REFERENCES approval_decisions (id) ON DELETE CASCADE,
    CONSTRAINT chk_approval_evidence_type CHECK (evidence_type IN (
        'REVIEW', 'TESTING', 'PATCH', 'GIT', 'PULL_REQUEST', 'CI', 'REPAIR', 'POLICY'
    ))
);

CREATE INDEX idx_approval_evidence_decision ON approval_evidence (approval_decision_id);

CREATE TABLE approval_requirements (
    id UUID PRIMARY KEY,
    approval_decision_id UUID NOT NULL,
    rule_code VARCHAR(80) NOT NULL,
    description VARCHAR(1000),
    expected_value VARCHAR(500),
    actual_value VARCHAR(2000),
    result VARCHAR(30) NOT NULL,
    blocking BOOLEAN NOT NULL,
    severity VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(2000),
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_approval_requirements_decision FOREIGN KEY (approval_decision_id)
        REFERENCES approval_decisions (id) ON DELETE CASCADE,
    CONSTRAINT chk_approval_requirements_result CHECK (result IN (
        'PASSED', 'FAILED', 'NOT_APPLICABLE', 'PENDING', 'ERROR'
    )),
    CONSTRAINT chk_approval_requirements_severity CHECK (severity IN ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_approval_requirements_decision ON approval_requirements (approval_decision_id);

CREATE TABLE approval_human_actions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    approval_decision_id UUID NOT NULL,
    actor_user_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    comment_text VARCHAR(2000),
    evidence_fingerprint VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_approval_human_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_approval_human_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_approval_human_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT fk_approval_human_decision FOREIGN KEY (approval_decision_id) REFERENCES approval_decisions (id),
    CONSTRAINT fk_approval_human_actor FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT chk_approval_human_action CHECK (action IN ('APPROVE', 'REJECT', 'WITHDRAW_APPROVAL'))
);

CREATE INDEX idx_approval_human_decision ON approval_human_actions (approval_decision_id);
CREATE INDEX idx_approval_human_evidence ON approval_human_actions (organization_id, task_id, evidence_fingerprint);
CREATE INDEX idx_approval_human_idempotency ON approval_human_actions (organization_id, task_id, actor_user_id, idempotency_key);

CREATE TABLE approval_decision_events (
    id UUID PRIMARY KEY,
    approval_decision_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    detail VARCHAR(2000),
    actor_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_approval_events_decision FOREIGN KEY (approval_decision_id)
        REFERENCES approval_decisions (id) ON DELETE CASCADE,
    CONSTRAINT chk_approval_events_type CHECK (event_type IN (
        'OPERATION_CREATED', 'EVIDENCE_COLLECTED', 'RULE_EVALUATED',
        'AUTOMATED_GATE_PASSED', 'AUTOMATED_GATE_BLOCKED',
        'HUMAN_APPROVAL_ADDED', 'HUMAN_APPROVAL_WITHDRAWN', 'HUMAN_REJECTION_ADDED',
        'DECISION_APPROVED', 'DECISION_REJECTED', 'DECISION_EXPIRED',
        'DECISION_SUPERSEDED', 'DECISION_INVALIDATED'
    ))
);

CREATE INDEX idx_approval_events_decision ON approval_decision_events (approval_decision_id);

-- Default ACTIVE org-scoped policy for seed organization
INSERT INTO approval_policies (
    id, organization_id, project_id, name, description, version, status, is_default,
    required_human_approvals, require_distinct_approvers, prohibit_author_approval,
    require_ci_success, require_review_approved, minimum_review_score,
    require_testing_success, minimum_estimated_coverage,
    require_no_critical_findings, require_no_high_findings,
    require_repair_success_when_failed, require_pull_request_open, require_exact_commit_match,
    decision_validity_minutes, created_by, created_at, updated_by, updated_at
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001',
    '11111111-1111-1111-1111-111111111111',
    NULL,
    'Default Approval Policy',
    'Seed default governance policy for Approval Gate',
    1,
    'ACTIVE',
    TRUE,
    1,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    70,
    TRUE,
    50,
    TRUE,
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    1440,
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP
);

INSERT INTO approval_policy_rules (
    id, approval_policy_id, rule_code, rule_type, operator, expected_value, severity, blocking, display_order, created_at
) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'CI_MUST_SUCCEED', 'CI', 'EQUALS', 'SUCCESS', 'CRITICAL', TRUE, 10, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'REVIEW_SCORE_MINIMUM', 'REVIEW', 'GTE', '70', 'HIGH', TRUE, 20, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'TESTING_MUST_SUCCEED', 'TESTING', 'EQUALS', 'true', 'HIGH', TRUE, 30, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0004', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'COVERAGE_MINIMUM', 'TESTING', 'GTE', '50', 'MEDIUM', TRUE, 40, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0005', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'NO_CRITICAL_FINDINGS', 'REVIEW', 'EQUALS', '0', 'CRITICAL', TRUE, 50, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0006', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'PULL_REQUEST_OPEN', 'PULL_REQUEST', 'EQUALS', 'open', 'HIGH', TRUE, 60, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0007', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'REQUIRED_HUMAN_APPROVALS', 'HUMAN', 'GTE', '1', 'HIGH', TRUE, 70, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0008', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'DISTINCT_APPROVERS_REQUIRED', 'HUMAN', 'EQUALS', 'true', 'MEDIUM', TRUE, 80, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0009', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'AUTHOR_CANNOT_APPROVE', 'HUMAN', 'EQUALS', 'true', 'MEDIUM', TRUE, 90, CURRENT_TIMESTAMP);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331067', 'APPROVAL_GATE_RUN', 'Run approval gate', 'Evaluate approval eligibility for orchestration tasks', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331068', 'APPROVAL_GATE_READ', 'Read approval decisions', 'View approval gate decisions and history', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331069', 'APPROVAL_GATE_APPROVE', 'Approve for merge eligibility', 'Record human approval on an approval decision', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331070', 'APPROVAL_GATE_REJECT', 'Reject approval eligibility', 'Record human rejection on an approval decision', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331071', 'APPROVAL_POLICY_READ', 'Read approval policies', 'View organization and project approval policies', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331072', 'APPROVAL_POLICY_MANAGE', 'Manage approval policies', 'Create and version approval policies', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331067'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331068'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331069'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331070'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331071'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331072'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331067'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331068'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331069'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331070'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331071'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331072'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331067'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331068'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331069'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331070'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331071'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331067'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331068'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331069'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331070'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331071');
