-- Release Manager (Sprint 4 Phase 1). Authoritative release lifecycle after Merge Agent.
-- Does not deploy, rollback, modify commits/merges/approvals, or store secrets.
-- Manifest is immutable after READY.

CREATE TABLE release_operations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    release_number BIGINT NOT NULL,
    semantic_version VARCHAR(64) NOT NULL,
    release_name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(30) NOT NULL,
    version_strategy VARCHAR(30) NOT NULL,
    bump_type VARCHAR(20),
    content_fingerprint VARCHAR(64) NOT NULL,
    manifest_json TEXT,
    manifest_hash VARCHAR(64),
    error_code VARCHAR(80),
    error_message VARCHAR(2000),
    created_by UUID NOT NULL,
    prepared_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_release_ops_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_release_ops_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_release_ops_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_release_ops_status CHECK (status IN (
        'DRAFT', 'PREPARING', 'READY', 'PUBLISHED', 'ARCHIVED', 'FAILED'
    )),
    CONSTRAINT chk_release_ops_strategy CHECK (version_strategy IN ('SEMVER')),
    CONSTRAINT chk_release_ops_bump CHECK (bump_type IS NULL OR bump_type IN ('MAJOR', 'MINOR', 'PATCH')),
    CONSTRAINT uq_release_ops_content UNIQUE (organization_id, project_id, content_fingerprint),
    CONSTRAINT uq_release_ops_version UNIQUE (organization_id, project_id, semantic_version),
    CONSTRAINT uq_release_ops_number UNIQUE (organization_id, project_id, release_number)
);

CREATE INDEX idx_release_ops_org_project_created ON release_operations (organization_id, project_id, created_at DESC);
CREATE INDEX idx_release_ops_status ON release_operations (organization_id, project_id, status);

CREATE TABLE release_versions (
    id UUID PRIMARY KEY,
    release_operation_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    semantic_version VARCHAR(64) NOT NULL,
    version_strategy VARCHAR(30) NOT NULL,
    bump_type VARCHAR(20),
    major_version INT NOT NULL,
    minor_version INT NOT NULL,
    patch_version INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_release_versions_operation FOREIGN KEY (release_operation_id)
        REFERENCES release_operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_release_versions_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_release_versions_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT chk_release_versions_strategy CHECK (version_strategy IN ('SEMVER')),
    CONSTRAINT chk_release_versions_bump CHECK (bump_type IS NULL OR bump_type IN ('MAJOR', 'MINOR', 'PATCH')),
    CONSTRAINT uq_release_versions_operation UNIQUE (release_operation_id)
);

CREATE INDEX idx_release_versions_project ON release_versions (organization_id, project_id, major_version DESC, minor_version DESC, patch_version DESC);

CREATE TABLE release_contents (
    id UUID PRIMARY KEY,
    release_operation_id UUID NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    reference_id UUID,
    commit_sha VARCHAR(64),
    sort_order INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_release_contents_operation FOREIGN KEY (release_operation_id)
        REFERENCES release_operations (id) ON DELETE CASCADE,
    CONSTRAINT chk_release_contents_type CHECK (content_type IN (
        'MERGE_OPERATION', 'APPROVAL_DECISION', 'PULL_REQUEST', 'PATCH', 'COMMIT'
    )),
    CONSTRAINT chk_release_contents_ref CHECK (
        (content_type = 'COMMIT' AND commit_sha IS NOT NULL AND reference_id IS NULL)
        OR (content_type <> 'COMMIT' AND reference_id IS NOT NULL AND commit_sha IS NULL)
    )
);

CREATE INDEX idx_release_contents_operation ON release_contents (release_operation_id, sort_order);

CREATE TABLE release_artifacts (
    id UUID PRIMARY KEY,
    release_operation_id UUID NOT NULL,
    artifact_type VARCHAR(80) NOT NULL,
    artifact_uri VARCHAR(2000) NOT NULL,
    artifact_hash VARCHAR(64),
    label VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_release_artifacts_operation FOREIGN KEY (release_operation_id)
        REFERENCES release_operations (id) ON DELETE CASCADE
);

CREATE INDEX idx_release_artifacts_operation ON release_artifacts (release_operation_id);

CREATE TABLE release_events (
    id UUID PRIMARY KEY,
    release_operation_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    detail VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_release_events_operation FOREIGN KEY (release_operation_id)
        REFERENCES release_operations (id) ON DELETE CASCADE,
    CONSTRAINT chk_release_events_type CHECK (event_type IN (
        'CREATED', 'PREPARE_STARTED', 'MANIFEST_GENERATED', 'READY',
        'PUBLISHED', 'ARCHIVED', 'FAILED', 'IDEMPOTENT_RETURN'
    ))
);

CREATE INDEX idx_release_events_operation ON release_events (release_operation_id, created_at);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331075', 'RELEASE_RUN', 'Run release manager', 'Create, prepare, and publish software releases', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331076', 'RELEASE_READ', 'Read releases', 'View release records, manifests, and history', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331075'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331076'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331075'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331076'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331075'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331076'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331075'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331076');
