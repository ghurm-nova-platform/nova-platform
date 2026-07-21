-- Multi-Agent Collaboration Framework (Sprint 6 Phase 1)

ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_source;
ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_source CHECK (source IN (
    'PORTAL', 'REST_API', 'SYSTEM', 'SCHEDULER', 'MERGE_AGENT', 'RELEASE_MANAGER',
    'DEPLOYMENT_OBSERVATION', 'ROLLBACK_MANAGER', 'RELEASE_POLICIES', 'ENVIRONMENT_MANAGEMENT',
    'ORCHESTRATION', 'PLANNER', 'CODING', 'REVIEW', 'TESTING', 'PATCH', 'GIT_INTEGRATION',
    'PULL_REQUEST', 'CI_OBSERVATION', 'REPAIR', 'APPROVAL_GATE', 'DEPLOYMENT_EXECUTION',
    'COLLABORATION'
));

CREATE TABLE collaboration_sessions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    orchestration_run_id UUID,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    shared_context_json TEXT NOT NULL,
    parallel_group VARCHAR(80),
    conflict_detected BOOLEAN NOT NULL DEFAULT FALSE,
    conflict_details_json TEXT,
    created_by UUID NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_collab_sessions_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_collab_sessions_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT chk_collab_sessions_status CHECK (status IN (
        'CREATED', 'STARTING', 'ACTIVE', 'WAITING', 'BLOCKED', 'COMPLETED', 'FAILED', 'CANCELLED'
    ))
);

CREATE INDEX idx_collab_sessions_org ON collaboration_sessions (organization_id, created_at DESC);
CREATE INDEX idx_collab_sessions_project ON collaboration_sessions (organization_id, project_id);
CREATE INDEX idx_collab_sessions_run ON collaboration_sessions (orchestration_run_id);

CREATE TABLE collaboration_participants (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    participant_role VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_task_id UUID,
    progress_percent INT NOT NULL DEFAULT 0,
    parallel_group VARCHAR(80),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_collab_participants_session FOREIGN KEY (session_id) REFERENCES collaboration_sessions (id),
    CONSTRAINT chk_collab_participants_status CHECK (status IN (
        'IDLE', 'ACTIVE', 'WAITING', 'BLOCKED', 'COMPLETED', 'FAILED'
    ))
);

CREATE INDEX idx_collab_participants_session ON collaboration_participants (session_id);

CREATE TABLE collaboration_tasks (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    participant_id UUID,
    task_key VARCHAR(120) NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    depends_on_task_id UUID,
    blocked_by_task_id UUID,
    completed_by_participant_id UUID,
    artifact_ref VARCHAR(500),
    parallel_group VARCHAR(80),
    assigned_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_collab_tasks_session FOREIGN KEY (session_id) REFERENCES collaboration_sessions (id),
    CONSTRAINT chk_collab_tasks_status CHECK (status IN (
        'PENDING', 'ASSIGNED', 'IN_PROGRESS', 'BLOCKED', 'COMPLETED', 'REJECTED', 'CANCELLED'
    ))
);

CREATE INDEX idx_collab_tasks_session ON collaboration_tasks (session_id);

CREATE TABLE collaboration_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    sender_role VARCHAR(40) NOT NULL,
    message_type VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    task_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_collab_messages_session FOREIGN KEY (session_id) REFERENCES collaboration_sessions (id),
    CONSTRAINT chk_collab_messages_type CHECK (message_type IN (
        'TASK', 'QUESTION', 'ANSWER', 'WARNING', 'ERROR', 'INFO', 'APPROVAL_REQUEST', 'DECISION'
    ))
);

CREATE INDEX idx_collab_messages_session ON collaboration_messages (session_id, created_at DESC);

CREATE TABLE collaboration_decisions (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    decision_type VARCHAR(40) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    details_json TEXT,
    decided_by UUID,
    task_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_collab_decisions_session FOREIGN KEY (session_id) REFERENCES collaboration_sessions (id),
    CONSTRAINT chk_collab_decisions_type CHECK (decision_type IN (
        'APPROVE', 'REJECT', 'RESOLVE_CONFLICT', 'PAUSE', 'RESUME', 'CANCEL',
        'REQUEST_REVIEW', 'REQUEST_APPROVAL', 'REQUEST_CLARIFICATION'
    ))
);

CREATE INDEX idx_collab_decisions_session ON collaboration_decisions (session_id, created_at DESC);

CREATE TABLE collaboration_timeline_events (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    actor_role VARCHAR(40),
    task_id UUID,
    message_id UUID,
    decision_id UUID,
    details_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_collab_timeline_session FOREIGN KEY (session_id) REFERENCES collaboration_sessions (id)
);

CREATE INDEX idx_collab_timeline_session ON collaboration_timeline_events (session_id, created_at ASC);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331090', 'COLLABORATION_READ', 'Read collaboration sessions', 'View collaboration sessions, timeline, messages, and participants', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331091', 'COLLABORATION_WRITE', 'Write collaboration sessions', 'Create sessions, send messages, assign tasks, and record decisions', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331092', 'COLLABORATION_ADMIN', 'Administer collaboration sessions', 'Pause, resume, cancel collaboration sessions and resolve conflicts', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331090'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331091'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331092'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331090'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331091'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331090'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331091'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331090');
