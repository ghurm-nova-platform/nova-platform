-- Enterprise Dashboard (Sprint 5 Phase 2). Read-only aggregation permissions and optional user preferences.

CREATE TABLE dashboard_user_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    preferences_json TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_dashboard_prefs_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_dashboard_prefs_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_dashboard_prefs_user_org UNIQUE (user_id, organization_id)
);

CREATE INDEX idx_dashboard_prefs_org ON dashboard_user_preferences (organization_id);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331088', 'DASHBOARD_READ', 'Read enterprise dashboard', 'View aggregated enterprise dashboard snapshots and exports', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331089', 'DASHBOARD_ADMIN', 'Administer enterprise dashboard', 'Refresh dashboard cache and manage dashboard preferences', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331088'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331089'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331088'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333331089'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331088'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333331089'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331088'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333331089');
