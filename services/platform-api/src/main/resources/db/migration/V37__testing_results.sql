-- Testing Agent results (Sprint 2 Phase 5). Generates test plans only — no execution.

CREATE TABLE testing_results (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    run_id UUID NOT NULL,
    task_id UUID NOT NULL,
    summary VARCHAR(4000) NOT NULL,
    coverage_estimate INTEGER NOT NULL,
    tokens_used BIGINT,
    model VARCHAR(150),
    provider VARCHAR(100),
    generation_time_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_testing_results_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_testing_results_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_testing_results_run FOREIGN KEY (run_id) REFERENCES agent_orchestration_runs (id),
    CONSTRAINT fk_testing_results_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT chk_testing_results_coverage CHECK (coverage_estimate >= 0 AND coverage_estimate <= 100)
);

CREATE INDEX idx_testing_results_task ON testing_results (task_id);
CREATE INDEX idx_testing_results_organization ON testing_results (organization_id);
CREATE INDEX idx_testing_results_org_task_created ON testing_results (organization_id, task_id, created_at DESC);

CREATE TABLE generated_tests (
    id UUID PRIMARY KEY,
    testing_result_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    test_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    artifact_id UUID,
    artifact_path VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_generated_tests_result FOREIGN KEY (testing_result_id) REFERENCES testing_results (id) ON DELETE CASCADE,
    CONSTRAINT fk_generated_tests_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_generated_tests_artifact FOREIGN KEY (artifact_id) REFERENCES generated_artifacts (id),
    CONSTRAINT chk_generated_tests_type CHECK (test_type IN (
        'UNIT', 'INTEGRATION', 'API', 'UI', 'DATABASE', 'SECURITY', 'PERFORMANCE', 'EDGE_CASE', 'NEGATIVE'
    )),
    CONSTRAINT chk_generated_tests_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_generated_tests_result ON generated_tests (testing_result_id);
CREATE INDEX idx_generated_tests_type ON generated_tests (testing_result_id, test_type);

CREATE TABLE generated_test_cases (
    id UUID PRIMARY KEY,
    testing_result_id UUID NOT NULL,
    generated_test_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    steps VARCHAR(4000),
    expected_result VARCHAR(4000),
    priority VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_generated_test_cases_result FOREIGN KEY (testing_result_id) REFERENCES testing_results (id) ON DELETE CASCADE,
    CONSTRAINT fk_generated_test_cases_test FOREIGN KEY (generated_test_id) REFERENCES generated_tests (id) ON DELETE CASCADE,
    CONSTRAINT fk_generated_test_cases_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT chk_generated_test_cases_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_generated_test_cases_test ON generated_test_cases (generated_test_id);
CREATE INDEX idx_generated_test_cases_result ON generated_test_cases (testing_result_id);

CREATE TABLE testing_reviewed_artifacts (
    id UUID PRIMARY KEY,
    testing_result_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    artifact_id UUID NOT NULL,
    path VARCHAR(1000) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    language VARCHAR(50) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_testing_reviewed_artifacts_result FOREIGN KEY (testing_result_id) REFERENCES testing_results (id) ON DELETE CASCADE,
    CONSTRAINT fk_testing_reviewed_artifacts_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_testing_reviewed_artifacts_artifact FOREIGN KEY (artifact_id) REFERENCES generated_artifacts (id),
    CONSTRAINT uq_testing_reviewed_artifacts_result_artifact UNIQUE (testing_result_id, artifact_id)
);

CREATE INDEX idx_testing_reviewed_artifacts_result ON testing_reviewed_artifacts (testing_result_id);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331055', 'TESTING_RUN', 'Run testing agent', 'Invoke the testing agent to generate test plans and cases', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331056', 'TESTING_READ', 'Read testing results', 'View generated test plans and cases for orchestration tasks', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331055'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331056'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331055'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331056'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331055'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331056'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331055'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331056');
