-- Multi-agent orchestration runs (Sprint 2 Phase 1).

CREATE TABLE agent_orchestration_runs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    initiated_by_agent_id UUID,
    root_execution_id UUID,
    name VARCHAR(255) NOT NULL,
    objective VARCHAR(4000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    execution_mode VARCHAR(30) NOT NULL,
    failure_policy VARCHAR(30) NOT NULL,
    max_parallel_tasks INTEGER NOT NULL DEFAULT 1,
    maximum_duration_ms BIGINT NOT NULL,
    event_sequence BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    deadline_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason VARCHAR(500),
    failure_code VARCHAR(100),
    failure_message VARCHAR(2000),
    input_json TEXT,
    output_json TEXT,
    metadata_json TEXT,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aor_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_aor_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_aor_initiated_by_agent FOREIGN KEY (initiated_by_agent_id) REFERENCES agents (id),
    CONSTRAINT fk_aor_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_aor_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT chk_aor_status CHECK (status IN (
        'DRAFT', 'READY', 'RUNNING', 'WAITING', 'SUCCEEDED', 'PARTIALLY_SUCCEEDED',
        'FAILED', 'CANCEL_REQUESTED', 'CANCELLED', 'TIMED_OUT', 'ARCHIVED'
    )),
    CONSTRAINT chk_aor_execution_mode CHECK (execution_mode IN ('SEQUENTIAL', 'DEPENDENCY_GRAPH')),
    CONSTRAINT chk_aor_failure_policy CHECK (failure_policy IN (
        'FAIL_FAST', 'CONTINUE_INDEPENDENT', 'BEST_EFFORT'
    )),
    CONSTRAINT chk_aor_max_parallel CHECK (max_parallel_tasks BETWEEN 1 AND 100),
    CONSTRAINT chk_aor_max_duration CHECK (maximum_duration_ms BETWEEN 1000 AND 86400000),
    CONSTRAINT chk_aor_event_sequence CHECK (event_sequence >= 0)
);

CREATE INDEX idx_aor_organization_id ON agent_orchestration_runs (organization_id);
CREATE INDEX idx_aor_project_id ON agent_orchestration_runs (project_id);
CREATE INDEX idx_aor_org_project ON agent_orchestration_runs (organization_id, project_id);
CREATE INDEX idx_aor_status ON agent_orchestration_runs (status);
CREATE INDEX idx_aor_org_status ON agent_orchestration_runs (organization_id, status);
CREATE INDEX idx_aor_created_at ON agent_orchestration_runs (created_at);
CREATE INDEX idx_aor_deadline_at ON agent_orchestration_runs (deadline_at);
