-- Patch Agent results (Sprint 3 Phase 1). Generates Unified Diff patches only — never applies git/shell.

CREATE TABLE patch_results (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    run_id UUID NOT NULL,
    task_id UUID NOT NULL,
    summary VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    files_changed INTEGER NOT NULL,
    insertions INTEGER NOT NULL,
    deletions INTEGER NOT NULL,
    patch_size INTEGER NOT NULL,
    patch_content TEXT NOT NULL,
    validation_message VARCHAR(2000),
    tokens_used BIGINT,
    model VARCHAR(150),
    provider VARCHAR(100),
    generation_time_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_patch_results_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_patch_results_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_patch_results_run FOREIGN KEY (run_id) REFERENCES agent_orchestration_runs (id),
    CONSTRAINT fk_patch_results_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT chk_patch_results_status CHECK (status IN ('VALID', 'INVALID')),
    CONSTRAINT chk_patch_results_files CHECK (files_changed >= 0),
    CONSTRAINT chk_patch_results_insertions CHECK (insertions >= 0),
    CONSTRAINT chk_patch_results_deletions CHECK (deletions >= 0),
    CONSTRAINT chk_patch_results_size CHECK (patch_size >= 0)
);

CREATE INDEX idx_patch_results_task ON patch_results (task_id);
CREATE INDEX idx_patch_results_organization ON patch_results (organization_id);
CREATE INDEX idx_patch_results_org_task_created ON patch_results (organization_id, task_id, created_at DESC);

CREATE TABLE generated_patches (
    id UUID PRIMARY KEY,
    patch_result_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    path VARCHAR(1000) NOT NULL,
    old_path VARCHAR(1000),
    new_path VARCHAR(1000),
    change_type VARCHAR(20) NOT NULL,
    insertions INTEGER NOT NULL,
    deletions INTEGER NOT NULL,
    patch_excerpt TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_generated_patches_result FOREIGN KEY (patch_result_id) REFERENCES patch_results (id) ON DELETE CASCADE,
    CONSTRAINT fk_generated_patches_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT chk_generated_patches_change_type CHECK (change_type IN ('ADD', 'MODIFY', 'DELETE', 'RENAME')),
    CONSTRAINT chk_generated_patches_insertions CHECK (insertions >= 0),
    CONSTRAINT chk_generated_patches_deletions CHECK (deletions >= 0)
);

CREATE INDEX idx_generated_patches_result ON generated_patches (patch_result_id);
CREATE INDEX idx_generated_patches_path ON generated_patches (patch_result_id, path);

CREATE TABLE patch_artifacts (
    id UUID PRIMARY KEY,
    patch_result_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    artifact_id UUID NOT NULL,
    path VARCHAR(1000) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    language VARCHAR(50) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_patch_artifacts_result FOREIGN KEY (patch_result_id) REFERENCES patch_results (id) ON DELETE CASCADE,
    CONSTRAINT fk_patch_artifacts_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_patch_artifacts_artifact FOREIGN KEY (artifact_id) REFERENCES generated_artifacts (id),
    CONSTRAINT uq_patch_artifacts_result_artifact UNIQUE (patch_result_id, artifact_id)
);

CREATE INDEX idx_patch_artifacts_result ON patch_artifacts (patch_result_id);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331057', 'PATCH_RUN', 'Run patch agent', 'Invoke the patch agent to generate Unified Diff patches', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331058', 'PATCH_READ', 'Read patch results', 'View generated patches for orchestration tasks', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331057'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331058'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331057'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331058'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331057'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331058'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331057'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331058');
