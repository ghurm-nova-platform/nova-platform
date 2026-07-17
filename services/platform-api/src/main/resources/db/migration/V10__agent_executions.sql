-- Agent execution engine tables.

CREATE TABLE agent_executions (
    id                 UUID PRIMARY KEY,
    organization_id    UUID NOT NULL,
    project_id         UUID NOT NULL,
    agent_id           UUID NOT NULL,
    prompt_version_id  UUID NOT NULL,
    conversation_id    UUID,
    provider           VARCHAR(50) NOT NULL,
    model              VARCHAR(100) NOT NULL,
    status             VARCHAR(30) NOT NULL,
    input_tokens       INTEGER,
    output_tokens      INTEGER,
    total_tokens       INTEGER,
    latency_ms         INTEGER,
    started_at         TIMESTAMP WITH TIME ZONE,
    completed_at       TIMESTAMP WITH TIME ZONE,
    created_by         UUID NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message      VARCHAR(4000),
    CONSTRAINT fk_agent_executions_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_agent_executions_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_executions_agent
        FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_agent_executions_prompt_version
        FOREIGN KEY (prompt_version_id) REFERENCES prompt_versions (id),
    CONSTRAINT fk_agent_executions_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_agent_executions_status CHECK (status IN (
        'PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'
    )),
    CONSTRAINT ck_agent_executions_tokens CHECK (
        (input_tokens IS NULL OR input_tokens >= 0)
        AND (output_tokens IS NULL OR output_tokens >= 0)
        AND (total_tokens IS NULL OR total_tokens >= 0)
    ),
    CONSTRAINT ck_agent_executions_latency CHECK (latency_ms IS NULL OR latency_ms >= 0)
);

CREATE INDEX idx_agent_executions_organization_id ON agent_executions (organization_id);
CREATE INDEX idx_agent_executions_project_id ON agent_executions (project_id);
CREATE INDEX idx_agent_executions_agent_id ON agent_executions (agent_id);
CREATE INDEX idx_agent_executions_status ON agent_executions (status);
CREATE INDEX idx_agent_executions_created_at ON agent_executions (created_at);
CREATE INDEX idx_agent_executions_conversation_id ON agent_executions (conversation_id);

CREATE TABLE execution_messages (
    id            UUID PRIMARY KEY,
    execution_id  UUID NOT NULL,
    role          VARCHAR(30) NOT NULL,
    content       TEXT NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_execution_messages_execution
        FOREIGN KEY (execution_id) REFERENCES agent_executions (id) ON DELETE CASCADE,
    CONSTRAINT ck_execution_messages_role CHECK (role IN (
        'SYSTEM', 'USER', 'ASSISTANT', 'TOOL'
    )),
    CONSTRAINT ck_execution_messages_content CHECK (LENGTH(TRIM(content)) > 0)
);

CREATE INDEX idx_execution_messages_execution_id ON execution_messages (execution_id);
CREATE INDEX idx_execution_messages_created_at ON execution_messages (created_at);

CREATE TABLE execution_metrics (
    id            UUID PRIMARY KEY,
    execution_id  UUID NOT NULL,
    metric_name   VARCHAR(100) NOT NULL,
    metric_value  VARCHAR(500) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_execution_metrics_execution
        FOREIGN KEY (execution_id) REFERENCES agent_executions (id) ON DELETE CASCADE,
    CONSTRAINT uq_execution_metrics_name UNIQUE (execution_id, metric_name)
);

CREATE INDEX idx_execution_metrics_execution_id ON execution_metrics (execution_id);
