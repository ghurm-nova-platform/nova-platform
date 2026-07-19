-- Repair Agent (Sprint 3 Phase 5). Creates NEW PatchResults after review/testing/CI failures.
-- Never overwrites prior patches, merges, approves, deploys, or executes shell/CI.

CREATE TABLE repair_operations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_number INT NOT NULL,
    prior_patch_result_id UUID NOT NULL,
    new_patch_result_id UUID,
    reason VARCHAR(2000) NOT NULL,
    summary VARCHAR(4000),
    confidence DOUBLE PRECISION,
    input_fingerprint VARCHAR(64) NOT NULL,
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_repair_ops_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_repair_ops_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_repair_ops_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT fk_repair_ops_prior_patch FOREIGN KEY (prior_patch_result_id) REFERENCES patch_results (id),
    CONSTRAINT fk_repair_ops_new_patch FOREIGN KEY (new_patch_result_id) REFERENCES patch_results (id),
    CONSTRAINT chk_repair_ops_status CHECK (status IN (
        'PENDING', 'COLLECTING', 'ANALYZING', 'GENERATING_PATCH', 'VALIDATING', 'SUCCEEDED', 'FAILED'
    )),
    CONSTRAINT chk_repair_ops_attempt CHECK (attempt_number >= 1),
    CONSTRAINT chk_repair_ops_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
    CONSTRAINT uq_repair_ops_task_fingerprint UNIQUE (organization_id, task_id, input_fingerprint)
);

CREATE INDEX idx_repair_ops_task ON repair_operations (task_id);
CREATE INDEX idx_repair_ops_organization ON repair_operations (organization_id);
CREATE INDEX idx_repair_ops_org_task_created ON repair_operations (organization_id, task_id, created_at DESC);

CREATE TABLE repair_inputs (
    id UUID PRIMARY KEY,
    repair_operation_id UUID NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_ref VARCHAR(255),
    priority INT NOT NULL,
    detail VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_repair_inputs_operation FOREIGN KEY (repair_operation_id)
        REFERENCES repair_operations (id) ON DELETE CASCADE,
    CONSTRAINT chk_repair_inputs_source CHECK (source_type IN (
        'COMPILE', 'TEST', 'CI', 'STATIC_ANALYSIS', 'REVIEW', 'FORMATTING', 'DEPENDENCY', 'COVERAGE'
    )),
    CONSTRAINT chk_repair_inputs_priority CHECK (priority >= 1)
);

CREATE INDEX idx_repair_inputs_operation ON repair_inputs (repair_operation_id);

CREATE TABLE repair_actions (
    id UUID PRIMARY KEY,
    repair_operation_id UUID NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    target_path VARCHAR(1000),
    description VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_repair_actions_operation FOREIGN KEY (repair_operation_id)
        REFERENCES repair_operations (id) ON DELETE CASCADE
);

CREATE INDEX idx_repair_actions_operation ON repair_actions (repair_operation_id);

CREATE TABLE repair_results (
    id UUID PRIMARY KEY,
    repair_operation_id UUID NOT NULL,
    patch_result_id UUID NOT NULL,
    repaired_files_json VARCHAR(8000) NOT NULL,
    summary VARCHAR(4000) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_repair_results_operation FOREIGN KEY (repair_operation_id)
        REFERENCES repair_operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_repair_results_patch FOREIGN KEY (patch_result_id) REFERENCES patch_results (id),
    CONSTRAINT uq_repair_results_operation UNIQUE (repair_operation_id),
    CONSTRAINT chk_repair_results_confidence CHECK (confidence >= 0 AND confidence <= 1)
);

CREATE INDEX idx_repair_results_operation ON repair_results (repair_operation_id);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331065', 'REPAIR_RUN', 'Run repair agent', 'Propose repair patches after review, testing, or CI failures', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331066', 'REPAIR_READ', 'Read repair operations', 'View repair agent results for orchestration tasks', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331065'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331066'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331065'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331066'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331065'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331066'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331065'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331066');
