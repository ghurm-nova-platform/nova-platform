-- Automated PR Review Engine (Sprint 6 Phase 3)



ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_source;

ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_source CHECK (source IN (

    'PORTAL', 'REST_API', 'SYSTEM', 'SCHEDULER', 'MERGE_AGENT', 'RELEASE_MANAGER',

    'DEPLOYMENT_OBSERVATION', 'ROLLBACK_MANAGER', 'RELEASE_POLICIES', 'ENVIRONMENT_MANAGEMENT',

    'ORCHESTRATION', 'PLANNER', 'CODING', 'REVIEW', 'TESTING', 'PATCH', 'GIT_INTEGRATION',

    'PULL_REQUEST', 'CI_OBSERVATION', 'REPAIR', 'APPROVAL_GATE', 'DEPLOYMENT_EXECUTION',

    'COLLABORATION', 'KNOWLEDGE', 'PR_REVIEW'

));



ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_entity_type;

ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_entity_type CHECK (entity_type IN (

    'RELEASE', 'DEPLOYMENT', 'ROLLBACK', 'ENVIRONMENT', 'POLICY', 'MERGE', 'APPROVAL',

    'TASK', 'REPOSITORY', 'USER', 'CONFIGURATION', 'KNOWLEDGE', 'PR_REVIEW'

));



ALTER TABLE audit_entities DROP CONSTRAINT IF EXISTS chk_audit_entities_type;

ALTER TABLE audit_entities ADD CONSTRAINT chk_audit_entities_type CHECK (entity_type IN (

    'RELEASE', 'DEPLOYMENT', 'ROLLBACK', 'ENVIRONMENT', 'POLICY', 'MERGE', 'APPROVAL',

    'TASK', 'REPOSITORY', 'USER', 'CONFIGURATION', 'KNOWLEDGE', 'PR_REVIEW'

));



CREATE TABLE pr_review_runs (

    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,

    project_id UUID NOT NULL,

    pull_request_operation_id UUID,

    pull_request_number INT,

    pull_request_title VARCHAR(500),

    repository_ref VARCHAR(500),

    source_branch VARCHAR(255),

    target_branch VARCHAR(255),

    commit_sha VARCHAR(100),

    changed_files_json TEXT,

    status VARCHAR(30) NOT NULL,

    result VARCHAR(40),

    overall_score INT NOT NULL DEFAULT 0,

    risk_score INT NOT NULL DEFAULT 0,

    architecture_score INT NOT NULL DEFAULT 0,

    security_score INT NOT NULL DEFAULT 0,

    performance_score INT NOT NULL DEFAULT 0,

    quality_score INT NOT NULL DEFAULT 0,

    testing_score INT NOT NULL DEFAULT 0,

    documentation_score INT NOT NULL DEFAULT 0,

    summary TEXT,

    diff_excerpt TEXT,

    created_by UUID NOT NULL,

    started_at TIMESTAMP WITH TIME ZONE,

    completed_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_pr_review_runs_org FOREIGN KEY (organization_id) REFERENCES organizations (id),

    CONSTRAINT fk_pr_review_runs_project FOREIGN KEY (project_id) REFERENCES projects (id),

    CONSTRAINT fk_pr_review_runs_pr_op FOREIGN KEY (pull_request_operation_id) REFERENCES pull_request_operations (id),

    CONSTRAINT fk_pr_review_runs_created_by FOREIGN KEY (created_by) REFERENCES users (id),

    CONSTRAINT chk_pr_review_runs_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),

    CONSTRAINT chk_pr_review_runs_result CHECK (result IS NULL OR result IN (

        'APPROVED', 'APPROVED_WITH_SUGGESTIONS', 'REQUEST_CHANGES', 'REJECTED'

    )),

    CONSTRAINT chk_pr_review_runs_overall_score CHECK (overall_score >= 0 AND overall_score <= 100),

    CONSTRAINT chk_pr_review_runs_risk_score CHECK (risk_score >= 0 AND risk_score <= 100),

    CONSTRAINT chk_pr_review_runs_architecture_score CHECK (architecture_score >= 0 AND architecture_score <= 100),

    CONSTRAINT chk_pr_review_runs_security_score CHECK (security_score >= 0 AND security_score <= 100),

    CONSTRAINT chk_pr_review_runs_performance_score CHECK (performance_score >= 0 AND performance_score <= 100),

    CONSTRAINT chk_pr_review_runs_quality_score CHECK (quality_score >= 0 AND quality_score <= 100),

    CONSTRAINT chk_pr_review_runs_testing_score CHECK (testing_score >= 0 AND testing_score <= 100),

    CONSTRAINT chk_pr_review_runs_documentation_score CHECK (documentation_score >= 0 AND documentation_score <= 100)

);



CREATE INDEX idx_pr_review_runs_org ON pr_review_runs (organization_id, created_at DESC);

CREATE INDEX idx_pr_review_runs_project ON pr_review_runs (project_id);

CREATE INDEX idx_pr_review_runs_pr_op ON pr_review_runs (pull_request_operation_id);



CREATE TABLE pr_review_findings (

    id UUID PRIMARY KEY,

    review_run_id UUID NOT NULL,

    organization_id UUID NOT NULL,

    category VARCHAR(40) NOT NULL,

    severity VARCHAR(20) NOT NULL,

    title VARCHAR(500) NOT NULL,

    description TEXT NOT NULL,

    recommendation TEXT NOT NULL,

    file_path VARCHAR(1000),

    line_hint INT,

    rule_code VARCHAR(100),

    evidence_excerpt TEXT,

    references_json TEXT,

    knowledge_document_ids_json TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_pr_review_findings_run FOREIGN KEY (review_run_id) REFERENCES pr_review_runs (id) ON DELETE CASCADE,

    CONSTRAINT chk_pr_review_findings_category CHECK (category IN (

        'Architecture', 'Security', 'Performance', 'CodeQuality', 'Testing', 'Maintainability',

        'Documentation', 'Database', 'ApiDesign', 'Frontend', 'Backend', 'Infrastructure'

    )),

    CONSTRAINT chk_pr_review_findings_severity CHECK (severity IN (

        'INFO', 'SUGGESTION', 'WARNING', 'ERROR', 'BLOCKER'

    ))

);



CREATE INDEX idx_pr_review_findings_run ON pr_review_findings (review_run_id);

CREATE INDEX idx_pr_review_findings_org ON pr_review_findings (organization_id, created_at DESC);



CREATE TABLE pr_review_recommendations (

    id UUID PRIMARY KEY,

    review_run_id UUID NOT NULL,

    organization_id UUID NOT NULL,

    finding_id UUID,

    priority VARCHAR(20) NOT NULL,

    title VARCHAR(500) NOT NULL,

    description TEXT NOT NULL,

    knowledge_document_ids_json TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_pr_review_recommendations_run FOREIGN KEY (review_run_id) REFERENCES pr_review_runs (id) ON DELETE CASCADE,

    CONSTRAINT fk_pr_review_recommendations_finding FOREIGN KEY (finding_id) REFERENCES pr_review_findings (id),

    CONSTRAINT chk_pr_review_recommendations_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'))

);



CREATE INDEX idx_pr_review_recommendations_run ON pr_review_recommendations (review_run_id);

CREATE INDEX idx_pr_review_recommendations_org ON pr_review_recommendations (organization_id, created_at DESC);



INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331095', 'PR_REVIEW_READ', 'Read PR reviews', 'View automated pull request review runs, findings, and recommendations', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331096', 'PR_REVIEW_RUN', 'Run PR reviews', 'Execute automated pull request review analysis', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331097', 'PR_REVIEW_ADMIN', 'Administer PR reviews', 'Administer automated pull request review configuration and runs', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331095'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331096'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331097'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331095'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331096'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331097'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331095'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331096'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331095');


