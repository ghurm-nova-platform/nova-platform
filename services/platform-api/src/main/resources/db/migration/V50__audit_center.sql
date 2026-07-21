-- Enterprise Audit Center (Sprint 4 Phase 6). Append-only authoritative audit trail.
-- Separate from domain *_audit_log and *_events tables. Never modifies business data.

CREATE TABLE audit_sessions (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    user_id UUID,
    organization_id UUID NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    CONSTRAINT fk_audit_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_audit_sessions_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_audit_sessions_session_id UNIQUE (session_id)
);

CREATE INDEX idx_audit_sessions_org_started ON audit_sessions (organization_id, started_at DESC);
CREATE INDEX idx_audit_sessions_user ON audit_sessions (user_id, started_at DESC);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID,
    user_id UUID,
    username VARCHAR(200),
    session_id UUID,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID,
    action VARCHAR(40) NOT NULL,
    result VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    source VARCHAR(60) NOT NULL,
    correlation_id VARCHAR(64),
    request_id VARCHAR(64),
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    details_json TEXT,
    event_fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_audit_events_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_audit_events_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_audit_events_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_audit_events_session FOREIGN KEY (session_id) REFERENCES audit_sessions (session_id),
    CONSTRAINT uq_audit_events_fingerprint UNIQUE (organization_id, event_fingerprint),
    CONSTRAINT chk_audit_events_entity_type CHECK (entity_type IN (
        'RELEASE', 'DEPLOYMENT', 'ROLLBACK', 'ENVIRONMENT', 'POLICY', 'MERGE', 'APPROVAL',
        'TASK', 'REPOSITORY', 'USER', 'CONFIGURATION'
    )),
    CONSTRAINT chk_audit_events_action CHECK (action IN (
        'CREATE', 'UPDATE', 'DELETE', 'ENABLE', 'DISABLE', 'ARCHIVE', 'APPROVE', 'REJECT',
        'MERGE', 'VALIDATE', 'OBSERVE', 'LOGIN', 'LOGOUT', 'ACCESS'
    )),
    CONSTRAINT chk_audit_events_result CHECK (result IN ('SUCCESS', 'FAILURE', 'WARNING', 'DENIED')),
    CONSTRAINT chk_audit_events_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_audit_events_source CHECK (source IN (
        'PORTAL', 'REST_API', 'SYSTEM', 'SCHEDULER', 'MERGE_AGENT', 'RELEASE_MANAGER',
        'DEPLOYMENT_OBSERVATION', 'ROLLBACK_MANAGER', 'RELEASE_POLICIES', 'ENVIRONMENT_MANAGEMENT'
    ))
);

CREATE INDEX idx_audit_events_org_created ON audit_events (organization_id, created_at DESC);
CREATE INDEX idx_audit_events_org_entity ON audit_events (organization_id, entity_type, entity_id);
CREATE INDEX idx_audit_events_org_user ON audit_events (organization_id, user_id);
CREATE INDEX idx_audit_events_correlation ON audit_events (correlation_id);
CREATE INDEX idx_audit_events_request ON audit_events (request_id);
CREATE INDEX idx_audit_events_severity ON audit_events (organization_id, severity);
CREATE INDEX idx_audit_events_action ON audit_events (organization_id, action);
CREATE INDEX idx_audit_events_result ON audit_events (organization_id, result);
CREATE INDEX idx_audit_events_project ON audit_events (project_id, created_at DESC);

CREATE TABLE audit_entities (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID NOT NULL,
    display_label VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_audit_entities_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_audit_entities_ref UNIQUE (organization_id, entity_type, entity_id),
    CONSTRAINT chk_audit_entities_type CHECK (entity_type IN (
        'RELEASE', 'DEPLOYMENT', 'ROLLBACK', 'ENVIRONMENT', 'POLICY', 'MERGE', 'APPROVAL',
        'TASK', 'REPOSITORY', 'USER', 'CONFIGURATION'
    ))
);

CREATE INDEX idx_audit_entities_org_type ON audit_entities (organization_id, entity_type);

CREATE TABLE audit_correlation (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    correlation_id VARCHAR(64),
    request_id VARCHAR(64),
    session_id UUID,
    audit_event_id UUID NOT NULL,
    chain_sequence INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_audit_correlation_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_audit_correlation_event FOREIGN KEY (audit_event_id) REFERENCES audit_events (id),
    CONSTRAINT fk_audit_correlation_session FOREIGN KEY (session_id) REFERENCES audit_sessions (session_id)
);

CREATE INDEX idx_audit_correlation_correlation ON audit_correlation (organization_id, correlation_id, created_at DESC);
CREATE INDEX idx_audit_correlation_request ON audit_correlation (organization_id, request_id, created_at DESC);
CREATE INDEX idx_audit_correlation_session ON audit_correlation (organization_id, session_id, created_at DESC);
CREATE INDEX idx_audit_correlation_event ON audit_correlation (audit_event_id);

CREATE TABLE audit_indexes (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    audit_event_id UUID NOT NULL,
    index_key VARCHAR(60) NOT NULL,
    index_value VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_audit_indexes_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_audit_indexes_event FOREIGN KEY (audit_event_id) REFERENCES audit_events (id)
);

CREATE INDEX idx_audit_indexes_lookup ON audit_indexes (organization_id, index_key, index_value);
CREATE INDEX idx_audit_indexes_event ON audit_indexes (audit_event_id);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331085', 'AUDIT_READ', 'Read audit center', 'View enterprise audit events and correlation history', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331086', 'AUDIT_WRITE', 'Write audit events', 'Publish append-only audit events from internal services', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331085'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331086'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331085'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331086'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331085'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331086'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331085'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331086');
