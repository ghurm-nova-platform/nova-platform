-- Project management roles and demo project for local development.

INSERT INTO roles (id, code, name, description, created_at)
VALUES
    ('22222222-2222-2222-2222-222222222203', 'PROJECT_ADMIN', 'Project Admin', 'Create, update, and archive projects', CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222204', 'USER', 'User', 'Read-only access to organizations and projects', CURRENT_TIMESTAMP);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333333305', 'org:create', 'Create organization', 'Create organizations', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333306', 'org:delete', 'Delete organization', 'Delete organizations', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333307', 'project:read', 'Read projects', 'View projects', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333308', 'project:write', 'Write projects', 'Create and update projects', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333309', 'project:archive', 'Archive projects', 'Archive or delete projects', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333333305'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333333306'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333333307'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333333307'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333333308'),
    ('22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333333309'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333333301'),
    ('22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333333307'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333333307');

INSERT INTO user_roles (user_id, role_id)
VALUES
    ('44444444-4444-4444-4444-444444444401', '22222222-2222-2222-2222-222222222203');

INSERT INTO projects (
    id, organization_id, name, description, status, visibility,
    created_at, updated_at, created_by, updated_by
)
VALUES (
    '55555555-5555-5555-5555-555555555501',
    '11111111-1111-1111-1111-111111111111',
    'Demo Project',
    'Seeded project for Sprint 1 local demos',
    'ACTIVE',
    'INTERNAL',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    '44444444-4444-4444-4444-444444444401',
    '44444444-4444-4444-4444-444444444401'
);
