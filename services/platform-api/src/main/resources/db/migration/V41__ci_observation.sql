-- CI Observation Agent (Sprint 3 Phase 4). Read-only observation of CI for successful PRs.
-- Never reruns workflows, approves, merges, or deploys.

CREATE TABLE ci_observation_operations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    pull_request_operation_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    repository_owner VARCHAR(255),
    repository_name VARCHAR(255),
    source_branch VARCHAR(255) NOT NULL,
    target_branch VARCHAR(255),
    commit_hash VARCHAR(64),
    pull_request_number BIGINT,
    overall_status VARCHAR(20) NOT NULL,
    failure_summary VARCHAR(4000),
    retry_recommendation VARCHAR(2000),
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ci_ops_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ci_ops_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_ci_ops_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT fk_ci_ops_pr_operation FOREIGN KEY (pull_request_operation_id) REFERENCES pull_request_operations (id),
    CONSTRAINT chk_ci_ops_status CHECK (status IN ('PENDING', 'FETCHING', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT chk_ci_ops_provider CHECK (provider IN ('GITHUB', 'GITLAB', 'LOCAL')),
    CONSTRAINT chk_ci_ops_overall CHECK (overall_status IN (
        'SUCCESS', 'FAILED', 'CANCELLED', 'TIMED_OUT', 'IN_PROGRESS', 'UNKNOWN'
    ))
);

CREATE INDEX idx_ci_ops_task ON ci_observation_operations (task_id);
CREATE INDEX idx_ci_ops_organization ON ci_observation_operations (organization_id);
CREATE INDEX idx_ci_ops_org_task_created ON ci_observation_operations (organization_id, task_id, created_at DESC);
CREATE INDEX idx_ci_ops_pr_operation ON ci_observation_operations (pull_request_operation_id);

CREATE TABLE ci_workflow_runs (
    id UUID PRIMARY KEY,
    ci_observation_operation_id UUID NOT NULL,
    external_workflow_id VARCHAR(100),
    workflow_name VARCHAR(500) NOT NULL,
    external_run_id VARCHAR(100) NOT NULL,
    run_url VARCHAR(2000),
    status VARCHAR(40) NOT NULL,
    conclusion VARCHAR(40),
    duration_ms BIGINT,
    trigger_event VARCHAR(100),
    commit_hash VARCHAR(64),
    branch VARCHAR(255),
    pull_request_number BIGINT,
    failure_reason VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ci_runs_operation FOREIGN KEY (ci_observation_operation_id)
        REFERENCES ci_observation_operations (id) ON DELETE CASCADE
);

CREATE INDEX idx_ci_runs_operation ON ci_workflow_runs (ci_observation_operation_id);

CREATE TABLE ci_jobs (
    id UUID PRIMARY KEY,
    ci_workflow_run_id UUID NOT NULL,
    external_job_id VARCHAR(100),
    job_name VARCHAR(500) NOT NULL,
    status VARCHAR(40) NOT NULL,
    conclusion VARCHAR(40),
    duration_ms BIGINT,
    failure_reason VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ci_jobs_run FOREIGN KEY (ci_workflow_run_id) REFERENCES ci_workflow_runs (id) ON DELETE CASCADE
);

CREATE INDEX idx_ci_jobs_run ON ci_jobs (ci_workflow_run_id);

CREATE TABLE ci_steps (
    id UUID PRIMARY KEY,
    ci_job_id UUID NOT NULL,
    step_number INT NOT NULL,
    step_name VARCHAR(500) NOT NULL,
    status VARCHAR(40) NOT NULL,
    conclusion VARCHAR(40),
    duration_ms BIGINT,
    failure_reason VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ci_steps_job FOREIGN KEY (ci_job_id) REFERENCES ci_jobs (id) ON DELETE CASCADE
);

CREATE INDEX idx_ci_steps_job ON ci_steps (ci_job_id);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331063', 'CI_RUN', 'Run CI observation agent', 'Observe CI workflow runs for pull requests (read-only)', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331064', 'CI_READ', 'Read CI observations', 'View CI observation results for orchestration tasks', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331063'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331064'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331063'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331064'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331063'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331064'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331063'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331064');
