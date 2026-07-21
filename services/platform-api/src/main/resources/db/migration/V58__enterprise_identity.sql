-- Enterprise Identity & SSO Platform (Sprint 6)

ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_source;
ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_source CHECK (source IN (
    'PORTAL', 'REST_API', 'SYSTEM', 'SCHEDULER', 'MERGE_AGENT', 'RELEASE_MANAGER',
    'DEPLOYMENT_OBSERVATION', 'ROLLBACK_MANAGER', 'RELEASE_POLICIES', 'ENVIRONMENT_MANAGEMENT',
    'ORCHESTRATION', 'PLANNER', 'CODING', 'REVIEW', 'TESTING', 'PATCH', 'GIT_INTEGRATION',
    'PULL_REQUEST', 'CI_OBSERVATION', 'REPAIR', 'APPROVAL_GATE', 'DEPLOYMENT_EXECUTION',
    'COLLABORATION', 'KNOWLEDGE', 'PR_REVIEW', 'IDENTITY'
));

ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS chk_audit_events_entity_type;
ALTER TABLE audit_events ADD CONSTRAINT chk_audit_events_entity_type CHECK (entity_type IN (
    'RELEASE', 'DEPLOYMENT', 'ROLLBACK', 'ENVIRONMENT', 'POLICY', 'MERGE', 'APPROVAL',
    'TASK', 'REPOSITORY', 'USER', 'CONFIGURATION', 'KNOWLEDGE', 'PR_REVIEW', 'IDENTITY'
));

ALTER TABLE audit_entities DROP CONSTRAINT IF EXISTS chk_audit_entities_type;
ALTER TABLE audit_entities ADD CONSTRAINT chk_audit_entities_type CHECK (entity_type IN (
    'RELEASE', 'DEPLOYMENT', 'ROLLBACK', 'ENVIRONMENT', 'POLICY', 'MERGE', 'APPROVAL',
    'TASK', 'REPOSITORY', 'USER', 'CONFIGURATION', 'KNOWLEDGE', 'PR_REVIEW', 'IDENTITY'
));

CREATE TABLE identity_providers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    provider_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    config_json TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_providers_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT chk_identity_providers_type CHECK (provider_type IN (
        'LOCAL', 'LDAP', 'ACTIVE_DIRECTORY', 'AZURE_AD', 'KEYCLOAK', 'OKTA', 'AUTH0',
        'GOOGLE_WORKSPACE', 'PING', 'OIDC', 'SAML', 'OAUTH2'
    )),
    CONSTRAINT chk_identity_providers_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_identity_providers_org_name ON identity_providers (organization_id, name);
CREATE INDEX idx_identity_providers_org ON identity_providers (organization_id, status);

CREATE TABLE identity_users (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    platform_user_id UUID,
    provider_id UUID NOT NULL,
    external_subject VARCHAR(500),
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    force_password_change BOOLEAN NOT NULL DEFAULT FALSE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    locked_until TIMESTAMP WITH TIME ZONE,
    failed_login_count INT NOT NULL DEFAULT 0,
    password_expires_at TIMESTAMP WITH TIME ZONE,
    password_reset_token_hash VARCHAR(128),
    password_reset_token_expires_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_users_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_identity_users_platform_user FOREIGN KEY (platform_user_id) REFERENCES users (id),
    CONSTRAINT fk_identity_users_provider FOREIGN KEY (provider_id) REFERENCES identity_providers (id),
    CONSTRAINT uq_identity_users_org_email UNIQUE (organization_id, email)
);

CREATE INDEX idx_identity_users_platform_user ON identity_users (platform_user_id);
CREATE INDEX idx_identity_users_provider ON identity_users (provider_id);

CREATE TABLE identity_groups (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    external_id VARCHAR(500),
    description VARCHAR(500),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_groups_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_identity_groups_org_name UNIQUE (organization_id, name)
);

CREATE TABLE identity_roles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_roles_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_identity_roles_org_code UNIQUE (organization_id, code)
);

CREATE TABLE identity_permissions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    code VARCHAR(150) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_permissions_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_identity_permissions_org_code UNIQUE (organization_id, code)
);

CREATE TABLE identity_user_roles (
    identity_user_id UUID NOT NULL,
    identity_role_id UUID NOT NULL,
    PRIMARY KEY (identity_user_id, identity_role_id),
    CONSTRAINT fk_identity_user_roles_user FOREIGN KEY (identity_user_id) REFERENCES identity_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_identity_user_roles_role FOREIGN KEY (identity_role_id) REFERENCES identity_roles (id) ON DELETE CASCADE
);

CREATE TABLE identity_group_roles (
    identity_group_id UUID NOT NULL,
    identity_role_id UUID NOT NULL,
    PRIMARY KEY (identity_group_id, identity_role_id),
    CONSTRAINT fk_identity_group_roles_group FOREIGN KEY (identity_group_id) REFERENCES identity_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_identity_group_roles_role FOREIGN KEY (identity_role_id) REFERENCES identity_roles (id) ON DELETE CASCADE
);

CREATE TABLE identity_sessions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    identity_user_id UUID NOT NULL,
    platform_user_id UUID,
    status VARCHAR(20) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_accessed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_identity_sessions_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_identity_sessions_user FOREIGN KEY (identity_user_id) REFERENCES identity_users (id),
    CONSTRAINT fk_identity_sessions_platform_user FOREIGN KEY (platform_user_id) REFERENCES users (id),
    CONSTRAINT chk_identity_sessions_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);

CREATE INDEX idx_identity_sessions_user ON identity_sessions (identity_user_id, status);
CREATE INDEX idx_identity_sessions_org ON identity_sessions (organization_id, created_at DESC);

CREATE TABLE identity_api_tokens (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    identity_user_id UUID,
    service_account_id UUID,
    name VARCHAR(255) NOT NULL,
    token_prefix VARCHAR(16) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    scopes_json TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_api_tokens_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_identity_api_tokens_user FOREIGN KEY (identity_user_id) REFERENCES identity_users (id),
    CONSTRAINT uq_identity_api_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_identity_api_tokens_org ON identity_api_tokens (organization_id);

CREATE TABLE identity_refresh_tokens (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    identity_user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_refresh_tokens_session FOREIGN KEY (session_id) REFERENCES identity_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_identity_refresh_tokens_user FOREIGN KEY (identity_user_id) REFERENCES identity_users (id),
    CONSTRAINT uq_identity_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_identity_refresh_tokens_user ON identity_refresh_tokens (identity_user_id);

CREATE TABLE identity_login_history (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    identity_user_id UUID,
    provider_id UUID,
    result VARCHAR(20) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_login_history_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_identity_login_history_user FOREIGN KEY (identity_user_id) REFERENCES identity_users (id),
    CONSTRAINT fk_identity_login_history_provider FOREIGN KEY (provider_id) REFERENCES identity_providers (id),
    CONSTRAINT chk_identity_login_history_result CHECK (result IN ('SUCCESS', 'FAILURE', 'MFA_REQUIRED'))
);

CREATE INDEX idx_identity_login_history_org ON identity_login_history (organization_id, created_at DESC);

CREATE TABLE identity_service_accounts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    client_secret_hash VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_service_accounts_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uq_identity_service_accounts_client_id UNIQUE (client_id),
    CONSTRAINT uq_identity_service_accounts_org_name UNIQUE (organization_id, name)
);

ALTER TABLE identity_api_tokens ADD CONSTRAINT fk_identity_api_tokens_service_account
    FOREIGN KEY (service_account_id) REFERENCES identity_service_accounts (id);

CREATE TABLE identity_mfa_factors (
    id UUID PRIMARY KEY,
    identity_user_id UUID NOT NULL,
    factor_type VARCHAR(20) NOT NULL,
    secret_encrypted VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    enrolled_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_mfa_factors_user FOREIGN KEY (identity_user_id) REFERENCES identity_users (id) ON DELETE CASCADE,
    CONSTRAINT chk_identity_mfa_factors_type CHECK (factor_type IN ('TOTP'))
);

CREATE INDEX idx_identity_mfa_factors_user ON identity_mfa_factors (identity_user_id, enabled);

CREATE TABLE identity_recovery_codes (
    id UUID PRIMARY KEY,
    identity_user_id UUID NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_recovery_codes_user FOREIGN KEY (identity_user_id) REFERENCES identity_users (id) ON DELETE CASCADE
);

CREATE INDEX idx_identity_recovery_codes_user ON identity_recovery_codes (identity_user_id, used_at);

CREATE TABLE identity_password_history (
    id UUID PRIMARY KEY,
    identity_user_id UUID NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_password_history_user FOREIGN KEY (identity_user_id) REFERENCES identity_users (id) ON DELETE CASCADE
);

CREATE INDEX idx_identity_password_history_user ON identity_password_history (identity_user_id, created_at DESC);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('33333333-3333-3333-3333-333333331098', 'IDENTITY_READ', 'Read identity', 'View identity configuration, sessions, and login history', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331099', 'IDENTITY_ADMIN', 'Administer identity', 'Full identity administration', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331100', 'IDENTITY_PROVIDER_ADMIN', 'Administer identity providers', 'Create and configure identity providers', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331101', 'IDENTITY_MFA_MANAGE', 'Manage MFA', 'Enroll and manage multi-factor authentication', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331102', 'SCIM_PROVISION', 'SCIM provisioning', 'Provision users and groups via SCIM', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331103', 'IDENTITY_SESSION_ADMIN', 'Administer identity sessions', 'View and revoke identity sessions', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331104', 'IDENTITY_USER_ADMIN', 'Administer identity users', 'Manage identity users and credentials', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331105', 'IDENTITY_GROUP_ADMIN', 'Administer identity groups', 'Manage identity groups and membership', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331106', 'IDENTITY_ROLE_ADMIN', 'Administer identity roles', 'Manage identity roles and assignments', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331107', 'IDENTITY_PERMISSION_ADMIN', 'Administer identity permissions', 'Manage identity permission definitions', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333331108', 'IDENTITY_AUDIT_READ', 'Read identity audit', 'View identity security events and login history', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331098'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331099'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331100'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331101'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331102'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331103'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331104'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331105'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331106'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331107'),
    ('22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333331108');

INSERT INTO identity_providers (id, organization_id, name, provider_type, status, config_json, is_default, version, created_at, updated_at)
VALUES (
    '55555555-5555-5555-5555-555555555501',
    '11111111-1111-1111-1111-111111111111',
    'Local Authentication',
    'LOCAL',
    'ENABLED',
    '{}',
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO identity_users (id, organization_id, platform_user_id, provider_id, external_subject, email, display_name, enabled, mfa_enabled, force_password_change, password_changed_at, version, created_at, updated_at)
VALUES (
    '66666666-6666-6666-6666-666666666601',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444401',
    '55555555-5555-5555-5555-555555555501',
    '44444444-4444-4444-4444-444444444401',
    'admin@nova.local',
    'Nova Admin',
    TRUE,
    FALSE,
    FALSE,
    CURRENT_TIMESTAMP,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
