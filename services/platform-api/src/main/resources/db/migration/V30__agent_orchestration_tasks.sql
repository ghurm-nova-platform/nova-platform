-- Orchestration tasks.

CREATE TABLE agent_orchestration_tasks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    run_id UUID NOT NULL,
    task_key VARCHAR(150) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    task_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    assigned_agent_id UUID,
    model_reference VARCHAR(150),
    required_capabilities_json TEXT,
    input_json TEXT,
    output_json TEXT,
    error_code VARCHAR(100),
    error_message VARCHAR(2000),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 1,
    retry_backoff_ms BIGINT NOT NULL DEFAULT 1000,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    priority INTEGER NOT NULL DEFAULT 100,
    timeout_seconds INTEGER NOT NULL DEFAULT 60,
    sequence_order INTEGER,
    idempotency_key VARCHAR(150) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claimed_by VARCHAR(100),
    claim_expires_at TIMESTAMP WITH TIME ZONE,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aot_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_aot_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_aot_run FOREIGN KEY (run_id) REFERENCES agent_orchestration_runs (id),
    CONSTRAINT fk_aot_assigned_agent FOREIGN KEY (assigned_agent_id) REFERENCES agents (id),
    CONSTRAINT fk_aot_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_aot_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_aot_run_task_key UNIQUE (run_id, task_key),
    CONSTRAINT uq_aot_run_idempotency UNIQUE (run_id, idempotency_key),
    CONSTRAINT chk_aot_task_type CHECK (task_type IN (
        'AGENT_TURN', 'HUMAN_APPROVAL', 'TRANSFORM', 'AGGREGATION'
    )),
    CONSTRAINT chk_aot_status CHECK (status IN (
        'DRAFT', 'BLOCKED', 'READY', 'CLAIMED', 'RUNNING', 'RETRY_WAIT',
        'WAITING_APPROVAL', 'SUCCEEDED', 'FAILED', 'SKIPPED',
        'CANCEL_REQUESTED', 'CANCELLED', 'TIMED_OUT'
    )),
    CONSTRAINT chk_aot_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT chk_aot_max_attempts CHECK (max_attempts BETWEEN 1 AND 20),
    CONSTRAINT chk_aot_retry_backoff CHECK (retry_backoff_ms BETWEEN 0 AND 3600000),
    CONSTRAINT chk_aot_priority CHECK (priority BETWEEN 1 AND 1000),
    CONSTRAINT chk_aot_timeout CHECK (timeout_seconds BETWEEN 1 AND 3600),
    CONSTRAINT chk_aot_task_key CHECK (task_key ~ '^[a-z0-9][a-z0-9._:-]{0,149}$')
);

CREATE INDEX idx_aot_organization_id ON agent_orchestration_tasks (organization_id);
CREATE INDEX idx_aot_project_id ON agent_orchestration_tasks (project_id);
CREATE INDEX idx_aot_run_id ON agent_orchestration_tasks (run_id);
CREATE INDEX idx_aot_status ON agent_orchestration_tasks (status);
CREATE INDEX idx_aot_run_status ON agent_orchestration_tasks (run_id, status);
CREATE INDEX idx_aot_ready_claim ON agent_orchestration_tasks (status, next_attempt_at, priority);
CREATE INDEX idx_aot_claim_expires ON agent_orchestration_tasks (status, claim_expires_at);
CREATE INDEX idx_aot_assigned_agent ON agent_orchestration_tasks (assigned_agent_id);
