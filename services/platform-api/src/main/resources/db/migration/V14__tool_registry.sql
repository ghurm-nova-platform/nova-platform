-- Tool registry: definitions, agent assignments, execution tool calls, audit.

CREATE TABLE tools (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NULL,
    tool_key VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    tool_type VARCHAR(30) NOT NULL,
    executor_key VARCHAR(100) NOT NULL,
    input_schema TEXT NOT NULL,
    output_schema TEXT NULL,
    status VARCHAR(30) NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    max_execution_seconds INTEGER NOT NULL DEFAULT 10,
    max_output_characters INTEGER NOT NULL DEFAULT 20000,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_tools_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_tools_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_tools_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_tools_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT chk_tools_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_tools_type CHECK (tool_type IN ('BUILT_IN', 'HTTP', 'WEBHOOK', 'INTEGRATION')),
    CONSTRAINT chk_tools_max_execution_seconds CHECK (max_execution_seconds BETWEEN 1 AND 30),
    CONSTRAINT chk_tools_max_output_characters CHECK (max_output_characters BETWEEN 1 AND 50000),
    CONSTRAINT uq_tools_org_project_key UNIQUE (organization_id, project_id, tool_key)
);

CREATE INDEX idx_tools_organization_id ON tools (organization_id);
CREATE INDEX idx_tools_project_id ON tools (project_id);
CREATE INDEX idx_tools_status ON tools (status);
CREATE INDEX idx_tools_tool_type ON tools (tool_type);
CREATE INDEX idx_tools_executor_key ON tools (executor_key);
CREATE INDEX idx_tools_org_project_status ON tools (organization_id, project_id, status);

CREATE TABLE agent_tool_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    agent_id UUID NOT NULL,
    tool_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_agent_tool_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_agent_tool_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_agent_tool_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_agent_tool_tool FOREIGN KEY (tool_id) REFERENCES tools (id),
    CONSTRAINT fk_agent_tool_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_agent_tool_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_agent_tool_assignment UNIQUE (agent_id, tool_id)
);

CREATE INDEX idx_agent_tool_agent_id ON agent_tool_assignments (agent_id);
CREATE INDEX idx_agent_tool_tool_id ON agent_tool_assignments (tool_id);
CREATE INDEX idx_agent_tool_project_id ON agent_tool_assignments (project_id);
CREATE INDEX idx_agent_tool_organization_id ON agent_tool_assignments (organization_id);
CREATE INDEX idx_agent_tool_enabled ON agent_tool_assignments (enabled);

CREATE TABLE execution_tool_calls (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    agent_id UUID NOT NULL,
    execution_id UUID NOT NULL,
    conversation_id UUID NULL,
    tool_id UUID NOT NULL,
    tool_key VARCHAR(100) NOT NULL,
    runtime_call_id VARCHAR(100) NOT NULL,
    sequence_number INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    input_payload TEXT NOT NULL,
    output_payload TEXT NULL,
    error_code VARCHAR(100) NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NULL,
    completed_at TIMESTAMP WITH TIME ZONE NULL,
    duration_ms BIGINT NULL,
    approved_by UUID NULL,
    approved_at TIMESTAMP WITH TIME ZONE NULL,
    created_by UUID NOT NULL,
    CONSTRAINT fk_exec_tool_call_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_exec_tool_call_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_exec_tool_call_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_exec_tool_call_execution FOREIGN KEY (execution_id) REFERENCES agent_executions (id),
    CONSTRAINT fk_exec_tool_call_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_exec_tool_call_tool FOREIGN KEY (tool_id) REFERENCES tools (id),
    CONSTRAINT fk_exec_tool_call_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT fk_exec_tool_call_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_exec_tool_call_runtime UNIQUE (execution_id, runtime_call_id),
    CONSTRAINT uq_exec_tool_call_sequence UNIQUE (execution_id, sequence_number),
    CONSTRAINT chk_exec_tool_call_sequence CHECK (sequence_number > 0),
    CONSTRAINT chk_exec_tool_call_duration CHECK (duration_ms IS NULL OR duration_ms >= 0),
    CONSTRAINT chk_exec_tool_call_status CHECK (status IN (
        'REQUESTED', 'APPROVAL_REQUIRED', 'APPROVED', 'RUNNING',
        'COMPLETED', 'FAILED', 'REJECTED', 'CANCELLED'
    ))
);

CREATE INDEX idx_exec_tool_call_execution_id ON execution_tool_calls (execution_id);
CREATE INDEX idx_exec_tool_call_conversation_id ON execution_tool_calls (conversation_id);
CREATE INDEX idx_exec_tool_call_tool_id ON execution_tool_calls (tool_id);
CREATE INDEX idx_exec_tool_call_status ON execution_tool_calls (status);
CREATE INDEX idx_exec_tool_call_requested_at ON execution_tool_calls (requested_at);
CREATE INDEX idx_exec_tool_call_organization_id ON execution_tool_calls (organization_id);
CREATE INDEX idx_exec_tool_call_project_id ON execution_tool_calls (project_id);
CREATE INDEX idx_exec_tool_call_exec_seq ON execution_tool_calls (execution_id, sequence_number);

CREATE TABLE tool_audit_log (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NULL,
    tool_id UUID NULL,
    agent_id UUID NULL,
    execution_id UUID NULL,
    tool_call_id UUID NULL,
    action VARCHAR(50) NOT NULL,
    metadata TEXT NULL,
    performed_by UUID NOT NULL,
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    correlation_id VARCHAR(100) NULL,
    CONSTRAINT fk_tool_audit_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_tool_audit_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_tool_audit_tool FOREIGN KEY (tool_id) REFERENCES tools (id),
    CONSTRAINT fk_tool_audit_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_tool_audit_execution FOREIGN KEY (execution_id) REFERENCES agent_executions (id),
    CONSTRAINT fk_tool_audit_tool_call FOREIGN KEY (tool_call_id) REFERENCES execution_tool_calls (id),
    CONSTRAINT fk_tool_audit_performed_by FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT chk_tool_audit_action CHECK (action IN (
        'TOOL_CREATED', 'TOOL_UPDATED', 'TOOL_ACTIVATED', 'TOOL_ARCHIVED',
        'TOOL_ASSIGNED', 'TOOL_UNASSIGNED',
        'TOOL_CALL_REQUESTED', 'TOOL_CALL_APPROVED', 'TOOL_CALL_REJECTED',
        'TOOL_CALL_STARTED', 'TOOL_CALL_COMPLETED', 'TOOL_CALL_FAILED',
        'TOOL_CALL_CANCELLED', 'TOOL_OUTPUT_TRUNCATED'
    ))
);

CREATE INDEX idx_tool_audit_organization_id ON tool_audit_log (organization_id);
CREATE INDEX idx_tool_audit_project_id ON tool_audit_log (project_id);
CREATE INDEX idx_tool_audit_tool_id ON tool_audit_log (tool_id);
CREATE INDEX idx_tool_audit_execution_id ON tool_audit_log (execution_id);
CREATE INDEX idx_tool_audit_tool_call_id ON tool_audit_log (tool_call_id);
CREATE INDEX idx_tool_audit_performed_at ON tool_audit_log (performed_at);

-- Seed built-in ACTIVE tools for the demo organization/project.
INSERT INTO tools (
    id, organization_id, project_id, tool_key, name, description, tool_type, executor_key,
    input_schema, output_schema, status, requires_approval, max_execution_seconds, max_output_characters,
    created_by, updated_by, created_at, updated_at, version
) VALUES
(
    '77777777-7777-7777-7777-777777777701',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    'CURRENT_DATETIME',
    'Current date and time',
    'Returns the current date and time for an allowlisted IANA timezone',
    'BUILT_IN',
    'CURRENT_DATETIME',
    '{"type":"object","properties":{"timezone":{"type":"string","maxLength":100}},"required":["timezone"],"additionalProperties":false}',
    '{"type":"object","properties":{"timezone":{"type":"string"},"isoDateTime":{"type":"string"}},"required":["timezone","isoDateTime"],"additionalProperties":false}',
    'ACTIVE',
    FALSE,
    5,
    5000,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    '77777777-7777-7777-7777-777777777702',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    'CALCULATOR',
    'Calculator',
    'Performs ADD, SUBTRACT, MULTIPLY, or DIVIDE on two numbers',
    'BUILT_IN',
    'CALCULATOR',
    '{"type":"object","properties":{"operation":{"type":"string","enum":["ADD","SUBTRACT","MULTIPLY","DIVIDE"]},"left":{"type":"number"},"right":{"type":"number"}},"required":["operation","left","right"],"additionalProperties":false}',
    '{"type":"object","properties":{"operation":{"type":"string"},"left":{"type":"number"},"right":{"type":"number"},"result":{"type":"number"}},"required":["operation","left","right","result"],"additionalProperties":false}',
    'ACTIVE',
    FALSE,
    5,
    5000,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    '77777777-7777-7777-7777-777777777703',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    'TEXT_STATISTICS',
    'Text statistics',
    'Computes deterministic character, word, and line statistics for text',
    'BUILT_IN',
    'TEXT_STATISTICS',
    '{"type":"object","properties":{"text":{"type":"string","maxLength":10000}},"required":["text"],"additionalProperties":false}',
    '{"type":"object","properties":{"characters":{"type":"integer"},"charactersWithoutSpaces":{"type":"integer"},"words":{"type":"integer"},"lines":{"type":"integer"}},"required":["characters","charactersWithoutSpaces","words","lines"],"additionalProperties":false}',
    'ACTIVE',
    FALSE,
    5,
    5000,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

-- Assign built-in tools to the demo agent.
INSERT INTO agent_tool_assignments (
    id, organization_id, project_id, agent_id, tool_id, enabled,
    created_by, created_at, updated_by, updated_at, version
) VALUES
(
    '77777777-7777-7777-7777-777777777711',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    '66666666-6666-6666-6666-666666666601',
    '77777777-7777-7777-7777-777777777701',
    TRUE,
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    0
),
(
    '77777777-7777-7777-7777-777777777712',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    '66666666-6666-6666-6666-666666666601',
    '77777777-7777-7777-7777-777777777702',
    TRUE,
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    0
),
(
    '77777777-7777-7777-7777-777777777713',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555501',
    '66666666-6666-6666-6666-666666666601',
    '77777777-7777-7777-7777-777777777703',
    TRUE,
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    '44444444-4444-4444-4444-444444444401',
    CURRENT_TIMESTAMP,
    0
);
