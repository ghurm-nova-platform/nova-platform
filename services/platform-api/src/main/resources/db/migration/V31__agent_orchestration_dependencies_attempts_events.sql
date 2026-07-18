-- Task dependencies, attempts, and orchestration events.

CREATE TABLE agent_task_dependencies (
    run_id UUID NOT NULL,
    predecessor_task_id UUID NOT NULL,
    successor_task_id UUID NOT NULL,
    dependency_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_atd PRIMARY KEY (predecessor_task_id, successor_task_id),
    CONSTRAINT fk_atd_run FOREIGN KEY (run_id) REFERENCES agent_orchestration_runs (id),
    CONSTRAINT fk_atd_predecessor FOREIGN KEY (predecessor_task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT fk_atd_successor FOREIGN KEY (successor_task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT chk_atd_type CHECK (dependency_type IN ('SUCCESS', 'COMPLETION')),
    CONSTRAINT chk_atd_no_self CHECK (predecessor_task_id <> successor_task_id)
);

CREATE INDEX idx_atd_run_id ON agent_task_dependencies (run_id);
CREATE INDEX idx_atd_successor ON agent_task_dependencies (successor_task_id);
CREATE INDEX idx_atd_predecessor ON agent_task_dependencies (predecessor_task_id);

CREATE TABLE agent_task_attempts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    run_id UUID NOT NULL,
    task_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    execution_id UUID,
    invocation_id UUID,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    input_snapshot_json TEXT,
    output_snapshot_json TEXT,
    error_code VARCHAR(100),
    error_message VARCHAR(2000),
    retryable BOOLEAN NOT NULL DEFAULT FALSE,
    worker_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ata_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ata_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_ata_run FOREIGN KEY (run_id) REFERENCES agent_orchestration_runs (id),
    CONSTRAINT fk_ata_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT uq_ata_task_attempt UNIQUE (task_id, attempt_number),
    CONSTRAINT chk_ata_status CHECK (status IN (
        'STARTED', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'TIMED_OUT', 'STALE'
    )),
    CONSTRAINT chk_ata_attempt_number CHECK (attempt_number >= 1)
);

CREATE INDEX idx_ata_task_id ON agent_task_attempts (task_id);
CREATE INDEX idx_ata_run_id ON agent_task_attempts (run_id);
CREATE INDEX idx_ata_execution_id ON agent_task_attempts (execution_id);
CREATE INDEX idx_ata_org_project ON agent_task_attempts (organization_id, project_id);

CREATE TABLE agent_orchestration_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    run_id UUID NOT NULL,
    task_id UUID,
    event_type VARCHAR(50) NOT NULL,
    event_sequence BIGINT NOT NULL,
    payload_json TEXT,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_aoe_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_aoe_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_aoe_run FOREIGN KEY (run_id) REFERENCES agent_orchestration_runs (id),
    CONSTRAINT fk_aoe_task FOREIGN KEY (task_id) REFERENCES agent_orchestration_tasks (id),
    CONSTRAINT fk_aoe_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_aoe_run_sequence UNIQUE (run_id, event_sequence),
    CONSTRAINT chk_aoe_sequence CHECK (event_sequence >= 1)
);

CREATE INDEX idx_aoe_run_id ON agent_orchestration_events (run_id);
CREATE INDEX idx_aoe_run_sequence ON agent_orchestration_events (run_id, event_sequence);
CREATE INDEX idx_aoe_task_id ON agent_orchestration_events (task_id);
CREATE INDEX idx_aoe_org_project ON agent_orchestration_events (organization_id, project_id);
CREATE INDEX idx_aoe_event_type ON agent_orchestration_events (event_type);
