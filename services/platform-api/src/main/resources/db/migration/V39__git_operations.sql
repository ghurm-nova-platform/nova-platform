-- Git Integration Agent (Sprint 3 Phase 2). Applies validated patches to isolated operation workspaces only.
-- Never merges, pushes to main/develop, deletes branches, or runs arbitrary shell.
-- Failed workspaces are preserved for diagnosis (see docs/029_GIT_INTEGRATION_AGENT.md).

CREATE TABLE git_operations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    run_id UUID NOT NULL,
    task_id UUID NOT NULL,
    patch_result_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    commit_hash VARCHAR(64),
    patch_hash VARCHAR(64) NOT NULL,
    repository_path VARCHAR(2000) NOT NULL,
    base_ref VARCHAR(255) NOT NULL,
    error_code VARCHAR(80),
    validation_message VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_git_operations_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_git_operations_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_git_operations_run FOREIGN KEY (run_id) REFERENCES agent_orchestration_runs (id),
    CONSTRAINT fk_git_operations_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT chk_git_operations_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_git_operations_task ON git_operations (task_id);
CREATE INDEX idx_git_operations_organization ON git_operations (organization_id);
CREATE INDEX idx_git_operations_org_task_created ON git_operations (organization_id, task_id, created_at DESC);
CREATE UNIQUE INDEX uq_git_operations_org_branch ON git_operations (organization_id, project_id, branch_name);

CREATE TABLE git_branches (
    id UUID PRIMARY KEY,
    git_operation_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    base_ref VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_git_branches_operation FOREIGN KEY (git_operation_id) REFERENCES git_operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_git_branches_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE INDEX idx_git_branches_operation ON git_branches (git_operation_id);

CREATE TABLE git_commits (
    id UUID PRIMARY KEY,
    git_operation_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    commit_hash VARCHAR(64) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_git_commits_operation FOREIGN KEY (git_operation_id) REFERENCES git_operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_git_commits_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE INDEX idx_git_commits_operation ON git_commits (git_operation_id);
CREATE INDEX idx_git_commits_hash ON git_commits (commit_hash);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331059', 'GIT_RUN', 'Run git integration agent', 'Apply validated patches onto isolated working branches', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331060', 'GIT_READ', 'Read git operations', 'View git integration results for orchestration tasks', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331059'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331060'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331059'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331060'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331059'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331060'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331059'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331060');
