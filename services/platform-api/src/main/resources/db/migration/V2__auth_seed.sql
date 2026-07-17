-- Local/demo seed data for authentication foundation.
-- Password for admin@nova.local is ChangeMe123! (BCrypt). Rotate before any shared environment.

INSERT INTO organizations (id, name, slug, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Nova Demo Organization',
    'nova-demo',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO roles (id, code, name, description, created_at)
VALUES
    ('22222222-2222-2222-2222-222222222201', 'ORG_ADMIN', 'Organization Admin', 'Full organization administration', CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222202', 'ORG_MEMBER', 'Organization Member', 'Standard organization member', CURRENT_TIMESTAMP);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333333301', 'org:read', 'Read organization', 'View organization profile', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333302', 'org:write', 'Write organization', 'Modify organization settings', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333303', 'users:read', 'Read users', 'View users in the organization', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333304', 'users:write', 'Write users', 'Manage users in the organization', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333333301'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333333302'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333333303'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333333304'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333333301'),
    ('22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333333303');

INSERT INTO users (id, organization_id, email, password_hash, display_name, enabled, created_at, updated_at)
VALUES (
    '44444444-4444-4444-4444-444444444401',
    '11111111-1111-1111-1111-111111111111',
    'admin@nova.local',
    '$2b$10$p/Tvvc0dGHC0eyXBTB1PYej88R/nmjFBll33CG1ySCdnwUKchA142',
    'Nova Admin',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO user_roles (user_id, role_id)
VALUES (
    '44444444-4444-4444-4444-444444444401',
    '22222222-2222-2222-2222-222222222201'
);
