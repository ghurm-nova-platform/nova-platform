-- Review Agent findings storage (Sprint 2 Phase 4).

CREATE TABLE review_results (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    run_id UUID NOT NULL,
    task_id UUID NOT NULL,
    summary VARCHAR(4000) NOT NULL,
    score INTEGER NOT NULL,
    approved BOOLEAN NOT NULL,
    tokens_used BIGINT,
    model VARCHAR(150),
    provider VARCHAR(100),
    review_time_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_review_results_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_review_results_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_review_results_run FOREIGN KEY (run_id) REFERENCES agent_orchestration_runs (id),
    CONSTRAINT fk_review_results_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT chk_review_results_score CHECK (score >= 0 AND score <= 100)
);

CREATE INDEX idx_review_results_task ON review_results (task_id);
CREATE INDEX idx_review_results_organization ON review_results (organization_id);
CREATE INDEX idx_review_results_org_task_created ON review_results (organization_id, task_id, created_at DESC);

CREATE TABLE review_findings (
    id UUID PRIMARY KEY,
    review_result_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    severity VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    recommendation VARCHAR(4000) NOT NULL,
    artifact_id UUID,
    artifact_path VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_review_findings_result FOREIGN KEY (review_result_id) REFERENCES review_results (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_findings_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_review_findings_artifact FOREIGN KEY (artifact_id) REFERENCES generated_artifacts (id),
    CONSTRAINT chk_review_findings_severity CHECK (severity IN (
        'INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    )),
    CONSTRAINT chk_review_findings_category CHECK (category IN (
        'CORRECTNESS', 'ARCHITECTURE', 'MAINTAINABILITY', 'READABILITY', 'SECURITY',
        'PERFORMANCE', 'CONCURRENCY', 'VALIDATION', 'ERROR_HANDLING', 'DOCUMENTATION',
        'NAMING', 'TESTING', 'BEST_PRACTICES'
    ))
);

CREATE INDEX idx_review_findings_result ON review_findings (review_result_id);
CREATE INDEX idx_review_findings_severity ON review_findings (review_result_id, severity);
CREATE INDEX idx_review_findings_category ON review_findings (review_result_id, category);

CREATE TABLE reviewed_artifacts (
    id UUID PRIMARY KEY,
    review_result_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    artifact_id UUID NOT NULL,
    path VARCHAR(1000) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    language VARCHAR(50) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reviewed_artifacts_result FOREIGN KEY (review_result_id) REFERENCES review_results (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviewed_artifacts_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_reviewed_artifacts_artifact FOREIGN KEY (artifact_id) REFERENCES generated_artifacts (id),
    CONSTRAINT uq_reviewed_artifacts_result_artifact UNIQUE (review_result_id, artifact_id)
);

CREATE INDEX idx_reviewed_artifacts_result ON reviewed_artifacts (review_result_id);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331053', 'REVIEW_RUN', 'Run review agent', 'Invoke the review agent to evaluate generated artifacts', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331054', 'REVIEW_READ', 'Read review results', 'View review scores and findings for orchestration tasks', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331053'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331054'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331053'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331054'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331053'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331054'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331053'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331054');
