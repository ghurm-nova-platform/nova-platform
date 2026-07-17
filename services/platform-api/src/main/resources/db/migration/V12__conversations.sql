-- Conversation memory: conversations, messages, audit, and execution idempotency.

CREATE TABLE conversations (
    id               UUID PRIMARY KEY,
    organization_id  UUID NOT NULL,
    project_id       UUID NOT NULL,
    agent_id         UUID NOT NULL,
    title            VARCHAR(255),
    status           VARCHAR(30) NOT NULL,
    created_by       UUID NOT NULL,
    updated_by       UUID NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at  TIMESTAMP WITH TIME ZONE,
    message_count    INTEGER NOT NULL DEFAULT 0,
    version          INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_conversations_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_conversations_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_agent
        FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_conversations_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_conversations_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_conversations_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_conversations_message_count CHECK (message_count >= 0)
);

CREATE INDEX idx_conversations_organization_id ON conversations (organization_id);
CREATE INDEX idx_conversations_project_id ON conversations (project_id);
CREATE INDEX idx_conversations_agent_id ON conversations (agent_id);
CREATE INDEX idx_conversations_status ON conversations (status);
CREATE INDEX idx_conversations_last_message_at ON conversations (last_message_at);
CREATE INDEX idx_conversations_created_at ON conversations (created_at);
CREATE INDEX idx_conversations_org_project_agent_status
    ON conversations (organization_id, project_id, agent_id, status);

CREATE TABLE conversation_messages (
    id                UUID PRIMARY KEY,
    conversation_id   UUID NOT NULL,
    organization_id   UUID NOT NULL,
    project_id        UUID NOT NULL,
    agent_id          UUID NOT NULL,
    execution_id      UUID,
    role              VARCHAR(30) NOT NULL,
    content           TEXT NOT NULL,
    sequence_number   INTEGER NOT NULL,
    token_count       INTEGER,
    created_by        UUID NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_request_id UUID,
    CONSTRAINT fk_conversation_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_messages_execution
        FOREIGN KEY (execution_id) REFERENCES agent_executions (id),
    CONSTRAINT fk_conversation_messages_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_conversation_messages_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_conversation_messages_agent
        FOREIGN KEY (agent_id) REFERENCES agents (id),
    CONSTRAINT fk_conversation_messages_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_conversation_messages_sequence UNIQUE (conversation_id, sequence_number),
    CONSTRAINT ck_conversation_messages_role CHECK (role IN ('SYSTEM', 'USER', 'ASSISTANT', 'TOOL')),
    CONSTRAINT ck_conversation_messages_sequence CHECK (sequence_number > 0),
    CONSTRAINT ck_conversation_messages_token_count CHECK (token_count IS NULL OR token_count >= 0),
    CONSTRAINT ck_conversation_messages_content CHECK (LENGTH(TRIM(content)) > 0)
);

CREATE INDEX idx_conversation_messages_conversation_id ON conversation_messages (conversation_id);
CREATE INDEX idx_conversation_messages_execution_id ON conversation_messages (execution_id);
CREATE INDEX idx_conversation_messages_created_at ON conversation_messages (created_at);
CREATE INDEX idx_conversation_messages_organization_id ON conversation_messages (organization_id);
CREATE INDEX idx_conversation_messages_project_id ON conversation_messages (project_id);
CREATE INDEX idx_conversation_messages_conversation_sequence
    ON conversation_messages (conversation_id, sequence_number);

CREATE TABLE conversation_audit_log (
    id               UUID PRIMARY KEY,
    conversation_id  UUID NOT NULL,
    organization_id  UUID NOT NULL,
    project_id       UUID NOT NULL,
    action           VARCHAR(50) NOT NULL,
    metadata         TEXT,
    performed_by     UUID NOT NULL,
    performed_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id   VARCHAR(100),
    CONSTRAINT fk_conversation_audit_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_audit_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_conversation_audit_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_conversation_audit_user
        FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT ck_conversation_audit_action CHECK (action IN (
        'CREATED', 'TITLE_UPDATED', 'ARCHIVED', 'RESTORED', 'MESSAGE_ADDED', 'MEMORY_TRUNCATED'
    ))
);

CREATE INDEX idx_conversation_audit_conversation_id ON conversation_audit_log (conversation_id);
CREATE INDEX idx_conversation_audit_organization_id ON conversation_audit_log (organization_id);
CREATE INDEX idx_conversation_audit_project_id ON conversation_audit_log (project_id);
CREATE INDEX idx_conversation_audit_performed_at ON conversation_audit_log (performed_at);

-- Track execution idempotency keyed by conversation + clientRequestId.
CREATE TABLE conversation_execution_requests (
    id                 UUID PRIMARY KEY,
    conversation_id    UUID NOT NULL,
    organization_id    UUID NOT NULL,
    project_id         UUID NOT NULL,
    agent_id           UUID NOT NULL,
    client_request_id  UUID NOT NULL,
    execution_id       UUID NOT NULL,
    user_message_id    UUID,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conv_exec_req_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_conv_exec_req_execution
        FOREIGN KEY (execution_id) REFERENCES agent_executions (id),
    CONSTRAINT fk_conv_exec_req_user_message
        FOREIGN KEY (user_message_id) REFERENCES conversation_messages (id),
    CONSTRAINT uq_conv_exec_req_client UNIQUE (conversation_id, client_request_id)
);

CREATE INDEX idx_conv_exec_req_conversation_id ON conversation_execution_requests (conversation_id);
CREATE INDEX idx_conv_exec_req_execution_id ON conversation_execution_requests (execution_id);
