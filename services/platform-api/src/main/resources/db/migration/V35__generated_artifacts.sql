-- Generated artifacts from the Coding Agent (Sprint 2 Phase 3).

CREATE TABLE generated_artifacts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    run_id UUID NOT NULL,
    task_id UUID NOT NULL,
    artifact_type VARCHAR(50) NOT NULL,
    language VARCHAR(50) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    tokens_used BIGINT,
    model VARCHAR(150),
    provider VARCHAR(100),
    generation_time_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_generated_artifacts_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_generated_artifacts_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_generated_artifacts_run FOREIGN KEY (run_id) REFERENCES agent_orchestration_runs (id),
    CONSTRAINT fk_generated_artifacts_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT chk_generated_artifacts_type CHECK (artifact_type IN (
        'SOURCE_FILE', 'PATCH', 'TEST', 'DOCUMENTATION', 'CONFIGURATION', 'SQL_MIGRATION', 'README'
    )),
    CONSTRAINT chk_generated_artifacts_language CHECK (language IN (
        'JAVA', 'KOTLIN', 'TYPESCRIPT', 'JAVASCRIPT', 'ANGULAR', 'HTML', 'CSS', 'SCSS',
        'SQL', 'ORACLE_SQL', 'POSTGRESQL', 'MYSQL', 'PYTHON', 'GO', 'CSHARP',
        'MARKDOWN', 'JSON', 'YAML', 'XML', 'SHELL'
    )),
    CONSTRAINT uq_generated_artifacts_task_path UNIQUE (task_id, path)
);

CREATE INDEX idx_generated_artifacts_run ON generated_artifacts (run_id);
CREATE INDEX idx_generated_artifacts_task ON generated_artifacts (task_id);
CREATE INDEX idx_generated_artifacts_organization ON generated_artifacts (organization_id);
CREATE INDEX idx_generated_artifacts_org_project ON generated_artifacts (organization_id, project_id);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331051', 'CODING_GENERATE', 'Generate code artifacts', 'Invoke the coding agent to generate structured source artifacts', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331052', 'CODING_READ', 'Read coding artifacts', 'View generated coding artifacts for orchestration tasks', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    -- ORG_ADMIN: all
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331051'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331052'),
    -- PROJECT_ADMIN: generate + read
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331051'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331052'),
    -- USER + ORG_MEMBER: generate + read
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331051'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331052'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331051'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331052');
