-- Pull Request Agent (Sprint 3 Phase 3). Publishes successful Git Integration branches and creates PRs.
-- Never merges, approves, force-pushes, or pushes to protected branches.
-- Credentials are never stored in these tables.

CREATE TABLE project_repository_configs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    provider VARCHAR(40) NOT NULL,
    repository_host VARCHAR(255) NOT NULL,
    repository_owner VARCHAR(255) NOT NULL,
    repository_name VARCHAR(255) NOT NULL,
    remote_url VARCHAR(2000) NOT NULL,
    target_base_ref VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_project_repo_configs_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_project_repo_configs_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT uq_project_repo_configs_project UNIQUE (organization_id, project_id),
    CONSTRAINT chk_project_repo_configs_provider CHECK (provider IN ('GITHUB', 'GITLAB'))
);

CREATE INDEX idx_project_repo_configs_org ON project_repository_configs (organization_id);

CREATE TABLE pull_request_operations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    git_operation_id UUID NOT NULL,
    patch_result_id UUID,
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    repository_owner VARCHAR(255),
    repository_name VARCHAR(255),
    remote_name VARCHAR(100),
    remote_url VARCHAR(2000),
    source_branch VARCHAR(255) NOT NULL,
    target_branch VARCHAR(255) NOT NULL,
    local_commit_hash VARCHAR(64) NOT NULL,
    remote_commit_hash VARCHAR(64),
    patch_hash VARCHAR(64) NOT NULL,
    pull_request_number BIGINT,
    pull_request_url VARCHAR(2000),
    pull_request_title VARCHAR(500),
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_pr_ops_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_pr_ops_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_pr_ops_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT fk_pr_ops_git_operation FOREIGN KEY (git_operation_id) REFERENCES git_operations (id),
    CONSTRAINT chk_pr_ops_status CHECK (status IN (
        'PENDING', 'VALIDATING', 'PUSHING', 'PUSHED', 'CREATING_PR', 'SUCCEEDED', 'FAILED'
    )),
    CONSTRAINT chk_pr_ops_provider CHECK (provider IN ('GITHUB', 'GITLAB', 'LOCAL'))
);

CREATE INDEX idx_pr_ops_task ON pull_request_operations (task_id);
CREATE INDEX idx_pr_ops_organization ON pull_request_operations (organization_id);
CREATE INDEX idx_pr_ops_org_task_created ON pull_request_operations (organization_id, task_id, created_at DESC);
CREATE INDEX idx_pr_ops_git_operation ON pull_request_operations (git_operation_id);
-- Uniqueness of successful PR per git operation is enforced in PullRequestStorageService / agent.

CREATE TABLE remote_pushes (
    id UUID PRIMARY KEY,
    pull_request_operation_id UUID NOT NULL,
    remote_name VARCHAR(100) NOT NULL,
    source_branch VARCHAR(255) NOT NULL,
    local_commit_hash VARCHAR(64) NOT NULL,
    remote_commit_hash VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    CONSTRAINT fk_remote_pushes_operation FOREIGN KEY (pull_request_operation_id)
        REFERENCES pull_request_operations (id) ON DELETE CASCADE,
    CONSTRAINT chk_remote_pushes_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_remote_pushes_operation ON remote_pushes (pull_request_operation_id);

CREATE TABLE pull_request_records (
    id UUID PRIMARY KEY,
    pull_request_operation_id UUID NOT NULL,
    provider VARCHAR(40) NOT NULL,
    external_id VARCHAR(255),
    pull_request_number BIGINT NOT NULL,
    pull_request_url VARCHAR(2000) NOT NULL,
    title VARCHAR(500) NOT NULL,
    source_branch VARCHAR(255) NOT NULL,
    target_branch VARCHAR(255) NOT NULL,
    state VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_pr_records_operation FOREIGN KEY (pull_request_operation_id)
        REFERENCES pull_request_operations (id) ON DELETE CASCADE,
    CONSTRAINT chk_pr_records_provider CHECK (provider IN ('GITHUB', 'GITLAB', 'LOCAL'))
);

CREATE INDEX idx_pr_records_operation ON pull_request_records (pull_request_operation_id);

-- Seed demo project repository binding (used by local/tests; remote_url overridden in tests).
INSERT INTO project_repository_configs (
    id, organization_id, project_id, provider, repository_host, repository_owner, repository_name,
    remote_url, target_base_ref, enabled, created_at, updated_at
) VALUES (
    '66666666-6666-6666-6666-666666666601',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    'GITHUB',
    'github.com',
    'ghurm-nova-platform',
    'nova-demo',
    'https://github.com/ghurm-nova-platform/nova-demo.git',
    'main',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331061', 'PR_RUN', 'Run pull request agent', 'Publish Git Integration branches and create pull requests', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331062', 'PR_READ', 'Read pull request operations', 'View pull request agent results for orchestration tasks', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331061'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331062'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331061'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331062'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331061'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331062'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331061'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331062');
